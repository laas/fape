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
package fape.scripting.stncomparison.fullstn;

import fape.exceptions.FAPEException;

/**
 *
 * @author FD
 */
public class STNManagerOrig {

    public int start;
    STN stn;

    public void Init(int initialCapacity) {
        stn = new STN(initialCapacity);
        if (STN.precalc == null) {
            STN.precalc_inic();
        }
        start = stn.add_v();
        if (start != 0) {
            throw new FAPEException("STN: Broken indexing.");
        }
    }

    /**
     *
     */
    public STNManagerOrig() {

    }

    public int getMax() {
        return stn.top;
    }

    /**
     *
     * @param a
     * @param b
     */
    public final void EnforceBefore(int a, int b) {
        //TinyLogger.LogInfo("Adding temporal constraint: "+a+" < "+b);
        stn.eless(a, b);
    }

    /**
     *
     * @param a
     * @param b
     * @param min
     * @param max
     * @return
     */
    public final boolean EnforceConstraint(int a, int b, int min, int max) {
        //TinyLogger.LogInfo("Adding temporal constraint: "+a+" ["+min+","+max+"] "+b);
        if (stn.edge_consistent(a, b, min, max)) {
            stn.propagate(a, b, min, max);
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
    public final boolean CanBeBefore(int first, int second) {
        boolean ret = stn.pless(first, second);
        //TinyLogger.LogInfo("STN: "+first+" can occour before "+second);        
        return ret;
    }

    public final boolean EdgeConsistent(int a, int b, int min, int max) {
        return stn.edge_consistent(a, b, min, max);
    }

    /**
     *
     * @return
     */
    public int getNewVariable() {
        // allocate new space if we are running out of it
        /*if(stn.capacity - 1 == stn.top){
         stn = new STN(stn);
         }*/
        int tv = stn.add_v();
        /*if (tv != test) {
         throw new UnsupportedOperationException("Broken STN indexing.");
         }*/
        EnforceBefore(start, tv);
        return tv;
    }

    /**
     *
     * @return
     */
    public STNManagerOrig DeepCopy() {
        STNManagerOrig nm = new STNManagerOrig();
        nm.start = this.start;
        nm.stn = new STN(this.stn);
        return nm;
    }

    public String Report() {
        String ret = "size: " + this.stn.top + "\n";
        int n = this.stn.top;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (stn.ga(i, j) != STN.inf || stn.gb(i, j) != STN.sup) {
                    ret += i + " [" + stn.ga(i, j) + "," + stn.gb(i, j) + "] " + j + "\n";
                }
                if (stn.ga(i, j) > stn.gb(i, j)) {
                    throw new FAPEException("Inconsistent STN.");
                }
            }
        }
        return ret;
    }

    public void TestConsistent() {
        int n = this.stn.top;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (stn.ga(i, j) > stn.gb(i, j)) {
                    throw new FAPEException("Inconsistent STN: " + stn.ga(i, j) + " > " + stn.gb(i, j));
                }
            }
        }
    }

    public static void main(String[] args) {
        //self test
        STNManagerOrig m = new STNManagerOrig();
        m.Init(10);
        int a = m.getNewVariable(),
                b = m.getNewVariable(),
                c = m.getNewVariable();
        //m.EnforceBefore(a, b);
        m.EnforceConstraint(a, b, 10, 10);
        m.EnforceBefore(b, c);
        m.EnforceBefore(c, a);
        STNManagerOrig m2 = m.DeepCopy();
        m2.CanBeBefore(a, b);

        int xx = 0;
    }

    public long GetEarliestStartTime(int start) {
        return stn.ga(0, start);
    }

    public int GetGlobalStart() {
        return start;
    }

    public boolean IsConsistent() {
        try {
            TestConsistent();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void OverrideConstraint(int GetGlobalStart, int end, int realEndTime, int realEndTime0) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
