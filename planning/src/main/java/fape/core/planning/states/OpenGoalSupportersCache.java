package fape.core.planning.states;

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

    /**
     * Retrieves all valid resolvers for this unsupported timeline.
     *
     * As a side effects, this method also cleans up the resolvers stored in this state to remove double entries
     * and supporters that are not valid anymore.
     */
    public Iterable<Resolver> getResolversForOpenGoal(Timeline og) {
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
                cne.getPotentialSupporters(og).stream().map(e -> new SupportingTimeline(e.supporterID, e.changeNumber, og)).collect(Collectors.toList()));


        return resolvers;
    }
}
