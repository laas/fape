package fr.laas.fape.planning.core.planning.search.strategies.plans.tsp;

import fr.laas.fape.anml.model.concrete.statements.LogStatement;
import fr.laas.fape.planning.core.planning.grounding.Fluent;
import fr.laas.fape.planning.core.planning.grounding.GAction;
import fr.laas.fape.planning.core.planning.planner.GlobalOptions;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.preprocessing.dtg.TemporalDTG;
import fr.laas.fape.planning.core.planning.search.Handler;
import fr.laas.fape.planning.core.planning.states.CausalNetworkExt;
import fr.laas.fape.planning.core.planning.states.Printer;
import fr.laas.fape.planning.core.planning.states.PartialPlan;
import fr.laas.fape.planning.core.planning.states.StateExtension;
import fr.laas.fape.planning.core.planning.timelines.ChainComponent;
import fr.laas.fape.planning.core.planning.timelines.Timeline;
import fr.laas.fape.planning.exceptions.NoSolutionException;
import fr.laas.fape.planning.util.Pair;
import fr.laas.fape.structures.DijkstraQueue;
import fr.laas.fape.structures.IRSet;
import fr.laas.fape.structures.IntRep;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import fr.laas.fape.anml.model.ParameterizedStateVariable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class MinSpanTreeExtFull implements StateExtension {

    private static class Level {
        Map<Integer,IRSet<Fluent>> map = new HashMap<>();

        boolean containsKey(Fluent f) { return map.containsKey(f.getID()); }
        boolean containsKey(int id) { return map.containsKey(id); }
        IRSet<Fluent> get(Fluent f) { return map.get(f.getID()); }
        IRSet<Fluent> get(int id) { return  map.get(id); }
        void put(Fluent f, IRSet<Fluent> fs) { map.put(f.getID(), fs); }
        void computeIfAbsent(Fluent f, Function<Integer, IRSet<Fluent>> prod) { map.computeIfAbsent(f.getID(), prod); }
    }

    private final boolean USE_SUM = GlobalOptions.getBooleanOption("heur-additive-pending-cost");
    private int dbgLvl = 0;

    final PartialPlan st;
    private final Map<Timeline,TimelineDTG> timelineDTGs = new HashMap<>();
    private final Map<Timeline, Integer> minPreviousCost = new HashMap<>();

    private final HashMap<LogStatement, HashSet<GAction.GLogStatement>> groundStatements = new HashMap<>();

    public final Map<Timeline,List<Integer>> allCosts;
    private IntRep<Fluent> fluentRep;

    private IRSet<Fluent> workingCopy1;
    private IRSet<Fluent> workingCopy2;

    public MinSpanTreeExtFull(PartialPlan st) {
        this.st = st;
        allCosts = new HashMap<>();
        fluentRep = st.pl.preprocessor.store.getIntRep(Fluent.class);
        workingCopy1 = new IRSet<Fluent>(fluentRep);
        workingCopy2 = new IRSet<Fluent>(fluentRep);
    }

    public MinSpanTreeExtFull(PartialPlan st, MinSpanTreeExtFull toCopy) {
        this.st = st;
        this.allCosts = new HashMap<>(toCopy.allCosts);
        fluentRep = st.pl.preprocessor.store.getIntRep(Fluent.class);
        workingCopy1 = new IRSet<Fluent>(fluentRep);
        workingCopy2 = new IRSet<Fluent>(fluentRep);
    }

    @Override
    public StateExtension clone(PartialPlan st) {
        return new MinSpanTreeExtFull(st, this);
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

    public boolean hasBeenProcessed() { return additionalCost != -1; }

    private void computeHeuristic() {
        if(additionalCost == -1) {
            allCosts.clear();
            processTimelines();
            computeDistance();
        }
    }

    public int getCostToGo() {
        assert hasBeenProcessed();
        assert additionalCost != -1;
        return additionalCost;
    }

    public int getCurrentCost() {
        assert hasBeenProcessed();
        assert additionalCost != -1;
        return currentCost;
    }

    private class TimelineDTG {
        Timeline tl;
        List<Level> precedingFluents = new ArrayList<>();

        TimelineDTG(Timeline tl) {
            this.tl = tl;
            assert !tl.hasSinglePersistence();
            for(int i=0; i< tl.numChanges() ; i++) {
                precedingFluents.add(new Level());
                LogStatement e = tl.getEvent(i);
                for(GAction.GLogStatement ge : st.getGroundStatements(e)) {
                    if (!e.needsSupport()) {
                        assert i == 0;
                        if(this.precedingFluents.get(i).containsKey(ge.getEndFluent())) {
                            assert precedingFluents.get(i).get(ge.getEndFluent()) == null;
                        } else {
                            precedingFluents.get(i).put(ge.getEndFluent(), null);
                        }
                    } else {
                        if(i > 0 && !precedingFluents.get(i-1).containsKey(ge.getStartFluent()))
                            continue; // there is no path to this fluent
                        this.precedingFluents.get(i).computeIfAbsent(ge.getEndFluent(), x -> new IRSet<Fluent>(fluentRep));
                        this.precedingFluents.get(i).get(ge.getEndFluent()).add(ge.getStartFluent());
                    }
                }
            }
        }

        IRSet<Fluent> leftMostsFrom(int level, IRSet<Fluent> sources) {
            workingCopy1.clear();
            workingCopy2.clear();
            workingCopy1.addAll(sources);
            return leftMostsFromImpl(level, workingCopy1);
        }
        private IRSet<Fluent> leftMostsFromImpl(int level, IRSet<Fluent> sources) {
            assert sources == workingCopy1 || sources == workingCopy2;
            IRSet<Fluent> output = sources == workingCopy1 ? workingCopy2 : workingCopy1;
            assert tl.isConsumer();
//            Fluent[] ss = sources.toArray(new Fluent[0]);
            Level pre = precedingFluents.get(level);
            output.clear();
            sources.stream().forEach(i -> {
                if(pre.containsKey(i))
                    output.addAll(pre.get(i));
            });
            if(level == 0) {
                return output;
            } else {
                return leftMostsFromImpl(level-1, output);
            }
        }

        boolean canSupportAtEnd(Fluent f) {
            return precedingFluents.get(precedingFluents.size()-1).containsKey(f);
        }
    }


    private void processTimelines() {
        for(Timeline tl : st.getTimelines()) {
            if(tl.hasSinglePersistence())
                continue;

            timelineDTGs.put(tl, new TimelineDTG(tl));
        }
    }

    private void computeDistance() {
        if(dbgLvl >= 1) {
            System.out.println("\n=================================================\n");
            System.out.println("State: +" + st.mID);
        }
        this.additionalCost = 0;
        this.currentCost = 0;

        for(Timeline og : st.tdb.getConsumers()) {
            try {
                Set<Pair<DijNode, Integer>> startNodes = new HashSet<>();

                for (Fluent f : startFluents(og.getFirst().getFirst())) {
                    startNodes.add(new Pair<>(new DijNode(f, og), 0));
                }
                for (CausalNetworkExt.Event e : st.getExtension(CausalNetworkExt.class).getPotentialIndirectSupporters(og)) {
                    Timeline sup = st.getTimeline(e.supporterID);
                    if (e.getChangeNumber() < sup.numChanges() - 1) {
                        if (sup.isConsumer()) {
                            // traverse this timeline and add any fluent that can be used to support 'sup' to the queue
                            IRSet<Fluent> supStartFluents = timelineDTGs.get(sup).leftMostsFrom(e.getChangeNumber(), startFluents(og.getFirst().getFirst()));
                            for (Fluent f : supStartFluents) {
                                startNodes.add(new Pair<>(new DijNode(f, sup), 1));
                            }
                        } else {
                            // this is a terminal node, add it to the queue
                            startNodes.add(new Pair<>(new DijNode(null, sup), 1));
                        }
                    }
                }
                if (dbgLvl >= 2)
                    System.out.println(Printer.inlineTimeline(st, og));

                int ret = distToFinalNode(startNodes);
                minPreviousCost.put(og, ret);
            } catch (NoSolutionException e) {
                throw new UnachievableGoalException(og);
            }
        }
        currentCost = 0; // num statements in state
        for(Timeline tl : st.getTimelines()) {
            for (ChainComponent cc : tl.chain) {
                currentCost += cc.size();
            }
        }
        // list of (sv, cost) sorted by descending cost
        List<Pair<ParameterizedStateVariable,Integer>> costsPerStateVariable =
                minPreviousCost.entrySet().stream()
                        .map(e -> new Pair<>(e.getKey().stateVariable, e.getValue()))
                        .sorted((e1,e2) -> e2.getValue2().compareTo(e1.getValue2()))
                        .collect(Collectors.toList());

        if(USE_SUM) {
            for (Pair<ParameterizedStateVariable, Integer> p : costsPerStateVariable) {
                additionalCost += p.getValue2();
            }
        } else {
            // greedily find a set non overlapping state variables whose sum of costs is maximal
            Set<ParameterizedStateVariable> accountedFor = new HashSet<>();
            for (Pair<ParameterizedStateVariable, Integer> p : costsPerStateVariable) {
                if (accountedFor.stream().allMatch(sv -> !st.unifiable(sv, p.getValue1()))) {
                    accountedFor.add(p.getValue1());
                    additionalCost += p.getValue2();
                    if (dbgLvl >= 1)
                        System.out.println("++ " + Printer.stateVariable(st, p.getValue1()) + "  " + p.getValue2());
                }
            }
        }
        for(Timeline tl : st.getTimelines())
            allCosts.computeIfAbsent(tl, (x) -> Collections.emptyList());
//        System.out.println();
//        allCosts.entrySet().stream().forEach(x -> System.out.println(x));
    }

    /** Estimate the minimal number of statements that must be added to the plan to support on of those nodes */
    private int distToFinalNode(Set<Pair<DijNode,Integer>> startNodes) throws NoSolutionException {
        int numIter = 0;
        DijkstraQueue<DijNode> q = new DijkstraQueue<>();
        for(Pair<DijNode,Integer> p : startNodes) {
            q.putIfBetter(p.getValue1(), p.getValue2(), 1, null);
        }

        DijNode sol = null;
        while(!q.isEmpty() && sol == null) {
            numIter++;
            DijNode cur = q.poll();
            if(cur.isTerminal()) {
                sol = cur;
                break;
            }
            int curCost = q.getCost(cur);
            TemporalDTG dtg = st.pl.preprocessor.getTemporalDTG(cur.getF().sv);
            for(TemporalDTG.Change c : dtg.getBaseNode(cur.f.value).inChanges(st.addableActions)) {
                if(c.isTransition()) {
                    q.putIfBetter(new DijNode(c.getFrom().getFluent(), cur.tl), curCost+ c.getContainer().getNumStatements(), 1, cur);
                } else {
                    q.putIfBetter(new DijNode(null,cur.tl), curCost+ c.getContainer().getNumStatements(), 0, cur);
                }
            }
            for(CausalNetworkExt.Event e: st.getExtension(CausalNetworkExt.class).getPotentialIndirectSupporters(cur.tl)) {
                Timeline sup = st.getTimeline(e.supporterID);
                if(e.getChangeNumber() == sup.numChanges()-1 && timelineDTGs.get(sup).canSupportAtEnd(cur.f)) {
                    if(sup.isConsumer()) {
                        for(Fluent left : timelineDTGs.get(sup).leftMostsFrom(e.getChangeNumber(), IRSet.ofSingleton(fluentRep, cur.f))) {
                            q.putIfBetter(new DijNode(left,  sup), curCost+1, 1, cur);
                        }
                    } else {
                        q.putIfBetter(new DijNode(null,sup), curCost+1, 0, cur);
                    }
                }

            }
        }
        if(Planner.debugging && numIter >= 1000)
            System.out.println("Warning: More than 1000 iterations in computation of critical path heuristic");

        if(sol == null)
            throw new NoSolutionException("");
        else {
            if(dbgLvl >= 2) {
                System.out.println(q.getPathTo(sol));
                System.out.println("\nDist: " + q.getCost(sol) + "\n");
            }
            final int cost = q.getCost(sol);
            q.getPathTo(sol).stream().map(n -> n.getTl()).distinct().forEach(
                    tl -> allCosts.computeIfAbsent(tl, (x) -> new ArrayList<Integer>()).add(cost)
            );

            return q.getCost(sol);
        }

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

    private HashSet<GAction.GLogStatement> getGrounded(LogStatement s) {
        if(!groundStatements.containsKey(s)) {
            groundStatements.put(s, new HashSet<>(st.getGroundStatements(s)));
        }
        return groundStatements.get(s);
    }

    private Fluent startFluent(GAction.GLogStatement s) {
        return st.pl.preprocessor.getFluent(s.sv, s.startValue());
    }
    private IRSet<Fluent> startFluents(LogStatement s) {
        IRSet<Fluent> fs = new IRSet<>(st.pl.preprocessor.store.getIntRep(Fluent.class));
        getGrounded(s).stream().forEach(gs -> fs.add(startFluent(gs)));
        return fs;
    }
}
