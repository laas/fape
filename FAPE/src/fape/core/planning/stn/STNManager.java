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
    List<TemporalVariable> variables = new LinkedList<>();

    public STNManager() {
        STN.precalc_inic();
        start = new TemporalVariable(); //0
        end = new TemporalVariable(); //1
        if (start.getID() != 0) {
            throw new FAPEException("STN: Broken indexing.");
        }
        EnforceBefore(start, end);
    }

    public final void EnforceBefore(TemporalVariable a, TemporalVariable b) {
        stn.eless(a.getID(), b.getID());
    }

    public TemporalVariable getNewTemporalVariable() {
        // allocate new space if we are running out of it
        if(stn.capacity - 1 == stn.top){
            stn = new STN(stn);
        }
        TemporalVariable tv = new TemporalVariable();
        EnforceBefore(start, tv);
        EnforceBefore(tv, end);
        return tv;
    }
}
