package fape.core.planning.heuristics.relaxed;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.TPRef;

import java.util.*;

public final class DTGCollection {

    final List<DTGImpl> dtgs = new ArrayList<>();
    final APlanner planner;
    final State st;

    public DTGCollection(APlanner planner, State st) {
        this.planner = planner; this.st = st;
    }

    public int add(DTGImpl dtg) {
        assert !dtgs.contains(dtg);
        dtgs.add(dtg);
        return dtgs.size()-1;
    }


    public DTGImpl get(int dtgID) {
        assert dtgs.size() > dtgID && dtgs.get(dtgID) != null;
        return dtgs.get(dtgID);
    }

    public boolean isAccepting(OpenGoalTransitionFinder.DualNode n) {
        return dtgs.get(n.dtgID).accepting(n.nodeID);
    }

    public GAction groundAction(OpenGoalTransitionFinder.DualEdge edge) {
        final int gActionID = dtgs.get(edge.dtgID).gaction(edge.edgeID);
        if(gActionID != -1)
            return planner.preprocessor.getGroundAction(gActionID);
        else
            return null;
    }

    public Action liftedAction(OpenGoalTransitionFinder.DualEdge edge) {
        final int liftedActionID = dtgs.get(edge.dtgID).laction(edge.edgeID);
        if(liftedActionID != -1)
            return st.getAction(liftedActionID);
        else
            return null;
    }

    public GStateVariable stateVariable(OpenGoalTransitionFinder.Path path) {
        final int fluentID = dtgs.get(path.end.dtgID).fluent(path.end.nodeID);
        return planner.preprocessor.getFluent(fluentID).sv;
    }

    public Collection<OpenGoalTransitionFinder.DualNode> entryPoints(Collection<Integer> usableDTGs, Collection<Fluent> fluents) {
        List<OpenGoalTransitionFinder.DualNode> entryPoints = new ArrayList<>();
        for(int dtgID : usableDTGs) {
            DTGImpl dtg = dtgs.get(dtgID);
            for (Fluent f : fluents) {
                PrimitiveIterator.OfInt it = dtg.entryNodes(f);
                while(it.hasNext()) {
                    int nodeID = it.next();
                    assert(dtg.entryPoint(nodeID) && dtg.fluent(nodeID) == f.ID);
                    entryPoints.add(new OpenGoalTransitionFinder.DualNode(dtgID, nodeID));
                }
            }
        }
        return entryPoints;
    }

    public Collection<OpenGoalTransitionFinder.DualNode> startPointForPersistence(Collection<Integer> usableDTGs, Collection<Fluent> fluents, TPRef start, TPRef end) {
        return entryPoints(usableDTGs, fluents); //TODO FIXME BADLY
    }
}
