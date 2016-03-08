package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.planner.APlanner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import planstack.anml.model.concrete.InstanceRef;

import static fape.core.planning.grounding.GAction.*;

import java.util.*;
import java.util.stream.Collectors;


public class TemporalDTG {
    public final APlanner planner;
    public final GStateVariable sv;

    /** Stays false until the postProcess method has been called.
     * Until then, the DTG might not be in a consistent state. */
    private boolean postProcessed = false;

    @AllArgsConstructor @Getter
    public class Node {
        final Fluent f;
        final int minStay;
        final int maxStay;
        final boolean isChangePossible;
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

    public interface Change {
        Node getFrom();
        Node getTo();
        int getDuration();
        List<DurativeCondition> getConditions();
        List<DurativeEffect> getSimultaneousEffects();
        GLogStatement getStatement();
        GAction getContainer();
        boolean isTransition();
    }

    @AllArgsConstructor @Getter
    public class Transition implements Change {
        final Node from;
        final Node to;
        final int duration;
        final List<DurativeCondition> conditions;
        final List<DurativeEffect> simultaneousEffects;
        final GAction.GLogStatement statement;
        final GAction container;
        public boolean isTransition() { return true; }
        @Override public String toString() { return from+" -["+duration+"]-> "+to; }
    }

    @AllArgsConstructor @Getter
    private class Assignment implements Change {
        final Node to;
        final int duration;
        final List<DurativeCondition> conditions;
        final List<DurativeEffect> simultaneousEffects;
        final GAction.GLogStatement statement;
        final GAction container;
        public Node getFrom() { throw new UnsupportedOperationException("Trying to get the origin of an assignment change."); }
        public boolean isTransition() { return false; }
        @Override public String toString() { return " :=["+duration+"]:= "+to; }
    }

    private Map<InstanceRef, Node> baseNodes = new HashMap<>();
    private Map<Node, List<Change>> outTransitions = new HashMap<>();
    private Map<Node, List<Change>> inTransitions = new HashMap<>();
    private List<Assignment> allAssignments = new ArrayList<>();

    public TemporalDTG(GStateVariable sv, Collection<InstanceRef> domain, APlanner pl) {
        this.planner = pl;
        this.sv = sv;
        for(InstanceRef val : domain) {
            Fluent f = pl.preprocessor.getFluent(sv, val);
            Node n = new Node(f, 0, Integer.MAX_VALUE, true);
            baseNodes.put(val, n);
            inTransitions.put(n, new ArrayList<>());
            outTransitions.put(n, new ArrayList<>());
        }
    }

    private void recordTransition(Transition tr) {
        assert !postProcessed;
        if(!outTransitions.containsKey(tr.getFrom()))
            outTransitions.put(tr.getFrom(), new ArrayList<>());
        if(!outTransitions.containsKey(tr.getTo()))
            outTransitions.put(tr.getTo(), new ArrayList<>());
        if(!inTransitions.containsKey(tr.getFrom()))
            inTransitions.put(tr.getFrom(), new ArrayList<>());
        if(!inTransitions.containsKey(tr.getTo()))
            inTransitions.put(tr.getTo(), new ArrayList<>());

        outTransitions.get(tr.getFrom()).add(tr);
        inTransitions.get(tr.getTo()).add(tr);
    }

    private void recordAssignment(Assignment ass) {
        assert !postProcessed;
        if(!outTransitions.containsKey(ass.getTo()))
            outTransitions.put(ass.getTo(), new ArrayList<>());
        if(!inTransitions.containsKey(ass.getTo()))
            inTransitions.put(ass.getTo(), new ArrayList<>());

        allAssignments.add(ass);
    }

    public List<Change> getChangesFrom(Node n) {
        assert postProcessed;
        return outTransitions.get(n);
    }

    public List<Change> getChangesTo(Node n) {
        assert postProcessed;
        return inTransitions.get(n);
    }

