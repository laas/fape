package fr.laas.fape.planning.core.planning.search.flaws.resolvers;

import fr.laas.fape.anml.model.concrete.Chronicle;
import fr.laas.fape.anml.model.concrete.MinDelayConstraint;
import fr.laas.fape.anml.model.concrete.TPRef;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.states.modification.ChronicleInsertion;
import fr.laas.fape.planning.core.planning.states.modification.StateModification;
import fr.laas.fape.planning.core.planning.timelines.Timeline;

public class TemporalSeparation implements Resolver {

    public final int firstDbID;
    public final int secondDbID;

    public TemporalSeparation(Timeline first, Timeline second) {
        this.firstDbID = first.mID;
        this.secondDbID = second.mID;
    }

    @Override
    public StateModification asStateModification(State state) {
        final Timeline firstDB = state.getTimeline(firstDbID);
        final Timeline secondDB = state.getTimeline(secondDbID);
        assert firstDB != null && secondDB != null;
        assert !firstDB.hasSinglePersistence() && !secondDB.hasSinglePersistence();

        Chronicle chronicle = new Chronicle();
        for(TPRef secondFirst : secondDB.getFirstTimePoints())
            chronicle.addConstraint(new MinDelayConstraint(firstDB.getSupportTimePoint(), secondFirst, 0));
        for(TPRef firstLast : firstDB.getLastTimePoints())
            chronicle.addConstraint(new MinDelayConstraint(firstLast, secondDB.getFirstChange().start(), 0));

        return new ChronicleInsertion(chronicle);
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof TemporalSeparation;
        TemporalSeparation o = (TemporalSeparation) e;
        if(firstDbID != o.firstDbID)
            return firstDbID - o.firstDbID;
        assert secondDbID != o.secondDbID : "Comparing two identical resolvers.";
        return secondDbID - o.secondDbID;
    }
}
