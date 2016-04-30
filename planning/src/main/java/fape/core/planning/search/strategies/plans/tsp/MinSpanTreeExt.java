package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.preprocessing.Preprocessor;
import fape.core.planning.preprocessing.dtg.TemporalDTG;
import fape.core.planning.preprocessing.dtg.TemporalDTG.*;
import fape.core.planning.states.CausalNetworkExt;
import fape.core.planning.states.CausalNetworkExt.*;
import fape.core.planning.states.State;
import fape.core.planning.states.StateExtension;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.NoSolutionException;
import fr.laas.fape.structures.DijkstraQueue;
import fr.laas.fape.structures.IRSet;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.statements.LogStatement;
import fape.core.planning.grounding.GAction.*;
import planstack.constraints.bindings.Domain;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class MinSpanTreeExt implements StateExtension {

    final State st;
    final boolean useNumChangesInAction;

    final HashMap<LogStatement, HashSet<GLogStatement>> groundStatements = new HashMap<>();


    @Override
    public StateExtension clone(State st) {
        return new MinSpanTreeExt(st, useNumChangesInAction);
    }

    public int costToGo() {
        processTimelines();
        return computeDistance();
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

            if(!restricted.isEmpty())
                System.out.println(restricted);

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

    private boolean support(Event e, Timeline og) {
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
            for(Event e : cn.getPotentialIndirectSupporters(og)) {
                potentialSupport.addAll(endFluents(e.getStatement()));
            }
            try {
                int d = minDist(potentialSupport, startFluents(og.getFirst().getFirst()));
                additionalChanges += d;
                numOpenGoals++;
            } catch (NoSolutionException e) {
                throw new UnachievableGoalException(og);
            }
        }

        int numChanges = 0;
        for(Timeline tl : st.getTimelines())
            numChanges += tl.numChanges();

        System.out.println("---------->  "+numChanges+"   "+additionalChanges);

        return additionalChanges;
    }

    private int minDist(Collection<Fluent> potentialSupporters, Collection<Fluent> targets) throws NoSolutionException {
        Preprocessor pp = st.pl.preprocessor;
        Set<GStateVariable> svs = targets.stream().map(f -> f.sv).collect(Collectors.toSet());

        DijkstraQueue<Node> q = new DijkstraQueue<>(pp.store.getIntRep(Node.class));

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
            }else {
                int c = q.getCost(cur);
                for(Change ch : cur.inChanges()) {
                    if(ch.isTransition()) {
                        if(q.hasCost(ch.getFrom())) {
                            if(q.getCost(ch.getFrom()) > c+1)
                                q.update(ch.getFrom(), c+1);
                        } else {
                            q.insert(ch.getFrom(), c+1);
                        }
                    } else {
                        // we have an assignment, hence there is a solution with an additional cost of 1
                        solutionAt = c +1;
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