    public void extendWith(GAction act) {
        assert !postProcessed;
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
                            return new DurativeCondition(f, act.minDuration(cond.start(), cond.end()));
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
                allAssignments.add(new Assignment(to, duration, conds, effs, s, act));
            }
        } else { // multiple changes on this state variable
            // sort all statements by increasing time
            changes.sort((s1, s2) -> Integer.compare(act.minDuration(act.abs.start(), s1.start()), act.minDuration(act.abs.start(), s2.start())));

            //split the chain of changes into subchains in which the effect of a change is compatible with the condition of the following one
            LinkedList<LinkedList<GLogStatement>> subchains = new LinkedList<>();
            subchains.add(new LinkedList<>());
            for(GLogStatement s : changes) {
                if(s.isAssignement())
                    // start new subchain on assignments
                    subchains.add(new LinkedList<>());
                if(s.isTransition() &&
                        (subchains.getLast().isEmpty() || !s.startValue().equals(subchains.getLast().getLast().endValue())))
                    // start new subchain on transitions whose value is incompatible with the previous one
                    subchains.add(new LinkedList<>());
                subchains.getLast().add(s);
            }
            for(List<GLogStatement> chain : subchains) {
                Node lastNode = null;
                for(int i=0 ; i<chain.size() ; i++) {
                    GLogStatement s = chain.get(i);
                    Optional<Node> from;
                    if(s.isAssignement())
                        from = Optional.empty();
                    else if(i == 0)
                        from = Optional.of(baseNodes.get(s.startValue()));
                    else
                        from = Optional.of(lastNode);
                    Node to;
                    if(i == chain.size()-1) {
                        to = baseNodes.get(s.endValue());
                    } else {
                        GLogStatement nextChange = chain.get(i+1);
                        int minStay = act.minDuration(s.end(), nextChange.start());
                        int maxStay = act.maxDuration(s.end(), nextChange.start());
                        // try to find out if there is a persistence condition until the next change
                        boolean isChangePossible = act.getStatements().stream()
                                .filter(pers -> pers.isPersistence())
                                .filter(pers -> pers.getStateVariable() == s.getStateVariable())
                                .filter(pers -> pers.endValue().equals(s.endValue()))
                                .filter(pers -> act.maxDuration(s.end(), pers.start()) <= 1)
                                .filter(pers -> nextChange.isAssignement() && act.maxDuration(pers.end(), nextChange.start()) == 0
                                        || nextChange.isTransition() && act.maxDuration(pers.end(), nextChange.start()) <= 1)
                                .count() == 0;

                        to = new Node(
                                planner.preprocessor.getFluent(s.sv, s.endValue()),
                                minStay, maxStay, isChangePossible);
                    }
                    int duration = act.minDuration(s.start(), s.end());
                    List<DurativeCondition> conds = act.conditionsAt(s.start(), planner).stream()
                            .map(cond -> {
                                Fluent f = planner.preprocessor.getFluent(cond.sv, cond.startValue());
                                if(cond.isTransition())
                                    return new DurativeCondition(f, 0);
                                else // persistence
                                    return new DurativeCondition(f, act.minDuration(cond.start(), cond.end()));
                            })
                            .collect(Collectors.toList());
                    List<DurativeEffect> effs = act.changesStartingAt(s.start(), planner).stream()
                            .map(eff -> new DurativeEffect(planner.preprocessor.getFluent(eff.sv, eff.endValue()), eff.getMinDuration()))
                            .collect(Collectors.toList());

                    if(s.isTransition())
                        recordTransition(new Transition(from.get(), to, duration, conds, effs, s, act));
                    else
                        allAssignments.add(new Assignment(to, duration, conds, effs, s, act));
                    lastNode = to;
                }
            }
        }
    }

    public void postProcess() {
        Map<Fluent, Set<Node>> nodesByValue = new HashMap<>();
        for(Node n : inTransitions.keySet()) {
            if(!nodesByValue.containsKey(n.getF()))
                nodesByValue.put(n.getF(), new HashSet<>());
            nodesByValue.get(n.getF()).add(n);
        }

        for(Assignment ass : allAssignments) {
            for(Node from : outTransitions.keySet()) {
                if(from.isChangePossible())
                    outTransitions.get(from).add(ass);
            }
            inTransitions.get(ass.getTo()).add(ass);
        }

        postProcessed = true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(sv);
        for(Node n : outTransitions.keySet()) {
            sb.append("\n  "); sb.append(n);
            for(Change tr : outTransitions.get(n)) {
                sb.append("\n    "); sb.append(tr);
            }
        }
        return sb.toString();
    }
}
