package planstack.constraints.stnu;


import java.util.Map;

public interface STNUManager<TPRef> {



    /** Creates and records a new ID for a timepoint
     *
     * @param tp Reference of the timepoint.
     * @return ID of the timepoint in the STN.
     */
    public int recordTimePoint(TPRef tp);

    /**
     *
     * @param a
     * @param b
     */
    public void EnforceBefore(TPRef a, TPRef b);

    /**
     * Enforces that b must happens at least minDelay after a
     * @param a
     * @param b
     * @param minDelay
     */
    public void EnforceDelay(TPRef a, TPRef b, int minDelay);

    /**
     * Adds a temporal constraint a --[min, max]--> b
     * @param a
     * @param b
     * @param min
     * @param max
     * @return
     */
    public boolean EnforceConstraint(TPRef a, TPRef b, int min, int max);

    public void addContingentConstraint(TPRef a, TPRef b, int min, int max);

    /**
     * Remove the edge (u,v) in the constraint graph. The edge (v,u) is not removed.
     * Performs a consistency check from scratch (expensive, try to use removeConstraints if you are to remove
     * more than one constraint)
     * @param u
     * @param v
     * @return true if the STN is consistent after removal
     */
    public boolean RemoveConstraint(int u, int v);

    /**
     * For all pairs, remove the corresponding directed edge in the constraint graph. After every pair is removed,
     * a consistency check is performed from scratch.
     * @param ps
     * @return true if the STN is consistent after removal
     */
//TODO    public boolean RemoveConstraints(Pair<TPRef, TPRef>... ps);

    /**
     *
     * @param first
     * @param second
     * @return
     */
    public boolean CanBeBefore(TPRef first, TPRef second);

    /**
     *
     * @return
     */
    public STNUManager DeepCopy();

    public String Report();

    public void AssertConsistent();


    public long GetEarliestStartTime(TPRef tp);

    public boolean IsConsistent();

    public Map<TPRef, Integer> getIdMap();

    public boolean checksPseudoControllability();

//TODO public Pair<List<Constraint>,List<Constraint>> getAllConstraints();
}
