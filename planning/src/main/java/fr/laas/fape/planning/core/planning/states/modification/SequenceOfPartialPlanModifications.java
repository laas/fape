package fr.laas.fape.planning.core.planning.states.modification;

import fr.laas.fape.planning.core.planning.states.PartialPlan;
import lombok.Value;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class SequenceOfPartialPlanModifications implements PartialPlanModification {

    public final List<PartialPlanModification> modifications;

    @Override
    public void apply(PartialPlan plan, boolean isFastForwarding) {
        for(PartialPlanModification mod : modifications)
            mod.apply(plan, false);
    }

    @Override
    public Collection<Object> involvedObjects() {
        return modifications.stream()
                .map(PartialPlanModification::involvedObjects)
                .collect(Collectors.toSet());
    }
}
