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

    public void progress(GAction.GLogStatement statement) { //TODO: update to take tie into account
        Label prev = labels.getOrDefault(statement.sv, new Label(null, -1, -1));
        Label next;
        if(statement instanceof GAssignment) {
            next = new Label(((GAssignment) statement).to, prev.until+1, prev.until+1);
        } else if(statement instanceof GPersistence) {
            assert prev.getVal() == ((GPersistence) statement).value;
            next = new Label(prev.val, prev.since, prev.until);
        } else {
            assert statement instanceof GTransition;
            assert ((GTransition) statement).from == prev.getVal();
            next = new Label(((GTransition) statement).to, prev.until+1, prev.until+1);
        }

        labels.put(statement.sv, next);
    }
}
