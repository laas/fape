package fape.core.planning.states;

import fape.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fape.core.planning.timelines.Timeline;
import fape.core.planning.timelines.TimelinesManager;
import lombok.Value;
import planstack.structures.ISet;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CausalNetworkExt implements StateExtension {

    @Value
    static class PotentialIndirectSupporter {
        final int supporterID;
        final int changeNumber;
    }

    private final State container;

    final HashMap<Integer, ISet<PotentialIndirectSupporter>> potentialSupporters;
    final HashMap<Integer,Integer> lastProcessedChange;
    final List<Integer> extendedTimelines;
    final Set<Integer> addedTimelines;
    final Map<PotentialIndirectSupporter, ISet<Integer>> possiblyThreateningTimelines;

    CausalNetworkExt(State container) {
        this.container = container;
        potentialSupporters = new HashMap<>();
        lastProcessedChange = new HashMap<>();
        extendedTimelines = new ArrayList<>();
        addedTimelines = new HashSet<>(container.tdb.getTimelinesStream().map(t ->t.mID).collect(Collectors.toList()));
        possiblyThreateningTimelines = new HashMap<>();
    }

    CausalNetworkExt(CausalNetworkExt toCopy, State container) {
        this.container = container;
        potentialSupporters = new HashMap<>(toCopy.potentialSupporters);
        lastProcessedChange = new HashMap<>(toCopy.lastProcessedChange);
        extendedTimelines = new ArrayList<>(toCopy.extendedTimelines);
        addedTimelines = new HashSet<>(toCopy.addedTimelines);
        possiblyThreateningTimelines = new HashMap<>(toCopy.possiblyThreateningTimelines);
    }

    @Override
    public StateExtension clone(State st) {
        return new CausalNetworkExt(this, st);
    }

    private void processPending() {
        if(extendedTimelines.isEmpty() && addedTimelines.isEmpty())
            return;

        for(int tlID : addedTimelines) {
            if(!container.tdb.containsTimelineWithID(tlID))
                continue;
            Timeline tl = container.tdb.getTimeline(tlID);
            if(!tl.isConsumer())
                continue;
            assert !potentialSupporters.containsKey(tlID);
            potentialSupporters.put(tlID, new ISet<>());

            for(Timeline sup : container.tdb.getTimelines()) {
                for(int i=0 ; i<sup.numChanges() ; i++) {
                    if (UnsupportedTimeline.mightIndirectlySupport(sup, i, tl, container)) {
                        PotentialIndirectSupporter pis = new PotentialIndirectSupporter(sup.mID, i);
                        potentialSupporters.put(tlID, potentialSupporters.get(tlID).with(pis));
                    }
                }
            }
        }

        TimelinesManager tlMan = container.tdb;


        for(int tlID : potentialSupporters.keySet()) {
            if(!container.tdb.containsTimelineWithID(tlID)) {
                potentialSupporters.remove(tlID);
                continue;
            }
            Timeline tl = tlMan.getTimeline(tlID);

            if(!addedTimelines.contains(tlID)) {
                // the timeline was previously there, process the updated timlines to see if there are new potential supporters
                addedTimelines.stream()
                        .filter(tlMan::containsTimelineWithID)
                        .map(tlMan::getTimeline)
                        .forEach(sup -> {
                            for (int i = 0; i < sup.numChanges(); i++) {
                                if (UnsupportedTimeline.mightIndirectlySupport(sup, i, tl, container)) {
                                    PotentialIndirectSupporter pis = new PotentialIndirectSupporter(sup.mID, i);
                                    potentialSupporters.put(tlID, potentialSupporters.get(tlID).with(pis));
                                }
                            }
                        });
                extendedTimelines.stream()
                        .filter(id -> !addedTimelines.contains(id))
                        .filter(tlMan::containsTimelineWithID)
                        .map(tlMan::getTimeline)
                        .forEach(sup -> {
                            assert lastProcessedChange.containsKey(sup.mID);
                            for (int i = lastProcessedChange.get(sup.mID)+1; i < sup.numChanges(); i++) {
                                if (UnsupportedTimeline.mightIndirectlySupport(sup, i, tl, container)) {
                                    PotentialIndirectSupporter pis = new PotentialIndirectSupporter(sup.mID, i);
                                    potentialSupporters.put(tlID, potentialSupporters.get(tlID).with(pis));
                                }
                            }
                        });
            }

            List<PotentialIndirectSupporter> toRemove = new ArrayList<>();
            for(PotentialIndirectSupporter pis : potentialSupporters.get(tlID)) {
                if (!tlMan.containsTimelineWithID(pis.supporterID)) {
                    toRemove.add(pis);
                    continue;
                }
                Timeline sup = tlMan.getTimeline(pis.supporterID);
                if (!UnsupportedTimeline.mightIndirectlySupport(sup, pis.changeNumber, tl, container)) {
                    toRemove.add(pis);
                    continue;
                }


                // update potential threats
                ISet<Integer> initialList = possiblyThreateningTimelines.containsKey(pis) ?
                        possiblyThreateningTimelines.get(pis) :
                        new ISet<>();
                Stream<Integer> additionsToConsider = possiblyThreateningTimelines.containsKey(pis) ?
                        addedTimelines.stream() :
                        tlMan.getTimelinesStream().map(x -> x.mID);

                List<Integer> toRemoveFromInitialList = initialList.stream()
                        .filter(i -> !tlMan.containsTimelineWithID(i) || !possiblyThreatening(sup, tl, tlMan.getTimeline(i)))
                        .collect(Collectors.toList());
                List<Integer> toAddToInitialList = additionsToConsider
                        .filter(i -> tlMan.containsTimelineWithID(i) && !possiblyThreatening(sup, tl, tlMan.getTimeline(i)))
                        .collect(Collectors.toList());

                ISet<Integer> updatedList = initialList.withoutAll(toRemoveFromInitialList).withAll(toAddToInitialList);
                possiblyThreateningTimelines.put(pis, updatedList);

                for (int threatID : updatedList) {
                    Timeline threat = tlMan.getTimeline(threatID);
                    if(necessarilyThreatening(sup, tl, threat)) {
                        toRemove.add(pis);
                        break;
                    }
                }
            }

            for(PotentialIndirectSupporter pis : toRemove) {
                possiblyThreateningTimelines.remove(pis);
            }
            potentialSupporters.put(tlID, potentialSupporters.get(tlID).withoutAll(toRemove));
        }


        addedTimelines.clear();
        extendedTimelines.clear();
        lastProcessedChange.clear();
        for(Timeline tl : tlMan.getTimelines()) {
            lastProcessedChange.put(tl.mID, tl.numChanges()-1);
        }
    }

    public boolean possiblyThreatening(Timeline supporter, Timeline consumer, Timeline threat) {
        if(threat.hasSinglePersistence())
            return false;
        if(!container.unifiable(supporter, threat))
            return false;
        if(!container.unifiable(consumer, threat))
            return false;
        if(!container.canAllBeBefore(supporter.getFirstChangeTimePoint(), threat.getLastTimePoints()))
            return false;
        if(!container.canAllBeBefore(threat.getFirstChangeTimePoint(), consumer.getLastTimePoints()))
            return false;
        return true;
    }

    public boolean necessarilyThreatening(Timeline supporter, Timeline consumer, Timeline threat) {
        if(threat.hasSinglePersistence())
            return false;
        if(container.unified(supporter, threat) || !container.unified(consumer, threat))
            return false;

        // check if [start(supporter),end(consumer)] must overlap [start(threat),end(threat)]
        boolean mustOverlap = !container.canAllBeBefore(supporter.getFirstChangeTimePoint(), threat.getLastTimePoints()) &&
                !container.canAllBeBefore(threat.getFirstChangeTimePoint(), consumer.getLastTimePoints());
        return mustOverlap;
    }

    @Override
    public void timelineRemoved(Timeline tl) {
        potentialSupporters.remove(tl.mID);
    }

    @Override
    public void timelineAdded(Timeline a) {
        addedTimelines.add(a.mID);
    }

    @Override
    public void timelineExtended(Timeline tl) {
        extendedTimelines.add(tl.mID);
    }


}
