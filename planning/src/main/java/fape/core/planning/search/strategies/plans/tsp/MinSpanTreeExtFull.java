package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction.GLogStatement;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.preprocessing.Preprocessor;
import fape.core.planning.preprocessing.dtg.TemporalDTG;
import fape.core.planning.preprocessing.dtg.TemporalDTG.Change;
import fape.core.planning.preprocessing.dtg.TemporalDTG.Node;
import fape.core.planning.search.Handler;
import fape.core.planning.states.CausalNetworkExt;
import fape.core.planning.states.CausalNetworkExt.Event;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.core.planning.states.StateExtension;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.NoSolutionException;
import fape.util.Pair;
import fr.laas.fape.structures.DijkstraQueue;
import fr.laas.fape.structures.IDijkstraQueue;
import fr.laas.fape.structures.IRSet;
import lombok.RequiredArgsConstructor;
import lombok.Value;
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

    class TimelineDTG {
        Timeline tl;
        List<List<Node>> producedByChange = new ArrayList<>();

        abstract class Node {
            public abstract boolean matches(Fluent f);
            public abstract Node leftmost();
        };
        @Value class ProducedNode extends Node {
            final Fluent f;
            final LogStatement producingStatement;
            final Node previous;
            @Override public boolean matches(Fluent f) { return f.equals(this.f); }
            @Override public Node leftmost() { return getPrevious().leftmost(); }
        }
        @Value class UnsupportedNode extends Node {
            final Fluent f;
            @Override public boolean matches(Fluent f) { return f.equals(this.f); }
            @Override public Node leftmost() { return this; }
        }
        class FinalNode extends Node {
            @Override public boolean matches(Fluent f) { return false; }
            @Override public Node leftmost() { return this; }
        }

        TimelineDTG(Timeline tl) {
            this.tl = tl;
            assert !tl.hasSinglePersistence();
            for(int i=0; i< tl.numChanges() ; i++) {
                producedByChange.add(new ArrayList<>());
                LogStatement e = tl.getEvent(i);
                for(GLogStatement ge : st.getGroundStatements(e)) {
                    if (!e.needsSupport()) {
                        Node n = new ProducedNode(ge.getEndFluent(), e, new FinalNode());
                        producedByChange.get(i).add(n);
                    } else if(i == 0) {
                        // first change
                        Node n = new ProducedNode(ge.getEndFluent(), e, new UnsupportedNode(ge.getStartFluent()));
                        producedByChange.get(i).add(n);
                    } else {
                        //find nodes
                        for(Node prev : produced(i-1, ge.getStartFluent())) {
                            Node n = new ProducedNode(ge.getEndFluent(), e, prev);
                            producedByChange.get(i).add(n);
                        }
                    }
                }
            }
        }

        private Collection<Node> produced(int level, Fluent f) {
            return producedByChange.get(level).stream().filter(n -> n.matches(f)).collect(Collectors.toList());
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
                                .map(TimelineDTG.Node::leftmost).collect(Collectors.toList())) {
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
        for(Timeline tl : st.getTimelines()) {
            int localCost = 0;
            int pastChanges = 0;
            for(ChainComponent cc : tl.chain) {
                currentCost += cc.size() * (pastChanges+1);
                additionalCost += cc.size() * minPreviousCost.getOrDefault(tl,0);
                if(cc.change)
                    pastChanges++;
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
            for(Change c : dtg.getBaseNode(cur.f.value).inChanges()) {
                if(c.isTransition()) {
                    q.putIfBetter(new DijNode(c.getFrom().getFluent(), cur.tl), curCost+1, cur);
                } else {
                    q.putIfBetter(new DijNode(null,cur.tl), curCost+1, cur);
                }
            }
            for(Event e: st.getExtension(CausalNetworkExt.class).getPotentialIndirectSupporters(cur.tl)) {
                Timeline sup = st.getTimeline(e.supporterID);
                if(e.getChangeNumber() == sup.numChanges()-1) {
                    //only consider those that are end of a chain of causal links
                    for(TimelineDTG.Node n : timelineDTGs.get(sup).produced(e.getChangeNumber(), cur.f)) {
                        if(n.leftmost() instanceof TimelineDTG.FinalNode) {
                            q.putIfBetter(new DijNode(null,sup), curCost+sup.numChanges(), cur);
                        } else {
                            assert n.leftmost() instanceof TimelineDTG.UnsupportedNode;
                            Fluent f = ((TimelineDTG.UnsupportedNode) n.leftmost()).getF();
                            q.putIfBetter(new DijNode(f,  sup), curCost+sup.numChanges(), cur);
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

    private int minDist(Collection<Fluent> potentialSupporters, Collection<Fluent> targets) throws NoSolutionException {
        Preprocessor pp = st.pl.preprocessor;
        Set<GStateVariable> svs = targets.stream().map(f -> f.sv).collect(Collectors.toSet());

        IDijkstraQueue<Node> q = new IDijkstraQueue<>(pp.store.getIntRep(Node.class));

        for(Fluent f : targets) {
            if(potentialSupporters.contains(f))
                return 0;
            q.insert(pp.getTemporalDTG(f.sv).getBaseNode(f.value), 0);
        }
        int cost = Integer.MAX_VALUE;
        int solutionAt = Integer.MAX_VALUE;
        while(!q.isEmpty() && cost == Integer.MAX_VALUE) {
            Node cur = q.poll();
            if(potentialSupporters.contains(cur.getFluent())) {
                cost = q.getCost(cur);
            } else if(q.getCost(cur) == solutionAt) {
                cost = solutionAt;
            } else {
                int c = q.getCost(cur);
                for(Change ch : cur.inChanges()) {
                    int costOfChange = useNumChangesInAction ?
                            (int) ch.getContainer().getStatements().stream().filter(s -> s.isChange()).count() : 1;
                    if(ch.isTransition()) {
                        if(q.hasCost(ch.getFrom())) {
                            if(q.getCost(ch.getFrom()) > c+costOfChange)
                                q.update(ch.getFrom(), c+costOfChange);
                        } else {
                            q.insert(ch.getFrom(), c+costOfChange);
                        }
                    } else {
                        // we have an assignment, hence there is a solution with an additional cost of 1
                        solutionAt = c + costOfChange;
                    }
                }
            }
        }
        cost = Math.min(cost, solutionAt);
        if(cost == Integer.MAX_VALUE)
            throw new NoSolutionException();
        return cost;
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
