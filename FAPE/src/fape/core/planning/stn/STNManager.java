/*
 * Author:  Filip Dvořák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package fape.core.planning.stn;

import fape.exceptions.FAPEException;
import fape.util.TinyLogger;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class STNManager {

    STN stn = new STN();
    TemporalVariable start, end; //global start and end of the world   
    //List<TemporalVariable> variables = new LinkedList<>();

    /**
     *
     */
    public void Init() {
        if (STN.precalc == null) {
            STN.precalc_inic();
        }
        start = new TemporalVariable(); //0
        start.mID = stn.add_v();
        end = new TemporalVariable(); //1
        end.mID = stn.add_v();
        if (start.getID() != 0) {
            throw new FAPEException("STN: Broken indexing.");
        }
        EnforceBefore(start, end);

    }

    /**
     *
     */
    public STNManager() {

    }

    /**
     *
     * @param a
     * @param b
     */
    public final void EnforceBefore(TemporalVariable a, TemporalVariable b) {
        TinyLogger.LogInfo("Adding temporal constraint: "+a.getID()+" < "+b.getID());
        stn.eless(a.getID(), b.getID());
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
        TinyLogger.LogInfo("Adding temporal constraint: "+a.getID()+" ["+min+","+max+"] "+b.getID());
        if (stn.edge_consistent(a.getID(), b.getID(), min, max)) {
            stn.propagate(a.getID(), b.getID(), min, max);
            return true;
        } else {
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
        boolean ret = stn.pless(first.getID(), second.getID());
        TinyLogger.LogInfo("STN: "+first.getID()+" can occour before "+second.getID());
        return ret;
    }

    /**
     *
     * @return
     */
    public TemporalVariable getNewTemporalVariable() {
        // allocate new space if we are running out of it
        /*if(stn.capacity - 1 == stn.top){
         stn = new STN(stn);
         }*/
        TemporalVariable tv = new TemporalVariable();
        int test = stn.add_v();
        tv.mID = test;
        /*if (tv.getID() != test) {
         throw new UnsupportedOperationException("Broken STN indexing.");
         }*/
        EnforceBefore(start, tv);
        EnforceBefore(tv, end);
        return tv;
    }

    /**
     *
     * @return
     */
    public STNManager DeepCopy() {
        STNManager nm = new STNManager();
        nm.end = this.end;
        nm.start = this.start;
        nm.stn = new STN(this.stn);
        return nm;
    }

    public String Report() {
        String ret = "size: "+this.stn.top+"\n";
        int n = this.stn.top;
        for(int i = 0; i < n; i++){
            for(int j = i + 1; j < n; j++){
                if(stn.ga(i, j) != STN.inf || stn.gb(i, j) != STN.sup){
                    ret += i+" ["+stn.ga(i, j)+","+stn.gb(i, j)+"] "+j+"\n";
                }
                if(stn.ga(i, j) > stn.gb(i, j)){
                    throw new FAPEException("Inconsistent STN.");
                }
            }
        }
        return ret;
    }

    public void TestConsistent(){
        int n = this.stn.top;
        for(int i = 0; i < n; i++){
            for(int j = i + 1; j < n; j++){
                if(stn.ga(i, j) > stn.gb(i, j)){
                    throw new FAPEException("Inconsistent STN: "+stn.ga(i, j)+" > "+stn.gb(i, j));
                }
            }
        }
    }
    
    public static void main(String[] args) {
        //self test
        STNManager m = new STNManager();
        m.Init();
        TemporalVariable a = m.getNewTemporalVariable(),
                b = m.getNewTemporalVariable(),
                c = m.getNewTemporalVariable();
        //m.EnforceBefore(a, b);
        m.EnforceConstraint(a, b, 10, 10);
        m.EnforceBefore(b, c);
        m.EnforceBefore(c, a);
        STNManager m2 = m.DeepCopy();
        m2.CanBeBefore(a, b);
        
        
        
        int xx = 0;
    }

    public long GetEarliestStartTime(TemporalVariable start) {
        return stn.ga(0, start.getID());
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
