package fape.core.planning.search.flaws.flaws.mutexes;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.planner.APlanner;
import fape.core.planning.preprocessing.Preprocessor;
import fape.core.planning.search.Handler;
import fape.core.planning.states.SearchNode;
import fape.core.planning.states.State;
import fape.core.planning.states.StateExtension;
import fape.util.Pair;
import fr.laas.fape.structures.IRSet;
import fr.laas.fape.structures.IntRep;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.InstanceRef;

import java.util.*;
import java.util.stream.Collectors;

public class MutexesHandler extends Handler {

    static boolean debug = false;

    APlanner planner = null;

    class Ext implements StateExtension {

        public IRSet<Fluent> getMutexesOf(Fluent f) {
            if(!mutexes.containsKey(f))
                mutexes.put(f, new IRSet<>(planner.preprocessor.store.getIntRep(Fluent.class)));
            return mutexes.get(f);
        }

        /** Returns true if we identified mutexes outside the ones between fluents on the same variable */
        public boolean hasUsefulMutexes() {
            return hasUsefulMutexes;
        }

        @Override
        public StateExtension clone() {
            return this;
        }
    }

    Map<Fluent, IRSet<Fluent>> mutexes = new HashMap<>();
    HashMap<GAction, Set<Fluent>> eDeletesLists = new HashMap<>();
    boolean hasUsefulMutexes = false;

    public void stateBindedToPlanner(State st, APlanner pl) {
        pl.options.flawFinders.add(new MutexThreatsFinder());
    }

    private boolean areMutex(Fluent p, Fluent q) {
        if(p.sv == q.sv)
            return p.value != q.value;
        else if(mutexes.containsKey(p))
            return mutexes.get(p).contains(q);
        else
            return false;
    }

    public Set<Fluent> eDeleteList(GAction ga) {
        if(!eDeletesLists.containsKey(ga)) {
            Set<Fluent> eDeletions = new HashSet<>();
            for(Fluent p : planner.preprocessor.getAllFluents()) {
                for(Fluent q : ga.add)
                    if(areMutex(p, q)) {
                        eDeletions.add(p);
                    }
                boolean hasPAsEffect = false;
                for(Fluent q : ga.add)
                    if(q == p)
                        hasPAsEffect = true;
                for(Fluent r : ga.pre)
                    if(!hasPAsEffect && areMutex(r, p)) {
                        eDeletions.add(p);
                    }
            }
            eDeletesLists.put(ga, eDeletions);
        }
        return eDeletesLists.get(ga);
    }

