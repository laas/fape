package fr.laas.fape.planning.core.planning.search.strategies.plans.tsp;

import fr.laas.fape.anml.model.concrete.InstanceRef;
import fr.laas.fape.planning.core.planning.grounding.Fluent;
import fr.laas.fape.planning.core.planning.grounding.GAction.*;
import fr.laas.fape.planning.core.planning.grounding.GStateVariable;
import fr.laas.fape.planning.core.planning.preprocessing.Preprocessor;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.structures.IDijkstraQueue;

import java.util.*;

import static fr.laas.fape.planning.core.planning.search.strategies.plans.tsp.Htsp.*;

public class DTGRoutePlanner implements TSPRoutePlanner {


    @Override
    public Result getPlan(Collection<Fluent> targets, PartialState ps, State st) {
        Preprocessor pp = st.pl.preprocessor;

        IDijkstraQueue<Fluent> q = new IDijkstraQueue<>(pp.store.getIntRep(Fluent.class));
        Map<Fluent, GLogStatement> predecessors = new HashMap<>();

        for(GStateVariable sv : pp.store.getInstances(GStateVariable.class)) {
            int baseTime = -1;
            if(ps.labels.containsKey(sv)) {
                PartialState.Label l = ps.latestLabel(sv);
                q.insert(pp.getFluent(sv, l.getVal()), 0);
                baseTime = 0;
            }

            for(GAssignment ass : pp.getDTG(sv).getAssignments(st.addableActions)) {
                Fluent f = pp.getFluent(sv, ass.endValue());
                if(!q.contains(f)) {
                    q.insert(f, baseTime + 1);
                    predecessors.put(f, ass);
                }
            }
        }
        if(Htsp.dbgLvl >= 2)
        log2("  Targets: "+targets);
        Fluent sol = null;
        while(!q.isEmpty() && sol == null) {
            Fluent cur = q.poll();
            if(Htsp.dbgLvl >= 3)
                log3("  dij current: "+cur+"  "+q.getCost(cur));
            if(targets.contains(cur)) {
                sol = cur;
            } else {
                DTG dtg = pp.getDTG(cur.sv);
                for(GTransition trans : dtg.outTransitions(cur.value, st.addableActions)) {
                    assert cur == pp.getFluent(trans.sv, trans.startValue());
                    Fluent succ = pp.getFluent(trans.sv, trans.endValue());
                    if(!q.hasCost(succ)) { // never inserted
                        q.insert(succ, q.getCost(cur)+1);
                        predecessors.put(succ, trans);
                    } else if(q.getCost(succ) > q.getCost(cur)+1) {
                        q.update(succ, q.getCost(cur)+1);
                        predecessors.put(succ, trans);
                    }
                }
            }
        }
        if(sol != null) {
            Fluent cur = sol;
            LinkedList<GLogStatement> preds = new LinkedList<>();
            while (cur != null) { // extract predecessor list
                if(!predecessors.containsKey(cur))
                    cur = null;
                else {
                    preds.addFirst(predecessors.get(cur));
                    if(predecessors.get(cur) instanceof GTransition)
                        cur = pp.getFluent(cur.sv, ((GTransition) predecessors.get(cur)).startValue());
                    else
                        cur = null;
                }
            }
            if(Htsp.dbgLvl >= 2)
                log2("Dij choice: "+sol);
            final Fluent endFluent = sol;
            return new Result(sol, q.getCost(sol), partialState -> {
                for(GLogStatement pred : preds) {
                    if(pred instanceof GTransition) {
                        InstanceRef endValue = ((GTransition) pred).endValue();
                        partialState.setValue(pred.sv, endValue, partialState.latestLabel(pred.sv).getUntil() +pred.minDuration, 0);
                    }
                }

                partialState.setValue(endFluent.sv, endFluent.value, q.getCost(endFluent), 0);
            });
        } else {
            if(Htsp.dbgLvl >= 1)
                log1("DEAD-END!!!!");
            return null;
        }
    }
}
