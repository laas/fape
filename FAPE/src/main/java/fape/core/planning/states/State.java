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

import fape.core.planning.constraints.ConservativeConstraintNetwork;
import fape.core.planning.constraints.ConstraintNetwork;
import fape.core.planning.search.*;
import fape.core.planning.stn.STNManager;
import fape.core.planning.tasknetworks.TaskNetworkManager;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.TemporalDatabaseManager;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import fape.util.Reporter;
import planstack.anml.model.*;
import planstack.anml.model.concrete.*;
import planstack.anml.model.concrete.statements.*;
import scala.Tuple2;
import scala.collection.immutable.HashMap;

import java.util.*;

/**
 *
 * @author FD
 */
public class State implements Reporter {

    private static int idCounter = 0;

    /**
     * Unique identifier of the database.
     */
    public final int mID = idCounter++;

    /** Depth of the state in the search tree */
    public final int depth;

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
     * Keep tracks of statements that must be supported by a particular decomposition.
     * (e.g. by a statements which is a consequence of that decomposition).
     * This map is populated when a decomposition is chosen as a resolver for an unsupported database.
     */
    private LinkedList<Pair<Integer, Decomposition>> supportConstraints;

    /**
     * All databases that require an enabling event (ie. whose first value is not an assignment).
     */
    public final List<TemporalDatabase> consumers;
    public final ConstraintNetwork conNet;

    public final AnmlProblem pb;

    /**
     * Index of the latest applied StateModifier in pb.jModifiers()
     */
    private int problemRevision;


    /**
     * this constructor is only for the initial state!! other states are
     * constructed from from the existing states
     */
    public State(AnmlProblem pb) {
        this.pb = pb;
        depth = 0;
        tdb = new TemporalDatabaseManager();
        tempoNet = new STNManager();
        taskNet = new TaskNetworkManager();
        consumers = new LinkedList<>();

        conNet = new ConservativeConstraintNetwork();
//        conNet = new ConstraintNetworkManager();
        supportConstraints = new LinkedList<>();

        // Insert all problem-defined modifications into the state
        problemRevision = -1;
        update();
    }

    /**
     * Produces a new State with the same content as state in parameter.
     * @param st State to copy
     */
    public State(State st) {
        pb = st.pb;
        depth = st.depth +1;
        problemRevision = st.problemRevision;
        conNet = st.conNet.DeepCopy();
        tempoNet = st.tempoNet.DeepCopy();
        tdb = st.tdb.DeepCopy();
        taskNet = st.taskNet.DeepCopy();
        supportConstraints = new LinkedList<>(st.supportConstraints);

        consumers = new LinkedList<>();
        for (TemporalDatabase sb : st.consumers) {
            consumers.add(this.GetDatabase(sb.mID));
        }
    }

    /**
     * @return True if the state is consistent (ie. stn and bindings consistent), False otherwise.
     */
    public boolean isConsistent() {
        return tempoNet.IsConsistent() && conNet.isConsistent();
    }

    /**
     * @return the sum of all actions cost.
     */
    public float GetCurrentCost() {
        return this.taskNet.GetAllActions().size() * 10;
    }

    /**
     * @return A rough estimation of the search distance to a state with no consumer
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

    /**
     * Retrieve the Database with the same ID.
     * @param dbID ID of the database to lookup
     * @return The database with the same ID.
     */
    public TemporalDatabase GetDatabase(int dbID) {
        for (TemporalDatabase db : tdb.vars) {
            if (db.mID == dbID) {
                return db;
            }
        }
        throw new FAPEException("Reference to unknown database.");
    }

