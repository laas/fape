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

import fape.core.planning.bindings.ObjectVariable;
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
import java.util.LinkedList;

import java.util.List;

/**
 * records the events for one state variable
 *
 * @author FD
 */
public class TemporalDatabase {

    private static int idCounter = 0;
    public int mID;

    public TemporalDatabase() {
        mID = idCounter++;
    }

    public TemporalDatabase(TemporalDatabase noCount) {
        mID = noCount.mID;
    }

    public static boolean Unifiable(TemporalDatabase db, TemporalDatabase b) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

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

    public TemporalDatabase DeepCopy() {
        TemporalDatabase newDB = new TemporalDatabase(this);
        newDB.domain = new LinkedList(this.domain);
        for (ChainComponent c : this.chain) {
            newDB.chain.add(c.DeepCopy());
        }
        return newDB;
    }

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

    public class ChainComponent {

        public boolean change = true;
        public LinkedList<TemporalEvent> contents = new LinkedList<>();

        public void Add(ChainComponent e) {
            contents.addAll(e.contents);
        }

        public ChainComponent(TemporalEvent ev) {
            contents.add(ev);
            if (ev instanceof PersistenceEvent) {
                change = false;
            }
        }

        private ChainComponent() {

        }

        public String GetSupportValue() {
            if (change) {
                return ((TransitionEvent) contents.get(0)).to.value;
            }
            return null;
        }

        public String GetConsumeValue() {
            if (change) {
                return ((TransitionEvent) contents.get(0)).to.value;
            }
            return null;
        }

        public TemporalVariable GetConsumeTimePoint() {
            return this.contents.getFirst().start;
        }

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

    public boolean HasSinglePersistence() {
        return chain.size() == 1 && !chain.get(0).change;
    }

    public String GetGlobalSupportValue() {
        return chain.getLast().GetSupportValue();
    }

    public String GetGlobalConsumeValue() {
        return chain.getFirst().GetSupportValue();
    }

    public TemporalVariable GetConsumeTimePoint() {
        return chain.getFirst().GetConsumeTimePoint();
    }

    public TemporalVariable GetSupportTimePoint() {
        return chain.getLast().GetSupportTimePoint();
    }

    //public ObjectVariable var;
    public List<StateVariable> domain = new LinkedList<>();
    //List<TemporalEvent> events = new LinkedList<>();
    public LinkedList<ChainComponent> chain = new LinkedList<>();
}
