package fr.laas.fape.planning.core.planning.search.strategies.plans.tsp;

import fr.laas.fape.anml.model.concrete.InstanceRef;
import fr.laas.fape.planning.core.planning.grounding.GAction;
import fr.laas.fape.planning.core.planning.grounding.GStateVariable;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.preprocessing.dtg.TemporalDTG;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
public class PartialState {

    public final Planner planner;

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
        labels.putIfAbsent(sv, new LinkedList<>(Collections.singletonList(new Label(null, -1, -1))));
        return labels.get(sv).getLast();
    }

    public void progress(GAction.GLogStatement statement, GoalNetwork.DisjunctiveGoal g) {
        labels.putIfAbsent(statement.sv, new LinkedList<>(Collections.singletonList(new Label(null, -1, -1))));
        Label prev = labels.get(statement.sv).getLast();

        if(statement instanceof GAction.GAssignment) {
            setValue(statement.sv, statement.endValue(), prev.until+statement.minDuration, 0);
            g.setEarliest(Math.max(prev.getUntil(), g.getEarliest()));
        } else if(statement instanceof GAction.GPersistence) {
            assert prev.getVal() == statement.startValue();
            int start = Math.max(g.earliest, prev.getSince());
            setValue(statement.sv, statement.endValue(), start, statement.minDuration);
            g.setEarliest(start);
        } else {
            assert statement instanceof GAction.GTransition;
            assert statement.startValue() == prev.getVal();
            int start = Math.max(prev.until,g.getEarliest());
            setValue(statement.sv, statement.endValue(), start +statement.minDuration, 0);
            g.setEarliest(Math.max(prev.getUntil(), g.getEarliest()));
        }
    }

    public void setValue(TemporalDTG.Node n, int earliest, int duration) {
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
            TemporalDTG.Node n = dtg.getBaseNode(value);
            labels.get(sv).add(new Label(n, earliestStart, earliestStart+duration));
        }
    }
}
