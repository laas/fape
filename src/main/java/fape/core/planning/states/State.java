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
import fape.core.inference.HReasoner;
import fape.core.inference.Term;
import fape.core.planning.Plan;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.TempFluents;
import fape.core.planning.heuristics.reachability.ReachabilityGraphs;
import fape.core.planning.planninggraph.FeasibilityReasoner;
import fape.core.planning.resources.Replenishable;
import fape.core.planning.resources.ResourceManager;
import fape.core.planning.search.flaws.finders.AllThreatFinder;
import fape.core.planning.search.flaws.flaws.*;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.search.flaws.resolvers.SupportingTaskDecomposition;
import fape.core.planning.search.flaws.resolvers.SupportingTimeline;
import fape.core.planning.stn.STNNodePrinter;
import fape.core.planning.tasknetworks.TaskNetworkManager;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
import fape.core.planning.timelines.TimelinesManager;
import fape.drawing.ChartLine;
import fape.drawing.RectElem;
import fape.drawing.TextLabel;
import fape.drawing.TimedCanvas;
import fape.exceptions.FAPEException;
import fape.util.EffSet;
import fape.util.Pair;
import fape.util.Reporter;
import planstack.anml.model.AbstractContext;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.LVarRef;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.*;
import planstack.anml.model.concrete.statements.*;
import planstack.constraints.MetaCSP;
import planstack.constraints.stnu.Controllability;
import planstack.constraints.stnu.STNUDispatcher;
import planstack.structures.IList;
import scala.Option;
import scala.Tuple2;

import java.util.*;
import java.util.stream.Collectors;
//import scala.collection.immutable.HashMap;

/**
 *
 * @author FD
 */
public class State implements Reporter {



    public float h = -1, g = -1, f=-1;

    

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

    public final MetaCSP<GlobalRef> csp;

    public final TaskNetworkManager taskNet;
    private final ArrayList<Action> actions;

    protected final ResourceManager resMan;

    public final RefCounter refCounter;

    /**
     * Keep tracks of statements that must be supported by a particular
     * decomposition. (e.g. by a statements which is a consequence of that
     * decomposition). This map is populated when a decomposition is chosen as a
     * resolver for an unsupported database.
     */
    private LinkedList<Pair<Integer, Action>> supportConstraints;

    public final AnmlProblem pb;

    public final Controllability controllability;

    /**
     *  The set of actions that might be addable in this state.
     * Those actions must (i) be reachable (plannning graph)
     * (ii) be derivable from an reachable action if motivated.
     * This field is filled by PlanningGraphReachibility when needed.
     */
    public EffSet<GAction> addableActions;
    public Set<AbstractAction> addableTemplates;

    /**
     * Contains all ground versions of fluents in the state with their associated time points
     */
    public List<TempFluents> fluents = null;

    public HReasoner<Term> reasoner = null;

    public ReachabilityGraphs reachabilityGraphs = null;

    class PotentialThreat {
        private final int id1, id2;
        public PotentialThreat(Timeline tl1, Timeline tl2) {
            assert tl1 != tl2;
            if(tl1.mID < tl2.mID) {
                id1 = tl1.mID;
                id2 = tl2.mID;
            } else {
                id1 = tl2.mID;
                id2 = tl1.mID;
            }
        }
        @Override public int hashCode() { return id1 + 42*id2; }
        @Override public boolean equals(Object o) {
            return o instanceof PotentialThreat && id1 == ((PotentialThreat) o).id1 && id2 == ((PotentialThreat) o).id2;
        }
    }

    final HashSet<PotentialThreat> threats;

    /**
     * Maps from timeline's IDs to potentially supporting (timeline, chain component).
     * The lists are exhaustive but might contain:
     *  - the same supporter twice.
     *  - supporters that are no longer valid (timeline removed or supporter not valid because of new constraints).
     */
    final HashMap<Integer, IList<SupportingTimeline>> potentialSupporters;

    /**
     * Index of the latest applied StateModifier in pb.jModifiers()
     */
    private int problemRevision;

    @Deprecated
    public FeasibilityReasoner pgr = null;

