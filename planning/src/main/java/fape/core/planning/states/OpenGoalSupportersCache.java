package fape.core.planning.states;

import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.preprocessing.HierarchicalEffects;
import fape.core.planning.preprocessing.TaskDecompositionsReasoner;
import fape.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fape.core.planning.search.flaws.resolvers.FutureTaskSupport;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.search.flaws.resolvers.SupportingAction;
import fape.core.planning.search.flaws.resolvers.SupportingTimeline;
import fape.core.planning.timelines.Timeline;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.Task;
import planstack.structures.IList;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenGoalSupportersCache implements StateExtension {

    private final State container;
    private final Map<ParameterizedStateVariable, TPRef> locked;

    final HashMap<Integer, IList<Resolver>> potentialActionSupporters;

    OpenGoalSupportersCache(State container) {
        this.container = container;
        potentialActionSupporters = new HashMap<>();
        locked = new HashMap<>();
    }

    OpenGoalSupportersCache(OpenGoalSupportersCache toCopy, State container) {
        this.container = container;
        potentialActionSupporters = new HashMap<>(toCopy.potentialActionSupporters);
        locked = new HashMap<>(toCopy.locked);
    }

    @Override
    public StateExtension clone(State st) {
        return new OpenGoalSupportersCache(this, st);
    }

    @Override
    public void timelineRemoved(Timeline tl) {
        potentialActionSupporters.remove(tl.mID);
    }

    public List<Resolver> getResolversForOpenGoal(Timeline og, boolean downwardOnly) {
        if(downwardOnly)
            return getDownwardResolversForOpenGoal(og);
        else
            return getTopDownResolversForOpenGoal(og);
    }

    /**
     * Retrieves all valid resolvers for this unsupported timeline.
     *
     * As a side effects, this method also cleans up the resolvers stored in this state to remove double entries
     * and supporters that are not valid anymore.
     */
    public List<Resolver> getTopDownResolversForOpenGoal(Timeline og) {
        assert og.isConsumer();
        assert container.tdb.getConsumers().contains(og) : "This timeline is not an open goal.";

        // first, update action resolvers if necessary
        if(!potentialActionSupporters.containsKey(og.mID)) {
            // generate optimistic resolvers
            IList<Resolver> resolvers = new IList<>();

            // get all action supporters (new actions whose insertion would result in an interesting timeline)
            // a list of (abstract-action, decompositionID) of supporters
            Collection<fape.core.planning.preprocessing.SupportingAction> actionSupporters =
                    container.pl.getActionSupporterFinder().getActionsSupporting(container, og);

            //now we can look for adding the actions ad-hoc ...
            for (fape.core.planning.preprocessing.SupportingAction aa : actionSupporters) {
                SupportingAction res = new SupportingAction(aa.absAct, aa.statementRef, og);
                if(UnsupportedTimeline.isValidResolver(res, og, container))
                    resolvers = resolvers.with(res);
            }
            potentialActionSupporters.put(og.mID, resolvers);
        } else {
            // we already have a set of resolvers, simply filter the invalid ones
            List<Resolver> toRemove = new ArrayList<>();
            for (Resolver sup : potentialActionSupporters.get(og.mID)) {
                if (!UnsupportedTimeline.isValidResolver(sup, og, container))
                    toRemove.add(sup);
            }

            if(!toRemove.isEmpty()) {
                IList<Resolver> onlyValidResolvers = potentialActionSupporters.get(og.mID).withoutAll(toRemove);
                potentialActionSupporters.put(og.mID, onlyValidResolvers);
            }
            toRemove.clear();
        }


        CausalNetworkExt cne = container.getExtension(CausalNetworkExt.class);

        // resolvers are the set of action resolvers and the timelines supporters (provided by the causal network)
        IList<Resolver> resolvers = potentialActionSupporters.get(og.mID).withAll(
                cne.getPotentialSupporters(og).stream()
                        .map(e -> new SupportingTimeline(e.supporterID, e.changeNumber, og))
                        .collect(Collectors.toList()));


        return resolvers.stream().collect(Collectors.toList());
    }

    public List<Resolver> getDownwardResolversForOpenGoal(Timeline og) {
        assert og.isConsumer();
        assert container.tdb.getConsumers().contains(og) : "This timeline is not an open goal.";

        HierarchicalEffects effs = container.pl.preprocessor.getHierarchicalEffects();

        Stream<FutureTaskSupport> taskSupports = container.getOpenTasks().stream()
                .filter(t -> effs.canIndirectlySupport(og, t, container))
                .filter(t -> container.getHierarchicalConstraints().isValidTaskSupport(t,og))
                .map(t -> new FutureTaskSupport(og, t));

        Stream<SupportingTimeline> timelineSupport = container.getExtension(CausalNetworkExt.class)
                 .getPotentialSupporters(og).stream()
                 .map(e -> new SupportingTimeline(e.supporterID, e.changeNumber, og));

        return Stream.concat(taskSupports, timelineSupport).collect(Collectors.toList());

        /*
        // a list of (abstract-action, decompositionID) of supporters
        Collection<fape.core.planning.preprocessing.SupportingAction> potentialSupporters = supporters.getActionsSupporting(container, og);

        // all actions that have an effect on the state variable
        Set<AbstractAction> potentiallySupportingAction = potentialSupporters.stream()
                .map(x -> x.absAct)
                .collect(Collectors.toSet());

        Set<Task> tasks = new HashSet<>();
        assert !container.getHierarchicalConstraints().isWaitingForADecomposition(og);

        for (Task t : container.getOpenTasks()) {
            if (!container.canBeStrictlyBefore(t.start(), og.getConsumeTimePoint()))
                continue;
            if (!container.getHierarchicalConstraints().isValidTaskSupport(t, og))
                continue;

            Collection<AbstractAction> decs = decompositions.possibleMethodsToDeriveTargetActions(t, potentiallySupportingAction);
            if (!decs.isEmpty())
                tasks.add(t);
        }

        // resolvers are only existing statements that validate the constraints and the future task supporters we found
        List<Resolver> newRes = Stream.concat(
                resolvers.stream()
                        .filter(resolver -> resolver instanceof SupportingTimeline)
                        .map(resolver1 -> (SupportingTimeline) resolver1)
                        .filter(res -> container.getHierarchicalConstraints().isValidSupport(res.getSupportingStatement(container), og)),
                tasks.stream()
                        .map(t -> new FutureTaskSupport(og, t)))
                .collect(Collectors.toList());

        resolvers = newRes;
        return resolvers;*/
    }
}
