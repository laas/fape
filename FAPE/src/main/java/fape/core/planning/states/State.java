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

import fape.core.planning.resources.Replenishable;
import fape.core.planning.resources.Resource;
import fape.core.planning.resources.ResourceManager;
import fape.core.planning.search.*;
import fape.core.planning.search.resolvers.*;
import fape.core.planning.stn.STNNodePrinter;
import fape.core.planning.tasknetworks.TaskNetworkManager;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.TemporalDatabaseManager;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import fape.util.Reporter;
import planstack.anml.model.*;
import planstack.anml.model.concrete.*;
import planstack.anml.model.concrete.Decomposition;
import planstack.anml.model.concrete.TemporalConstraint;
import planstack.anml.model.concrete.statements.*;
import planstack.constraints.CSP;
import scala.Tuple2;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
//import scala.collection.immutable.HashMap;

/**
 *
 * @author FD
 */
public class State implements Reporter {



    public float h = -1, g = -1;

    

    private static int idCounter = 0;

    /**
     * Unique identifier of the database.
     */
    public final int mID = idCounter++;

    /**
     * Depth of the state in the search tree
     */
    public final int depth;

    /**
     *
     */
    protected final TemporalDatabaseManager tdb;

    protected final CSP<VarRef,TPRef> csp;

    protected final TaskNetworkManager taskNet;

    protected final ResourceManager resMan;

    /**
     * Keep tracks of statements that must be supported by a particular
     * decomposition. (e.g. by a statements which is a consequence of that
     * decomposition). This map is populated when a decomposition is chosen as a
     * resolver for an unsupported database.
     */
    private LinkedList<Pair<Integer, Decomposition>> supportConstraints;

    /**
     * All databases that require an enabling event (ie. whose first value is
     * not an assignment).
     */
    public final List<TemporalDatabase> consumers;


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
        csp = new CSP<>();
        taskNet = new TaskNetworkManager();
        consumers = new LinkedList<>();
        resMan = new ResourceManager();

        supportConstraints = new LinkedList<>();

