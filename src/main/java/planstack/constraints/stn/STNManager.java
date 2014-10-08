package planstack.constraints.stn;


import planstack.graph.core.LabeledEdge;
import planstack.graph.printers.NodeEdgePrinter;

import java.util.HashMap;

/**
 * This class is a wrapper around an STN to allow usage of time points of any type.
 * @param <TPRef> Type of the time points.
 * @param <ID> Type of identifiers that are attached to constraints.
 */
public class STNManager<TPRef, ID> extends GenSTNManager<TPRef, ID>{

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
    @Override
    public int recordTimePoint(TPRef tp) {
        assert !ids.containsKey(tp) : "TimePoint " + tp + " is already recorded.";
        ids.put(tp, stn.addVar());

        return ids.get(tp);
    }

    @Override
    public void removeTimePoint(TPRef tp) {
        stn.removeVar(id(tp));
        ids.remove(tp);
    }

    @Override
    public void setTime(TPRef tp, int time) {
        stn.enforceInterval(stn.start(), id(tp), time, time);
    }

    /**
     * Returns the id of a timepoint in the stn
     */
    private int id(TPRef tp) {
        assert ids.containsKey(tp) : "TimePoint not declared: " + tp;
        return ids.get(tp);
    }

    @Override
    public void addConstraint(TPRef u, TPRef v, int w) { stn.addConstraint(id(u), id(v), w); }

    @Override
    public void addConstraintWithID(TPRef u, TPRef v, int w, ID id) {
        stn.addConstraintWithID(id(u), id(v), w, id);
    }


    /**
     * Removes all constraints that were recorded with the given ID
     * @return True if the resulting STN is consistent.
     */
    @Override
    public boolean removeConstraintsWithID(ID id) {
        return stn.removeConstraintsWithID(id);
    }

    @Override
    public boolean isConstraintPossible(TPRef u, TPRef v, int w) {
        return stn.isConstraintPossible(id(u), id(v), w);
    }

    /**
     * @return
     */
    @Override
    public STNManager<TPRef,ID> deepCopy() {
        return new STNManager<>(this);
    }

    public void AssertConsistent() {
        if (!stn.consistent()) {
            throw new RuntimeException("Inconsistent STN:");
        }
    }

    @Override
    public void exportToDotFile(String filename, NodeEdgePrinter<Object,Object,LabeledEdge<Object,Object>> printer) {
        if(stn instanceof STN)
            ((STN<ID>) stn).g().exportToDotFile(filename, printer);
    }

    @Override
    public int getEarliestStartTime(TPRef start) {
        return stn.earliestStart(id(start));
    }

    @Override
    public int getLatestStartTime(TPRef tp) { return stn.latestStart(id(tp)); }

    @Override
    public boolean isConsistent() {
        return stn.consistent();
    }
}