package fape.core.planning.stn;


import planstack.constraints.stn.STN;
import planstack.constraints.stn.STNIncBellmanFord;
import fape.util.TinyLogger;
import fape.util.Pair;
import planstack.graph.printers.GraphDotPrinter;

public class STNManagerPlanStack extends STNManager {

    STN stn = new STNIncBellmanFord();
    //TemporalVariable start, end; //global start and end of the world
    //List<TemporalVariable> variables = new LinkedList<>();

    /**
     *
     */
    @Override
    public void Init() {
        start = new TemporalVariable(); //0
        start.mID = stn.start();
        end = new TemporalVariable(); //1
        end.mID = stn.end();
        earliestExecution = new TemporalVariable();
        earliestExecution.mID = stn.addVar();
    }

    /**
     *
     */
    public STNManagerPlanStack() {

    }

    /**
     *
     * @param a
     * @param b
     */
    @Override
    public final void EnforceBefore(TemporalVariable a, TemporalVariable b) {
        TinyLogger.LogInfo("Adding temporal constraint: "+a.getID()+" < "+b.getID());
        stn.enforceBefore(a.getID(), b.getID());
    }

    /**
     * Enforces that b must happens at least minDelay after a
     * @param a
     * @param b
     * @param minDelay
     */
    public final void EnforceDelay(TemporalVariable a, TemporalVariable b, int minDelay) {
        stn.addConstraint(b.getID(), a.getID(), -minDelay);
    }

    /**
     *
     * @param a
     * @param b
     * @param min
     * @param max
     * @return
     */
    @Override
    public final boolean EnforceConstraint(TemporalVariable a, TemporalVariable b, int min, int max) {
        STN backup = stn.cc();
        TinyLogger.LogInfo("Adding temporal constraint: "+a.getID()+" ["+min+","+max+"] "+b.getID());
        stn.enforceInterval(a.getID(), b.getID(), min, max);
        if(stn.consistent()) {
            return true;
        } else {
            stn = backup;
            return false;
        }
    }

    /**
     * Remove the edge (u,v) in the constraint graph. The edge (v,u) is not removed.
     * Performs a consistency check from scratch (expensive, try to use removeConstraints if you are to remove
     * more than one constraint)
     * @param u
     * @param v
     * @return true if the STN is consistent after removal
     */
    @Override
    public boolean RemoveConstraint(int u, int v) {
        return stn.removeConstraint(u, v);
    }

    /**
     * For all pairs, remove the corresponding directed edge in the constraint graph. After every pair is removed,
     * a consistency check is performed from scratch.
     * @param ps
     * @return true if the STN is consistent after removal
     */
    @Override
    public boolean RemoveConstraints(Pair<TemporalVariable, TemporalVariable>... ps) {
        for(Pair<TemporalVariable, TemporalVariable> p : ps) {
            stn.removeConstraintUnsafe(p.value1.getID(), p.value2.getID());
        }
        return stn.checkConsistencyFromScratch();
    }

    /**
     *
     * @param first
     * @param second
     * @return
     */
    @Override
    public final boolean CanBeBefore(TemporalVariable first, TemporalVariable second) {
        boolean ret = stn.canBeBefore(first.getID(), second.getID());
        TinyLogger.LogInfo("STN: "+first.getID()+" can occur before "+second.getID());
        return ret;
    }

    /**
     *
     * @return
     */
    @Override
    public TemporalVariable getNewTemporalVariable() {
        TemporalVariable tv = new TemporalVariable();
        tv.mID = stn.addVar();
        return tv;
    }

    /**
     *
     * @return
     */
    @Override
    public STNManager DeepCopy() {
        STNManagerPlanStack nm = new STNManagerPlanStack();
        nm.end = this.end;
        nm.start = this.start;
        nm.earliestExecution = this.earliestExecution;
        nm.stn = stn.cc();
        return nm;
    }

    @Override
    public String Report() {
        String ret = "size: "+this.stn.size()+"\n";
        ret += stn.g().edges().mkString("\n");
        return ret;
    }

    @Override
    public void TestConsistent(){
        if(!stn.consistent()) {
            throw new RuntimeException("Inconsistent STN:");
        }
    }


    @Override
    public long GetEarliestStartTime(TemporalVariable start) {
        return stn.earliestStart(start.getID());
    }

    @Override
    public boolean IsConsistent() {
        try{
            TestConsistent();
            return true;
        }catch(Exception e){
            return false;
        }
    }

    @Override
    public void OverrideConstraint(TemporalVariable start, TemporalVariable end, int realEndTime, int realEndTime0) {
        stn.removeConstraint(start.getID(), end.getID());
        stn.removeConstraint(end.getID(), start.getID());
        stn.addConstraint(start.getID(), end.getID(), realEndTime);
    }

    @Override
    public void printToFile(String file) {
        GraphDotPrinter printer = new GraphDotPrinter(stn.g());
        printer.print2Dot(file);
    }
}
