package fr.laas.fape.planning.core.planning.search.flaws.flaws.mutexes;

import fr.laas.fape.anml.model.Function;
import fr.laas.fape.anml.model.concrete.VarRef;
import fr.laas.fape.planning.core.planning.grounding.DisjunctiveFluent;
import fr.laas.fape.planning.core.planning.grounding.Fluent;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.finders.FlawFinder;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.states.Printer;

import java.util.*;
import java.util.stream.Collectors;

import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.timelines.FluentHolding;
import lombok.Value;
import fr.laas.fape.anml.model.ParameterizedStateVariable;

public class MutexThreatsFinder implements FlawFinder {

    static boolean debug = false;

    @Value
    class LiftedFluent {
        final Function f;
        final List<Integer> paramsIds;
        final int valId;
    }

    @Override
    public List<Flaw> getFlaws(State st, Planner planner) {
        List<Flaw> threats = new LinkedList<>();

        MutexesHandler.Ext ext = st.getExtension(MutexesHandler.Ext.class);

        if(!ext.hasUsefulMutexes())
            return threats; // we could only have identified regular threats

        Collection<FluentHolding> cls = st.tdb.getAllCausalLinks();

        Map<FluentHolding, LiftedFluent> lfs = new HashMap<>();
        Map<LiftedFluent, Set<Fluent>> instantiations = new HashMap<>();
        Map<LiftedFluent, Set<Fluent>> mutexes = new HashMap<>();

        for(FluentHolding cl : cls) {
            LiftedFluent lf = getLiftedFluent(cl.getSv(), cl.getValue(), st);
            lfs.put(cl, lf);

            if(!instantiations.containsKey(lf)) {
                instantiations.put(lf, DisjunctiveFluent.fluentsOf(cl.getSv(), cl.getValue(), st, st.pl));
            }
            if(!mutexes.containsKey(lf)) {
                for(Fluent instantiation : instantiations.get(lf)) {
                    if(!mutexes.containsKey(lf)) {
                        mutexes.put(lf, ext.getMutexesOf(instantiation).clone());
                    } else {
                        mutexes.get(lf).retainAll(ext.getMutexesOf(instantiation));
                    }
                }
            }
        }

        for(FluentHolding cl1 : cls) {
            LiftedFluent lf1 = lfs.get(cl1);
            for(FluentHolding cl2 : cls) {
                LiftedFluent lf2 = lfs.get(cl2);
                if(cl1 == cl2)
                    continue; // identical
                if(!mutexes.get(lf1).containsAll(instantiations.get(lf2)))
                    continue; // not mutex

                boolean firstNecessarilyAfterSecond = !st.canAnyBeStrictlyBefore(cl2.getStart(), cl1.getEnd());
                boolean secondNecessarilyAfterFirst = !st.canAnyBeStrictlyBefore(cl1.getStart(), cl2.getEnd());

                if(firstNecessarilyAfterSecond || secondNecessarilyAfterFirst)
                    continue; // they are not temporally overlapping;

                threats.add(new MutexThreat(cl1, cl2));
            }
        }

        if(debug) {
            System.out.println("State id: "+st.mID);
            for(FluentHolding cl : cls) {
                System.out.println("  "+Printer.p(st, cl));
            }
        }


        return threats;
    }


    public LiftedFluent getLiftedFluent(ParameterizedStateVariable sv, VarRef value, State st) {
        return new LiftedFluent(
                sv.func(),
                Arrays.stream(sv.args()).map(v -> st.csp.bindings().domID(v)).collect(Collectors.toList()),
                st.csp.bindings().domID(value));
    }
}
