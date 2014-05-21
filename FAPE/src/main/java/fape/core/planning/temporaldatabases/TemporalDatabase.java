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
package fape.core.planning.temporaldatabases;

import fape.exceptions.FAPEException;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.VarRef;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.Persistence;

import java.lang.Deprecated;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * records the events for one state variable
 *
 * @author FD
 */
public class TemporalDatabase {

    private static int nextID = 0;
    public final int mID;

    public final ParameterizedStateVariable stateVariable;

    public final LinkedList<ChainComponent> chain = new LinkedList<>();

    public TemporalDatabase(LogStatement s) {
        chain.add(new ChainComponent(s));
        mID = nextID++;
        stateVariable = s.sv();
    }

    public TemporalDatabase(ParameterizedStateVariable sv) {
        mID = nextID++;
        stateVariable = sv;
    }

    public TemporalDatabase(TemporalDatabase toCopy) {
        mID = toCopy.mID;
        for(ChainComponent cc : toCopy.chain) {
            chain.add(cc.DeepCopy());
        }
        stateVariable = toCopy.stateVariable;
    }

    public boolean contains(LogStatement s) {
        for(ChainComponent cc : chain) {
            if(cc.contains(s)) {
                return true;
            }
        }
        return false;
    }

    public String Report() {
        String ret = "";
        //ret += "{\n";

        ret += "    " + this.stateVariable + "  :  id="+mID+"\n";
        for (ChainComponent c : chain) {
            for (LogStatement e : c.contents) {
                ret += "    " + e;
            }
            ret += "\n";
        }

        //ret += "}\n";
        return ret;
    }

    /**
     * @return True if the first statement of this TDB requires support (ie not an assignment)
     */
    public boolean isConsumer() {
        return chain.getFirst().contents.getFirst().needsSupport();
    }

    /**
     *
     * @param event
     */
    public void AddEvent(LogStatement event) {
        chain.add(new ChainComponent(event));
    }

    /**
     * @return
     */
    public TemporalDatabase DeepCopy() {
        return new TemporalDatabase(this);
    }


    /**
     * @return The end time point of the last component inducing a change.
     */
    public TPRef getSupportTimePoint() {
        assert getSupportingComponent() != null : "This database appears to be containing only a persitence. " +
                "Hence it not available for support. " + this.toString();
        return getSupportingComponent().getSupportTimePoint();
    }

    public TPRef getConsumeTimePoint() {
        return chain.getFirst().getConsumeTimePoint();
    }

    /**
     * @return The last component of the database containing a change (i.e. an assignment
     * or a transition). It returns null if no such element exists.
     */
    public ChainComponent getSupportingComponent() {
        for(int i=chain.size()-1 ; i>=0 ; i--) {
            if(chain.get(i).change)
                return chain.get(i);
        }
        return null;
    }

    public ChainComponent GetChainComponent(int precedingChainComponent) {
        return chain.get(precedingChainComponent);
    }



    /**
     * @return A global variable representing the value at the end of the temporal database
     */
    public boolean HasSinglePersistence() {
        return chain.size() == 1 && !chain.get(0).change;
    }

    /**
     * @return A global variable representing the value at the end of the temporal database
     */
    public VarRef GetGlobalSupportValue() {
        return chain.getLast().GetSupportValue();
    }

    /**
     *
     * @return
     */
    public VarRef GetGlobalConsumeValue() {
        return chain.getFirst().GetConsumeValue();
    }

    @Override
    public String toString() {
        String res = "(tdb:" + mID + " dom=[" + this.stateVariable + "] chains=[";

        for (ChainComponent comp : this.chain) {
            for (LogStatement ev : comp.contents) {
                res += ev.toString() + ", ";
            }
        }
        res += "])";

        return res;
    }

    /**
     * Checks if there is not two persistence events following each other in the chain.
     */
    public void CheckChainComposition() {
        boolean wasPreviousTransition = true;
        for(ChainComponent cc : this.chain) {
            if(!wasPreviousTransition && ! cc.change) {
                throw new FAPEException("We have two persistence events following each other.");
            }
            wasPreviousTransition = cc.change;
        }
    }
}
