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
import fape.util.Pair;
import fape.util.TinyLogger;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public abstract class STNManager {

    public static STNManager newInstance() {
        return new STNManagerPlanStack();
    }
    TemporalVariable start, end; //global start and end of the world   
    //List<TemporalVariable> variables = new LinkedList<>();

    /**
     *
     */
    abstract public void Init();

    /**
     *
     * @param a
     * @param b
     */
    abstract public void EnforceBefore(TemporalVariable a, TemporalVariable b) ;

    /**
     *
     * @param a
     * @param b
     * @param min
     * @param max
     * @return
     */
    abstract public boolean EnforceConstraint(TemporalVariable a, TemporalVariable b, int min, int max) ;

    public boolean RemoveConstraint(int u, int v) {
        throw new RuntimeException("Not Implemented");
    }

    public boolean RemoveConstraints(Pair<Integer, Integer>... ps) {
        throw new RuntimeException("Not Implemented");
    }
    /**
     *
     * @param first
     * @param second
     * @return
     */
    abstract public boolean CanBeBefore(TemporalVariable first, TemporalVariable second) ;

    /**
     *
     * @return
     */
    abstract public TemporalVariable getNewTemporalVariable() ;

    /**
     *
     * @return
     */
    abstract public STNManager DeepCopy();

    abstract public String Report();

    abstract public void TestConsistent();

    abstract public long GetEarliestStartTime(TemporalVariable start);

    abstract public TemporalVariable GetGlobalStart();

    abstract public TemporalVariable GetGlobalEnd();

    abstract public boolean IsConsistent();

    public abstract void OverrideConstraint(TemporalVariable GetGlobalStart, TemporalVariable end, int realEndTime, int realEndTime0);
}
