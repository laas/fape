package fr.laas.fape.planning.core.planning.search.flaws.resolvers;

import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.states.modification.CausalLinkInsertion;
import fr.laas.fape.planning.core.planning.states.modification.StateModification;
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
    public StateModification asStateModification(State state) {
        return new CausalLinkInsertion(
                state.getTimeline(supporterID).getEvent(supportingComponent),
                state.getTimeline(consumerID).getFirst().getFirst());
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
