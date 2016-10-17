package fr.laas.fape.planning.core.planning.states;

import fr.laas.fape.anml.model.concrete.TPRef;
import fr.laas.fape.planning.core.planning.planner.PlanningOptions;
import fr.laas.fape.planning.core.planning.preprocessing.HierarchicalEffects;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.*;
import fr.laas.fape.planning.core.planning.timelines.Timeline;
import fr.laas.fape.planning.exceptions.FAPEException;
import fr.laas.fape.anml.model.ParameterizedStateVariable;
import planstack.structures.IList;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class OpenGoalSupportersCache implements StateExtension {

    private final State container;
    private final Map<ParameterizedStateVariable, TPRef> locked;

    private final HashMap<Integer, IList<Resolver>> potentialActionSupporters;

    OpenGoalSupportersCache(State container) {
        this.container = container;
        potentialActionSupporters = new HashMap<>();
        locked = new HashMap<>();
    }

    private OpenGoalSupportersCache(OpenGoalSupportersCache toCopy, State container) {
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

    List<Resolver> getResolversForOpenGoal(Timeline og, PlanningOptions.ActionInsertionStrategy strategy) {
        if(strategy == PlanningOptions.ActionInsertionStrategy.DOWNWARD_ONLY)
            return getDownwardResolversForOpenGoal(og);
        else if(strategy == PlanningOptions.ActionInsertionStrategy.UP_OR_DOWN)
            return getTopDownResolversForOpenGoal(og);
        else
            throw new FAPEException("Unrecognized action insertion strategy: "+strategy);
    }

    /**
     * Retrieves all valid resolvers for this unsupported timeline.
     *
     * As a side effects, this method also cleans up the resolvers stored in this state to remove double entries
     * and supporters that are not valid anymore.
     */
    private List<Resolver> getTopDownResolversForOpenGoal(Timeline og) {
        assert og.isConsumer();
        assert container.tdb.getConsumers().contains(og) : "This timeline is not an open goal.";

        // first, update action resolvers if necessary
        if(!potentialActionSupporters.containsKey(og.mID)) {
            // generate optimistic resolvers
            IList<Resolver> resolvers = new IList<>();

            // get all action supporters (new actions whose insertion would result in an interesting timeline)
            // a list of (abstract-action, decompositionID) of supporters
            Collection<fr.laas.fape.planning.core.planning.preprocessing.SupportingAction> actionSupporters =
                    container.pl.getActionSupporterFinder().getActionsSupporting(container, og);

            //now we can look for adding the actions ad-hoc ...
            for (fr.laas.fape.planning.core.planning.preprocessing.SupportingAction aa : actionSupporters) {
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

    private List<Resolver> getDownwardResolversForOpenGoal(Timeline og) {
        assert og.isConsumer();
        assert container.tdb.getConsumers().contains(og) : "This timeline is not an open goal.";

        HierarchicalEffects effs = container.pl.preprocessor.getHierarchicalEffects();

        Stream<FutureTaskSupport> taskSupports = container.getOpenTasks().stream() //TODO: only consider possible decompositions
                .filter(t -> effs.canIndirectlySupport(og, t, container))
                .filter(t -> container.getHierarchicalConstraints().isValidTaskSupport(t,og))
                .map(t -> new FutureTaskSupport(og, t));

        Stream<FutureActionSupport> actionSupports =
                container.getHierarchicalConstraints().isConstrained(og) ?
                        Stream.empty() :
                        container.pb.abstractActions().stream()
                                .filter(act -> !act.isTaskDependent())
                                .filter(act -> effs.canSupport(og, act, container))
                                .filter(act -> container.addableTemplates == null || container.addableTemplates.contains(act))
                                .map(act -> new FutureActionSupport(og, act));

        Stream<SupportingTimeline> timelineSupport = container.getExtension(CausalNetworkExt.class)
                 .getPotentialSupporters(og).stream()
                 .map(e -> new SupportingTimeline(e.supporterID, e.changeNumber, og));

        return Stream.concat(Stream.concat(taskSupports, actionSupports), timelineSupport)
                .collect(Collectors.toList());
    }
}
