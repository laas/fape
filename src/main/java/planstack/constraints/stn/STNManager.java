package planstack.constraints.stn;


import scala.Tuple2;

import java.util.HashMap;

public class STNManager<TPRef> {

    public final STN stn;
    public final HashMap<TPRef, Integer> ids;

    /**
     * Creates a new empty STN
     */
    public STNManager() {
        stn = new STNIncBellmanFord();
        ids = new HashMap<>();
    }

    public STNManager(STNManager toCopy) {
        stn = toCopy.stn.cc();
        ids = new HashMap<>(toCopy.ids);
    }

    /**
     * Creates and records a new ID for a timepoint
     *
     * @param tp Reference of the timepoint.
     * @return ID of the timepoint in the STN.
     */
    public int recordTimePoint(TPRef tp) {
        assert !ids.containsKey(tp) : "TimePoint " + tp + " is already recorded.";
        ids.put(tp, stn.addVar());

        return ids.get(tp);
    }

    /**
     * Returns the id of a timepoint int the stn
     */
    private int id(TPRef tp) {
        assert ids.containsKey(tp) : "TimePoint not declared: " + tp;
        return ids.get(tp);
    }

    /**
     * Returns true if there can be a delay of 1 time unit from a to b.
     * The STN is not modified.
     * In a distance graph this done by checking if the edge (b, a, 1) is consistent.
     */
    public final boolean CanBeStrictlyBefore(TPRef a, TPRef b) {
        return stn.isConstraintPossible(id(b), id(a), 1);
    }

    /**
     * Enforces a <= b
     */
    public final void EnforceBefore(TPRef a, TPRef b) {
        stn.enforceBefore(id(a), id(b));
    }

    /**
     * Enforces that b must happens at least minDelay after a
     */
    public final void EnforceMinDelay(TPRef a, TPRef b, int minDelay) {
        System.out.println(b+" -- -"+minDelay+" --> "+a);
        stn.addConstraint(id(b), id(a), -minDelay);
    }

    /**
     * Enforces that b must happens at most maxDelay after a
     */
    public final void EnforceMaxDelay(TPRef a, TPRef b, int maxDelay) {
        System.out.println(a+" -- "+maxDelay+" --> "+b);
        stn.addConstraint(id(a), id(b), maxDelay);
    }

    /**
     * Adds a temporal constraint a --[min, max]--> b and propagates it.
     * @return True if the resulting STN is consistent.
     */
    public final boolean EnforceConstraint(TPRef a, TPRef b, int min, int max) {
        STN backup = stn.cc();
        //TinyLogger.LogInfo("Adding temporal constraint: "+a+" ["+min+","+max+"] "+b);
        stn.enforceInterval(id(a), id(b), min, max);

        return stn.consistent();
    }

    /**
     * Remove the edge (u,v) in the constraint graph. The edge (v,u) is not removed.
     * Performs a consistency check from scratch (expensive, try to use removeConstraints if you are to remove
     * more than one constraint)
     *
     * @param u
     * @param v
     * @return true if the STN is consistent after removal
     */
    public boolean RemoveConstraint(int u, int v) {
        return stn.removeConstraint(u, v);
    }

    /**
     * For all pairs, remove the corresponding directed edge in the constraint graph. After every pair is removed,
     * a consistency check is performed from scratch.
     *
     * @param ps
     * @return true if the STN is consistent after removal
     */
    public boolean RemoveConstraints(Tuple2<TPRef, TPRef>... ps) {
        for (Tuple2<TPRef, TPRef> p : ps) {
            stn.removeConstraintUnsafe(id(p._1()), id(p._2()));
        }
        return stn.checkConsistencyFromScratch();
    }

    /**
     * @param first
     * @param second
     * @return
     */
    public final boolean CanBeBefore(TPRef first, TPRef second) {
        return stn.canBeBefore(id(first), id(second));
    }

    /**
     * @return
     */
    public STNManager<TPRef> DeepCopy() {
        return new STNManager(this);
    }

    public String Report() {
        String ret = "size: " + this.stn.size() + "\n";
        ret += stn.g().edges().mkString("\n");
        return ret;
    }

    public void AssertConsistent() {
        if (!stn.consistent()) {
            throw new RuntimeException("Inconsistent STN:");
        }
    }


    public long GetEarliestStartTime(TPRef start) {
        return stn.earliestStart(id(start));
    }

    public boolean IsConsistent() {
        return stn.consistent();
    }
}