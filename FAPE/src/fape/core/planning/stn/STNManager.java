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
        return stn.pless(first.getID(), second.getID());
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
}
