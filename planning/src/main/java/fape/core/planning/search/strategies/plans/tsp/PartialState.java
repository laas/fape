package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import lombok.Value;
import planstack.anml.model.concrete.InstanceRef;

import java.util.HashMap;
import java.util.Map;
import static fape.core.planning.grounding.GAction.*;

public class PartialState {

    @Value public static class Label {
        public final InstanceRef val;
        public final int since;
        public final int until;
    }

    Map<GStateVariable, Label> labels = new HashMap<>();

    public void progress(GAction.GLogStatement statement, GoalNetwork.DisjunctiveGoal g) {
        Label prev = labels.getOrDefault(statement.sv, new Label(null, -1, -1));
        Label next;
        if(statement instanceof GAssignment) {
            next = new Label(((GAssignment) statement).to, prev.until+1, prev.until+1);
            g.setEarliest(Math.max(prev.getUntil(), g.getEarliest()));
        } else if(statement instanceof GPersistence) {
            assert prev.getVal() == ((GPersistence) statement).value;
            int start = Math.max(g.earliest, prev.getSince());
            int end = Math.max(start+1, prev.until);
            g.setEarliest(start);
            next = new Label(prev.val, prev.since, end);
        } else {
            assert statement instanceof GTransition;
            assert ((GTransition) statement).from == prev.getVal();
            g.setEarliest(Math.max(prev.getUntil(), g.getEarliest()));
            next = new Label(((GTransition) statement).to, prev.until+1, prev.until+1);
        }

        labels.put(statement.sv, next);
    }
}
