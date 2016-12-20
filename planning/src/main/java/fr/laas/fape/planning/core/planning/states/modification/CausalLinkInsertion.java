package fr.laas.fape.planning.core.planning.states.modification;

import fr.laas.fape.anml.model.concrete.statements.LogStatement;
import fr.laas.fape.planning.core.planning.states.PartialPlan;
import fr.laas.fape.planning.core.planning.timelines.ChainComponent;
import fr.laas.fape.planning.core.planning.timelines.Timeline;
import lombok.Value;

import java.util.Arrays;
import java.util.Collection;

@Value
public class CausalLinkInsertion implements PartialPlanModification {

    public final LogStatement supporter;
    public final LogStatement consumer;

    @Override
    public void apply(PartialPlan plan, boolean isFastForwarding) {
        Timeline supTL = plan.tdb.getTimelineContaining(supporter);
        Timeline consTL = plan.tdb.getTimelineContaining(consumer);
        assert consTL != supTL;
        assert consTL.indexOfContainer(consumer) == 0;
        ChainComponent supportingComponent = supTL.getChainComponent(supTL.indexOfContainer(supporter));
        plan.insertTimelineAfter(supTL, consTL, supportingComponent);
    }

    @Override
    public Collection<Object> involvedObjects() {
        return Arrays.asList(supporter, consumer);
    }
}
