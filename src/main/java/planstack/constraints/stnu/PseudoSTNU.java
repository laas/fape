package planstack.constraints.stnu;

import planstack.structures.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PseudoSTNU<TPRef> {

    private static final int initialCapacity = 10;

    public final int start, end;
    final FullSTN stn;
    public final HashMap<TPRef, Integer> ids;

    public final List<Constraint<TPRef>> requirements;
    public final List<Constraint<TPRef>> contingents;

    public PseudoSTNU() {
        stn = new FullSTN(initialCapacity);
        ids = new HashMap<>();
        contingents = new LinkedList<>();
        requirements = new LinkedList<>();
        start = stn.add_v();
        end = stn.add_v();
        if (start != 0) {
            throw new RuntimeException("FullSTN: Broken indexing.");
        }
    }

    public PseudoSTNU(PseudoSTNU<TPRef> toCopy) {
        stn = new FullSTN(toCopy.stn);
        ids = new HashMap<>(toCopy.ids);
        contingents = new LinkedList<>(toCopy.contingents);
        requirements = new LinkedList<>(toCopy.requirements);
        start = toCopy.start;
        end = toCopy.end;
    }


    /** Creates and records a new ID for a timepoint
     *
     * @param tp Reference of the timepoint.
     * @return ID of the timepoint in the STN.
     */
    public int recordTimePoint(TPRef tp) {
        assert !ids.containsKey(tp) : "TimePoint "+tp+" is already recorded.";
        ids.put(tp, stn.add_v());

        return ids.get(tp);
    }

    /** Returns the id of a timepoint int the stn */
    private int id(TPRef tp) {
        assert ids.containsKey(tp) : "TimePoint not declared: "+tp;
        return ids.get(tp);
    }

    /**
     *
     * @param a
     * @param b
     */
    public final void EnforceBefore(TPRef a, TPRef b) {
        stn.eless(id(a), id(b));
        requirements.add(new Constraint<>(b, a, 0));
    }

    /**
     * Enforces that b must happens at least minDelay after a
     * @param a
     * @param b
     * @param minDelay
     */
    public final void EnforceDelay(TPRef a, TPRef b, int minDelay) {
        EnforceConstraint(a, b, minDelay, stn.sup);
        requirements.add(new Constraint<>(b, a, -minDelay));
    }

    /**
     * Adds a temporal constraint a --[min, max]--> b
     * @param a
     * @param b
     * @param min
     * @param max
     * @return
     */
    public final boolean EnforceConstraint(TPRef a, TPRef b, int min, int max) {
        requirements.add(new Constraint<>(a, b, max));
        requirements.add(new Constraint<>(b, a, -min));
        if(stn.edge_consistent(id(a), id(b), min, max)) {
            stn.propagate(id(a), id(b), min, max);
            return true;
        } else {
            return false;
        }
    }

    public void addContingentConstraint(TPRef a, TPRef b, int min, int max) {
        contingents.add(new Constraint<>(a, b, max));
        contingents.add(new Constraint<>(b, a, -min));
    }

    /**
     * Remove the edge (u,v) in the constraint graph. The edge (v,u) is not removed.
     * Performs a consistency check from scratch (expensive, try to use removeConstraints if you are to remove
     * more than one constraint)
     * @param u
     * @param v
     * @return true if the STN is consistent after removal
     */
    public boolean RemoveConstraint(int u, int v) {
        throw new UnsupportedOperationException("");
    }

    /**
     * For all pairs, remove the corresponding directed edge in the constraint graph. After every pair is removed,
     * a consistency check is performed from scratch.
     * @param ps
     * @return true if the STN is consistent after removal
     */
    public boolean RemoveConstraints(Pair<TPRef, TPRef>... ps) {
        throw new UnsupportedOperationException("");
    }

    /**
     *
     * @param first
     * @param second
     * @return
     */
    public final boolean CanBeBefore(TPRef first, TPRef second) {
        return stn.pless(id(first), id(second));
    }

    /**
     *
     * @return
     */
    public PseudoSTNU<TPRef> DeepCopy() {
        return new PseudoSTNU<>(this);
    }

    public String Report() {
        return "Empty report for Pseudo STNU";
    }

    public void AssertConsistent(){
        if(!stn.isConsistent()) {
            throw new RuntimeException("Inconsistent STN:");
        }
    }


    public long GetEarliestStartTime(TPRef tp) {
        return stn.ga(start, id(tp));
    }

    public boolean IsConsistent() {
        boolean notSqueezed = true;
        for(Constraint<TPRef> cont : contingents) {
            notSqueezed &= stn.edge_consistent(id(cont.a), id(cont.b), cont.weight, cont.weight);
        }
        return notSqueezed && stn.isConsistent();
    }

    public Map<TPRef, Integer> getIdMap() {
        return ids;
    }

    public boolean checksPseudoControllability() {
        return true;
    }

    public Pair<List<Constraint<TPRef>>, List<Constraint<TPRef>>> getAllConstraints() {
        return new Pair<>(requirements, contingents);
    }
}
