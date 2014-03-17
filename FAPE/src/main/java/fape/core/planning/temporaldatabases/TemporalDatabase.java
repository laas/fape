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
import planstack.anml.model.VarRef;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.Persistence;

import java.lang.Deprecated;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * records the events for one state variable
 *
 * @author FD
 */
public class TemporalDatabase {

    @Deprecated
    public int mID;

    public ParameterizedStateVariable stateVariable;

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
     *
     * @param assignNewUniqueID
     */
    public TemporalDatabase(boolean assignNewUniqueID) {
        if (assignNewUniqueID) {
            throw new FAPEException("No way to create a new unique ID");
        }
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
        TemporalDatabase newDB = new TemporalDatabase(false);
        newDB.mID = mID;
        newDB.stateVariable = stateVariable;
        for (ChainComponent c : this.chain) {
            newDB.chain.add(c.DeepCopy());
        }
        return newDB;
    }

    /** TODO: Recreate
     *
     * @param first
     * @param second
     * @param st
     *
    public static void PropagatePrecedence(ChainComponent first, ChainComponent second, State st) {
        for (LogStatement f : first.contents) {
            for(LogStatement s : second.contents){
                st.tempoNet.EnforceBefore(f.end(), s.start());
            }
        }
    }*/

    public ChainComponent GetChainComponent(int precedingChainComponent) {
        return chain.get(precedingChainComponent);
    }

    /**
     *
     */
    public class ChainComponent {

        @Override
        public String toString() {
            return contents.toString();
        }

        /**
         *
         */
        public boolean change = true;

        /**
         *
         */
        public LinkedList<LogStatement> contents = new LinkedList<>();

        /**
         * Add all events of the parameterized chain component to the current chain component.
         * @param cc
         */
        public void Add(ChainComponent cc) {
            assert cc.change == this.change : "Error: merging transition and persistence events in the same chain component.";
            contents.addAll(cc.contents);
        }

        /**
         *
         * @param ev
         */
        public ChainComponent(LogStatement ev) {
            contents.add(ev);
            if (ev instanceof Persistence) {
                change = false;
            }
        }

        private ChainComponent() {

        }

        /**
         *
         * @return
         */
        public VarRef GetSupportValue() {
            return contents.getFirst().endValue();
        }

        /**
         *
         * @return
         */
        public VarRef GetConsumeValue() {
            return contents.getFirst().startValue();
        }

        public ChainComponent DeepCopy() {
            ChainComponent cp = new ChainComponent();
            cp.change = this.change;
            cp.contents = new LinkedList<>(this.contents);
            return cp;
        }
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

    //public void ReduceDomainByObjectConstants
    //List<TemporalEvent> events = new LinkedList<>();
    /**
     *
     */
    public LinkedList<ChainComponent> chain = new LinkedList<>();

    /**
     * Check if there is not two persistence events following each other in the chain.
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
