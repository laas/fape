package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;

public class SupportingTimeline extends Resolver {

    /** Database that will support the consumer */
    public final int supporterID;

    /** Index of the chain component that will support the consumer.
     * If -1 this means the last one?
     */
    public final int precedingChainComponent;

    /** Database that needs to be supported */
    public final int consumerID;

    public SupportingTimeline(int supporterID, Timeline consumer) {
        this.supporterID = supporterID;
        precedingChainComponent = -1;
        this.consumerID = consumer.mID;
    }

    public SupportingTimeline(int supporterID, int chainComponent, Timeline consumer) {
        this.supporterID = supporterID;
        precedingChainComponent = chainComponent;
        this.consumerID = consumer.mID;
    }

    @Override
    public boolean apply(State st, APlanner planner) {
        final Timeline supporter = st.getDatabase(supporterID);
        final Timeline consumer = st.getDatabase(consumerID);
        assert supporter != null;
        assert consumer != null;

        ChainComponent precedingComponent = null;
        if (precedingChainComponent != -1) {
            precedingComponent = supporter.getChainComponent(precedingChainComponent);
        }

        // now perfrom the merging of the two timelines
        if (precedingComponent != null) {
            // will include the consumer into the other timeline

            // this is database merge of one persistence into another
            assert consumer.chain.size() == 1 && !consumer.chain.get(0).change :
                    "This is restricted to databases containing single persistence only";

            assert precedingComponent.change : "Support by a component that does not change the value.";
            planner.causalLinkAdded(st, precedingComponent.contents.getFirst(), consumer.chain.getFirst().contents.getFirst());

            st.insertTimelineAfter(supporter, consumer, precedingComponent);

        } else {
            // we concatenate the two timelines
            ChainComponent supportingStatement = supporter.getSupportingComponent();

            assert supportingStatement != null && supportingStatement.change;
            planner.causalLinkAdded(st, supportingStatement.contents.getFirst(), consumer.chain.getFirst().contents.getFirst());

            // database concatenation
            st.insertTimelineAfter(supporter, consumer, supporter.chain.getLast());
        }

        return true;
    }
}