    /**
     * this constructor is only for the initial state!! other states are
     * constructed from from the existing states
     */
    public State(AnmlProblem pb, Controllability controllability) {
        this.pb = pb;
        this.controllability = controllability;
        this.refCounter = new RefCounter(pb.refCounter());
        this.actions = new ArrayList<>();
        depth = 0;
        tdb = new TimelinesManager(this);
        csp = planstack.constraints.Factory.getMetaWithGivenControllability(controllability);
        taskNet = new TaskNetworkManager();
        resMan = new ResourceManager();
        threats = new HashSet<>();
        potentialSupporters = new HashMap<>();
        addableActions = null;
        addableTemplates = null;

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
        this.refCounter = new RefCounter(st.refCounter);
        this.pgr = st.pgr;
        this.actions = new ArrayList<>(st.actions);
        depth = st.depth + 1;
        problemRevision = st.problemRevision;
        csp = new MetaCSP<>(st.csp);
        tdb = new TimelinesManager(st.tdb, this); //st.tdb.deepCopy();
        taskNet = st.taskNet.deepCopy();
        supportConstraints = new LinkedList<>(st.supportConstraints);
        resMan = st.resMan.DeepCopy();
        threats = new HashSet<>(st.threats);
        potentialSupporters = new HashMap<>(st.potentialSupporters);
        addableActions = st.addableActions != null ? st.addableActions.clone() : null;
        addableTemplates = st.addableTemplates != null ? st.addableTemplates : null;
    }

    public State cc() {
        return new State(this);
    }

    /**
     * Returns true if the action can be added to the State. This is populated by reasonning on derivabilities,
     * depending on the planner used.
     */
    public boolean isAddable(AbstractAction a) {
        if(addableTemplates == null) {
            assert addableActions == null : "Addable action where added without generating the related templates.";
            return true; // addable by default
        } else {
            return addableTemplates.contains(a);
        }
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
        ret += "  consumers: " + tdb.getConsumers().size() + "\n";
        for (Timeline b : tdb.getConsumers()) {
            ret += b.Report();
        }
        ret += "\n";
        ret += Printer.taskNetwork(this, taskNet);
        //ret += "  databases: "+this.tdb.report()+"\n";

        ret += "}\n";

        return ret;
    }

