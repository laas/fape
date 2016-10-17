package fr.laas.fape.planning.core.planning.search.strategies.plans.tsp;

import fr.laas.fape.anml.model.concrete.InstanceRef;
import fr.laas.fape.planning.core.planning.grounding.Fluent;
import fr.laas.fape.planning.core.planning.grounding.GStateVariable;
import fr.laas.fape.planning.core.planning.preprocessing.Preprocessor;
import fr.laas.fape.planning.core.planning.preprocessing.dtg.TemporalDTG;
import fr.laas.fape.planning.core.planning.preprocessing.dtg.TemporalDTG.*;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.structures.IDijkstraQueue;


import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static fr.laas.fape.planning.core.planning.search.strategies.plans.tsp.Htsp.*;

public class TemporalDTGRoutePlanner implements TSPRoutePlanner {


    @Override
    public Result getPlan(Collection<Fluent> targets, PartialState ps, State st) {
        Preprocessor pp = st.pl.preprocessor;

        IDijkstraQueue<TemporalDTG.Node> q = new IDijkstraQueue<>(pp.store.getIntRep(TemporalDTG.Node.class));
        Map<Node, TemporalDTG.Change> predecessors = new HashMap<>();

        for(GStateVariable sv : pp.store.getInstances(GStateVariable.class)) {
            if(ps.labels.containsKey(sv) && !ps.latestLabel(sv).isUndefined()) {
                PartialState.Label l = ps.latestLabel(sv);
                q.insert(l.getNode(), 0);
            } else {
                // the only way in is through assignments
                for(TemporalDTG.Assignment ass : pp.getTemporalDTG(sv).getAssignments(st.addableActions)) {
                    if(!q.contains(ass.getTo())) {
                        q.insert(ass.getTo(), ass.getDuration() -1); // smallest duration is 1
                        predecessors.put(ass.getTo(), ass);
                    } else if(q.getCost(ass.getTo()) > ass.getDuration() -1) {
                        q.update(ass.getTo(), ass.getDuration() -1); // smallest duration is 1
                        predecessors.put(ass.getTo(), ass);
                    }
                }
            }
        }
        if(Htsp.dbgLvl >= 2)
            log2("  Targets: "+targets);

        Node sol = null;
        while(!q.isEmpty() && sol == null) {
            Node cur = q.poll();
            if(Htsp.dbgLvl >= 3)
                log3("  dij current: "+cur+"  "+q.getCost(cur));
            if(targets.contains(cur.getFluent())) {
                sol = cur;
            } else {
                TemporalDTG dtg = pp.getTemporalDTG(cur.getFluent().sv);
                for(Change ch : dtg.getChangesFrom(cur, st.addableActions)) {
                    assert !ch.isTransition() || ch.getFrom() == cur;
                    Node succ = ch.getTo();
                    if(!q.hasCost(succ)) { // never inserted
                        q.insert(succ, q.getCost(cur)+1);
                        predecessors.put(succ, ch);
                    } else if(q.getCost(succ) > q.getCost(cur)+1) {
                        q.update(succ, q.getCost(cur)+1);
                        predecessors.put(succ, ch);
                    }
                }
            }
        }
        if(sol != null) {
            Node cur = sol;
            LinkedList<Change> preds = new LinkedList<>();
            while (cur != null) { // extract predecessor list
                if(!predecessors.containsKey(cur))
                    cur = null;
                else {
                    preds.addFirst(predecessors.get(cur));
                    if(predecessors.get(cur).isTransition())
                        cur = predecessors.get(cur).getFrom();
                    else
                        cur = null;
                }
            }
            if(Htsp.dbgLvl >= 2)
                log2("Dij choice: "+sol);
            final Node endNode = sol;
            return new Result(sol.getFluent(), q.getCost(sol), partialState -> {
                for(Change pred : preds) {
                    if(pred instanceof Transition) {
                        InstanceRef endValue = pred.getTo().getFluent().value;
                        partialState.setValue(
                                pred.getStateVariable(),
                                endValue,
                                partialState.latestLabel(pred.getStateVariable()).getUntil() +pred.getDuration(),
                                0);
                    }
                }

                partialState.setValue(endNode, q.getCost(endNode), 0);
            });
        } else {
            if(Htsp.dbgLvl >= 1)
                log1("DEAD-END!!!!");
            return null;
        }
    }
}
