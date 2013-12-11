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


import fape.core.planning.model.StateVariable;
import fape.core.planning.states.State;
import fape.core.planning.stn.TemporalVariable;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.core.planning.temporaldatabases.events.propositional.PersistenceEvent;
import fape.core.planning.temporaldatabases.events.propositional.TransitionEvent;
import fape.core.planning.temporaldatabases.events.resources.ConditionEvent;
import fape.core.planning.temporaldatabases.events.resources.ConsumeEvent;
import fape.core.planning.temporaldatabases.events.resources.ProduceEvent;
import fape.core.planning.temporaldatabases.events.resources.SetEvent;
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
public class TemporalDatabase {

    private static int idCounter = 0;

    /**
     *
     */
    public int mID;

    /**
     *
     */
    public TemporalDatabase() {
        mID = idCounter++;
    }

    /**
     *
     * @param noCount
     */
    public TemporalDatabase(TemporalDatabase noCount) {
        mID = noCount.mID;
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
     * @return
     */
    public TemporalDatabase DeepCopy() {
        TemporalDatabase newDB = new TemporalDatabase(this);
        newDB.domain = new LinkedList(this.domain);
        for (ChainComponent c : this.chain) {
            newDB.chain.add(c.DeepCopy());
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
     *
     */
    public class ChainComponent {

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
        public String GetSupportValue() {
            if (change) {
                return ((TransitionEvent) contents.get(0)).to.value;
            }else{
                return((PersistenceEvent) contents.get(0)).value.value;
            }
        }

        /**
         *
         * @return
         */
        public String GetConsumeValue() {
            if (change) {
                return ((TransitionEvent) contents.get(0)).to.value;
            }
            return null;
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

        private ChainComponent DeepCopy() {
            ChainComponent cp = new ChainComponent();
            cp.change = this.change;
            cp.contents = new LinkedList<>(this.contents);
            return cp;
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
    public String GetGlobalSupportValue() {
        return chain.getLast().GetSupportValue();
    }

    /**
     *
     * @return
     */
    public String GetGlobalConsumeValue() {
        return chain.getFirst().GetSupportValue();
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

    public String toString() {
        String res =  "(tdb:" + mID + " dom=[";
        for(StateVariable d : this.domain) {
            res += d.name + " ";
        }
        res += "] chains=[";

        for(ChainComponent comp : this.chain) {
            for(TemporalEvent ev : comp.contents) {
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
    //List<TemporalEvent> events = new LinkedList<>();

    /**
     *
     */
        public LinkedList<ChainComponent> chain = new LinkedList<>();
}
