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

import fape.core.planning.constraints.ConstraintNetworkManager;
import fape.core.planning.model.StateVariable;
import fape.core.planning.model.StateVariableValue;
import fape.core.planning.states.State;
import fape.core.planning.stn.TemporalVariable;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.core.planning.temporaldatabases.events.propositional.PersistenceEvent;
import fape.core.planning.temporaldatabases.events.propositional.TransitionEvent;
import fape.core.planning.temporaldatabases.events.resources.ConditionEvent;
import fape.core.planning.temporaldatabases.events.resources.ConsumeEvent;
import fape.core.planning.temporaldatabases.events.resources.ProduceEvent;
import fape.core.planning.temporaldatabases.events.resources.SetEvent;
import fape.util.TinyLogger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import java.util.List;

/**
 * records the events for one state variable
 *
 * @author FD
 */
public class TemporalDatabase extends IUnifiable {

    public String Report(){
        String ret = "";
        //ret += "{\n";
        
        ret += "    "+this.domain+"\n";
        for(ChainComponent c:chain){
            for(TemporalEvent e:c.contents){
                ret += "    "+e.Report();
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
            mID = idCounter++;
        }
    }

    /**
     *
     * @param db
     * @param b
     * @return
     */
    public static boolean Unifiable(TemporalDatabase db, TemporalDatabase b) {
        LinkedList<StateVariable> inter = new LinkedList(db.domain);
        inter.retainAll(b.domain);
        return inter.size() > 0;
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
        newDB.mID = mID;
        newDB.domain = new LinkedList(this.domain);
        for (ChainComponent c : this.chain) {
            newDB.chain.add(c.DeepCopy(m));
        }
        m.AddUnifiable(newDB);
        // set the mDatabase variables in the events
        for(ChainComponent c :newDB.chain){
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
        if (!first.change) {
            for (TemporalEvent e : first.contents) {
                if (!second.change) {
                    for (TemporalEvent e2 : second.contents) {
                        st.tempoNet.EnforceBefore(e.end, e2.start);
                    }
                } else {
                    st.tempoNet.EnforceBefore(e.end, second.GetConsumeTimePoint());
                }
            }
        } else {
            if (!second.change) {
                for (TemporalEvent e2 : second.contents) {
                    st.tempoNet.EnforceBefore(first.GetSupportTimePoint(), e2.start);
                }
            } else {
                st.tempoNet.EnforceBefore(first.GetSupportTimePoint(), second.GetConsumeTimePoint());
            }
        }
    }

    /**
     * reduces the domain, if elements were removed, returns true
     *
     * @param supported
     * @return
     */
    @Override
    public boolean ReduceDomain(HashSet<String> supported) {
        LinkedList<StateVariable> remove = new LinkedList<>();
        for (StateVariable v : domain) {
            if (!supported.contains(v.GetObjectConstant())) {
                remove.add(v);
            }
        }
        if (TinyLogger.logging) {
            if (!remove.isEmpty()) {
                TinyLogger.LogInfo("Reducing domain " + this.mID + " by: " + remove.toString());
            }
        }
        domain.removeAll(remove);
        return !remove.isEmpty();
    }

    @Override
    public List<String> GetDomainObjectConstants() {
        List<String> ret = new LinkedList<>();
        for (StateVariable sv : domain) {
            ret.add(sv.GetObjectConstant());
        }
        return ret;
    }

    @Override
    public int GetUniqueID() {
        return mID;
    }

    @Override
    public boolean EmptyDomain() {
        return domain.isEmpty();
    }

    @Override
    public String Explain() {
        return " tdb";
    }

    public ChainComponent GetChainComponent(int precedingChainComponent) {
        return chain.get(precedingChainComponent);
    }

    /**
     *
     */
    public class ChainComponent {

        @Override
        public String toString() {
            return contents.toString(); //To change body of generated methods, choose Tools | Templates.
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
         *
         * @param e
         */
        public void Add(ChainComponent e) {
            contents.addAll(e.contents);
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
        public StateVariableValue GetSupportValue() {
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
        public StateVariableValue GetConsumeValue() {
            if (change) {
                return ((TransitionEvent) contents.get(0)).from;
            }else{
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

        private ChainComponent DeepCopy(ConstraintNetworkManager m) {
            ChainComponent cp = new ChainComponent();
            cp.change = this.change;
            cp.contents = new LinkedList<>();
            for(TemporalEvent e:this.contents){
                cp.contents.add(e.DeepCopy(m, false));
            }
            return cp;
        }

        private void SetDatabase(TemporalDatabase db) {
            for(TemporalEvent e:contents){
                e.mDatabase = db;
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
    public StateVariableValue GetGlobalSupportValue() {
        return chain.getLast().GetSupportValue();
    }

    /**
     *
     * @return
     */
    public StateVariableValue GetGlobalConsumeValue() {
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
        String res = "(tdb:" + mID + " dom=[";
        for (StateVariable d : this.domain) {
            res += d.name + " ";
        }
        res += "] chains=[";

        for (ChainComponent comp : this.chain) {
            for (TemporalEvent ev : comp.contents) {
                res += ev.toString() + ", ";
            }
        }
        res += "])";

        return res;
    }

    //public ObjectVariable var;
    /**
     *
     */
    public LinkedList<StateVariable> domain = new LinkedList<>();

    //public void ReduceDomainByObjectConstants
    //List<TemporalEvent> events = new LinkedList<>();
    /**
     *
     */
    public LinkedList<ChainComponent> chain = new LinkedList<>();
}
