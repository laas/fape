package planstack.constraints.stn;


import planstack.graph.core.LabeledEdge;
import planstack.graph.printers.NodeEdgePrinter;
import scala.Tuple2;
import scala.collection.JavaConversions;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is a wrapper around an STN to allow usage of time points of any type.
 * @param <TPRef> Type of the time points.
 * @param <ID> Type of identifiers that are attached to constraints.
 */
public class STNManager<TPRef, ID> {

    public final ISTN<ID> stn;
    public final HashMap<TPRef, Integer> ids;

    /**
     * Creates a new empty STN
     */
    public STNManager() {
        stn = new STNIncBellmanFord<ID>();
        ids = new HashMap<>();
    }

    public STNManager(STNManager<TPRef,ID> toCopy) {
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
     * Enforces a < b
     */
    public final void EnforceStrictlyBefore(TPRef a, TPRef b) {
        stn.enforceStrictlyBefore(id(a), id(b));
    }

    /**
     * Enforces that b must happens at least minDelay after a
     */
    public final void EnforceMinDelay(TPRef a, TPRef b, int minDelay) {
        stn.addConstraint(id(b), id(a), -minDelay);
    }

    /**
     * Enforces that b must happens at least minDelay after a.
     * The constraint is added with an ID than can be used for later removal.
     */
    public final void EnforceMinDelayWithID(TPRef a, TPRef b, int minDelay, ID id) {
        stn.addConstraintWithID(id(b), id(a), -minDelay, id);
    }


    /**
     * Enforces that b must happens at most maxDelay after a
     */
    public final void EnforceMaxDelay(TPRef a, TPRef b, int maxDelay) {
        stn.addConstraint(id(a), id(b), maxDelay);
    }

    /**
     * Enforces that b must happens at most maxDelay after a.
     * The constraint is associated with an id that can be used for constraint removal.
     */
    public final void EnforceMaxDelayWithID(TPRef a, TPRef b, int maxDelay, ID id) {
        stn.addConstraintWithID(id(a), id(b), maxDelay, id);
    }

    /**
     * Adds a temporal constraint a --[min, max]--> b and propagates it.
     * @return True if the resulting STN is consistent.
     */
    public final boolean EnforceConstraint(TPRef a, TPRef b, int min, int max) {
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
    @Deprecated
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
    @Deprecated
    public boolean RemoveConstraints(Collection<Tuple2<TPRef, TPRef>> ps) {

        List<Tuple2<Object,Object>> toRemove = new LinkedList<>();
        for (Tuple2<TPRef, TPRef> p : ps) {
            Integer a = id(p._1());
            Integer b = id(p._2());
            toRemove.add(new Tuple2<Object, Object>(a, b));
        }
        stn.removeConstraints(JavaConversions.asScalaBuffer(toRemove));
        return stn.checkConsistencyFromScratch();
    }

    /**
     * Removes all constraints that were recorded with the given ID
     * @return True if the resulting STN is consistent.
     */
    public boolean removeConstraintsWithID(ID id) {
        return stn.removeConstraintsWithID(id);
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
    public STNManager<TPRef,ID> DeepCopy() {
        return new STNManager<>(this);
    }

    public void AssertConsistent() {
        if (!stn.consistent()) {
            throw new RuntimeException("Inconsistent STN:");
        }
    }

    public void exportToDotFile(String filename, NodeEdgePrinter<Object,Object,LabeledEdge<Object,Object>> printer) {
        if(stn instanceof STN)
            ((STN<ID>) stn).g().exportToDotFile(filename, printer);
    }


    public long GetEarliestStartTime(TPRef start) {
        return stn.earliestStart(id(start));
    }

    public long GetLatestStartTime(TPRef tp) { return stn.latestStart(id(tp)); }

    public boolean IsConsistent() {
        return stn.consistent();
    }
}