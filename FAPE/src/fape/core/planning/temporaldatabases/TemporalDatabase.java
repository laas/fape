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

import fape.core.execution.model.Instance;
import fape.core.planning.constraints.ConstraintNetworkManager;
import fape.core.planning.model.ParameterizedStateVariable;
import fape.core.planning.model.VariableRef;
import fape.core.planning.states.State;
import fape.core.planning.stn.TemporalVariable;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.core.planning.temporaldatabases.events.propositional.PersistenceEvent;
import fape.core.planning.temporaldatabases.events.propositional.TransitionEvent;
import fape.core.planning.temporaldatabases.events.resources.ConditionEvent;
import fape.core.planning.temporaldatabases.events.resources.ConsumeEvent;
import fape.core.planning.temporaldatabases.events.resources.ProduceEvent;
import fape.core.planning.temporaldatabases.events.resources.SetEvent;
import fape.exceptions.FAPEException;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * records the events for one state variable
 *
 * @author FD
 */
public class TemporalDatabase {

    public int mID;

    public ParameterizedStateVariable stateVariable;

    public String Report() {
        String ret = "";
        //ret += "{\n";

        ret += "    " + this.stateVariable + "  :  id="+mID+"\n";
        for (ChainComponent c : chain) {
            for (TemporalEvent e : c.contents) {
                ret += "    " + e.Report();
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
            mID = IUnifiable.idCounter++;
        }
    }

    public boolean isConsumer() {
        return !chain.getFirst().GetConsumeValue().isNull();
    }

    /**
     *
     * @param event
     */
    public void AddEvent(TemporalEvent event) {
        //events.add(event);

        if (event instanceof SetEvent) {

        } else if (event instanceof ProduceEvent) {

        } else if (event instanceof ConsumeEvent) {

        } else if (event instanceof ConditionEvent) {

        } else if (event instanceof PersistenceEvent) {
            chain.add(new ChainComponent(event));
        } else if (event instanceof TransitionEvent) {
            chain.add(new ChainComponent(event));
        }
    }

    /**
     *
     * @param m
     * @return
     */
    public TemporalDatabase DeepCopy(ConstraintNetworkManager m) {
        TemporalDatabase newDB = new TemporalDatabase(false);
        newDB.actionAssociations = new HashMap<>(this.actionAssociations);
        newDB.mID = mID;
        newDB.stateVariable = stateVariable;
        for (ChainComponent c : this.chain) {
            newDB.chain.add(c.DeepCopy(m));
        }

        // set the mDatabase variables in the events
        for (ChainComponent c : newDB.chain) {
            c.SetDatabase(newDB);
        }
        return newDB;
    }

    /**
     *
     * @param first
     * @param second
     * @param st
     */
    public static void PropagatePrecedence(ChainComponent first, ChainComponent second, State st) {
        for (TemporalEvent f : first.contents) {
            for(TemporalEvent s : second.contents){
                st.tempoNet.EnforceBefore(f.end, s.start);
            }
        }
    }

    public ChainComponent GetChainComponent(int precedingChainComponent) {
        return chain.get(precedingChainComponent);
    }

    public HashMap<Integer, String> actionAssociations = new HashMap<>();
    
    public void AddActionParam(int mID, Instance instanceOfTheParameter) {
        actionAssociations.put(mID, instanceOfTheParameter.name);
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
        public LinkedList<TemporalEvent> contents = new LinkedList<>();

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
        public ChainComponent(TemporalEvent ev) {
            contents.add(ev);
            if (ev instanceof PersistenceEvent) {
                change = false;
            }
        }

        private ChainComponent() {

        }

        /**
         *
         * @return
         */
        public VariableRef GetSupportValue() {
            if (change) {
                return ((TransitionEvent) contents.get(0)).to;
            } else {
                return ((PersistenceEvent) contents.get(0)).value;
            }
        }

        /**
         *
         * @return
         */
        public VariableRef GetConsumeValue() {
            if (change) {
                return ((TransitionEvent) contents.get(0)).from;
            } else {
                return ((PersistenceEvent) contents.get(0)).value;
            }
        }

        /**
         *
         * @return
         */
        public TemporalVariable GetConsumeTimePoint() {
            return this.contents.getFirst().start;
        }

        /**
         *
         * @return
         */
        public TemporalVariable GetSupportTimePoint() {
            return this.contents.getLast().end;
        }

        public ChainComponent DeepCopy(ConstraintNetworkManager m) {
            ChainComponent cp = new ChainComponent();
            cp.change = this.change;
            cp.contents = new LinkedList<>();
            for (TemporalEvent e : this.contents) {
                cp.contents.add(e.DeepCopy(false));
            }
            return cp;
        }

        private void SetDatabase(TemporalDatabase db) {
            for (TemporalEvent e : contents) {
                e.tdbID = db.mID;
            }
        }
    }

    /**
     *
     * @return
     */
    public boolean HasSinglePersistence() {
        return chain.size() == 1 && !chain.get(0).change;
    }

    /**
     *
     * @return
     */
    public VariableRef GetGlobalSupportValue() {
        return chain.getLast().GetSupportValue();
    }

    /**
     *
     * @return
     */
    public VariableRef GetGlobalConsumeValue() {
        return chain.getFirst().GetConsumeValue();
    }

    /**
     *
     * @return
     */
    public TemporalVariable GetConsumeTimePoint() {
        return chain.getFirst().GetConsumeTimePoint();
    }

    /**
     *
     * @return
     */
    public TemporalVariable GetSupportTimePoint() {
        return chain.getLast().GetSupportTimePoint();
    }

    @Override
    public String toString() {
        String res = "(tdb:" + mID + " dom=[" + this.stateVariable + "] chains=[";

        for (ChainComponent comp : this.chain) {
            for (TemporalEvent ev : comp.contents) {
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
