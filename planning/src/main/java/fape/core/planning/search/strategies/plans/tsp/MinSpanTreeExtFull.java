package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction.GLogStatement;
import fape.core.planning.preprocessing.dtg.TemporalDTG;
import fape.core.planning.preprocessing.dtg.TemporalDTG.Change;
import fape.core.planning.search.Handler;
import fape.core.planning.states.CausalNetworkExt;
import fape.core.planning.states.CausalNetworkExt.Event;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.core.planning.states.StateExtension;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
import fape.util.Pair;
import fr.laas.fape.structures.DijkstraQueue;
import fr.laas.fape.structures.IRSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.concrete.statements.LogStatement;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class MinSpanTreeExtFull implements StateExtension {

    private int dbgLvl = 0;

    final State st;
    final boolean useNumChangesInAction;
    private final Map<Timeline,TimelineDTG> timelineDTGs = new HashMap<>();
    private final Map<Timeline, Integer> minPreviousCost = new HashMap<>();

    private final HashMap<LogStatement, HashSet<GLogStatement>> groundStatements = new HashMap<>();

    @Override
    public StateExtension clone(State st) {
        return new MinSpanTreeExtFull(st, useNumChangesInAction);
    }

    private int additionalCost = -1;
    private int currentCost = -1;

    public void notify(Handler.StateLifeTime stateLifeTime) {
        switch (stateLifeTime) {
            case PRE_QUEUE_INSERTION:
                computeHeuristic();
                break;
            default:
        }
    }

    private void computeHeuristic() {
        if(additionalCost == -1) {
            processTimelines();
            computeDistance();
        }
    }

    public int getCostToGo() {
        computeHeuristic();
        assert additionalCost != -1;
        return additionalCost;
    }

    public int getCurrentCost() {
        computeHeuristic();
        assert additionalCost != -1;
        return currentCost;
    }

    private class TimelineDTG {
        Timeline tl;
        List<Map<Fluent,Collection<Node>>> producedByChange = new ArrayList<>();

        abstract class Node {
            public abstract boolean matches(Fluent f);
            public abstract Node getLeftmost();
        }
        @Getter class ProducedNode extends Node {
            final Fluent f;
            final LogStatement producingStatement;
            final Node previous;
            final Node leftmost;
            ProducedNode(Fluent f, LogStatement producingStatement, Node previous) {
                this.f = f;
                this.producingStatement = producingStatement;
                this.previous = previous;
                this.leftmost = previous.getLeftmost();
            }
            @Override public boolean matches(Fluent f) { return f.equals(this.f); }
            @Override public Node getLeftmost() { return leftmost; }
        }
        @AllArgsConstructor @Getter class UnsupportedNode extends Node {
            final Fluent f;
            @Override public boolean matches(Fluent f) { return f.equals(this.f); }
            @Override public Node getLeftmost() { return this; }
        }
        class FinalNode extends Node {
            @Override public boolean matches(Fluent f) { return false; }
            @Override public Node getLeftmost() { return this; }
        }

        TimelineDTG(Timeline tl) {
            this.tl = tl;
            assert !tl.hasSinglePersistence();
            for(int i=0; i< tl.numChanges() ; i++) {
                producedByChange.add(new HashMap<>());
                LogStatement e = tl.getEvent(i);
                for(GLogStatement ge : st.getGroundStatements(e)) {
                    producedByChange.get(i).computeIfAbsent(ge.getEndFluent(), (f) -> new HashSet<>());
                    if (!e.needsSupport()) {
                        Node n = new ProducedNode(ge.getEndFluent(), e, new FinalNode());
                        producedByChange.get(i).get(ge.getEndFluent()).add(n);
                    } else if(i == 0) {
                        // first change
                        Node n = new ProducedNode(ge.getEndFluent(), e, new UnsupportedNode(ge.getStartFluent()));
                        producedByChange.get(i).get(ge.getEndFluent()).add(n);
                    } else {
                        //find nodes
                        for(Node prev : produced(i-1, ge.getStartFluent())) {
                            Node n = new ProducedNode(ge.getEndFluent(), e, prev);
                            producedByChange.get(i).get(ge.getEndFluent()).add(n);
                        }
                    }
                }
            }
        }

        private Collection<Node> produced(int level, Fluent f) {
            return producedByChange.get(level).computeIfAbsent(f, x -> Collections.emptyList());
        }
    }

    private void processTimelines() {
        for(Timeline tl : st.getTimelines()) {
            if(tl.hasSinglePersistence())
                continue;

            timelineDTGs.put(tl, new TimelineDTG(tl));
        }
    }

    private boolean support(Event e, Timeline og) {
        Set<Fluent> supporters = endFluents(e.getStatement());
        for(Fluent f : startFluents(og.getFirst().getFirst()))
            if(supporters.contains(f))
                return true;
        return false;
    }

    private void computeDistance() {
        if(dbgLvl >= 1) {
            System.out.println("\n=================================================\n");
            System.out.println("State: +" + st.mID);
        }
        this.additionalCost = 0;
        this.currentCost = 0;

        for(Timeline og : st.tdb.getConsumers()) {
            Set<Pair<DijNode,Integer>> startNodes = new HashSet<>();

            for(Fluent f : startFluents(og.getFirst().getFirst())) {
                startNodes.add(new Pair<>(new DijNode(f, og),0));
                for(Event e: st.getExtension(CausalNetworkExt.class).getPotentialIndirectSupporters(og)) {
                    Timeline sup = st.getTimeline(e.supporterID);
                    if(e.getChangeNumber() < sup.numChanges()-1) {
                        // not last change
                        for(TimelineDTG.Node n : timelineDTGs.get(sup).produced(e.getChangeNumber(), f).stream()
                                .map(TimelineDTG.Node::getLeftmost).collect(Collectors.toList())) {
                            if(n instanceof TimelineDTG.FinalNode) {
                                startNodes.add(new Pair<>(new DijNode(null, sup), e.getChangeNumber()+1));
                            } else {
                                assert n instanceof TimelineDTG.UnsupportedNode;
                                startNodes.add(new Pair<>(new DijNode(((TimelineDTG.UnsupportedNode) n).f, sup),e.getChangeNumber()+1));
                            }
                        }

                    }
                }
            }
            if(dbgLvl >= 2)
                System.out.println(Printer.inlineTemporalDatabase(st, og));

            int ret = distToFinalNode(startNodes);
            minPreviousCost.put(og, ret);
        }
        currentCost = 0; // num statements in state
        for(Timeline tl : st.getTimelines()) {
            for (ChainComponent cc : tl.chain) {
                currentCost += cc.size();
            }
        }
        List<List<Pair<ParameterizedStateVariable,Integer>>> costsPerFunction =
                minPreviousCost.entrySet().stream()
                        .collect(Collectors.groupingBy(e -> e.getKey().stateVariable.func())).entrySet().stream()
                        .map(e -> e.getValue())
                        .map(s -> s.stream().map(e -> new Pair<>(e.getKey().stateVariable, e.getValue()))
                                .sorted((e1,e2) -> e2.getValue2().compareTo(e1.getValue2()))
                                .collect(Collectors.toList()))
                        .collect(Collectors.toList());
        for(List<Pair<ParameterizedStateVariable,Integer>> l : costsPerFunction) {
            Set<ParameterizedStateVariable> accountedFor = new HashSet<>();
            for(Pair<ParameterizedStateVariable,Integer> p : l) {
                if(accountedFor.stream().allMatch(sv -> !st.unifiable(sv, p.getValue1()))) {
                    accountedFor.add(p.getValue1());
                    additionalCost += p.getValue2();
                    if(dbgLvl >= 1)
                        System.out.println("++ "+Printer.stateVariable(st,p.getValue1())+"  "+p.getValue2());
                }
            }
        }
    }


    private int distToFinalNode(Set<Pair<DijNode,Integer>> startNodes) {
        DijkstraQueue<DijNode> q = new DijkstraQueue<>();
        for(Pair<DijNode,Integer> p : startNodes) {
            q.putIfBetter(p.getValue1(), p.getValue2(), null);
        }

        DijNode sol = null;
        while(!q.isEmpty() && sol == null) {
            DijNode cur = q.poll();
            if(cur.isTerminal()) {
                sol = cur;
                break;
            }
            int curCost = q.getCost(cur);
            TemporalDTG dtg = st.pl.preprocessor.getTemporalDTG(cur.getF().sv);
            for(Change c : dtg.getBaseNode(cur.f.value).inChanges(st.addableActions)) {
                if(c.isTransition()) {
                    q.putIfBetter(new DijNode(c.getFrom().getFluent(), cur.tl), curCost+ c.getContainer().getNumStatements(), cur);
                } else {
                    q.putIfBetter(new DijNode(null,cur.tl), curCost+ c.getContainer().getNumStatements(), cur);
                }
            }
            for(Event e: st.getExtension(CausalNetworkExt.class).getPotentialIndirectSupporters(cur.tl)) {
                Timeline sup = st.getTimeline(e.supporterID);
                if(e.getChangeNumber() == sup.numChanges()-1) {
                    //only consider those that are end of a chain of causal links
                    for(TimelineDTG.Node n : timelineDTGs.get(sup).produced(e.getChangeNumber(), cur.f)) {
                        if(n.getLeftmost() instanceof TimelineDTG.FinalNode) {
                            q.putIfBetter(new DijNode(null,sup), curCost+1, cur);
                        } else {
                            assert n.getLeftmost() instanceof TimelineDTG.UnsupportedNode;
                            Fluent f = ((TimelineDTG.UnsupportedNode) n.getLeftmost()).getF();
                            q.putIfBetter(new DijNode(f,  sup), curCost+1, cur);
                        }
                    }
                }

            }
        }

        if(dbgLvl >= 2) {
            DijNode c = sol;
            while (q.getPredecessor(c) != null) {
                System.out.print(c + " ");
                c = q.getPredecessor(c);
            }
            System.out.println("\nDist: " + q.getCost(sol) + "\n");
        }
        return q.getCost(sol);
    }

    @Value private class DijNode {
        final Fluent f;
        final Timeline tl;
        DijNode(Fluent f, Timeline tl) {
            assert tl != null;
            this.f = f;
            this.tl = tl;
        }
        boolean isTerminal() { return f == null; }
        @Override public String toString() { return "["+f+", "+tl.mID+"]"; }
    }

    private HashSet<GLogStatement> getGrounded(LogStatement s) {
        if(!groundStatements.containsKey(s)) {
            groundStatements.put(s, new HashSet<>(st.getGroundStatements(s)));
        }
        return groundStatements.get(s);
    }

    private Fluent startFluent(GLogStatement s) {
        return st.pl.preprocessor.getFluent(s.sv, s.startValue());
    }
    private Fluent endFluent(GLogStatement s) {
        return st.pl.preprocessor.getFluent(s.sv, s.endValue());
    }
    private Set<Fluent> startFluents(LogStatement s) {
        IRSet<Fluent> fs =new IRSet<>(st.pl.preprocessor.store.getIntRep(Fluent.class));
        getGrounded(s).stream().forEach(gs -> fs.add(startFluent(gs)));
        return fs;
    }
    private Set<Fluent> endFluents(LogStatement s) {
        IRSet<Fluent> fs =new IRSet<>(st.pl.preprocessor.store.getIntRep(Fluent.class));
        getGrounded(s).stream().forEach(gs -> fs.add(endFluent(gs)));
        return fs;
    }
}
