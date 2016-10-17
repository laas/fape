package fr.laas.fape.planning.core.planning.search.flaws.resolvers;

import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.timelines.ChainComponent;
import fr.laas.fape.planning.core.planning.timelines.Timeline;

public class SupportingTimeline implements Resolver {

    /** Database that will support the consumer */
    public final int supporterID;

    /** Number of the change statement supporting the consumer.
     * This count ignores the persistence change component. Use numChanges() and
     * getChangeNumber() methods in Timeline to access this.
     */
    public final int supportingComponent;

    /** Database that needs to be supported */
    private final int consumerID;

    public SupportingTimeline(int supporterID, int supportingChangeNumber, Timeline consumer) {
        this.supporterID = supporterID;
        supportingComponent = supportingChangeNumber;
        this.consumerID = consumer.mID;
    }

    @Override
    public boolean apply(State st, Planner planner, boolean isFastForwarding) {
        final Timeline supporter = st.getTimeline(supporterID);
        final Timeline consumer = st.getTimeline(consumerID);
        assert supporter != null;
        assert consumer != null;
        assert supporter != consumer : "Error: a resolver was generated that supports a timeline with itself.";

        ChainComponent precedingComponent = supporter.getChangeNumber(supportingComponent);

        // now perform the merging of the two timelines
        // we concatenate the two timelines

        assert precedingComponent != null && precedingComponent.change;

        // database concatenation
        st.insertTimelineAfter(supporter, consumer, precedingComponent);

        return true;
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof SupportingTimeline;
        SupportingTimeline o = (SupportingTimeline) e;
        if(consumerID != o.consumerID)
            return consumerID - o.consumerID;
        if(supporterID != o.supporterID)
            return supporterID - o.supporterID;
        assert supportingComponent != o.supportingComponent : "Error: comparing two identical resolvers.";
        return supportingComponent - o.supportingComponent;
    }

    @Override
    public int hashCode() {
        return supporterID + 42*supportingComponent + 42*42*consumerID;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof SupportingTimeline) {
            SupportingTimeline ost = (SupportingTimeline) o;
            return supporterID == ost.supporterID && supportingComponent == ost.supportingComponent && consumerID == ost.consumerID;
        } else {
            return false;
        }
    }
}
