package fape.core.planning.states;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.search.flaws.resolvers.SupportingAction;
import fape.core.planning.search.flaws.resolvers.SupportingTimeline;
import fape.core.planning.timelines.Timeline;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.concrete.TPRef;
import planstack.structures.IList;

import java.util.*;
import java.util.stream.Collectors;

public class OpenGoalSupportersCache implements StateExtension {

    private final State container;
    private final Map<ParameterizedStateVariable, TPRef> locked;
    /**
     * Maps from timeline's IDs to potentially supporting (timeline, chain component).
     * The lists are exhaustive but might contain:
     *  - the same supporter twice.
     *  - supporters that are no longer valid (timeline removed or supporter not valid because of new constraints).
     */
    final HashMap<Integer, IList<Resolver>> potentialSupporters;

    OpenGoalSupportersCache(State container) {
        this.container = container;
        potentialSupporters = new HashMap<>();
        locked = new HashMap<>();
    }

    OpenGoalSupportersCache(OpenGoalSupportersCache toCopy, State container) {
        this.container = container;
        potentialSupporters = new HashMap<>(toCopy.potentialSupporters);
        locked = new HashMap<>(toCopy.locked);
    }

    @Override
    public StateExtension clone(State st) {
        return new OpenGoalSupportersCache(this, st);
    }

    @Override
    public void timelineRemoved(Timeline tl) {
        potentialSupporters.remove(tl.mID);
    }

    @Override
    public void timelineAdded(Timeline a) {
        // checks if this new timeline can provide support to others
        for(int i=0 ; i < a.numChanges() ; i++) {
            for(Timeline b : container.tdb.getConsumers()) {
                if(!potentialSupporters.containsKey(b.mID))
                    continue; // resolvers have not been initialized yet
                if (!UnsupportedTimeline.isSupporting(a, i, b, container))
                    continue; // a cannot support b

                // check if don't already have a has a support for b (to avoid duplication)
                boolean alreadyPresent = false;
                IList<Resolver> previous = potentialSupporters.get(b.mID);
                for(Resolver r : previous) {
                    if(r instanceof SupportingTimeline
                            && ((SupportingTimeline) r).supporterID == a.mID
                            && ((SupportingTimeline) r).supportingComponent == i) {
                        alreadyPresent = true;
                        break;
                    }
                }
                if(!alreadyPresent)
                    potentialSupporters.put(b.mID, previous.with(new SupportingTimeline(a.mID, i, b)));
            }
        }
    }

    @Override
    public void timelineExtended(Timeline tl) {
        // checks if the modifications on this timeline creates new supporters for others
        for(int i=0 ; i < tl.numChanges() ; i++) {
            for(Timeline b : container.tdb.getConsumers()) {
                if(!potentialSupporters.containsKey(b.mID))
                    continue; // resolvers of b have not been initialized yet
                if (!UnsupportedTimeline.isSupporting(tl, i, b, container))
                    continue; // a cannot support b

                // check if don't already have a has a support for b (to avoid duplication)
                boolean alreadyPresent = false;
                IList<Resolver> previous = potentialSupporters.get(b.mID);
                for(Resolver r : previous) {
                    if(r instanceof SupportingTimeline
                            && ((SupportingTimeline) r).supporterID == tl.mID
                            && ((SupportingTimeline) r).supportingComponent == i) {
                        alreadyPresent = true;
                        break;
                    }
                }
                if(!alreadyPresent)
                    potentialSupporters.put(b.mID, previous.with(new SupportingTimeline(tl.mID, i, b)));
            }
        }

        // keep track of state variables that are locked from start to given timepoint
        if(container.getLatestStartTime(tl.getFirstChangeTimePoint()) <= 0) {
            if(!locked.containsKey(tl.stateVariable)) {
                locked.put(tl.stateVariable, tl.getLastTimePoints().getFirst());
            } else if(container.getEarliestStartTime(tl.getLastTimePoints().getFirst()) > container.getEarliestStartTime(locked.get(tl.stateVariable))) {
                locked.put(tl.stateVariable, tl.getLastTimePoints().getFirst());
            }
        }
    }

