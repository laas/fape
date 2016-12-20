package fr.laas.fape.planning.core.planning.search.flaws.resolvers;

import fr.laas.fape.anml.model.concrete.Factory;
import fr.laas.fape.planning.core.planning.states.PartialPlan;
import fr.laas.fape.planning.core.planning.states.modification.ActionInsertion;
import fr.laas.fape.planning.core.planning.states.modification.SequenceOfPartialPlanModifications;
import fr.laas.fape.planning.core.planning.states.modification.PartialPlanModification;
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
    public PartialPlanModification asStateModification(PartialPlan partialPlan) {
        Action action = Factory.getStandaloneAction(partialPlan.pb, act, partialPlan.refCounter);
        return new SequenceOfPartialPlanModifications(Arrays.asList(
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
