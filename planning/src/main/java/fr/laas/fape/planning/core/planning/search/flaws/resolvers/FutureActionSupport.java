package fr.laas.fape.planning.core.planning.search.flaws.resolvers;

import fr.laas.fape.anml.model.concrete.Factory;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.states.modification.ActionInsertion;
import fr.laas.fape.planning.core.planning.states.modification.SequenceOfStateModifications;
import fr.laas.fape.planning.core.planning.states.modification.StateModification;
import fr.laas.fape.planning.core.planning.states.modification.SupportRestriction;
import fr.laas.fape.planning.core.planning.timelines.Timeline;
import lombok.Value;
import fr.laas.fape.anml.model.abs.AbstractAction;
import fr.laas.fape.anml.model.concrete.Action;

import java.util.Arrays;

@Value
public class FutureActionSupport implements Resolver {

    private final Timeline consumer;
    private final AbstractAction act;

    @Override
    public StateModification asStateModification(State state) {
        Action action = Factory.getStandaloneAction(state.pb, act, state.refCounter);
        return new SequenceOfStateModifications(Arrays.asList(
                new ActionInsertion(action),
                new SupportRestriction(consumer.getFirst().getFirst(), action)));
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof FutureActionSupport;
        FutureActionSupport fts = (FutureActionSupport) e;
        assert fts.consumer == consumer : "Comparing resolvers of different flaws.";
        return act.name().compareTo(fts.act.name());
    }
}
