package fr.laas.fape.planning.core.planning.states;

import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Threat;
import fr.laas.fape.planning.core.planning.timelines.Timeline;

import java.util.*;

class ThreatsCache implements StateExtension {

    private final HashSet<PotentialThreat> threats;
    private final State st;

    ThreatsCache(State initialState) {
        this.st = initialState;
        this.threats = new HashSet<>();

        st.tdb.getTimelinesStream().forEach(tl -> timelineAdded(tl));
    }

    private ThreatsCache(ThreatsCache toCopy, State st) {
        this.st = st;
        this.threats = new HashSet<>(toCopy.threats);
    }

    private class PotentialThreat {
        private final int id1, id2;
        private PotentialThreat(Timeline tl1, Timeline tl2) {
            assert tl1 != tl2;
            if(tl1.mID < tl2.mID) {
                id1 = tl1.mID;
                id2 = tl2.mID;
            } else {
                id1 = tl2.mID;
                id2 = tl1.mID;
            }
        }
        @Override public int hashCode() { return id1 + 42*id2; }
        @Override public boolean equals(Object o) {
            return o instanceof PotentialThreat && id1 == ((PotentialThreat) o).id1 && id2 == ((PotentialThreat) o).id2;
        }
    }

    @Override
    public StateExtension clone(State st) {
        return new ThreatsCache(this, st);
    }

    @Override
    public void timelineAdded(Timeline a) {
        for(Timeline b : st.tdb.getTimelines()) {
            if(!st.unifiable(a.stateVariable, b.stateVariable))
                continue;

            if(isThreatening(st, a, b)) {
                threats.add(new PotentialThreat(a, b));
            }
        }
    }

    @Override
    public void timelineRemoved(Timeline tl) {
        List<PotentialThreat> toRemove = new ArrayList<>();
        for(PotentialThreat t : threats)
            if(t.id1 == tl.mID || t.id2 == tl.mID)
                toRemove.add(t);
        threats.removeAll(toRemove);
    }

    @Override
    public void timelineExtended(Timeline tl) {
        List<PotentialThreat> toRemove = new ArrayList<>();
        for(PotentialThreat pt : threats)
            if(pt.id1 == tl.mID || pt.id2 == tl.mID)
                toRemove.add(pt);
        threats.removeAll(toRemove);

        for(Timeline b : st.tdb.getTimelines()) {
            if(isThreatening(st, tl, b)) {
                threats.add(new PotentialThreat(tl, b));
            }
        }
    }

    List<Flaw> getAllThreats() {
        List<PotentialThreat> toRemove = new ArrayList<>();
        List<Flaw> verifiedThreats = new ArrayList<>();
        for(PotentialThreat pt : threats) {
            Timeline tl1 = st.getTimeline(pt.id1);
            Timeline tl2 = st.getTimeline(pt.id2);
            if(isThreatening(st, tl1, tl2)) {
                verifiedThreats.add(new Threat(tl1, tl2));
            } else {
                assert !isThreatening(st, tl2, tl1);
                toRemove.add(pt);
            }
        }
        threats.removeAll(toRemove);

        return verifiedThreats;
    }

    public static boolean isThreatening(State st, Timeline tl1, Timeline tl2) {
        if(tl1 == tl2)
            return false;

        if(!st.unifiable(tl1, tl2))
            return false;

        else if(tl1.hasSinglePersistence() && tl2.hasSinglePersistence())
            return false;

        else if(tl1.hasSinglePersistence())
            return false;

        else if(tl2.hasSinglePersistence())
            return false;

        else {
            boolean firstNecessarilyAfterSecond =
                    !st.canAnyBeStrictlyBefore(tl2.getFirstTimePoints(), tl1.getSupportTimePoint()) &&
                            !st.canAnyBeStrictlyBefore(tl2.getFirstChange().start(), tl1.getLastTimePoints());

            boolean secondNecessarilyAfterFirst =
                    !st.canAnyBeStrictlyBefore(tl1.getFirstTimePoints(), tl2.getSupportTimePoint()) &&
                            !st.canAnyBeStrictlyBefore(tl1.getFirstChange().start(), tl2.getLastTimePoints());

            return !(firstNecessarilyAfterSecond || secondNecessarilyAfterFirst);
        }
    }
}
