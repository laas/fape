package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.planner.APlanner;
import fape.core.planning.preprocessing.dtg.TemporalDTG;
import fape.core.planning.preprocessing.dtg.TemporalDTG.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import planstack.anml.model.concrete.InstanceRef;

import java.util.*;

import static fape.core.planning.grounding.GAction.*;

@RequiredArgsConstructor
public class PartialState {

    public final APlanner planner;

    @Data @AllArgsConstructor
    public static class Label {
        public TemporalDTG.Node node;
        public InstanceRef getVal() { return node.getFluent().value; }
        public int since;
        public int until;
        public boolean isUndefined() { return node == null; }
    }

    Map<GStateVariable, LinkedList<Label>> labels = new HashMap<>();

    public Label latestLabel(GStateVariable sv) {
        return labels.get(sv).getLast();
    }

    public void progress(GAction.GLogStatement statement, GoalNetwork.DisjunctiveGoal g) {
        labels.putIfAbsent(statement.sv, new LinkedList<>(Collections.singletonList(new Label(null, -1, -1))));
        Label prev = labels.get(statement.sv).getLast();

        if(statement instanceof GAssignment) {
            setValue(statement.sv, ((GAssignment) statement).to, prev.until+statement.minDuration, 0);
            g.setEarliest(Math.max(prev.getUntil(), g.getEarliest()));
        } else if(statement instanceof GPersistence) {
            assert prev.getVal() == ((GPersistence) statement).value;
            int start = Math.max(g.earliest, prev.getSince());
            setValue(statement.sv, ((GPersistence) statement).value, start, statement.minDuration);
            g.setEarliest(start);
        } else {
            assert statement instanceof GTransition;
            assert ((GTransition) statement).from == prev.getVal();
            int start = Math.max(prev.until,g.getEarliest());
            setValue(statement.sv, ((GTransition) statement).to, start +statement.minDuration, 0);
            g.setEarliest(Math.max(prev.getUntil(), g.getEarliest()));
        }
    }

    public void setValue(Node n, int earliest, int duration) {
        assert !n.isUndefined();
        GStateVariable sv = n.getStateVariable();
        labels.putIfAbsent(sv, new LinkedList<>(Collections.singletonList(new Label(null, -1, -1))));
        Label prev = labels.get(sv).getLast();
        if(!prev.isUndefined() && prev.getNode().equals(n)) {
            // same node, just increase its length if necessary
            int endsAtLeast = Math.max(earliest, prev.getSince()) + duration;
            if(endsAtLeast > prev.getUntil())
                prev.until = endsAtLeast;
        } else {
            // value change, append a new label
            int earliestStart = Math.max(earliest, prev.getUntil()+1);
            TemporalDTG dtg = planner.preprocessor.getTemporalDTG(sv);
            labels.get(sv).add(new Label(n, earliestStart, earliestStart+duration));
        }
    }

    public void setValue(GStateVariable sv, InstanceRef value, int earliest, int duration) {
        assert value != null;
        labels.putIfAbsent(sv, new LinkedList<>(Collections.singletonList(new Label(null, -1, -1))));
        Label prev = labels.get(sv).getLast();
        if(!prev.isUndefined() && prev.getVal().equals(value)) {
            int endsAtLeast = Math.max(earliest, prev.getSince()) + duration;
            if(endsAtLeast > prev.getUntil())
                prev.until = endsAtLeast;
        } else {
            // value change, append a new label
            int earliestStart = Math.max(earliest, prev.getUntil()+1);
            TemporalDTG dtg = planner.preprocessor.getTemporalDTG(sv);
            Node n = dtg.getBaseNode(value);
            labels.get(sv).add(new Label(n, earliestStart, earliestStart+duration));
        }
    }
}
