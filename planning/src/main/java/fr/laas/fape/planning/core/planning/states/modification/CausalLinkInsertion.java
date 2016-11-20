package fr.laas.fape.planning.core.planning.states.modification;

import fr.laas.fape.anml.model.concrete.statements.LogStatement;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.timelines.ChainComponent;
import fr.laas.fape.planning.core.planning.timelines.Timeline;
import lombok.Value;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@Value
public class CausalLinkInsertion implements StateModification {

    public final LogStatement supporter;
    public final LogStatement consumer;

    @Override
    public void apply(State st, boolean isFastForwarding) {
        Timeline supTL = st.tdb.getTimelineContaining(supporter);
        Timeline consTL = st.tdb.getTimelineContaining(consumer);
        assert consTL != supTL;
        assert consTL.indexOfContainer(consumer) == 0;
        ChainComponent supportingComponent = supTL.getChainComponent(supTL.indexOfContainer(supporter));
        st.insertTimelineAfter(supTL, consTL, supportingComponent);
    }

    @Override
    public Collection<Object> involvedObjects() {
        return Arrays.asList(supporter, consumer);
    }
}