    public boolean containsTimelineWithID(int tlID) {
        for (Timeline db : tdb.getTimelines()) {
            if (db.mID == tlID) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieve the Timeline with the same ID.
     *
     * @param timelineID ID of the timeline to lookup
     * @return The timeline with the same ID.
     */
    public Timeline getTimeline(int timelineID) {
        return tdb.getTimeline(timelineID);
    }

    /**
     * @param s a logical statement to look for.
     * @return the Action containing s. Returns null if no action containing s
     * was found.
     */
    public Action getActionContaining(LogStatement s) {
        if(s.container() instanceof Action) {
            Action a = (Action) s.container();
            assert a.contains(s);
            assert getAllActions().contains(a);
            return a;
        } else {
            assert getAllActions().stream().noneMatch(a -> a.contains(s));
            return null;
        }
    }

    public void breakCausalLink(LogStatement supporter, LogStatement consumer) {
        tdb.breakCausalLink(supporter, consumer);
    }

    /**
     * Remove a statement from the state. It does so by identifying the temporal
     * database in which the statement appears and removing it from the
     * database. If necessary, the database is split in two.
     *
     * @param s Statement to remove.
     */
    private void removeStatement(LogStatement s) {
        tdb.removeStatement(s);
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
        for(int i=0 ; i<a.args().length ; i++) {
            if(!unified(a.arg(i), b.arg(i)))
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
            return unifiable(a.args(), b.args());
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
    public boolean unifiable(VarRef[] as, VarRef[] bs) {
        assert as.length == bs.length : "The two lists have different size.";
        for (int i = 0; i < as.length ; i++) {
            if (!unifiable(as[i], bs[i])) {
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
     * @param db the timeline (to be enabled)
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
        actions.add(act);
        assert taskNet.getNumActions() == actions.size();
        assert actions.get(act.id().id()) == act;
        apply(act);

        // make sure that this action is executed after earliest execution.
        // this constraint is flagged with the start of the action time point which can be used for removal
        // (when an action is executed)
        csp.stn().enforceMinDelayWithID(pb.earliestExecution(), act.start(), 0, act.start());

        if(pgr != null)
            pgr.createActionInstantiationVariable(act, this);

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
        assert !s.sv().func().isConstant() : "LogStatement on a constant function: "+s;
        csp.stn().enforceBefore(s.start(), s.end());
        tdb.addNewTimeline(s);
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
            for (VarRef v : c.sv().args()) {
                assert v instanceof InstanceRef;
                values.add(((InstanceRef) v).instance());
            }
            assert c.variable() instanceof InstanceRef;
            values.add(((InstanceRef) c.variable()).instance());
            csp.bindings().addAllowedTupleToNAryConstraint(c.sv().func().name(), values);
        } else if (bc instanceof VarEqualityConstraint) {
            VarEqualityConstraint c = (VarEqualityConstraint) bc;
            csp.bindings().AddUnificationConstraint(c.leftVar(), c.rightVar());
        } else if (bc instanceof VarInequalityConstraint) {
            VarInequalityConstraint c = (VarInequalityConstraint) bc;
            csp.bindings().AddSeparationConstraint(c.leftVar(), c.rightVar());
        } else if (bc instanceof EqualityConstraint) {
            EqualityConstraint c = (EqualityConstraint) bc;
            List<VarRef> variables = new LinkedList<>(Arrays.asList(c.sv().args()));
            variables.add(c.variable());
            csp.bindings().addNAryConstraint(variables, c.sv().func().name());
        } else if (bc instanceof InequalityConstraint) {
            // create a new value tmp such that
            // c.sv == tmp and tmp != c.variable
            InequalityConstraint c = (InequalityConstraint) bc;
            List<VarRef> variables = new LinkedList<>(Arrays.asList(c.sv().args()));
            VarRef tmp = new VarRef(c.sv().func().valueType(), refCounter);
            csp.bindings().AddVariable(tmp, pb.instances().jInstancesOfType(tmp.typ()));
            variables.add(tmp);
            csp.bindings().addNAryConstraint(variables, c.sv().func().name());
            csp.bindings().AddSeparationConstraint(tmp, c.variable());
        } else if (bc instanceof IntegerAssignmentConstraint) {
            IntegerAssignmentConstraint c = (IntegerAssignmentConstraint) bc;
            List<String> values = new LinkedList<>();
            for (VarRef v : c.sv().args()) {
                assert v instanceof InstanceRef;
                values.add(((InstanceRef) v).instance());
            }
            csp.bindings().addPossibleValue(c.value());
            csp.bindings().addAllowedTupleToNAryConstraint(c.sv().func().name(), values, c.value());
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

        if(tc instanceof MinDelayConstraint) {
            csp.stn().enforceMinDelay(((MinDelayConstraint) tc).src(), ((MinDelayConstraint) tc).dst(), ((MinDelayConstraint) tc).minDelay());
        } else if(tc instanceof ParameterizedMinDelayConstraint) {
            ParameterizedMinDelayConstraint pmd = (ParameterizedMinDelayConstraint) tc;
            assert pmd.minDelay().func().isConstant() : "Cannot parameterize an action duration with non-constant functions.";
            assert pmd.minDelay().func().valueType().equals("integer") : "Cannot parameterize an action duration with a non-integer function.";
            VarRef var = new VarRef("integer", refCounter);
            csp.bindings().AddIntVariable(var);
            List<VarRef> varsOfExtConst = new ArrayList<>(Arrays.asList(pmd.minDelay().args()));
            varsOfExtConst.add(var);
            csp.bindings().addNAryConstraint(varsOfExtConst, pmd.minDelay().func().name());
            csp.addMinDelay(pmd.src(), pmd.dst(), var, pmd.trans());
        } else {
            throw new UnsupportedOperationException("Temporal contrainst: "+tc+" is not supported yet.");
        }
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

        for (Tuple2<TPRef, String> tp : mod.flexibleTimepoints()) {
            final TPRef t = tp._1();
            if(t.equals(pb.start()) || t.equals(pb.end()) || t.equals(pb.earliestExecution()))
                continue;

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
                    throw new FAPEException("Unknown time point type: " + tp._2());
            }
        }

        // for every instance declaration, create a new CSP Var with itself as domain
        for (String instance : mod.instances()) {
            csp.bindings().addPossibleValue(instance);
            List<String> domain = new LinkedList<>();
            domain.add(instance);
            csp.bindings().AddVariable(pb.instances().referenceOf(instance), domain);
        }

        // Declare new variables to the constraint network.
        for (VarRef var : mod.vars()) {
            Collection<String> domain = pb.instances().jInstancesOfType(var.typ());
            csp.bindings().AddVariable(var, domain);
        }

        for(BindingConstraint bc : mod.bindingConstraints())
            apply(mod, bc);

        // last: virtual time points might refer to start/end of nested actions
        for(AnchoredTimepoint anchored : mod.anchoredTimepoints()) {
            csp.stn().addVirtualTimePoint(anchored.timepoint(), anchored.anchor(), anchored.delay());
        }

        // apply all remaining temporal constraints (those not represented with rigid time points)
        for (TemporalConstraint tc : mod.temporalConstraints()) {
            apply(mod, tc);
        }

        // needs time points to be defined
        for(Task ac : mod.tasks()) {
            csp.stn().enforceBefore(ac.start(), ac.end());

            if(mod instanceof Action)
                taskNet.insert(ac, (Action) mod);
            else
                taskNet.insert(ac);

            if(pgr != null)
                pgr.createTaskSupportersVariables(ac, this);
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
    public List<Resolver> retainValidResolvers(Flaw f, List<Resolver> opts) {
        if (f instanceof Threat || f instanceof UnboundVariable ||
                f instanceof ResourceFlaw || f instanceof UnsupportedTaskCond || f instanceof UnmotivatedAction) {
            return opts;
        } else if (f instanceof UnsupportedTimeline) {
            Action requiredAncestor = getSupportConstraint(((UnsupportedTimeline) f).consumer);
            if (requiredAncestor == null) {
                return opts;
            } else {
                // we have a constraint stating that any resolver must be deriving from this action, filter resolvers
                return opts.stream()
                        .filter(res -> isOptionDerivedFrom(res, requiredAncestor))
                        .collect(Collectors.toList());
            }
        } else {
            throw new FAPEException("Error: Unrecognized flaw type.");
        }
    }

    /**
     * Checks if there is a support constraint for a temporal database.
     *
     * @param db The temporal database that needs to be supported.
     * @return An action if the resolvers for this database must derive
     * from this action (e.g. this flaw was previously addressed by
     * decomposing a task with this action. null if there is no constraints.
     */
    private Action getSupportConstraint(Timeline db) {
        for (Pair<Integer, Action> constraint : supportConstraints) {
            if (constraint.value1 == mID) {
                return constraint.value2;
            }
        }
        return null;
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
     * @param requiredAncestor Action from which the resolver must be issued
     *                         (either task or action in its descendants)
     * @return True if the resolver is valid, False otherwise.
     */
    private boolean isOptionDerivedFrom(Resolver opt, Action requiredAncestor) {
        // this unsupported db must be supported by a descendant of a decomposition
        // this is a consequence of an earlier commitment.
        if (opt instanceof SupportingTimeline) {
            // DB supporters are limited to those coming from an action descending from dec.
            Timeline db = getTimeline(((SupportingTimeline) opt).supporterID);

            // get the supporting chain component. (must provide a change on the state variable)
            ChainComponent cc = db.getChangeNumber(((SupportingTimeline) opt).supportingComponent);

            assert cc != null : "There is no support statement in " + db;
            assert cc.change : "Support is not a change.";
            assert cc.size() == 1;
            Action a = getActionContaining(cc.getFirst());

            return a != null && taskNet.isDescendantOf(a, requiredAncestor);
        } else if(opt instanceof SupportingTaskDecomposition) {
            return taskNet.isDescendantOf(((SupportingTaskDecomposition) opt).task, requiredAncestor);
        }else {
            return false;
        }
    }

    public void addSupportConstraint(ChainComponent cc, Action act) {
        supportConstraints.removeIf(p -> p.value1 == cc.mID);
        supportConstraints.add(new Pair<>(cc.mID, act));
    }


    public void timelineAdded(Timeline a) {
        for(Timeline b : tdb.getTimelines()) {
            if(!unifiable(a.stateVariable, b.stateVariable))
                continue;

            if(AllThreatFinder.isThreatening(this, a, b)) {
                threats.add(new PotentialThreat(a, b));
            }
        }

        if(a.isConsumer()) {
            // gather all potential supporters for this new timeline
            potentialSupporters.put(a.mID, new IList<SupportingTimeline>());
            for(Timeline sup : getTimelines()) {
                for(int i = 0 ; i < sup.numChanges() ; i++) {
                    if(UnsupportedTimeline.isSupporting(sup, i, a, this))
                        potentialSupporters.put(a.mID, potentialSupporters.get(a.mID).with(new SupportingTimeline(sup.mID, i, a)));
                }
            }
        }

        // checks if this new timeline can provide support to others
        for(int i=0 ; i < a.numChanges() ; i++) {
            for(Timeline b : tdb.getConsumers()) {
                if (UnsupportedTimeline.isSupporting(a, i, b, this)) {
                    potentialSupporters.put(b.mID, potentialSupporters.get(b.mID).with(new SupportingTimeline(a.mID, i, b)));
                }
            }
        }


    }

    public void timelineExtended(Timeline tl) {
        List<PotentialThreat> toRemove = new LinkedList<>();
        for(PotentialThreat pt : threats)
            if(pt.id1 == tl.mID || pt.id2 == tl.mID)
                toRemove.add(pt);
        threats.removeAll(toRemove);

        for(Timeline b : tdb.getTimelines()) {
            if(AllThreatFinder.isThreatening(this, tl, b)) {
                threats.add(new PotentialThreat(tl, b));
            }
        }

        if(tl.isConsumer()) assert tdb.getConsumers().contains(tl);

        // checks if the modifications on this timeline creates new supporters for others
        for(int i=0 ; i < tl.numChanges() ; i++) {
            for(Timeline b : tdb.getConsumers()) {
                if (UnsupportedTimeline.isSupporting(tl, i, b, this))
                    potentialSupporters.put(b.mID, potentialSupporters.get(b.mID).with(new SupportingTimeline(tl.mID, i, b)));
            }
        }
    }

    public void timelineRemoved(Timeline tl) {
        List<PotentialThreat> toRemove = new LinkedList<>();
        for(PotentialThreat t : threats)
            if(t.id1 == tl.mID || t.id2 == tl.mID)
                toRemove.add(t);
        threats.removeAll(toRemove);
    }

    /**
     * Retrieves all valid supporters for this timeline.
     *
     * As a side effects, this method also cleans up the supporters store in this state to remove double entries
     * and supporters that are not valid anymore.
     */
    public Set<SupportingTimeline> getTimelineSupportersFor(Timeline consumer) {
        assert tdb.getConsumers().contains(consumer);
        HashSet<SupportingTimeline> supporters = new HashSet<>();
        for(SupportingTimeline sup : potentialSupporters.get(consumer.mID)) {
            assert sup.consumerID == consumer.mID;
            if(!supporters.contains(sup)
                    && containsTimelineWithID(sup.supporterID)
                    && UnsupportedTimeline.isSupporting(getTimeline(sup.supporterID), sup.supportingComponent, consumer, this)) {
                supporters.add(sup);
            }
        }
        potentialSupporters.put(consumer.mID, new IList<SupportingTimeline>(supporters));
        return supporters;
    }

    public List<Flaw> getAllThreats() {
        List<PotentialThreat> toRemove = new LinkedList<>();
        List<Flaw> verifiedThreats = new LinkedList<>();
        for(PotentialThreat pt : threats) {
            Timeline tl1 = getTimeline(pt.id1);
            Timeline tl2 = getTimeline(pt.id2);
            if(AllThreatFinder.isThreatening(this, tl1, tl2)) {
                verifiedThreats.add(new Threat(tl1, tl2));
            } else {
                assert !AllThreatFinder.isThreatening(this, tl2, tl1);
                toRemove.add(pt);
            }
        }
        threats.removeAll(toRemove);

        // this check is quite expensive
//        assert verifiedThreats.size() == AllThreatFinder.getAllThreats(this).size();
        return verifiedThreats;
    }

    public TimedCanvas getCanvasOfActions() {
        List<Action> acts = new LinkedList<>(getAllActions());
        Collections.sort(acts, new Comparator<Action>() {
            @Override
            public int compare(Action a1, Action a2) {
                return (int) (getEarliestStartTime(a1.start()) - getEarliestStartTime(a2.start()));
            }
        });
        List<ChartLine> lines = new LinkedList<>();

        for (Action a : acts) {
            int start = (int) getEarliestStartTime(a.start());
            int earliestEnd = (int) getEarliestStartTime(a.end());
            String name = Printer.action(this, a);
            TextLabel label = new TextLabel(name, "action-name");

            switch (a.status()) {
                case EXECUTED:
                    lines.add(new ChartLine(label, new RectElem(start, earliestEnd - start, "successful")));
//                    sb.append(String.format("%s started:%s ended:%s  [EXECUTED]\n", name, start, earliestEnd));
                    break;
                case EXECUTING:
                case PENDING:
                    if (getDurationBounds(a).nonEmpty()) {
                        int min = getDurationBounds(a).get()._1();
                        int max = getDurationBounds(a).get()._2();
                        lines.add(new ChartLine(label,
                                new RectElem(start, min, "pending"),
                                new RectElem(start + min + 0.1f, max - min, "uncertain")));
//                        sb.append(String.format("%s \t\tstarted: %s\tduration in [%s, %s]  [EXECUTING]\n", name, start, min, max));
                    } else {
                        lines.add(new ChartLine(label, new RectElem(start, earliestEnd - start, "pending")));
//                        sb.append(String.format("%s \t\tstarted: %s\tmin-duration: %s  [EXECUTING]\n", name, start, earliestEnd-start));
                    }
                    break;
                case FAILED:
            }
        }
        return new TimedCanvas(lines, getEarliestStartTime(pb.earliestExecution()));
    }

    /*** Wrapper around STN ******/

    public void enforceBefore(TPRef a, TPRef b) { csp.stn().enforceBefore(a, b); }

    public void enforceBefore(Collection<TPRef> as, TPRef b) {
        for(TPRef a : as)
            enforceBefore(a, b);
    }

    public void enforceBefore(TPRef a, Collection<TPRef> bs) {
        for(TPRef b : bs)
            enforceBefore(a, b);
    }

    public void enforceStrictlyBefore(TPRef a, TPRef b) { csp.stn().enforceStrictlyBefore(a, b); }

    public boolean canBeBefore(Timeline tl1, Timeline tl2) {
        if(!tl1.hasSinglePersistence()) {
            if (tl2.hasSinglePersistence()) {
                return canAllBeBefore(tl1.getSupportTimePoint(), tl2.getFirstTimePoints());
            } else {
                assert tl2.get(0).change : "First statement of timeline containing changes is a persistence";
                return //canAllBeBefore(tl1.getSupportTimePoint(), tl2.getFirstTimePoints()) &&
                        canAllBeBefore(tl1.getLastTimePoints(), tl2.getFirstChangeTimePoint());
            }
        } else {
            assert !tl2.hasSinglePersistence() : "Temporal comparison of two timelien with persistences only is not supported. " +
                    "Usually this case should have been filtered out.";
            return canAllBeBefore(tl1.getLastTimePoints(), tl2.getFirstChangeTimePoint());
        }
    }

    public void enforceStrictlyBefore(Collection<TPRef> as, TPRef b) {
        for(TPRef a : as)
            enforceStrictlyBefore(a, b);
    }

    public void enforceStrictlyBefore(TPRef a, Collection<TPRef> bs) {
        for(TPRef b : bs)
            enforceStrictlyBefore(a, b);
    }

    public boolean canBeBefore(TPRef a, TPRef b) { return csp.stn().canBeBefore(a, b); }

    /** Returns true if any time point in as can be before a time point in bs */
    public boolean canAnyBeBefore(Collection<TPRef> as, Collection<TPRef> bs) {
        for(TPRef a : as) {
            for(TPRef b : bs) {
                if(canBeBefore(a, b))
                    return true;
            }
        }
        return false;
    }

    /** Returns true if any time point in as can be before b */
    public boolean canAnyBeBefore(Collection<TPRef> as, TPRef b) {
        for(TPRef a : as) {
            if(canBeBefore(a, b))
                return true;
        }
        return false;
    }

    /** Returns true if a can be before a time point in bs */
    public boolean canAnyBeBefore(TPRef a, Collection<TPRef> bs) {
        for(TPRef b : bs) {
            if(canBeBefore(a, b))
                return true;
        }
        return false;
    }

    /** Returns true if any time point in as can be before b */
    public boolean canAnyBeStrictlyBefore(Collection<TPRef> as, TPRef b) {
        for(TPRef a : as) {
            if(canBeStrictlyBefore(a, b))
                return true;
        }
        return false;
    }

    /** Returns true if a can be before a time point in bs */
    public boolean canAnyBeStrictlyBefore(TPRef a, Collection<TPRef> bs) {
        for(TPRef b : bs) {
            if(canBeStrictlyBefore(a, b))
                return true;
        }
        return false;
    }

    /** Returns true if all time point in as can be before all time points in bs */
    public boolean canAllBeBefore(Collection<TPRef> as, Collection<TPRef> bs) {
        for(TPRef a : as) {
            for(TPRef b : bs) {
                if(!canBeBefore(a, b))
                    return false;
            }
        }
        return true;
    }

    /** Returns true if all time point in as can be before b */
    public boolean canAllBeBefore(Collection<TPRef> as, TPRef b) {
        for(TPRef a : as) {
            if(!canBeBefore(a, b))
                return false;
        }
        return true;
    }

    /** Returns true if a can be before all time point in bs */
    public boolean canAllBeBefore(TPRef a, Collection<TPRef> bs) {
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

    public STNUDispatcher<GlobalRef> getDispatchableSTNU() {
        return new STNUDispatcher<>(csp.stn());
    }

    public Option<Tuple2<Integer,Integer>> getDurationBounds(Action a) {
        return csp.stn().contingentDelay(a.start(), a.end());
    }


    /********* Wrapper around the task network **********/

    public Action getAction(ActRef actionID) { return getAction(actionID.id()); }

    public List<Action> getAllActions() { return actions; }

    public Action getAction(int actionID) {
        if(actionID == -1)
            return null;
        assert actions.get(actionID).id().id() == actionID;
        return actions.get(actionID);
    }

    public List<Task> getOpenTasks() { return taskNet.getOpenTasks(); }

    public List<Action> getUnmotivatedActions() { return taskNet.getNonSupportedMotivatedActions(); };

    /**
     *  Unifies the time points of the action condition and those of the action, and add
     * the support link in the task network.
     */
    public void addSupport(Task cond, Action act) {
        csp.stn().enforceConstraint(cond.start(), act.start(), 0, 0);
        csp.stn().enforceConstraint(cond.end(), act.end(), 0, 0);
        if(this.pgr != null)
            addUnificationConstraint(cond.groundSupportersVar(), act.instantiationVar());
        taskNet.addSupport(cond, act);
    }

    public int getNumActions() { return taskNet.getNumActions(); }

    public int getNumRoots() { return taskNet.getNumRoots(); }

    public void exportTaskNetwork(String filename) { taskNet.exportToDot(this, filename); }

    /******** Wrapper around the constraint network ***********/

    public boolean canSupport(Action act, Task task) {
        if(!act.taskName().equals(task.name()))
            return false;
        assert act.args().size() == task.args().size();
        for(int i=0 ; i<act.args().size() ; i++)
            if(!unifiable(act.args().get(i), task.args().get(i)))
                return false;

        return canBeBefore(act.start(), task.start()) &&
                canBeBefore(task.start(), act.start()) &&
                canBeBefore(act.end(), task.end()) &&
                canBeBefore(task.end(), act.end());
    }

    public List<String> domainOf(VarRef var) { return csp.bindings().domainOf(var); }

    public int domainSizeOf(VarRef var) { return csp.bindings().domainSize(var); }

    public void addUnificationConstraint(VarRef a, VarRef b) { csp.bindings().AddUnificationConstraint(a, b); }

    public void addUnificationConstraint(ParameterizedStateVariable a, ParameterizedStateVariable b) {
        assert a.func() == b.func() : "Error unifying two state variable on different functions.";
        assert a.args().length == b.args().length : "Error different number of arguments.";
        for (int i = 0; i < a.args().length; i++) {
            csp.bindings().AddUnificationConstraint(a.arg(i), b.arg(i));
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

    public boolean unified(VarRef a, VarRef b) { return csp.bindings().unified(a, b); }

    public boolean unifiable(VarRef a, VarRef b) { return csp.bindings().unifiable(a, b); }

    public boolean separable(VarRef a, VarRef b) { return csp.bindings().separable(a, b); }

    public void addValuesToValuesSet(String setID, List<String> values) { csp.bindings().addAllowedTupleToNAryConstraint(setID, values);}

    public void addValuesSetConstraint(List<VarRef> variables, String setID) { csp.bindings().addNAryConstraint(variables, setID);}


    /************ Wrapper around TemporalDatabaseManager **********************/

    public Iterable<Timeline> getTimelines() { return tdb.getTimelines(); }

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
        Action toRemove = taskNet.getAction(actRef);
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
                Action copy = null; //Factory.getInstantiatedAction(pb, oldAction.abs(), params, oldAction.id()); TODO: depecated, action should be immutable

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
