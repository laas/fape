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

import fape.core.execution.model.AtomicAction;
import fape.core.planning.Plan;
import fape.core.planning.resources.Replenishable;
import fape.core.planning.resources.ResourceManager;
import fape.core.planning.search.flaws.flaws.*;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.search.flaws.resolvers.SupportingTimeline;
import fape.core.planning.stn.STNNodePrinter;
import fape.core.planning.tasknetworks.TaskNetworkManager;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
import fape.core.planning.timelines.TimelinesManager;
import fape.exceptions.FAPEException;
import fape.util.ActionsChart;
import fape.util.Pair;
import fape.util.Reporter;
import planstack.anml.model.*;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.*;
import planstack.anml.model.concrete.Factory;
import planstack.anml.model.concrete.statements.*;
import planstack.constraints.*;
import planstack.constraints.stnu.Controllability;
import planstack.constraints.stnu.STNUDispatcher;
import planstack.structures.IList;
import scala.Option;
import scala.Tuple2;
import scala.Tuple3;

import java.util.*;
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
    public final TimelinesManager tdb;

    public final MetaCSP<VarRef,TPRef,GlobalRef> csp;

    public final TaskNetworkManager taskNet;

    protected final ResourceManager resMan;

    public Set<AbstractAction> notAddable = new HashSet<>();

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
    public final List<Timeline> consumers;


    public final AnmlProblem pb;

    public final Controllability controllability;

    /**
     * Index of the latest applied StateModifier in pb.jModifiers()
     */
    private int problemRevision;

    /**
     * this constructor is only for the initial state!! other states are
     * constructed from from the existing states
     */
    public State(AnmlProblem pb, Controllability controllability) {
        this.pb = pb;
        this.controllability = controllability;
        depth = 0;
        tdb = new TimelinesManager();
        csp = planstack.constraints.Factory.getMetaWithGivenControllability(controllability);
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
        this.controllability = st.controllability;
        depth = st.depth + 1;
        problemRevision = st.problemRevision;
        csp = new MetaCSP<>(st.csp);
        tdb = st.tdb.deepCopy();
        taskNet = st.taskNet.DeepCopy();
        supportConstraints = new LinkedList<>(st.supportConstraints);
        resMan = st.resMan.DeepCopy();
        consumers = new LinkedList<>();

        for (Timeline sb : st.consumers) {
            consumers.add(this.getDatabase(sb.mID));
        }
    }

    public State cc() {
        return new State(this);
    }

    /**
     * Returns true if the action can be added to the State. This is populated by reasonning on derivabilities,
     * depending on the planner used.
     */
    public boolean isAddable(AbstractAction a) {
        return !notAddable.contains(a);
    }

    /**
     * @return True if the state is consistent (ie. stn and bindings
     * consistent), False otherwise.
     */
    public boolean isConsistent() {
        return csp.isConsistent() && resMan.isConsistent(this);
    }

    public String report() {
        String ret = "";
        ret += "{\n";
        ret += "  state[" + mID + "]\n";
        ret += "  cons: " + csp.bindings().Report() + "\n";
        //ret += "  stn: " + this.csp.stn().report() + "\n";
        ret += "  consumers: " + this.consumers.size() + "\n";
        for (Timeline b : consumers) {
            ret += b.Report();
        }
        ret += "\n";
        ret += Printer.taskNetwork(this, taskNet);
        //ret += "  databases: "+this.tdb.report()+"\n";

        ret += "}\n";

        return ret;
    }

    /**
     * Retrieve the Database with the same ID.
     *
     * @param dbID ID of the database to lookup
     * @return The database with the same ID.
     */
    public Timeline getDatabase(int dbID) {
        for (Timeline db : tdb.vars) {
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

    public void breakCausalLink(LogStatement supporter, LogStatement consumer) {
        Timeline db = tdb.getTimelineContaining(supporter);
        assert db == tdb.getTimelineContaining(consumer) : "The statements are not in the same database.";

        int index = db.indexOfContainer(consumer);
        Timeline newDB = new Timeline(consumer.sv());
        //add all extra chain components to the new database

        List<ChainComponent> toRemove = new LinkedList<>();
        for (int i = index; i < db.chain.size(); i++) {
            ChainComponent origComp = db.chain.get(i);
            toRemove.add(origComp);
            ChainComponent pc = origComp.deepCopy();
            newDB.chain.add(pc);
        }
        db.chain.removeAll(toRemove);
        assert !db.chain.isEmpty();
        assert !newDB.chain.isEmpty();
        this.consumers.add(newDB);
        this.tdb.vars.add(newDB);
    }

    /**
     * Remove a statement from the state. It does so by identifying the temporal
     * database in which the statement appears and removing it from the
     * database. If necessary, the database is split in two.
     *
     * @param s Statement to remove.
     */
    private void removeStatement(LogStatement s) {
        Timeline theDatabase = tdb.getTimelineContaining(s);

        // First find which component contains s
        final int ct = theDatabase.indexOfContainer(s);
        final ChainComponent comp = theDatabase.chain.get(ct);

        assert comp != null && theDatabase.chain.get(ct) == comp;

        if (s instanceof Transition) {
            if (ct + 1 < theDatabase.chain.size()) {
                //this was not the last element, we need to create another database and make split

                // the two databases share the same state variable
                Timeline newDB = new Timeline(theDatabase.stateVariable);

                //add all extra chain components to the new database
                List<ChainComponent> remove = new LinkedList<>();
                for (int i = ct + 1; i < theDatabase.chain.size(); i++) {
                    ChainComponent origComp = theDatabase.chain.get(i);
                    remove.add(origComp);
                    ChainComponent pc = origComp.deepCopy();
                    newDB.chain.add(pc);
                }
                this.consumers.add(newDB);
                this.tdb.vars.add(newDB);
                theDatabase.chain.remove(comp);
                theDatabase.chain.removeAll(remove);
                assert !newDB.chain.isEmpty();
            } else {
                assert comp.contents.size() == 1;
                //this was the last element so we can just remove it and we are done
                theDatabase.chain.remove(comp);
            }

            if(theDatabase.chain.isEmpty()) {
                tdb.vars.remove(theDatabase);
                if (consumers.contains(theDatabase))
                    consumers.remove(theDatabase);
            }
        } else if (s instanceof Persistence) {
            if (comp.contents.size() == 1) {
                // only one statement, remove the whole component
                theDatabase.chain.remove(comp);
            } else {
                // more than one statement, remove only this statement
                comp.contents.remove(s);
            }
            if(theDatabase.chain.isEmpty()) {
                tdb.vars.remove(theDatabase);
                if (consumers.contains(theDatabase))
                    consumers.remove(theDatabase);
            }

        } else if(s instanceof Assignment) {
            theDatabase.chain.remove(comp);
            if(theDatabase.chain.isEmpty()) {
                this.tdb.vars.remove(theDatabase);
                if (consumers.contains(theDatabase))
                    consumers.remove(theDatabase);
            } else {
                assert theDatabase.isConsumer() : "Removing the first element yields a non-consuming database.";
                this.consumers.add(theDatabase);
            }
        } else {
            throw new FAPEException("Unknown event type: "+s);
        }
        for(Timeline db : tdb.vars)
            assert !db.chain.isEmpty();
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
     * @return all possible values of local variable (based on its type).
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
     * True if the state variables on which those databases apply are necessarily unified (i.e. same functions and same variables).
     */
    public boolean unified(Timeline a, Timeline b) {
        return unified(a.stateVariable, b.stateVariable);
    }

    /**
     * True iff the state variables are necessarily unified (same functions and same variables).
     */
    public boolean unified(ParameterizedStateVariable a, ParameterizedStateVariable b) {
        if(a.func() != b.func())
            return false;
        for(int i=0 ; i<a.jArgs().size() ; i++) {
            if(!unified(a.jArgs().get(i), b.jArgs().get(i)))
                return false;
        }
        return true;
    }

    /**
     * @return True if both TemporalDatabases might be unifiable (ie. the refer
     * to two unifiable state variables).
     */
    public boolean unifiable(Timeline a, Timeline b) {
        return unifiable(a.stateVariable, b.stateVariable);
    }

    /**
     * Returns true if two state variables are unifiable (ie: they are on the
     * same function and their variables are unifiable).
     */
    public boolean unifiable(ParameterizedStateVariable a, ParameterizedStateVariable b) {
        if (a.func().equals(b.func())) {
            return unifiable(a.jArgs(), b.jArgs());
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
    public boolean unifiable(List<VarRef> as, List<VarRef> bs) {
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
    public boolean canBeEnabler(LogStatement s, Timeline db) {
        boolean canSupport = s instanceof Transition || s instanceof Assignment;
        canSupport = canSupport && unifiable(s.sv(), db.stateVariable);
        canSupport = canSupport && unifiable(s.endValue(), db.getGlobalConsumeValue());
        return canSupport;
    }

    /**
     * Insert an action into the state, applying all needed modifications.
     *
     * @param act Action to insert.
     */
    public void insert(Action act) {
        taskNet.insert(act);
        apply(act);

        // make sure that this action is executed after earliest execution.
        // this constraint is flagged with the start of the action time point which can be used for removal
        // (when an action is executed)
        csp.stn().enforceMinDelayWithID(pb.earliestExecution(), act.start(), 0, act.start());

        // creates constraints associated with the action duration.
        // all those constraints are given the action's reference as ID to allow for later removal.
        if(act.minDuration() != null) {
            assert act.maxDuration() != null : "Error: a min duration without a max duration.";

            // variables in the CSP representing the min/max value of the duration.
            VarRef min, max;

            // create domain/constraints for the min variable
            if(act.minDuration().isFunction()) {
                // create a constraint defined in extension sv(a1,...,an) = tmp
                // add a constraint act.start +tmp < act.end
                // the last one will be propagated in the CSP when tmp is binded in the CSP
                ParameterizedStateVariable sv = act.minDuration().sv();
                assert sv.func().isConstant() : "Cannot parameterize an action duration with non-constant functions.";
                assert sv.func().valueType().equals("integer") : "Cannot parameterize an action duration with a non-integer function.";
                min = new VarRef();
                csp.bindings().AddIntVariable(min);
                List<VarRef> varsOfExtConst = new LinkedList<>(sv.jArgs());
                varsOfExtConst.add(min);
                csp.bindings().addValuesSetConstraint(varsOfExtConst, sv.func().name());
            } else {
                // create a var with a singleton domain
                min = new VarRef();
                List<Integer> domain = new LinkedList<>();
                domain.add(act.minDuration().d());
                csp.bindings().AddIntVariable(min, domain);
            }

            // create domain/constraints for the max variable
            if(act.maxDuration().isFunction()) {
                // create a constraint defined in extension sv(a1,...,an) = tmp
                // add a constraint act.start +tmp > act.end
                // the last one will be propagated in the STN when tmp is binded in the CSP
                ParameterizedStateVariable sv = act.maxDuration().sv();
                assert sv.func().isConstant() : "Cannot parameterize an action duration with non-constant functions.";
                assert sv.func().valueType().equals("integer") : "Cannot parameterize an action duration with a non-integer function.";
                max = new VarRef();
                csp.bindings().AddIntVariable(max);
                List<VarRef> varsOfExtConst = new LinkedList<>(sv.jArgs());
                varsOfExtConst.add(max);
                csp.bindings().addValuesSetConstraint(varsOfExtConst, sv.func().name());
            } else {
                // create a var with a singleton domain
                max = new VarRef();
                List<Integer> domain = new LinkedList<>();
                domain.add(act.maxDuration().d());
                csp.bindings().AddIntVariable(max, domain);
            }

            csp.addContingentConstraintWithID(act.start(), act.end(), min, max, act.id());
        } else {
            assert act.maxDuration() == null : "Error: max duration was defined without a min duration.";
            csp.stn().enforceMinDelayWithID(act.start(), act.end(), 1, act.id());
        }

        csp.isConsistent();
    }

    /**
     * Applies all pending modifications of the problem. A problem comes with a
     * sequence of StateModifiers that depict the current status of the problem.
     * This method simply applies all chronicles that were not previously
     * applied.
     */
    public void update() {
        if (problemRevision == -1) {
            csp.stn().recordTimePointAsStart(pb.start());
            csp.stn().recordTimePointAsEnd(pb.end());
            csp.stn().recordTimePoint(pb.earliestExecution());
            csp.stn().enforceBefore(pb.start(), pb.earliestExecution());
        }
        for (int i = problemRevision + 1; i < pb.chronicles().size(); i++) {
            apply(pb.chronicles().get(i));
            problemRevision = i;
        }
    }

    /**
     * Inserts a logical statement into a state
     *
     * @param s Statement to insert
     */
    private void apply(LogStatement s) {
        csp.stn().enforceBefore(s.start(), s.end());

        assert !s.sv().func().isConstant() : "LogStatement on a constant function: "+s;

        Timeline db = new Timeline(s);

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
    public void apply(Chronicle mod, BindingConstraint bc) {
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
     */
    private void apply(ResourceStatement s) {
        // Resources management is too buggy to allow people using it.
        throw new RuntimeException("Resource statements are not supported.");
        /*
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
        */
    }

    /**
     * Inserts a statement into a state
     *
     * @param mod StateModifier in which the statement appears
     * @param s Statement to insert
     */
    private void apply(Chronicle mod, Statement s) {
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
     */
    private void apply(Chronicle mod, TemporalConstraint tc) {
        TPRef tp1 = tc.tp1();
        TPRef tp2 = tc.tp2();

        switch (tc.op()) {
            case "<":
                // tp1 < tp2 + x => tp1 --[-x, inf] --> tp2
                csp.stn().enforceMinDelay(tp1, tp2, -tc.plus());
                break;
            case "=":
                // tp1 --- [x, x] ---> tp2
                csp.stn().enforceConstraint(tp2, tp1, tc.plus(), tc.plus());
        }
    }

    /**
     * Applies the given decomposition to the current state. It mainly consists
     * in inserting the decomposition's timepoints and link them to the
     * containing action. Then the chronicle is applied.
     *
     * @param dec Decomposition to insert
     */
    public void applyDecomposition(Decomposition dec) {
//        recordTimePoints(dec);
//
//        interval of the decomposition is equal to the one of the containing action.
//        csp.stn().enforceConstraint(dec.start(), dec.container().start(), 0, 0);
//        csp.stn().enforceConstraint(dec.end(), dec.container().end(), 0, 0);

        taskNet.insert(dec, dec.container());

        apply(dec);
    }

    public void applyChronicle(Chronicle chron) {
        assert chron instanceof BaseChronicle : "Other chronicles should have specialized methods.";

        apply(chron);
    }

    /**
     * Applies all modifications stated in a StateModifier in this this State
     *
     * @param mod StateModifier to apply
     */
    private void apply(Chronicle mod) {

        // get all timepoints to be declared (in categories real, virtual and pending-virtual)
        // add the time points tight away, the constraints will be add last as they might apply on
        // time points not included yet (e.g. start/end of nested actions)
        TemporalObjects timedObjects = mod.getTemporalObjects();

        for (Tuple2<TPRef, String> tp : timedObjects.timePoints()) {
            switch (tp._2()) {
                case "dispatchable":
                    csp.stn().addControllableTimePoint(tp._1());
                    break;
                case "contingent":
                    csp.stn().addContingentTimePoint(tp._1());
                    break;
                case "controllable":
                    csp.stn().recordTimePoint(tp._1());
                    break;
                default:
                    throw new FAPEException("Unknown time point tipe: " + tp._2());
            }
        }

        for(TPRef pendingVirt : timedObjects.pendingVirtuals())
            csp.stn().addPendingVirtualTimePoint(pendingVirt);


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

        for(BindingConstraint bc : mod.bindingConstraints())
            apply(mod, bc);

        for (Action act : mod.actions()) {
            insert(act);
        }


        // last: virtual time points might refer to start/end of nested actions
        for(Tuple3<TPRef,TPRef,Integer> virtual : timedObjects.virtualTimePoints()) {
            csp.stn().addVirtualTimePoint(virtual._1(), virtual._2(), virtual._3());
        }

        // apply all remaining temporal constraints (those not represented with rigid time points)
        for (TemporalConstraint tc : timedObjects.nonRigidConstraints()) {
            apply(mod, tc);
        }

        // needs time points to be defined
        for(ActionCondition ac : mod.actionConditions()) {
            csp.stn().enforceBefore(ac.start(), ac.end());

            if(mod instanceof Decomposition)
                taskNet.insert(ac, (Decomposition) mod);
            else if(mod instanceof Action)
                taskNet.insert(ac, (Action) mod);
            else
                taskNet.insert(ac);
        }

        // needs its timepoints to be defined
        for (Statement ts : mod.statements()) {
            apply(mod, ts);
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
        } else if (f instanceof UnsupportedTimeline) {
            Decomposition mustDeriveFrom = getSupportConstraint(((UnsupportedTimeline) f).consumer);
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
    private Decomposition getSupportConstraint(Timeline db) {
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
        } else if (opt instanceof SupportingTimeline) {
            // DB supporters are limited to those coming from an action descending from dec.
            Timeline db = getDatabase(((SupportingTimeline) opt).supporterID);

            // get the supporting chain component. (must provide a change on the state variable)
            ChainComponent cc;
            if (((SupportingTimeline) opt).precedingChainComponent != -1) {
                cc = db.getChainComponent(((SupportingTimeline) opt).precedingChainComponent);
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

    public int numFlaws() {
        return consumers.size() + taskNet.getNumOpenActionConditions() + taskNet.getNumOpenLeaves() +taskNet.getNumUnmotivatedActions();
    }



    /*** Wrapper around STN ******/

    public void enforceBefore(TPRef a, TPRef b) { csp.stn().enforceBefore(a, b); }

    public void enforceStrictlyBefore(TPRef a, TPRef b) { csp.stn().enforceStrictlyBefore(a, b); }

    public boolean canBeBefore(TPRef a, TPRef b) { return csp.stn().canBeBefore(a, b); }

    public boolean canBeBefore(Collection<TPRef> as, Collection<TPRef> bs) {
        for(TPRef a : as) {
            for(TPRef b : bs) {
                if(!canBeBefore(a, b))
                    return false;
            }
        }
        return true;
    }

    public boolean canBeBefore(Collection<TPRef> as, TPRef b) {
        for(TPRef a : as) {
            if(!canBeBefore(a, b))
                return false;
        }
        return true;
    }

    public boolean canBeBefore(TPRef a, Collection<TPRef> bs) {
        for(TPRef b : bs) {
            if(!canBeBefore(a, b))
                return false;
        }
        return true;
    }

    public boolean canBeStrictlyBefore(TPRef a, TPRef b) { return csp.stn().canBeStrictlyBefore(a, b); }

    public boolean enforceConstraint(TPRef a, TPRef b, int min, int max) { return csp.stn().enforceConstraint(a, b, min, max); }

    public boolean removeActionDurationOf(ActRef id) {
        return csp.removeConstraintsWithID(id);
    }

    public void enforceDelay(TPRef a, TPRef b, int delay) { csp.stn().enforceMinDelay(a, b, delay); }

    public int getEarliestStartTime(TPRef a) { return csp.stn().getEarliestStartTime(a); }
    public int getLatestStartTime(TPRef a) { return csp.stn().getLatestStartTime(a); }

    public void exportTemporalNetwork(String filename) {
        csp.stn().exportToDotFile(filename, new STNNodePrinter(this));
    }

    public boolean checksDynamicControllability() { return csp.stn().checksDynamicControllability(); }

    public STNUDispatcher<TPRef,GlobalRef> getDispatchableSTNU() {
        return new STNUDispatcher<>(csp.stn());
    }

    public Option<Tuple2<Integer,Integer>> getDurationBounds(Action a) {
        return csp.stn().contingentDelay(a.start(), a.end());
    }


    /********* Wrapper around the task network **********/

    public Action getAction(ActRef actionID) { return taskNet.GetAction(actionID); }

    public List<Action> getAllActions() { return taskNet.GetAllActions(); }

    public List<Action> getOpenLeaves() { return taskNet.GetOpenLeaves(); }

    public List<ActionCondition> getOpenTaskConditions() { return taskNet.getOpenTaskConditions(); }

    public List<Action> getUnmotivatedActions() { return taskNet.getUnmotivatedActions(); };

    /**
     *  Unifies the time points of the action condition and those of the action, and add
     * the support link in the task network.
     */
    public void addSupport(ActionCondition cond, Action act) {
        csp.stn().enforceConstraint(cond.start(), act.start(), 0, 0);
        csp.stn().enforceConstraint(cond.end(), act.end(), 0, 0);
        taskNet.addSupport(cond, act);
    }

    public int getNumActions() { return taskNet.getNumActions(); }

    public int getNumOpenLeaves() { return taskNet.getNumOpenLeaves(); }

    public int getNumRoots() { return taskNet.getNumRoots(); }

    public void exportTaskNetwork(String filename) { taskNet.exportToDot(this, filename); }

    /******** Wrapper around the constraint network ***********/

    public List<String> domainOf(VarRef var) { return csp.bindings().domainOf(var); }

    public int domainSizeOf(VarRef var) { return csp.bindings().domainSize(var); }

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

    public boolean separable(VarRef a, VarRef b) { return csp.bindings().separable(a, b); }

    public void addValuesToValuesSet(String setID, List<String> values) { csp.bindings().addValuesToValuesSet(setID, values);}

    public void addValuesSetConstraint(List<VarRef> variables, String setID) { csp.bindings().addValuesSetConstraint(variables, setID);}


    /************ Wrapper around TemporalDatabaseManager **********************/

    public List<Timeline> getDatabases() { return tdb.vars; }

    public void removeTimeline(Timeline tl) {
        tdb.vars.remove(tl);
        if(consumers.contains(tl))
            consumers.remove(tl);
    }

    public void insertTimelineAfter(Timeline supporter, Timeline consumer, ChainComponent precedingComponent) {
        tdb.insertTimelineAfter(this, supporter, consumer, precedingComponent);
    }

    public Timeline getDBContaining(LogStatement s) { return tdb.getTimelineContaining(s); }


    /************** Wrapper around the Resource Manager ****************/

    public Collection<Resolver> getResolvingBindings(Replenishable replenishable, float f) {
        return resMan.GetResolvingBindings(replenishable, f, this);
    }

    public Collection<Flaw> resourceFlaws() { return resMan.GatherFlaws(this); }



    /***************** Execution related methods **************************/

    /**
     * Marks an action as being executed. It forces it start time to the one given in parameter.
     */
    public void setActionExecuting(ActRef actRef, int startTime) {
        Action a = getAction(actRef);
        csp.stn().removeConstraintsWithID(a.start());
        enforceConstraint(pb.start(), a.start(), (int) startTime, (int) startTime);
        a.setStatus(ActionStatus.EXECUTING);
    }

    /**
     * Marks an action as successfully executed. It forces its end time to the one given.
     */
    public void setActionSuccess(ActRef actRef, int endTime) {
        Action a = getAction(actRef);
        // remove the duration constraints of the action
        removeActionDurationOf(a.id());
        // insert new constraint specifying the end time of the action
        enforceConstraint(pb.start(), a.end(), endTime, endTime);
        a.setStatus(ActionStatus.EXECUTED);
    }


    /**
     * Marks an action as failed. All statement of the action are removed from
     * this state.
     *
     * @param actRef Reference of the action to update.
     */
    public void setActionFailed(ActRef actRef, int failureTime) {
        Action toRemove = taskNet.GetAction(actRef);
        // remove the duration constraints of the action
        removeActionDurationOf(toRemove.id());
        // insert new constraint specifying the end time of the action
        enforceConstraint(pb.start(), toRemove.end(), failureTime, failureTime);

        toRemove.setStatus(ActionStatus.FAILED);

        for (LogStatement s : toRemove.logStatements()) {
            // ignore statements on constant functions that are handled through constraints
            if(!s.sv().func().isConstant())
                removeStatement(s);
        }
        for(Timeline db : tdb.vars) {
            if(db.chain.isEmpty()) {
                boolean breakHere = true;
            }
        }
    }

    /**
     * Pushes the earliest execution time point forward in time. This causes all pending actions
     * to be pushed after it.
     * @param currentTime Absolute value for the earliest execution time point.
     */
    public void setCurrentTime(int currentTime) {
        // push earliest execution (and thus all pending actions)
        enforceDelay(pb.start(), pb.earliestExecution(), currentTime);

        // Update bounds of all executing actions
        for(Action a : getAllActions()) {
            if(a.status() == ActionStatus.EXECUTING) {
                int startTime = getEarliestStartTime(a.start());
                int minDur = getEarliestStartTime(a.end()) -startTime;
                int maxDur = getLatestStartTime(a.end()) -startTime;

                if(startTime+minDur < currentTime) {
                    // we can narrow the uncertain duration
                    int realMin = currentTime - startTime;
                    int realMax = currentTime < startTime +maxDur ? maxDur : currentTime -startTime +1;
                    removeActionDurationOf(a.id());
                    csp.stn().enforceContingentWithID(a.start(), a.end(), realMin, realMax, a.id());
                }
            }
        }

        if(Plan.showChart)
            ActionsChart.setCurrentTime((int) currentTime);
    }

    /**
     * Returns all actions that are dispatchable at "currentTime".
     * This also updates the earliest execution to the currentTime and propagates the temporal constraints.
     * This might make the state inconsistent (in which case an empty list of actions is returned).
     * @return List of dispatchable actions.
     */
    public IList<AtomicAction> getDispatchableActions(int currentTime) {
        IList<AtomicAction> toDispatch = new IList<>();
        assert isConsistent() : "Trying to get dispatchable actions from an inconsistent state.";
        setCurrentTime(currentTime);

        if(!isConsistent()) {
            // propagation made the plan inconsistent
            System.err.println("Error: propagation (during dispatching) made the state inconsistent.");
            return toDispatch;
        }

        Plan plan = new Plan(this);
        return plan.getDispatchableActions(currentTime);
    }

    /**
     * Builds from scratch a new state based on the problem only. This state will also contains
     * all executing or executed actions since they are now part of the problem.
     *
     * This state should be readily usable for replanning.
     *
     * @return A new state containing only the problem definition and executing/executed actions.
     */
    public State getCleanState() {
        State st = new State(pb, controllability);

        for(Action oldAction : getAllActions()) {

            if(oldAction.status() == ActionStatus.EXECUTING || oldAction.status() == ActionStatus.EXECUTED) {
                // we need to insert it in the new state

                // make a copy with the same ID and parameters
                List<VarRef> params = new LinkedList<>();
                for(VarRef arg : oldAction.args()) {
                    List<String> possibleValues = new LinkedList<>(domainOf(arg));
                    assert possibleValues.size() == 1 : "Argument "+arg+" of action "+oldAction+" has more than one possible value.";
                    params.add(pb.instances().referenceOf(possibleValues.get(0)));
                }
                Action copy = Factory.getInstantiatedAction(pb, oldAction.abs(), params, oldAction.id());

                st.insert(copy);
                st.setActionExecuting(copy.id(), getEarliestStartTime(oldAction.start()));


                if(oldAction.status() == ActionStatus.EXECUTED) {
                    st.setActionSuccess(copy.id(), getEarliestStartTime(oldAction.end()));
                }
            }

        }
        // set earliest start
        st.enforceDelay(pb.start(), pb.earliestExecution(), getEarliestStartTime(pb.earliestExecution()));
        // propagate
        st.isConsistent();

        return st;
    }
}
