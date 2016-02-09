package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.DisjunctiveFluent;
import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.planner.APlanner;
import fape.core.planning.search.strategies.plans.Heuristic;
import fape.core.planning.search.strategies.plans.PartialPlanComparator;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import planstack.anml.model.LStatementRef;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.statements.Assignment;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.Persistence;

import java.util.Collection;
import java.util.stream.Collectors;

public class Htsp implements PartialPlanComparator, Heuristic {
    @Override
    public String shortName() {
        return "tsp";
    }

    @Override
    public String reportOnState(State st) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int compare(State state, State t1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float g(State st) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float h(State st) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float hc(State st) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    public static GoalNetwork goalNetwork(State st) {
        APlanner planner = st.pl;

        GoalNetwork gn = new GoalNetwork();

        //TODO: (1) add temporal constraints

        for (Timeline tl : st.getTimelines()) {
            // ordered goals that will be extracted from this timeline
            GoalNetwork.DisjunctiveGoal[] goals;

            if(tl.hasSinglePersistence()) {
                goals = new GoalNetwork.DisjunctiveGoal[1];

                LogStatement s = tl.getChainComponent(0).getFirst();
                assert s instanceof Persistence;
                Collection<Fluent> fluents = DisjunctiveFluent.fluentsOf(s.sv(), s.endValue(), st, planner);

                goals[0] = new GoalNetwork.DisjunctiveGoal(fluents.stream()
                        .map(f -> new GAction.GPersistence(f.sv, f.value))
                        .collect(Collectors.toSet()));

            } else {
                goals = new GoalNetwork.DisjunctiveGoal[tl.numChanges()];
                for (int i = 0; i < tl.numChanges(); i++) {
                    LogStatement s = tl.getChangeNumber(i).getFirst();

                    // action by which this statement was introduced (null if no action)
                    Action containingAction = st.getActionContaining(s);

                    if (containingAction == null) { // statement was not added as part of an action
                        assert s instanceof Assignment;
                        assert s.endValue() instanceof InstanceRef;
                        assert i == 0;
                        Collection<Fluent> fluents = DisjunctiveFluent.fluentsOf(s.sv(), s.endValue(), st, planner);

                        goals[i] = new GoalNetwork.DisjunctiveGoal(fluents.stream()
                                .map(f -> new GAction.GAssignment(f.sv, f.value))
                                .collect(Collectors.toSet()));

                    } else { // statement was added as part of an action or a decomposition
                        Collection<GAction> acts = st.getGroundActions(containingAction);

                        // local reference of the statement, used to extract the corresponding ground statement from the GAction
                        assert containingAction.context().contains(s);
                        LStatementRef statementRef = containingAction.context().getRefOfStatement(s);

                        goals[i] = new GoalNetwork.DisjunctiveGoal(acts.stream()
                                .map(ga -> ga.statementWithRef(statementRef))
                                .collect(Collectors.toSet()));
                    }
                }
            }

            for(int i=0 ; i<goals.length ; i++) {
                if(i>0)
                    gn.addGoal(goals[i], goals[i-1]);
                else
                    gn.addGoal(goals[i], null);
            }
        }
        return gn;
    }
}
