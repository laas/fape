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
package fape.core.planning.states;

import fape.core.planning.constraints.ConstraintNetworkManager;
import fape.core.planning.stn.STNManager;
import fape.core.planning.tasknetworks.TaskNetworkManager;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.TemporalDatabaseManager;
import fape.exceptions.FAPEException;
import fape.util.Utils;
import planstack.anml.model.*;
import planstack.anml.model.ActRef;
import planstack.anml.model.TPRef;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.time.TemporalAnnotation;
import planstack.anml.model.concrete.StateModifier;
import planstack.anml.model.concrete.TemporalConstraint;
import planstack.anml.model.concrete.TemporalInterval;
import planstack.anml.model.concrete.statements.*;
import planstack.anml.model.concrete.time.TimepointRef;
import scala.Tuple2;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class State {

    private static int idCounter = 0;
    public final int mID = idCounter++;
    /**
     *
     */
    public final TemporalDatabaseManager tdb;

    /**
     *
     */
    public final STNManager tempoNet;

    /**
     *
     */
    public final TaskNetworkManager taskNet;

    /**
     *
     */
    public final List<TemporalDatabase> consumers;
    public final ConstraintNetworkManager conNet;

    public final AnmlProblem pb;
    public int problemRevision = -1;


    /**
     * this constructor is only for the initial state!! other states are
     * constructed from from the existing states
     */
    public State(AnmlProblem pb) {
        this.pb = pb;
        tdb = new TemporalDatabaseManager();
        tempoNet = new STNManager();
        taskNet = new TaskNetworkManager();
        consumers = new LinkedList<>();
        conNet = new ConstraintNetworkManager();
        apply(pb);
    }

    /**
     *
     * @param st
     */
    public State(State st) {
        pb = st.pb;
        problemRevision = st.problemRevision;
        conNet = st.conNet.DeepCopy(); //goes first, since we need to keep track of unifiables
        tempoNet = st.tempoNet.DeepCopy();
        tdb = st.tdb.DeepCopy();
        taskNet = st.taskNet.DeepCopy();

        consumers = new LinkedList<>();
        for (TemporalDatabase sb : st.consumers) {
            consumers.add(this.GetDatabase(sb.mID));
        }
    }

    public boolean isConsistent() {
        return tempoNet.IsConsistent() && conNet.isConsistent();
    }

    /**
     *
     * @return
     */
    public float GetCurrentCost() {
        float costs = this.taskNet.GetActionCosts();
        return costs;
    }

    /**
     *
     * @return
     */
    public float GetGoalDistance() {
        float distance = this.consumers.size();
        return distance;
    }

    public String Report() {
        String ret = "";
        ret += "{\n";
        ret += "  state[" + mID + "]\n";
        ret += "  cons: " + conNet.Report() + "\n";
        //ret += "  stn: " + this.tempoNet.Report() + "\n";
        ret += "  consumers: " + this.consumers.size() + "\n";
        for (TemporalDatabase b : consumers) {
            ret += b.Report();
        }
        ret += "\n";
        ret += "  tasks: " + this.taskNet.Report() + "\n";
        //ret += "  databases: "+this.tdb.Report()+"\n";

        ret += "}\n";

        return ret;
    }

    public TemporalDatabase GetDatabase(int temporalDatabase) {
        for (TemporalDatabase db : tdb.vars) {
            if (db.mID == temporalDatabase) {
                return db;
            }
        }
        throw new FAPEException("Reference to unknown database.");
    }

    /* TODO: Recreate
    public void FailAction(Integer pop) {
            taskNet.FailAction(pop);
    }*/

    public void SplitDatabase(LogStatement s) {
        TemporalDatabase theDatabase = tdb.getDBContaining(s);

        // First find which component contains s
        ChainComponent comp = null; // component containing the statement
        int ct = 0; // index of the component in the chain
        for(ct=0 ; ct<theDatabase.chain.size() ; ct++) {
            if(theDatabase.chain.get(ct).contains(s)) {
                comp = theDatabase.chain.get(ct);
                break;
            }
        }

        assert comp != null && theDatabase.chain.get(ct) == comp;

        if (s instanceof Transition) {
            if (ct + 1 < theDatabase.chain.size()) {
                //this was not the last element, we need to create another database and make split

                // the two databases share the same state variable
                TemporalDatabase newDB = new TemporalDatabase(theDatabase.stateVariable);

                //add all extra chain components to the new database
                List<ChainComponent> remove = new LinkedList<>();
                for (int i = ct + 1; i < theDatabase.chain.size(); i++) {
                    ChainComponent origComp = theDatabase.chain.get(i);
                    remove.add(origComp);
                    ChainComponent pc = origComp.DeepCopy();
                    newDB.chain.add(pc);
                }
                this.consumers.add(newDB);
                this.tdb.vars.add(newDB);
                theDatabase.chain.remove(comp);
                theDatabase.chain.removeAll(remove);
            } else {
                assert comp.contents.size() == 1;
                //this was the last element so we can just remove it and we are done
                theDatabase.chain.remove(comp);
            }

        } else if (s instanceof Persistence) {
            if (comp.contents.size() == 1) {
                // only one statement, remove the whole component
                theDatabase.chain.remove(comp);
            } else {
                // more than one statement, remove only this statement
                comp.contents.remove(s);
            }
        } else {
            throw new FAPEException("Unknown event type.");
        }
    }

    /**
     * Return all possible values of a global variable.
     * @param var
     * @return
     */
    public Collection<String> possibleValues(VarRef var) {
        assert conNet.contains(var);
        return conNet.domainOf(var);
    }

    /**
     * Returns all possible values of local variable
     * @param locVar Reference to the local variable.
     * @param context Context in which the variables appears (such as action or problem).
     *                This is used to retrieve the type or the global variable linked to the local var.
     * @return
     */
    public Collection<String> possibleValues(LVarRef locVar, AbstractContext context) {
        Tuple2<String, VarRef> def = context.getDefinition(locVar);
        if(def._2().isEmpty()) {
            return pb.instances().jInstancesOfType(def._1());
        } else {
            return possibleValues(def._2());
        }
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public boolean Unifiable(TemporalDatabase a, TemporalDatabase b) {
        return Unifiable(a.stateVariable, b.stateVariable);
    }

    /**
     * Returns true if two state variables are unifiable (ie: they are on the same function
     * and their variables are unifiable).
     * @param a
     * @param b
     * @return
     */
    public boolean Unifiable(ParameterizedStateVariable a, ParameterizedStateVariable b) {
        if(a.func().equals(b.func())) {
            return Unifiable(a.jArgs(), b.jArgs());
        } else {
            return false;
        }
    }

    public boolean Unifiable(List<VarRef> as, List<VarRef> bs) {
        assert as.size() == bs.size() : "The two collections have different size.";
        for(int i=0 ; i<as.size() ; i++) {
            if(!Unifiable(as.get(i), bs.get(i)))
                return false;
        }
        return true;
    }

    /**
     * Return true if the two variables are unifiable (ie: share at least one value)
     * @param a
     * @param b
     * @return
     */
    public boolean Unifiable(VarRef a, VarRef b) {
        return Utils.nonEmptyIntersection(possibleValues(a), possibleValues(b));
    }

    /**
     * Returns true if the statement s can be an enabler for the database db.
     *
     * Its means e has to be a transition event and that both state variables and the
     * consume/produce values must be unifiable.
     * @param s The logical statement (enabler)
     * @param db the temporal database (to be enabled)
     */
    public boolean canBeEnabler(LogStatement s, TemporalDatabase db) {
        boolean canSupport = s instanceof Transition || s instanceof Assignment;
        canSupport = canSupport && Unifiable(s.sv(), db.stateVariable);
        canSupport = canSupport && Unifiable(s.endValue(), db.GetGlobalConsumeValue());
        return canSupport;
    }

    public TPRef getTimePoint(StateModifier mod, TimepointRef ref) {
        TemporalInterval interval = null;
        if(ref.id() instanceof ActRef) {
            interval = taskNet.GetAction((ActRef) ref.id());
        } else if(ref.id().isEmpty()) {
            interval = mod.container();
        } else {
            throw new FAPEException("Unsupported: time point extraction on something other than action.");
        }

        switch (ref.extractor()) {
            case "GStart":
                return pb.start();
            case "GEnd":
                return pb.end();
            case "start":
                return interval.start();
            case "end":
                return interval.end();
            default:
                throw new FAPEException("Unknown extractor: "+ref);
        }
    }

    public boolean insert(Action act) {
        recordTimePoints(act);
        taskNet.insert(act);
        return apply(act);
    }

    public void recordTimePoints(TemporalInterval interval) {
        tempoNet.recordTimePoint(interval.start());
        tempoNet.recordTimePoint(interval.end());
    }

    public void apply(AnmlProblem pb) {
        recordTimePoints(pb);

        for(StateModifier mod : pb.jModifiers()) {
            apply(mod);
        }
    }

    /**
     * Inserts a new temporal statement in the State.
     * @param ts
     * @return
     */
    public boolean apply(StateModifier mod, TemporalStatement ts) {
        recordTimePoints(ts.statement());

        TemporalDatabase db = tdb.GetNewDatabase(ts.statement());

        TPRef containerStart = getTimePoint(mod, ts.interval().start().timepoint());
        TPRef containerEnd = getTimePoint(mod, ts.interval().end().timepoint());

        tempoNet.EnforceConstraint(containerStart, ts.statement().start(), ts.interval().start().delta(), ts.interval().start().delta());
        tempoNet.EnforceConstraint(containerEnd, ts.statement().end(), ts.interval().end().delta(), ts.interval().end().delta());

        if(db.isConsumer()) {
            consumers.add(db);
        }
        return tdb.vars.add(db);
    }

    public boolean apply(StateModifier mod, TemporalConstraint tc) {
        TPRef tp1 = getTimePoint(mod, tc.tp1());
        TPRef tp2 = getTimePoint(mod, tc.tp2());

        switch (tc.op()) {
            case "<":
                tempoNet.EnforceDelay(tp1, tp2, tc.plus());
                break;
            case "=":
                tempoNet.EnforceConstraint(tp1, tp2, tc.plus(), tc.plus());
        }

        return tempoNet.IsConsistent();
    }

    public boolean apply(StateModifier mod) {
        // for every instance declaration, create a new CSP Var with itself as domain
        for(String instance : mod.jInstances()) {
            List<String> domain = new LinkedList<>();
            domain.add(instance);
            conNet.AddVariable(new VarRef(instance), domain);
        }

        // Declare new variables to the constraint network.
        for(Tuple2<String, VarRef> declaration : mod.jVars()) {
            Collection<String> domain = pb.instances().jInstancesOfType(declaration._1());
            conNet.AddVariable(declaration._2(), domain);
        }

        for(TemporalStatement ts : mod.jStatements()) {
            apply(mod, ts);
        }

        for(TemporalConstraint tc : mod.jTemporalConstraints()) {
            apply(mod, tc);
        }

        return this.isConsistent();
    }
}