    /**
     * Retrieves all valid resolvers for this unsupported timeline.
     *
     * As a side effects, this method also cleans up the resolvers stored in this state to remove double entries
     * and supporters that are not valid anymore.
     */
    public Iterable<Resolver> getResolversForOpenGoal(Timeline og) {
        assert og.isConsumer();
        assert container.tdb.getConsumers().contains(og) : "This timeline is not an open goal.";

        if(!potentialSupporters.containsKey(og.mID)) {
            // generate optimistic resolvers
            IList<Resolver> resolvers = new IList<>();

            // gather all potential supporters for this timeline
            for(Timeline sup : container.getTimelines()) {
                for(int i = 0 ; i < sup.numChanges() ; i++) {
                    SupportingTimeline supporter = new SupportingTimeline(sup.mID, i, og);
                    if(UnsupportedTimeline.isValidResolver(supporter, og, container))
                        resolvers = resolvers.with(supporter);
                }
            }

            // get all action supporters (new actions whose insertion would result in an interesting timeline)
            // a list of (abstract-action, decompositionID) of supporters
            Collection<fape.core.planning.preprocessing.SupportingAction> actionSupporters =
                    container.pl.getActionSupporterFinder().getActionsSupporting(container, og);

            //now we can look for adding the actions ad-hoc ...
            if (APlanner.actionResolvers) {
                for (fape.core.planning.preprocessing.SupportingAction aa : actionSupporters) {
                    SupportingAction res = new SupportingAction(aa.absAct, aa.statementRef, og);
                    if(UnsupportedTimeline.isValidResolver(res, og, container))
                        resolvers = resolvers.with(res);
                }
            }
            potentialSupporters.put(og.mID, resolvers);
        } else {
            // we already have a set of resolvers, simply filter the invalid ones
            List<Resolver> toRemove = new ArrayList<>();
            for (Resolver sup : potentialSupporters.get(og.mID)) {
                if (!UnsupportedTimeline.isValidResolver(sup, og, container))
                    toRemove.add(sup);
            }

            if(!toRemove.isEmpty()) {
                IList<Resolver> onlyValidResolvers = potentialSupporters.get(og.mID).withoutAll(toRemove);
                potentialSupporters.put(og.mID, onlyValidResolvers);
            }
            toRemove.clear();
        }


        if(container.pl.options.checkUnsolvableThreatsForOpenGoalsResolvers) {
            List<Resolver> toRemove = new ArrayList<>();

            // here we check to see if some resolvers are not valid because they would result in unsolvable threats
            for (Resolver res : potentialSupporters.get(og.mID).stream().filter(r -> r instanceof SupportingTimeline).collect(Collectors.toList())) {
                Timeline supporter = container.getTimeline(((SupportingTimeline) res).supporterID);
                for (Timeline other : container.getTimelines()) {
                    if (other != og && other != supporter)
                        if (!other.hasSinglePersistence() && (container.unified(other, supporter) || container.unified(other, og))) {
                            // other is on the same state variable and chages the value of the state variable
                            // if it must be between the two, it will result in an unsolvable threat
                            // hence it must can be after the end of the consumer or before the start of the
                            if (!container.canAllBeBefore(og.getLastTimePoints(), other.getFirstChangeTimePoint())
                                    && !container.canAllBeBefore(other.getSupportTimePoint(), supporter.getFirstTimePoints())) {
                                // will result in unsolvable threat
                                toRemove.add(res);
                            }
                        }
                }
            }

            if (!toRemove.isEmpty()) {
                IList<Resolver> onlyValidResolvers = potentialSupporters.get(og.mID).withoutAll(toRemove);
                potentialSupporters.put(og.mID, onlyValidResolvers);
            }
        }





        return potentialSupporters.get(og.mID);
    }
}