    /**
     * Flow based mutexes identification.
     */
    public void getMutexes(State st) {
        Preprocessor pp = st.pl.preprocessor;
        IntRep<Fluent> fluentRep = pp.store.getIntRep(Fluent.class);

        for(Fluent p : pp.getAllFluents()) {
            IRSet<Fluent> incompatible = new IRSet<Fluent>(fluentRep);
            for(InstanceRef i : st.pb.instances().instancesOfType(p.sv.f.valueType()).stream().map(str -> st.pb.instance(str)).collect(Collectors.toList())) {
                Fluent f = pp.getFluent(p.sv, i);
                if(!i.equals(p.value) && pp.getAllFluents().contains(f))
                    incompatible.add(f);
            }
            mutexes.put(p, incompatible);
        }

        Map<Fluent, List<Set<Fluent>>> candidateMutexDisjuncts = new HashMap<>();
        Map<Fluent, Set<Fluent>> candidateMutexes = new HashMap<>();
        Set<Pair<Fluent,Fluent>> simultaneousProductions = new HashSet<>();

        Set<Fluent> fluentsInState = pp.getGroundProblem().allFluents(st);
        for(Fluent p : fluentsInState)
            for(Fluent q : fluentsInState)
                if(p != q)
                    simultaneousProductions.add(new Pair<>(p,q));

        for(GAction ga : pp.getAllActions()) {
            for(Map.Entry<Fluent,List<Fluent>> e : ga.concurrentChanges(planner).entrySet()) {
                Fluent p = e.getKey();
                List<Fluent> qs = e.getValue();
                IRSet<Fluent> localMutexes = new IRSet<>(fluentRep);
                for(Fluent q : qs) {
                    if(mutexes.containsKey(q))
                        localMutexes.addAll(mutexes.get(q));
                    simultaneousProductions.add(new Pair<>(p,q));
                }
                candidateMutexDisjuncts.putIfAbsent(p, new ArrayList<>());
                candidateMutexDisjuncts.get(p).add(localMutexes);

                if(candidateMutexes.containsKey(p))
                    candidateMutexes.get(p).retainAll(localMutexes);
                else
                    candidateMutexes.put(p, localMutexes.clone());
            }
        }

        // process mutexes for fluent p
        for(Fluent p : candidateMutexDisjuncts.keySet()) {
            // we try to determine if there is a set of fluents Q, including p, such that
            // producing a fluent in Q require deleting a fluent in Q
            Set<Fluent> fluentsThatAreOneSidedMutexWithP = new IRSet<>(fluentRep);
            boolean allDisjunctHaveCycles = true;
            for(Set<Fluent> disjunct : candidateMutexDisjuncts.get(p)) {
                boolean hasCycle = false;
                for(Fluent q : disjunct) {
                    if(candidateMutexes.containsKey(q) && candidateMutexes.get(q).contains(p)) {
                        hasCycle = true;
                        fluentsThatAreOneSidedMutexWithP.add(q);
                    }
                }
                allDisjunctHaveCycles &= hasCycle;
                if(!allDisjunctHaveCycles)
                    break;
            }

            if(allDisjunctHaveCycles) {
                Set<Fluent> potentiallyExclusive = new HashSet<>(fluentsThatAreOneSidedMutexWithP);
                potentiallyExclusive.add(p);

                // any addition to a fluent in 'potentiallyExclusive' leads to the deletion of another.
                // now lets checks, whether there is never 2 becoming true simultaneously

                boolean atMostOneTrue = true;

                for(Fluent q1 : potentiallyExclusive) {
                    for(Fluent q2 : potentiallyExclusive) {
                        if(q1 != q2 && simultaneousProductions.contains(new Pair<>(q1,q2)))
                            atMostOneTrue = false;
                    }
                }
                if(atMostOneTrue) {
                    // all those fluents are mutually exclusive
                    for (Fluent q : potentiallyExclusive) {
                        for(Fluent q2 : potentiallyExclusive) {
                            if(q != q2) {
                                mutexes.get(q2).add(q);
                                mutexes.get(q).add(q2);
                                if(q.sv != q2.sv)
                                    hasUsefulMutexes = true;
                            }
                        }
                    }
                }
            }
        }

        if(debug) {
            System.out.println("Mutexes (except between different values of same SV:");
            for(Fluent p : mutexes.keySet()) {
                boolean firstPrinted = false;
                for(Fluent q : mutexes.get(p)) {
                    if(p.sv != q.sv) {
                        if(!firstPrinted)
                            System.out.println("  "+p);
                        firstPrinted = true;
                        System.out.println("    "+q);
                    }
                }
            }

        }

        for(Fluent p : pp.getGroundProblem().allFluents(st)) {
            for(Fluent q : pp.getGroundProblem().allFluents(st)) {
                assert !mutexes.get(p).contains(q);
            }
        }
    }

    @Override
    protected void apply(State st, StateLifeTime time, APlanner planner) {
        this.planner = planner;
        if (time == StateLifeTime.SELECTION) {
            if (!st.hasExtension(Ext.class)) {
                getMutexes(st);

                Ext ext = new Ext();
                st.addExtension(ext);
            }
        }
    }

    @Override
    public void actionInserted(Action act, State st, APlanner pl) {

    }
}
