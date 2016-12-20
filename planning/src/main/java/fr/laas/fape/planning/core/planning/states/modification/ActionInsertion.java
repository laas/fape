package fr.laas.fape.planning.core.planning.states.modification;

import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.planning.core.planning.states.PartialPlan;
import lombok.Value;

import java.util.Collection;

@Value
public class ActionInsertion implements PartialPlanModification {

    public final Action action;

    @Override
    public void apply(PartialPlan plan, boolean isFastForwarding) {
        plan.insert(action);
    }

    @Override
    public Collection<Object> involvedObjects() {
        Collection<Object> objs = new ChronicleInsertion(action.chronicle()).involvedObjects();
        objs.addAll(action.jUsedVariables());
        objs.add(action);
        return objs;
    }
}