        // Insert all problem-defined modifications into the state
        problemRevision = -1;
        update();
    }

    /**
     * Produces a new State with the same content as state in parameter.
     *
     * @param st State to copy
     */
    public State(State st) {
        pb = st.pb;
        depth = st.depth + 1;
        problemRevision = st.problemRevision;
        csp = new CSP<VarRef,TPRef>(st.csp);
        tdb = st.tdb.DeepCopy();
        taskNet = st.taskNet.DeepCopy();
        supportConstraints = new LinkedList<>(st.supportConstraints);
        resMan = st.resMan.DeepCopy();
        consumers = new LinkedList<>();

        for (TemporalDatabase sb : st.consumers) {
            consumers.add(this.GetDatabase(sb.mID));
        }
    }

    /**
     * @return True if the state is consistent (ie. stn and bindings
     * consistent), False otherwise.
     */
    public boolean isConsistent() {
        boolean consistency = true;
        // note that this order is important so that binded constraint can be propagated into the stn
        consistency &= csp.bindings().isConsistent();
        consistency &= csp.stn().IsConsistent();
        consistency &= resMan.isConsistent(this);
        return consistency;
    }

    /**
     * @return the sum of all actions cost.
     */
    public float GetCurrentCost() {
        return this.taskNet.GetAllActions().size() * 10;
    }

    /**
     * @return A rough estimation of the search distance to a state with no
     * consumer
     */
    public float GetGoalDistance() {
        float distance = this.consumers.size();
        return distance;
    }

    public String Report() {
        String ret = "";
        ret += "{\n";
        ret += "  state[" + mID + "]\n";
        ret += "  cons: " + csp.bindings().Report() + "\n";
        //ret += "  stn: " + this.csp.stn().Report() + "\n";
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
     *
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
     * @return the Action containing s. Returns null if no action containing s
     * was found.
     */
    public Action getActionContaining(LogStatement s) {
        for (Action a : taskNet.GetAllActions()) {
            if (a.contains(s)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Marks an action as being currently executed.
     *
     * @param actRef Reference of the action to update.
     */
    private void setActionExecuting(ActRef actRef) {
        taskNet.GetAction(actRef).setStatus(ActionStatus.EXECUTING);
    }

    /**
     * Marks an action as executed (ie. was carried out with success).
     *
     * @param actRef Reference to the action to update.
     */
    private void setActionSuccess(ActRef actRef) {
        taskNet.GetAction(actRef).setStatus(ActionStatus.EXECUTED);
    }

    /**
     * Marks an action as failed. All statement of the action are removed from
     * this state.
     *
     * @param actRef Reference of the action to update.
     */
    public void setActionFailed(ActRef actRef) {
        Action toRemove = taskNet.GetAction(actRef);
        toRemove.setStatus(ActionStatus.FAILED);

        for (LogStatement s : toRemove.logStatements()) {
            // ignore statements on constant functions that are handled through constraints
            if(!s.sv().func().isConstant())
                removeStatement(s);
        }
    }

    /**
     * Remove a statement from the state. It does so by identifying the temporal
     * database in which the statement appears and removing it from the
     * database. If necessary, the database is split in two.
     *
     * @param s Statement to remove.
     */
    private void removeStatement(LogStatement s) {
        TemporalDatabase theDatabase = tdb.getDBContaining(s);

        // First find which component contains s
        ChainComponent comp = null; // component containing the statement
        int ct = 0; // index of the component in the chain
        for (ct = 0; ct < theDatabase.chain.size(); ct++) {
            if (theDatabase.chain.get(ct).contains(s)) {
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
     *
     * @param var Reference to a global variable
     * @return All possible values for this variable.
     */
    public Collection<String> possibleValues(VarRef var) {
        assert csp.bindings().contains(var) : "Constraint Network doesn't contains " + var;
        return csp.bindings().domainOf(var);
    }

    /**
     * Returns all possible values of local variable
     *
     * @param locVar Reference to the local variable.
     * @param context Context in which the variables appears (such as action or
     * problem). This is used to retrieve the type or the global variable linked
     * to the local var.
     * @return
     */
    public Collection<String> possibleValues(LVarRef locVar, AbstractContext context) {
        Tuple2<String, VarRef> def = context.getDefinition(locVar);
        if (def._2().isEmpty()) {
            return pb.instances().jInstancesOfType(def._1());
        } else {
            return possibleValues(def._2());
        }
    }

    /**
     * @return True if both TemporalDatabases might be unifiable (ie. the refer
     * to two unifiable state variables).
     */
    public boolean Unifiable(TemporalDatabase a, TemporalDatabase b) {
        return Unifiable(a.stateVariable, b.stateVariable);
    }

    /**
     * Returns true if two state variables are unifiable (ie: they are on the
     * same function and their variables are unifiable).
     *
     * @param a
     * @param b
     * @return
     */
    public boolean Unifiable(ParameterizedStateVariable a, ParameterizedStateVariable b) {
        if (a.func().equals(b.func())) {
            return Unifiable(a.jArgs(), b.jArgs());
        } else {
            return false;
        }
    }

    /**
     * Tests Unifiability of a sequence of global variables. The two lists must
     * be of the same size.
     *
     * @return True if, for all i in 0..size(as), as[i] and bs[i] are unifiable.
     */
    public boolean Unifiable(List<VarRef> as, List<VarRef> bs) {
        assert as.size() == bs.size() : "The two lists have different size.";
        for (int i = 0; i < as.size(); i++) {
            if (!unifiable(as.get(i), bs.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the statement s can be an enabler for the database db.
     *
     * Its means e has to be a transition event and that both state variables
     * and the consume/produce values must be unifiable.
     *
     * @param s The logical statement (enabler)
     * @param db the temporal database (to be enabled)
     */
    public boolean canBeEnabler(LogStatement s, TemporalDatabase db) {
        boolean canSupport = s instanceof Transition || s instanceof Assignment;
        canSupport = canSupport && Unifiable(s.sv(), db.stateVariable);
        canSupport = canSupport && unifiable(s.endValue(), db.GetGlobalConsumeValue());
        return canSupport;
    }

    /**
     * Insert an action into the state, applying all needed modifications.
     *
     * @param act Action to insert.
     * @return True if the resulting state is consistent, false otherwise.
     */
    public void insert(Action act) {
        recordTimePoints(act);
        csp.stn().EnforceBefore(pb.earliestExecution(), act.start());
        taskNet.insert(act);
        apply(act);

        if(act.minDuration() != null) {
            if(act.minDuration().isFunction()) {
                // create a constraint defined in extension sv(a1,...,an) = tmp
                // add a constraint act.start +tmp < act.end
                // the last one will be propagated in the CSP when tmp is binded in the CSP
                ParameterizedStateVariable sv = act.minDuration().sv();
                assert sv.func().isConstant() : "Cannot parameterize an action duration with non-constant functions.";
                assert sv.func().valueType() == "integer" : "Cannot parameterize an action duration with a non-integer function.";
                VarRef intValue = new VarRef();
                csp.bindings().AddIntVariable(intValue);
                List<VarRef> varsOfExtConst = new LinkedList<>(sv.jArgs());
                varsOfExtConst.add(intValue);
                csp.bindings().addValuesSetConstraint(varsOfExtConst, sv.func().name());
                csp.addMinDelay(act.start(), act.end(), intValue);
            } else {
                csp.stn().EnforceMinDelay(act.start(), act.end(), act.minDuration().d());
            }
        } else {
            csp.stn().EnforceMinDelay(act.start(), act.end(), 1);
        }

        if(act.maxDuration() != null) {
            if(act.maxDuration().isFunction()) {
                // create a constraint defined in extension sv(a1,...,an) = tmp
                // add a constraint act.start +tmp > act.end
                // the last one will be propagated in the STN when tmp is binded in the CSP
                ParameterizedStateVariable sv = act.maxDuration().sv();
                assert sv.func().isConstant() : "Cannot parameterize an action duration with non-constant functions.";
                assert sv.func().valueType() == "integer" : "Cannot parameterize an action duration with a non-integer function.";
                VarRef intValue = new VarRef();
                csp.bindings().AddIntVariable(intValue);
                List<VarRef> varsOfExtConst = new LinkedList<>(sv.jArgs());
                varsOfExtConst.add(intValue);
                csp.bindings().addValuesSetConstraint(varsOfExtConst, sv.func().name());
                csp.addMaxDelay(act.start(), act.end(), intValue);
            } else {
                assert !act.maxDuration().isFunction() : "Parameterized durations are not supported yet.";
                csp.stn().EnforceMaxDelay(act.start(), act.end(), act.maxDuration().d());
            }
        }

        csp.bindings().isConsistent();
        csp.stn().IsConsistent();
    }

    /**
     * Records the start and end timepoint of the given interval in the temporal
     * network manager. It also adds a constraint specifying that start must
     * happen before end.
     */
    private void recordTimePoints(TemporalInterval interval) {
        csp.stn().recordTimePoint(interval.start());
        csp.stn().recordTimePoint(interval.end());
        csp.stn().EnforceBefore(interval.start(), interval.end());
    }

    /**
     * Applies all pending modifications of the problem. A problem comes with a
     * sequence of StateModifiers that depict the current status of the problem.
     * This method simply applies all modifiers that were not previously
     * applied.
     *
     * @return True if the resulting state is consistent, False otherwise.
     */
    public void update() {
        if (problemRevision == -1) {
            csp.stn().recordTimePoint(pb.start());
            csp.stn().recordTimePoint(pb.end());
            csp.stn().recordTimePoint(pb.earliestExecution());
            csp.stn().EnforceBefore(pb.start(), pb.earliestExecution());

            // TODO this voodoo to make pb.start == stn.start && pb.end == stn.end
            csp.stn().stn.enforceInterval(csp.stn().ids.get(pb.start()), csp.stn().stn.start(), 0, 0);
            csp.stn().stn.enforceInterval(csp.stn().ids.get(pb.end()), csp.stn().stn.end(), 0, 0);
        }
        for (int i = problemRevision + 1; i < pb.modifiers().size(); i++) {
            apply(pb.modifiers().get(i));
            problemRevision = i;
        }
    }

    /**
     * Inserts a logical statement into a state
     *
     * @param s Statement to insert
     * @return True if the resulting state is consistent
     */
    private void apply(LogStatement s) {
        recordTimePoints(s);
        assert !s.sv().func().isConstant() : "LogStatement on a constant function: "+s;

        TemporalDatabase db = new TemporalDatabase(s);

        if (db.isConsumer()) {
            consumers.add(db);
        }
        tdb.vars.add(db);
    }

    /**
     * Enforce a binding constraint in the state.
     *
     * Those have effect on the constraint network only.
     * @param mod Modifier in which the constraint appears.
     * @param bc BindingConstraint to be enforced.
     */
    public void apply(StateModifier mod, BindingConstraint bc) {
        if (bc instanceof AssignmentConstraint) {
            AssignmentConstraint c = (AssignmentConstraint) bc;
            List<String> values = new LinkedList<>();
            for (VarRef v : c.sv().jArgs()) {
                assert v instanceof InstanceRef;
                values.add(((InstanceRef) v).instance());
            }
            assert c.variable() instanceof InstanceRef;
            values.add(((InstanceRef) c.variable()).instance());
            csp.bindings().addValuesToValuesSet(c.sv().func().name(), values);
        } else if (bc instanceof VarEqualityConstraint) {
            VarEqualityConstraint c = (VarEqualityConstraint) bc;
            csp.bindings().AddUnificationConstraint(c.leftVar(), c.rightVar());
        } else if (bc instanceof VarInequalityConstraint) {
            VarInequalityConstraint c = (VarInequalityConstraint) bc;
            csp.bindings().AddSeparationConstraint(c.leftVar(), c.rightVar());
        } else if (bc instanceof EqualityConstraint) {
            EqualityConstraint c = (EqualityConstraint) bc;
            List<VarRef> variables = new LinkedList<>(c.sv().jArgs());
            variables.add(c.variable());
            csp.bindings().addValuesSetConstraint(variables, c.sv().func().name());
        } else if (bc instanceof InequalityConstraint) {
            // create a new value tmp such that
            // c.sv == tmp and tmp != c.variable
            InequalityConstraint c = (InequalityConstraint) bc;
            List<VarRef> variables = new LinkedList<>(c.sv().jArgs());
            VarRef tmp = new VarRef();
            csp.bindings().AddVariable(tmp, pb.instances().jInstancesOfType(c.sv().func().valueType()), c.sv().func().valueType());
            variables.add(tmp);
            csp.bindings().addValuesSetConstraint(variables, c.sv().func().name());
            csp.bindings().AddSeparationConstraint(tmp, c.variable());
        } else if (bc instanceof IntegerAssignmentConstraint) {
            IntegerAssignmentConstraint c = (IntegerAssignmentConstraint) bc;
            List<String> values = new LinkedList<>();
            for (VarRef v : c.sv().jArgs()) {
                assert v instanceof InstanceRef;
                values.add(((InstanceRef) v).instance());
            }
            csp.bindings().addPossibleValue(c.value());
            csp.bindings().addValuesToValuesSet(c.sv().func().name(), values, c.value());
        } else {
            throw new FAPEException("Unhandled constraint type: "+bc);
        }
    }

    /**
     * Inserts a resource statement into a state.
     *
     * @param s Statement to insert
     * @return True if the resulting state is consistent.
     */
    private void apply(ResourceStatement s) {
        recordTimePoints(s);

        Resource r = ResourceManager.generateResourcePrototype((NumFunction) s.sv().func());

        if (s instanceof SetResource) {
            r.addAssignement(this, s.start(), s.end(), s.param());
        } else if (s instanceof UseResource) {
            r.addUsage(this, s.start(), s.end(), s.param());
        } else if (s instanceof RequireResource) {
            r.addRequirement(this, s.start(), s.end(), s.param());
        } else if (s instanceof ProduceResource) {
            r.addProduction(this, s.start(), s.end(), s.param());
        } else if (s instanceof ConsumeResource) {
            r.addConsumption(this, s.start(), s.end(), s.param());
        } else if (s instanceof LendResource) {
            r.addLending(this, s.start(), s.end(), s.param());
        } else {
            throw new FAPEException("Unsupported resource event.");
        }
        r.stateVariable = s.sv();

        resMan.AddResourceEvent(r, this);
    }

    /**
     * Inserts a statement into a state
     *
     * @param mod StateModifier in which the statement appears
     * @param s Statement to insert
     * @return True if the resulting state is consistent
     */
    private void apply(StateModifier mod, Statement s) {
        if (s instanceof LogStatement) {
            apply((LogStatement) s);
        } else if (s instanceof ResourceStatement) {
            apply((ResourceStatement) s);
        } else {
            throw new FAPEException("Unsupported statement: " + s);
        }
    }

    /**
     * Applies the modification implied by a temporal constraint. All time
     * points referenced in the constraint must have been previously recorded in
     * the STN.
     *
     * @param mod StateModifier in which the constraint appears.
     * @param tc The TemporalConstraint to insert.
     * @return True if the resulting state is consistent, False otherwise.
     */
    private void apply(StateModifier mod, TemporalConstraint tc) {
        TPRef tp1 = tc.tp1();
        TPRef tp2 = tc.tp2();

        switch (tc.op()) {
            case "<":
                // tp1 < tp2 + x => tp1 --[-x, inf] --> tp2
                csp.stn().EnforceMinDelay(tp1, tp2, -tc.plus());
                break;
            case "=":
                // tp1 --- [x, x] ---> tp2
                csp.stn().EnforceConstraint(tp2, tp1, tc.plus(), tc.plus());
        }
    }

    /**
     * Applies the given decomposition to the current state. It mainly consists
     * in inserting the decomposition's timepoints and link them to the
     * containing action. Then the state modifier is applied.
     *
     * @param dec Decomposition to insert
     * @return True if the resulting state is consistent.
     */
    public void applyDecomposition(Decomposition dec) {
        recordTimePoints(dec);

        // interval of the decomposition is equal to the one of the containing action.
        csp.stn().EnforceConstraint(dec.start(), dec.container().start(), 0, 0);
        csp.stn().EnforceConstraint(dec.end(), dec.container().end(), 0, 0);

        taskNet.insert(dec, dec.container());

        apply(dec);
    }

    /**
     * Applies all modifications stated in a StateModifier in this this State
     *
     * @param mod StateModifier to apply
     * @return True if the resulting State is consistent, False otherwise.
     */
    private void apply(StateModifier mod) {
        // for every instance declaration, create a new CSP Var with itself as domain
        for (String instance : mod.instances()) {
            csp.bindings().addPossibleValue(instance);
            List<String> domain = new LinkedList<>();
            domain.add(instance);
            csp.bindings().AddVariable(pb.instances().referenceOf(instance), domain, pb.instances().typeOf(instance));
        }

        // Declare new variables to the constraint network.
        for (Tuple2<String, VarRef> declaration : mod.vars()) {
            Collection<String> domain = pb.instances().jInstancesOfType(declaration._1());
            csp.bindings().AddVariable(declaration._2(), domain, declaration._1());
        }

        for (Statement ts : mod.statements()) {
            apply(mod, ts);
        }

        for (Action act : mod.actions()) {
            insert(act);
        }

        for(BindingConstraint bc : mod.bindingConstraints())
            apply(mod, bc);

        for(ActionCondition ac : mod.actionConditions()) {
            recordTimePoints(ac);
            if(mod instanceof Decomposition)
                taskNet.insert(ac, (Decomposition) mod);
            else if(mod instanceof Action)
                taskNet.insert(ac, (Action) mod);
            else
                taskNet.insert(ac);
        }

        for (TemporalConstraint tc : mod.temporalConstraints()) {
            apply(mod, tc);
        }
    }

    /**
     * Given a flaw and a set of resolvers, retain only the valid resolvers. It
     * is currently used to filter out the resolvers of flaws that have
     * partially addressed by an action decomposition.
     *
     * @param f The flaw for which the resolvers are emitted.
     * @param opts The set of resolvers to address the flaw
     * @return A list of resolvers containing only the valid ones.
     */
    public List<Resolver> retainValidOptions(Flaw f, List<Resolver> opts) {
        if (f instanceof UndecomposedAction || f instanceof Threat || f instanceof UnboundVariable ||
                f instanceof ResourceFlaw || f instanceof UnsupportedTaskCond || f instanceof UnmotivatedAction) {
            return opts;
        } else if (f instanceof UnsupportedDatabase) {
            Decomposition mustDeriveFrom = getSupportConstraint(((UnsupportedDatabase) f).consumer);
            if (mustDeriveFrom == null) {
                return opts;
            } else {
                List<Resolver> retained = new LinkedList<>();
                for (Resolver opt : opts) {
                    if (isOptionDerivedFrom(opt, mustDeriveFrom)) {
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
     *
     * @param db The temporal database that needs to be supported.
     * @return A decomposition if the resolvers for this database must derive
     * from a decomposition (e.g. this flaw was previously addressed by
     * decomposing a method. null if there is no constraints.
     */
    private Decomposition getSupportConstraint(TemporalDatabase db) {
        int compID = db.GetChainComponent(0).mID;
        Decomposition dec = null;
        for (Pair<Integer, Decomposition> constraint : supportConstraints) {
            if (constraint.value1 == mID) {
                dec = constraint.value2;
            }
        }
        return dec;
    }

    /**
     * Checks if the option is a consequence of the given decomposition. A
     * resolver is considered to be derived from an decomposition if (i) it is a
     * decomposition of an action descending from the decomposition. (ii) it is
     * a decomposition of an action descending from the given decomposition.
     *
     * It is currently used to check if a resolver doesn't contradict an earlier
     * commitment: when an unsupported database is "resolved" with a
     * decomposition but no causal link is added (the database is still
     * unsupported), the resolvers for it are then limited to those being a
     * consequence of the decomposition.
     *
     * @param opt Resolver whose validity is to be checked.
     * @param dec Decomposition from which the resolver must be derived.
     * @return True if the resolver is valid, False otherwise.
     */
    private boolean isOptionDerivedFrom(Resolver opt, Decomposition dec) {
        // this unsupported db must be supported by a descendant of a decomposition
        // this is a consequence of an earlier commitment.
        if (opt.hasDecomposition()) {
            // decomposition is allowed only if the decomposed action is issued from dec.
            return taskNet.isDescendantOf(opt.actionToDecompose(), dec);
        } else if (opt instanceof SupportingDatabase) {
            // DB supporters are limited to those coming from an action descending from dec.
            TemporalDatabase db = GetDatabase(((SupportingDatabase) opt).temporalDatabase);

            // get the supporting chain component. (must provide a change on the state variable)
            ChainComponent cc;
            if (((SupportingDatabase) opt).precedingChainComponent != -1) {
                cc = db.GetChainComponent(((SupportingDatabase) opt).precedingChainComponent);
            } else {
                cc = db.getSupportingComponent();
            }

            assert cc != null : "There is no support statement in " + db;
            assert cc.change : "Support is not a change.";
            assert cc.contents.size() == 1;
            Action a = getActionContaining(cc.contents.getFirst());

            return a != null && taskNet.isDescendantOf(a, dec);
        } else {
            return false;
        }
    }

    public void addSupportConstraint(ChainComponent cc, Decomposition dec) {
        this.supportConstraints.add(new Pair<>(cc.mID, dec));
    }



    /*** Wrapper around STN ******/

    public void enforceBefore(TPRef a, TPRef b) { csp.stn().EnforceBefore(a, b); }

    public void enforceStrictlyBefore(TPRef a, TPRef b) { csp.stn().EnforceStrictlyBefore(a, b); }

    public boolean canBeBefore(TPRef a, TPRef b) { return csp.stn().CanBeBefore(a, b); }

    public boolean canBeStrictlyBefore(TPRef a, TPRef b) { return csp.stn().CanBeStrictlyBefore(a, b); }

    public boolean enforceConstraint(TPRef a, TPRef b, int min, int max) { return csp.stn().EnforceConstraint(a, b, min, max); }

    public boolean removeConstraints(Pair<TPRef,TPRef>... pairs) {
        List<Tuple2<TPRef,TPRef>> ps = new LinkedList<>();
        for(Pair<TPRef,TPRef> p : pairs)
            ps.add(new Tuple2<TPRef, TPRef>(p.value1, p.value2));
        return csp.stn().RemoveConstraints(ps);
    }

    public void enforceDelay(TPRef a, TPRef b, int delay) { csp.stn().EnforceMinDelay(a, b, delay); }

    public long getEarliestStartTime(TPRef a) { return csp.stn().GetEarliestStartTime(a); }

    public void exportTemporalNetwork(String filename) {
        csp.stn().stn.g().exportToDotFile(filename, new STNNodePrinter(this));
    }


    /********* Wrapper around the task network **********/

    public Action getAction(ActRef actionID) { return taskNet.GetAction(actionID); }

    public List<Action> getAllActions() { return taskNet.GetAllActions(); }

    public List<Action> getOpenLeaves() { return taskNet.GetOpenLeaves(); }

    public List<ActionCondition> getOpenTaskConditions() { return taskNet.getOpenTaskConditions(); }

    public List<Action> getUnmotivatedActions() { return taskNet.getUnmotivatedActions(); };

    public void addSupport(ActionCondition cond, Action act) { taskNet.addSupport(cond, act); }

    public int getNumActions() { return taskNet.getNumActions(); }

    public int getNumOpenLeaves() { return taskNet.getNumOpenLeaves(); }

    public int getNumRoots() { return taskNet.getNumRoots(); }

    /******** Wrapper around the constraint network ***********/

    public Collection<String> domainOf(VarRef var) { return csp.bindings().domainOf(var); }

    public void addUnificationConstraint(VarRef a, VarRef b) { csp.bindings().AddUnificationConstraint(a, b); }

    public void addUnificationConstraint(ParameterizedStateVariable a, ParameterizedStateVariable b) {
        assert a.func() == b.func() : "Error unifying two state variable on different functions.";
        assert a.jArgs().size() == b.jArgs().size() : "Error different number of arguments.";
        for (int i = 0; i < a.jArgs().size(); i++) {
            csp.bindings().AddUnificationConstraint(a.jArgs().get(i), b.jArgs().get(i));
        }
    }

    public void addSeparationConstraint(VarRef a, VarRef b) { csp.bindings().AddSeparationConstraint(a, b); }

    public String typeOf(VarRef var) { return csp.bindings().typeOf(var); }

    public void restrictDomain(VarRef var, Collection<String> values) { csp.bindings().restrictDomain(var, values); }

    public void bindVariable(VarRef var, String value) {
        List<String> values = new LinkedList<>();
        values.add(value);
        restrictDomain(var, values);
    }

    public List<VarRef> getUnboundVariables() { return csp.bindings().getUnboundVariables(); }

    public void assertConstraintNetworkGroundAndConsistent() { csp.bindings().assertGroundAndConsistent(); }

    public boolean unified(VarRef a, VarRef b) { return csp.bindings().unified(a, b); }

    public boolean unifiable(VarRef a, VarRef b) { return csp.bindings().unifiable(a, b); }

    public void addValuesToValuesSet(String setID, List<String> values) { csp.bindings().addValuesToValuesSet(setID, values);}

    public void addValuesSetConstraint(List<VarRef> variables, String setID) { csp.bindings().addValuesSetConstraint(variables, setID);}


    /************ Wrapper around TemporalDatabaseManager **********************/

    public List<TemporalDatabase> getDatabases() { return tdb.vars; }

    public void removeDatabase(TemporalDatabase db) {
        tdb.vars.remove(db);
        if(consumers.contains(db))
            consumers.remove(db);
    }

    public void insertDatabaseAfter(TemporalDatabase supporter, TemporalDatabase consumer, ChainComponent precedingComponent) {
        tdb.InsertDatabaseAfter(this, supporter, consumer, precedingComponent);
    }

    public TemporalDatabase getDBContaining(LogStatement s) { return tdb.getDBContaining(s); }


    /************** Wrapper around the Resource Manager ****************/

    public Collection<Resolver> getResolvingBindings(Replenishable replenishable, float f) {
        return resMan.GetResolvingBindings(replenishable, f, this);
    }

    public Collection<Flaw> resourceFlaws() { return resMan.GatherFlaws(this); }


    public void setActionExecuting(Action a, int startTime) {
        setActionExecuting(a.id());
        removeConstraints(new Pair(pb.earliestExecution(), a.start()),
                new Pair(a.start(), pb.earliestExecution()));
        enforceConstraint(pb.start(), a.start(), (int) startTime, (int) startTime);
    }

    public void setActionSuccess(Action a, int endTime) {
        setActionSuccess(a.id());
        // remove the duration constraints of the action
        removeConstraints(new Pair(a.start(), a.end()), new Pair(a.end(), a.start()));
        // insert new constraint specifying the end time of the action
        enforceConstraint(pb.start(), a.end(), endTime, endTime);

    }

}