    /**
     * @param s a logical statement to look for.
     * @return the Action containing s. Returns null if no action containing s was found.
     */
    public Action getActionContaining(LogStatement s) {
        for(Action a : taskNet.GetAllActions()) {
            if(a.contains(s)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Marks an action as being currently executed.
     * @param actRef Reference of the action to update.
     */
    public void setActionExecuting(ActRef actRef) {
        taskNet.GetAction(actRef).setStatus(ActionStatus.EXECUTING);
    }

    /**
     * Marks an action as executed (ie. was carried out with success).
     * @param actRef Reference to the action to update.
     */
    public void setActionSuccess(ActRef actRef) {
        taskNet.GetAction(actRef).setStatus(ActionStatus.EXECUTED);
    }

    /**
     * Marks an action as failed. All statement of the action are removed from this state.
     * @param actRef Reference of the action to update.
     */
    public void setActionFailed(ActRef actRef) {
        Action toRemove = taskNet.GetAction(actRef);
        toRemove.setStatus(ActionStatus.FAILED);

        for (LogStatement s : toRemove.logStatements()) {
            removeStatement(s);
        }
    }

    /**
     * Remove a statement from the state.
     * It does so by identifying the temporal database in which the statement appears and removing
     * it from the database. If necessary, the database is split in two.
     * @param s Statement to remove.
     */
    public void removeStatement(LogStatement s) {
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
     * @param var Reference to a global variable
     * @return All possible values for this variable.
     */
    public Collection<String> possibleValues(VarRef var) {
        assert conNet.contains(var) : "Constraint Network doesn't contains "+var;
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
     * @return True if both TemporalDatabases might be unifiable (ie. the refer to two unifiable state variables).
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

    /**
     * Tests Unifiability of a sequence of global variables.
     * The two lists must be of the same size.
     *
     * @return True if, for all i in 0..size(as), as[i] and bs[i] are unifiable.
     */
    public boolean Unifiable(List<VarRef> as, List<VarRef> bs) {
        assert as.size() == bs.size() : "The two lists have different size.";
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
        return conNet.unifiable(a, b);
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

    /**
     * Insert an action into the state, applying all needed modifications.
     * @param act Action to insert.
     * @return True if the resulting state is consistent, false otherwise.
     */
    public boolean insert(Action act) {
        recordTimePoints(act);
        tempoNet.EnforceBefore(pb.earliestExecution(), act.start());
        tempoNet.EnforceDelay(act.start(), act.end(), 1);
        taskNet.insert(act);
        return apply(act);
    }

    /**
     * Records the start and end timepoint of the given interval in the temporal network manager.
     * It also adds a constraint specifying that start must happen before end.
     */
    public void recordTimePoints(TemporalInterval interval) {
        tempoNet.recordTimePoint(interval.start());
        tempoNet.recordTimePoint(interval.end());
        tempoNet.EnforceBefore(interval.start(), interval.end());
    }

    /**
     * Applies all pending modifications of the problem.
     * A problem comes with a sequence of StateModifiers that depict the
     * current status of the problem.
     * This method simply applies all modifiers that were not previously applied.
     * @return True if the resulting state is consistent, False otherwise.
     */
    public boolean update() {
        if(problemRevision == -1) {
            tempoNet.recordTimePoint(pb.start());
            tempoNet.recordTimePoint(pb.end());
            tempoNet.recordTimePoint(pb.earliestExecution());
            tempoNet.EnforceBefore(pb.start(), pb.earliestExecution());

        }
        for(int i=problemRevision+1 ; i<pb.modifiers().size() ; i++) {
            apply(pb.modifiers().get(i));
            problemRevision = i;
        }

        return isConsistent();
    }

    /**
     * Inserts a logical statement into a state
     * @param s Statement to insert
     * @return True if the resulting state is consistent
     */
    public boolean apply(LogStatement s) {
        recordTimePoints(s);

        TemporalDatabase db = new TemporalDatabase(s);

        if(db.isConsumer()) {
            consumers.add(db);
        }
        tdb.vars.add(db);

        return isConsistent();
    }

    /**
     * Inserts a resource statement into a state.
     * @param s Statement to insert
     * @return True if the resulting state is consistent.
     */
    public boolean apply(ResourceStatement s) {
        recordTimePoints(s);

        throw new UnsupportedOperationException("Resource statements are not supported yet.");
    }

    /**
     * Inserts a statement into a state
     * @param mod StateModifier in which the statement appears
     * @param s Statement to insert
     * @return True if the resulting state is consistent
     */
    public boolean apply(StateModifier mod, Statement s) {
        if(s instanceof LogStatement)
            return apply((LogStatement) s);
        else if(s instanceof ResourceStatement)
            return apply((ResourceStatement) s);
        else
            throw new FAPEException("Unsupported statement: "+s);
    }

    /**
     * Applies the modification implied by a temporal constraint.
     * All time points referenced in the constraint must have been previously recorded in the STN.
     *
     * @param mod StateModifier in which the constraint appears.
     * @param tc The TemporalConstraint to insert.
     * @return True if the resulting state is consistent, False otherwise.
     */
    public boolean apply(StateModifier mod, TemporalConstraint tc) {
        TPRef tp1 = tc.tp1();
        TPRef tp2 = tc.tp2();

        switch (tc.op()) {
            case "<":
                // tp1 < tp2 + x => tp1 --[-x, inf] --> tp2
                tempoNet.EnforceDelay(tp1, tp2, - tc.plus());
                break;
            case "=":
                // tp1 --- [x, x] ---> tp2
                tempoNet.EnforceConstraint(tp2, tp1, tc.plus(), tc.plus());
        }

        return isConsistent();
    }

    /**
     * Applies the given decomposition to the current state.
     * It mainly consists in inserting the decomposition's timepoints and link them to the containing action.
     * Then the state modifier is applied.
     * @param dec Decomposition to insert
     * @return True if the resulting state is consistent.
     */
    public boolean applyDecomposition(Decomposition dec) {
        recordTimePoints(dec);

        // interval of the decomposition is equal to the one of the containing action.
        tempoNet.EnforceConstraint(dec.start(), dec.container().start(), 0, 0);
        tempoNet.EnforceConstraint(dec.end(), dec.container().end(), 0, 0);

        taskNet.insert(dec, dec.container());

        return apply(dec);
    }

    /**
     * Applies all modifications stated in a StateModifier in this this State
     * @param mod StateModifier to apply
     * @return True if the resulting State is consistent, False otherwise.
     */
    public boolean apply(StateModifier mod) {
        // for every instance declaration, create a new CSP Var with itself as domain
        for(String instance : mod.instances()) {
            conNet.addPossibleValue(instance);
            List<String> domain = new LinkedList<>();
            domain.add(instance);
            conNet.AddVariable(pb.instances().referenceOf(instance), domain, pb.instances().typeOf(instance));
        }

        // Declare new variables to the constraint network.
        for(Tuple2<String, VarRef> declaration : mod.vars()) {
            Collection<String> domain = pb.instances().jInstancesOfType(declaration._1());
            conNet.AddVariable(declaration._2(), domain, declaration._1());
        }

        for(Statement ts : mod.statements()) {
            apply(mod, ts);
        }

        for(Action act : mod.actions()) {
            insert(act);
        }

        for(TemporalConstraint tc : mod.temporalConstraints()) {
            apply(mod, tc);
        }

        return isConsistent();
    }

    /**
     * Given a flaw and a set of resolvers, retain only the valid resolvers.
     * It is currently used to filter out the resolvers of flaws that have partially addressed by
     * an action decomposition.
     * @param f The flaw for which the resolvers are emitted.
     * @param opts The  set of resolvers to address the flaw
     * @return A list of resolvers containing only the valid ones.
     */
    public List<SupportOption> retainValidOptions(Flaw f, List<SupportOption> opts) {
        if(f instanceof UndecomposedAction || f instanceof Threat || f instanceof UnboundVariable) {
            return opts;
        } else if(f instanceof UnsupportedDatabase) {
            Decomposition mustDeriveFrom = getSupportConstraint(((UnsupportedDatabase) f).consumer);
            if(mustDeriveFrom == null) {
                return opts;
            } else {
                List<SupportOption> retained = new LinkedList<>();
                for(SupportOption opt : opts) {
                    if(isOptionDerivedFrom(opt, mustDeriveFrom)) {
                        retained.add(opt);
                    }
                }
                return retained;
            }
        } else {
            throw new FAPEException("Error: Unrecognized flaw type.");
        }
    }

    /**
     * Checks if there is a support constraint for a temporal database.
     * @param db The temporal database that needs to be supported.
     * @return A decomposition if the resolvers for this database must derive from a decomposition
     *         (e.g. this flaw was previously addressed by decomposing a method. null if there is no constraints.
     */
    private Decomposition getSupportConstraint(TemporalDatabase db) {
        int compID = db.GetChainComponent(0).mID;
        Decomposition dec = null;
        for(Pair<Integer, Decomposition> constraint : supportConstraints) {
            if(constraint.value1 == mID)
                dec = constraint.value2;
        }
        return dec;
    }

    /**
     * Checks if the option is a consequence of the given decomposition.
     * A resolver is considered to be derived from an decomposition if (i) it is a
     * decomposition of an action descending from the decomposition. (ii) it is a decomposition
     * of an action descending from the given decomposition.
     *
     * It is currently used to check if a resolver doesn't contradict an earlier commitment:
     * when an unsupported database is "resolved" with a decomposition but no causal link is added
     * (the database is still unsupported), the resolvers for it are then limited to those being a
     * consequence of the decomposition.
     * @param opt Resolver whose validity is to be checked.
     * @param dec Decomposition from which the resolver must be derived.
     * @return True if the resolver is valid, False otherwise.
     */
    private boolean isOptionDerivedFrom(SupportOption opt, Decomposition dec) {
        // this unsupported db must be supported by a descendant of a decomposition
        // this is a consequence of an earlier commitment.
        if(opt.supportingAction != null || opt.actionWithBindings != null) {
            // insertion of a new action is prohibited
            return false;
        } else if(opt.actionToDecompose != null) {
            // decomposition is allowed only if the decomposed action is issued from dec.
            return taskNet.isDescendantOf(opt.actionToDecompose, dec);
        } else if(opt.temporalDatabase != -1) {
            // DB supporters are limited to those coming from an action descending from dec.
            TemporalDatabase db = GetDatabase(opt.temporalDatabase);

            // get the supporting chain component. (must provide a change on the state variable)
            ChainComponent cc;
            if(opt.precedingChainComponent != -1)
                cc = db.GetChainComponent(opt.precedingChainComponent);
            else
                cc = db.getSupportingComponent();

            assert cc != null : "There is no support statement in "+db;
            assert cc.change : "Support is not a change.";
            assert cc.contents.size() == 1;
            Action a = getActionContaining(cc.contents.getFirst());

            return a != null && taskNet.isDescendantOf(a, dec);
        } else {
            throw new FAPEException("Unhandled option: "+opt);
        }
    }

    public void addSupportConstraint(ChainComponent cc, Decomposition dec) {
        this.supportConstraints.add(new Pair(cc.mID, dec));
    }
}
