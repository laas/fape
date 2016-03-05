package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.planner.APlanner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.statements.LogStatement;

import static fape.core.planning.grounding.GAction.*;

import java.util.*;
import java.util.stream.Collectors;


public class TemporalDTG {
    public final APlanner planner;
    public final GStateVariable sv;

    @AllArgsConstructor @Getter
    public class Node {
        final Fluent f;
        final int minStay;
        final int maxStay;
        @Override public String toString() { return "["+minStay+","+(maxStay<Integer.MAX_VALUE?maxStay:"inf")+"] "+f; }
    }

    @AllArgsConstructor @Getter
    public class DurativeCondition {
        final Fluent f;
        final int minStay;
        @Override public String toString() { return "["+minStay+"] "+f; }
    }

    @AllArgsConstructor @Getter
    public class DurativeEffect {
        final Fluent f;
        final int minDuration;
        @Override public String toString() { return "["+minDuration+"] "+f; }
    }

    @AllArgsConstructor @Getter
    public class Transition {
        final Node from;
        final Node to;
        final int duration;
        final List<DurativeCondition> conditions;
        final List<DurativeEffect> simultaneousEffects;
        final GAction.GLogStatement statement;
        final GAction container;
        @Override public String toString() { return from+" -["+duration+"]-> "+to; }
    }

    public Map<InstanceRef, Node> baseNodes = new HashMap<>();
    public Map<Node, List<Transition>> outTransitions = new HashMap<>();
    public Map<Node, List<Transition>> inTransitions = new HashMap<>();

    public TemporalDTG(GStateVariable sv, Collection<InstanceRef> domain, APlanner pl) {
        this.planner = pl;
        this.sv = sv;
        for(InstanceRef val : domain) {
            Fluent f = pl.preprocessor.getFluent(sv, val);
            baseNodes.put(val, new Node(f, 0, Integer.MAX_VALUE));
        }
    }

    private void recordTransition(Transition tr) {
        if(!outTransitions.containsKey(tr.getFrom()))
            outTransitions.put(tr.getFrom(), new ArrayList<>());
        if(!inTransitions.containsKey(tr.getTo()))
            inTransitions.put(tr.getTo(), new ArrayList<>());
        outTransitions.get(tr.getFrom()).add(tr);
        inTransitions.get(tr.getTo()).add(tr);

        // TODO check that we are not missing any edge going out of non-base nodes
    }

    public void extendWith(GAction act) {
        List<GAction.GLogStatement> changes = act.getStatements().stream()
                .filter(s -> s.getStateVariable() == sv && s.isChange())
                .collect(Collectors.toList());
        if(changes.size() == 1) {
            GLogStatement s = changes.get(0);
            Node to = baseNodes.get(s.endValue());
            int duration = s.getMinDuration();
            List<DurativeCondition> conds = act.conditionsAt(s.start(), planner).stream()
                    .map(cond -> {
                        Fluent f = planner.preprocessor.getFluent(cond.sv, cond.startValue());
                        if(cond.isTransition())
                            return new DurativeCondition(f, 0);
                        else // persistence
                            return new DurativeCondition(f, act.abs.minDelay(cond.start(), cond.end()));
                    })
                    .collect(Collectors.toList());
            List<DurativeEffect> effs = act.changesStartingAt(s.start(), planner).stream()
                    .map(eff -> new DurativeEffect(planner.preprocessor.getFluent(eff.sv, eff.endValue()), eff.getMinDuration()))
                    .collect(Collectors.toList());
            if(s.isTransition()) {
                Node from = baseNodes.get(s.startValue());
                Transition tr = new Transition(from, to, duration, conds, effs, s, act);
                recordTransition(tr);
            } else { // assignement
                for(Node from : baseNodes.values())
                    recordTransition(new Transition(from, to, duration, conds, effs, s, act));
            }
        } else { // multiple changes on this state variable
            // sort all statements by increasing time
            changes.sort((s1, s2) -> Integer.compare(act.minDuration(act.abs.start(), s1.start()), act.minDuration(act.abs.start(), s2.start())));

            //split the chain of changes into subchains in which the effect of a change is compatible with the condition of the following one
            LinkedList<LinkedList<GLogStatement>> subchains = new LinkedList<>();
            subchains.add(new LinkedList<>());
            for(GLogStatement s : changes) {
                if(s.isAssignement())
                    subchains.add(new LinkedList<>());
                if(s.isTransition() && !s.startValue().equals(subchains.getLast().getLast().endValue()))
                    subchains.add(new LinkedList<>());
                subchains.getLast().add(s);
            }
            for(List<GLogStatement> chain : subchains) {
                Node lastNode = null;
                for(int i=0 ; i<chain.size() ; i++) {
                    GLogStatement s = chain.get(i);
                    List<Node> froms;
                    if(s.isAssignement())
                        froms = baseNodes.values().stream().collect(Collectors.toList());
                    else if(i == 0)
                        froms = Collections.singletonList(baseNodes.get(s.startValue()));
                    else
                        froms = Collections.singletonList(lastNode);
                    Node to;
                    if(i == chain.size()-1) {
                        to = baseNodes.get(s.endValue());
                    } else {
                        int minStay = act.minDuration(s.end(), chain.get(i+1).start());
                        int maxStay = act.maxDuration(s.end(), chain.get(i+1).start());
                        to = new Node(
                                planner.preprocessor.getFluent(s.sv, s.endValue()),
                                minStay, maxStay);
                    }
                    int duration = act.minDuration(s.start(), s.end());
                    List<DurativeCondition> conds = act.conditionsAt(s.start(), planner).stream()
                            .map(cond -> {
                                Fluent f = planner.preprocessor.getFluent(cond.sv, cond.startValue());
                                if(cond.isTransition())
                                    return new DurativeCondition(f, 0);
                                else // persistence
                                    return new DurativeCondition(f, act.abs.minDelay(cond.start(), cond.end()));
                            })
                            .collect(Collectors.toList());
                    List<DurativeEffect> effs = act.changesStartingAt(s.start(), planner).stream()
                            .map(eff -> new DurativeEffect(planner.preprocessor.getFluent(eff.sv, eff.endValue()), eff.getMinDuration()))
                            .collect(Collectors.toList());

                    for(Node from : froms)
                        recordTransition(new Transition(from, to, duration, conds, effs, s, act));
                    lastNode = to;
                }
            }
        }
    }
}
