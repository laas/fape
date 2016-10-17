package fr.laas.fape.planning.core.planning.search.strategies.plans.tsp;

import fr.laas.fape.anml.model.concrete.statements.LogStatement;
import fr.laas.fape.constraints.bindings.Domain;
import fr.laas.fape.planning.core.planning.grounding.Fluent;
import fr.laas.fape.planning.core.planning.grounding.GAction;
import fr.laas.fape.planning.core.planning.grounding.GAction.*;
import fr.laas.fape.planning.core.planning.grounding.GStateVariable;
import fr.laas.fape.planning.core.planning.preprocessing.Preprocessor;
import fr.laas.fape.planning.core.planning.preprocessing.dtg.TemporalDTG;
import fr.laas.fape.planning.core.planning.search.Handler;
import fr.laas.fape.planning.core.planning.states.CausalNetworkExt;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.states.StateExtension;
import fr.laas.fape.planning.core.planning.timelines.Timeline;
import fr.laas.fape.planning.exceptions.NoSolutionException;
import fr.laas.fape.structures.IDijkstraQueue;
import fr.laas.fape.structures.IRSet;
import lombok.RequiredArgsConstructor;
import fr.laas.fape.anml.model.concrete.Action;


import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class MinSpanTreeExt implements StateExtension {

    private int dbgLvl = 0;

    final State st;
    final boolean useNumChangesInAction;

    private final HashMap<LogStatement, HashSet<GLogStatement>> groundStatements = new HashMap<>();

    @Override
    public StateExtension clone(State st) {
        return new MinSpanTreeExt(st, useNumChangesInAction);
    }

    private int numChangesInPartialPlan = -1;
    private int numAdditionalChanges = -1;

    public void notify(Handler.StateLifeTime stateLifeTime) {
        switch (stateLifeTime) {
            case PRE_QUEUE_INSERTION:
                computeHeuristic();
                break;
            default:
        }
    }

    private void computeHeuristic() {
        if(numAdditionalChanges == -1) {
            numChangesInPartialPlan = st.tdb.getTimelinesStream().mapToInt(tl -> tl.numChanges()).sum();
            processTimelines();
            numAdditionalChanges = computeDistance();
        }
    }

    public int getCostToGo() {
        assert numAdditionalChanges != -1;
        return numAdditionalChanges;
    }

    public int getCurrentCost() {
        computeHeuristic();
        return numChangesInPartialPlan;
    }

    private void processTimelines() {
        for(Timeline tl : st.getTimelines()) {
            if(tl.hasSinglePersistence())
                continue;

            Set<LogStatement> restricted = new HashSet<>();

            for(int i=1; i<tl.numChanges(); i++) {
                // fluents supported by statement i-1
                Set<Fluent> supportFluents = endFluents(tl.getEvent(i-1));

                // restrict instantiations of statement i to those supported by i-1
                boolean removal = getGrounded(tl.getEvent(i))
                        .removeIf(gs -> !supportFluents.contains(startFluent(gs)));
                if(removal)
                    restricted.add(tl.getEvent(i));
            }

            for(int i=tl.numChanges()-2; i>=0; i--) {
                // fluents required by statement i+1
                Set<Fluent> targetFluents = startFluents(tl.getEvent(i+1));

                // restrict instantiations of statement i to those supported by i-1
                boolean removal = getGrounded(tl.getEvent(i))
                        .removeIf(gs -> !targetFluents.contains(endFluent(gs)));
                if(removal)
                    restricted.add(tl.getEvent(i));
            }

            if(dbgLvl >= 1 && !restricted.isEmpty())
                System.out.println("Restricted: "+restricted);

            for(LogStatement s : restricted) {
                Optional<Action> optAct = st.getActionContaining(s);
                if(optAct.isPresent()) {
                    IRSet<GAction> allowed = new IRSet<>(st.pl.preprocessor.store.getIntRep(GAction.class));
                    for(GLogStatement gs : getGrounded(s))
                        allowed.add(gs.container.get());
                    Domain dom = new Domain(allowed.toBitSet());
                    st.csp.bindings().restrictDomain(optAct.get().instantiationVar(), dom);
                }
            }
        }
    }

    private boolean support(CausalNetworkExt.Event e, Timeline og) {
        Set<Fluent> supporters = endFluents(e.getStatement());
        for(Fluent f : startFluents(og.getFirst().getFirst()))
            if(supporters.contains(f))
                return true;
        return false;
    }

    private int computeDistance() {
        CausalNetworkExt cn = st.getExtension(CausalNetworkExt.class);

        int additionalChanges = 0;
        int numOpenGoals = 0;

        for(Timeline og : st.tdb.getConsumers()) {
            int minDist = Integer.MAX_VALUE;
            IRSet<Fluent> potentialSupport = new IRSet<>(st.pl.preprocessor.store.getIntRep(Fluent.class));
            for(CausalNetworkExt.Event e : cn.getPotentialIndirectSupporters(og)) {
                potentialSupport.addAll(endFluents(e.getStatement()));
            }
            try {
                int d = minDist(potentialSupport, startFluents(og.getFirst().getFirst()));
                additionalChanges += d;
                numOpenGoals++;
            } catch (NoSolutionException e) {
//                throw new UnachievableGoalException(og);
            }
        }

        int numChanges = 0;
        for(Timeline tl : st.getTimelines())
            numChanges += tl.numChanges();

        if(dbgLvl >= 1)
            System.out.println("---------->  "+numChanges+"   "+additionalChanges);

        return additionalChanges;
    }

    private int minDist(Collection<Fluent> potentialSupporters, Collection<Fluent> targets) throws NoSolutionException {
        Preprocessor pp = st.pl.preprocessor;
        Set<GStateVariable> svs = targets.stream().map(f -> f.sv).collect(Collectors.toSet());

        IDijkstraQueue<TemporalDTG.Node> q = new IDijkstraQueue<>(pp.store.getIntRep(TemporalDTG.Node.class));

        for(Fluent f : targets) {
            if(potentialSupporters.contains(f))
                return 0;
            q.insert(pp.getTemporalDTG(f.sv).getBaseNode(f.value), 0);
        }
        int cost = Integer.MAX_VALUE;
        int solutionAt = Integer.MAX_VALUE;
        while(!q.isEmpty() && cost == Integer.MAX_VALUE) {
            TemporalDTG.Node cur = q.poll();
            if(potentialSupporters.contains(cur.getFluent())) {
                cost = q.getCost(cur);
            } else if(q.getCost(cur) == solutionAt) {
                cost = solutionAt;
            } else {
                int c = q.getCost(cur);
                for(TemporalDTG.Change ch : cur.inChanges()) {
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

    private HashSet<GAction.GLogStatement> getGrounded(LogStatement s) {
        if(!groundStatements.containsKey(s)) {
            groundStatements.put(s, new HashSet<>(st.getGroundStatements(s)));
        }
        return groundStatements.get(s);
    }

    private Fluent startFluent(GAction.GLogStatement s) {
        return st.pl.preprocessor.getFluent(s.sv, s.startValue());
    }
    private Fluent endFluent(GAction.GLogStatement s) {
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
