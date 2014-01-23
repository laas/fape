package fape.core.planning.stn;


import planstack.constraints.stn.STN;
import planstack.constraints.stn.STNIncBellmanFord;
import fape.util.TinyLogger;

public class STNManagerPlanStack extends STNManager {

    STN stn = new STNIncBellmanFord();
    TemporalVariable start, end; //global start and end of the world
    //List<TemporalVariable> variables = new LinkedList<>();

    /**
     *
     */
    public void Init() {
        start = new TemporalVariable(); //0
        start.mID = stn.start();
        end = new TemporalVariable(); //1
        end.mID = stn.end();
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
    public final void EnforceBefore(TemporalVariable a, TemporalVariable b) {
        TinyLogger.LogInfo("Adding temporal constraint: "+a.getID()+" < "+b.getID());
        stn.enforceBefore(a.getID(), b.getID());
    }

    /**
     *
     * @param a
     * @param b
     * @param min
     * @param max
     * @return
     */
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
     *
     * @param first
     * @param second
     * @return
     */
    public final boolean CanBeBefore(TemporalVariable first, TemporalVariable second) {
        boolean ret = stn.canBeBefore(first.getID(), second.getID());
        TinyLogger.LogInfo("STN: "+first.getID()+" can occur before "+second.getID());
        return ret;
    }

    /**
     *
     * @return
     */
    public TemporalVariable getNewTemporalVariable() {
        TemporalVariable tv = new TemporalVariable();
        tv.mID = stn.addVar();
        return tv;
    }

    /**
     *
     * @return
     */
    public STNManager DeepCopy() {
        STNManagerPlanStack nm = new STNManagerPlanStack();
        nm.end = this.end;
        nm.start = this.start;
        nm.stn = stn.cc();
        return nm;
    }

    public String Report() {
        String ret = "size: "+this.stn.size()+"\n";
        ret += stn.g().edges().mkString("\n");
        return ret;
    }

    public void TestConsistent(){
        if(!stn.consistent())
            throw new RuntimeException("Inconsistent STN:");
    }


    public long GetEarliestStartTime(TemporalVariable start) {
        return stn.earliestStart(start.getID());
    }

    public TemporalVariable GetGlobalStart() {
        return start;
    }

    public TemporalVariable GetGlobalEnd() {
        return end;
    }

    public boolean IsConsistent() {
        try{
            TestConsistent();
            return true;
        }catch(Exception e){
            return false;
        }
    }
}
