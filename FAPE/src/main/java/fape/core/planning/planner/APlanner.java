package fape.core.planning.planner;

import fape.core.execution.model.AtomicAction;
import fape.core.planning.Plan;
import fape.core.planning.Planner;
import fape.core.planning.preprocessing.ActionDecompositions;
import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.preprocessing.LiftedDTG;
import fape.core.planning.resources.Resource;
import fape.core.planning.search.*;
import fape.core.planning.search.resolvers.*;
import fape.core.planning.search.resolvers.TemporalConstraint;
import fape.core.planning.search.strategies.flaws.FlawCompFactory;
import fape.core.planning.search.strategies.plans.LMC;
import fape.core.planning.search.strategies.plans.PlanCompFactory;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import fape.util.TimeAmount;
import fape.util.TinyLogger;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractDecomposition;
import planstack.anml.model.concrete.*;
import planstack.anml.model.concrete.Decomposition;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.ResourceStatement;
import planstack.anml.model.concrete.statements.Statement;
import planstack.anml.parser.ParseResult;

import java.util.*;

/**
 * Base for any planner in FAPE. It defines all basic operations useful for
 * planning such as alterations of search states, inclusions of ANML blocks ...
 *
 * Classes that inherit from it only have to implement the abstract methods to
 * provide a search policy. Overriding methods can also be done to override the
 * default behaviour.
 */
public abstract class APlanner {

    public class ActionExecution {
        final ActRef id;
        final AbstractAction abs;
        long startTime;
        long endTime;
        List<String> args;
        ActionStatus status;


        public ActionExecution(Action a, List<String> args, long startTime) {
            this.id = a.id();
            this.abs = a.abs();
            this.startTime = startTime;
            this.args = args;
            this.status = ActionStatus.EXECUTING;
        }

        public void setSuccess(long endTime) {
            this.endTime = endTime;
            this.status = ActionStatus.EXECUTED;
        }

        public void setFailed() {
            this.status = ActionStatus.FAILED;
        }

        public Action createNewGroundAction() {
            List<VarRef> argVars = new LinkedList<>();
            for(String arg : args) {
                argVars.add(pb.instances().referenceOf(arg));
            }
            return Factory.getInstantiatedAction(pb, abs, argVars, id);
        }
    }

    public Map<ActRef, ActionExecution> executedAction = new HashMap<>();
    public int currentTime = 0;

    public static APlanner currentPlanner = null;

    public static boolean debugging = true;
    public static boolean logging = true;
    public static boolean actionResolvers = true; // do we add actions to resolve flaws?

    public int GeneratedStates = 1; //count the initial state
    public int OpenedStates = 0;

    public final AnmlProblem pb = new AnmlProblem();
    LiftedDTG dtg = null;

    public List<Resource> resourcePrototype = new LinkedList<>();

    /**
     * Used to build comparators for flaws. Default to a least commiting first.
     */
    public String[] flawSelStrategies = {"lcf"};

    /**
     * Used to build comparators for partial plans.
     */
    public String[] planSelStrategies = {"soca"};

    /**
     * A short identifier for the planner.
     *
     * @return THe planner ID.
     */
    public abstract String shortName();

    public abstract ActionSupporterFinder getActionSupporterFinder();

    /**
     *
     */
    public PriorityQueue<State> queue;

    /**
     * applies a resolver to the state
     *
     * TODO: rewrite to a polymorphic switch instead of checking nulls in
     * variables
     *
     * @param next
     * @param o
     * @param consumer
     * @return
     */
    public boolean ApplyOption(State next, Resolver o, TemporalDatabase consumer) {
        TemporalDatabase supporter = null;
        ChainComponent precedingComponent = null;
        if (o instanceof SupportingDatabase) {
            supporter = next.GetDatabase(((SupportingDatabase) o).temporalDatabase);
            if (((SupportingDatabase) o).precedingChainComponent != -1) {
                precedingComponent = supporter.GetChainComponent(((SupportingDatabase) o).precedingChainComponent);
            }
        }

        TinyLogger.LogInfo("[" + next.mID + "] Adding " + o);

        //now we can happily apply all the options
        if (supporter != null && precedingComponent != null) {

            assert consumer != null : "Consumer was not passed as an argument";
            // this is database merge of one persistence into another
            assert consumer.chain.size() == 1 && !consumer.chain.get(0).change : "This is restricted to databases containing single persistence only";

            assert precedingComponent.change;
            causalLinkAdded(next, precedingComponent.contents.getFirst(), consumer.chain.getFirst().contents.getFirst());

            next.insertDatabaseAfter(supporter, consumer, precedingComponent);

        } else if (supporter != null) {

            assert consumer != null : "Consumer was not passed as an argument";

            ChainComponent supportingStatement = supporter.getSupportingComponent();

            assert supportingStatement != null && supportingStatement.change;
            causalLinkAdded(next, supportingStatement.contents.getFirst(), consumer.chain.getFirst().contents.getFirst());

            // database concatenation
            next.insertDatabaseAfter(supporter, consumer, supporter.chain.getLast());

        } else if (o instanceof SupportingAction) {

            assert consumer != null : "Consumer was not passed as an argument";

            Action action = Factory.getStandaloneAction(pb, ((SupportingAction) o).act);
            next.insert(action);

            if(((SupportingAction) o).values != null)
                // restrict domain of given variables to the given set of variables.
                for (LVarRef lvar : ((SupportingAction) o).values.keySet()) {
                    next.restrictDomain(action.context().getGlobalVar(lvar), ((SupportingAction) o).values.get(lvar));
                }

            // create the binding between consumer and the new statement in the action that supports it
            TemporalDatabase supportingDatabase = null;
            for (Statement s : action.statements()) {
                if (s instanceof LogStatement && next.canBeEnabler((LogStatement) s, consumer)) {
                    assert supportingDatabase == null : "Error: several statements might support the database";
                    supportingDatabase = next.getDBContaining((LogStatement) s);
                }
            }
            if (supportingDatabase == null) {
                return false;
            } else {
                Resolver opt = new SupportingDatabase(supportingDatabase.mID);
                return ApplyOption(next, opt, consumer);
            }

        } else if (o instanceof fape.core.planning.search.resolvers.Decomposition) {
            // Apply the i^th decomposition of o.actionToDecompose, where i is given by
            // o.decompositionID

            // Action to decomposed
            Action decomposedAction = o.actionToDecompose();

            // Abstract version of the decomposition
            AbstractDecomposition absDec = decomposedAction.decompositions().get(((fape.core.planning.search.resolvers.Decomposition) o).decID);

            // Decomposition (ie implementing StateModifier) containing all changes to be made to a search state.
            Decomposition dec = Factory.getDecomposition(pb, decomposedAction, absDec);

            // remember that the consuming db has to be supporting by a descendant of this decomposition.
            if (consumer != null) {
                next.addSupportConstraint(consumer.GetChainComponent(0), dec);
            }

            next.applyDecomposition(dec);

        } else if (o instanceof TemporalSeparation) {
            for (LogStatement first : ((TemporalSeparation) o).first.chain.getLast().contents) {
                for (LogStatement second : ((TemporalSeparation) o).second.chain.getLast().contents) {
                    next.enforceBefore(first.end(), second.start());
                }
            }
        } else if (o instanceof BindingSeparation) {
            next.addSeparationConstraint(((BindingSeparation) o).a, ((BindingSeparation) o).b);
        } else if (o instanceof VarBinding) {
            List<String> values = new LinkedList<>();
            values.add(((VarBinding) o).value);
            next.restrictDomain(((VarBinding) o).var, values);
        } else if (o instanceof StateVariableBinding) {
            next.addUnificationConstraint(((StateVariableBinding) o).one, ((StateVariableBinding) o).two);
        } else if (o instanceof ResourceSupportingAction) {
            ResourceSupportingAction opt = (ResourceSupportingAction) o;
            Action action = Factory.getStandaloneAction(pb, opt.action);
            //add the actual action
            next.insert(action);

            //unify the state variables supporting the resource
            ResourceStatement theSupport = null;
            for (ResourceStatement s : action.resourceStatements()) {
                if (s.sv().func().name().equals(opt.unifyingResourceVariable.func().name())) {
                    if (theSupport != null) {
                        throw new FAPEException("Distinguishing resource events upon the same resource in one action needs to be implemented.");
                    } else {
                        theSupport = s;
                    }
                }
            }
            next.addUnificationConstraint(theSupport.sv(), opt.unifyingResourceVariable);

            //add temporal constraint
            if (opt.before) {
                //the new action must occour before the given time point
                next.enforceBefore(action.end(), opt.when);
            } else {
                //vice-versa
                next.enforceBefore(opt.when, action.start());
            }
        } else if (o instanceof ResourceSupportingDecomposition) {
            ResourceSupportingDecomposition opt = (ResourceSupportingDecomposition) o;
             // Apply the i^th decomposition of o.actionToDecompose, where i is given by
            // o.decompositionID

            // Action to decomposed
            Action decomposedAction = opt.resourceMotivatedActionToDecompose;

            // Abstract version of the decomposition
            AbstractDecomposition absDec = decomposedAction.decompositions().get(((ResourceSupportingDecomposition) o).decompositionID);

            // Decomposition (ie implementing StateModifier) containing all changes to be made to a search state.
            Decomposition dec = Factory.getDecomposition(pb, decomposedAction, absDec);

            // remember that the consuming db has to be supporting by a descendant of this decomposition.
            if (consumer != null) {
                next.addSupportConstraint(consumer.GetChainComponent(0), dec);
            }

            next.applyDecomposition(dec);

            //TODO(fdvorak): here we should add the binding between the statevariable of supporting resource event in one of the decomposed actions
            //for now we leave it to search
        } else if (o instanceof TemporalConstraint) {
            TemporalConstraint tc = (TemporalConstraint) o;
            next.enforceConstraint(tc.first, tc.second, tc.min, tc.max);
        } else {
            throw new FAPEException("Unknown option.");
        }

        // if the propagation failed and we have achieved an inconsistent state
        return next.isConsistent();
    }

    /**
     * This method is invoked whenever a causal link is added and offers a way
     * to react to it for planner extending this class.
     *
     * @param st
     * @param supporter
     * @param consumer
     */
    public void causalLinkAdded(State st, LogStatement supporter, LogStatement consumer) {
    }

    /**
     * we remove the action results from the system
     *
     * @param
     */
    public void FailAction(ActRef actionRef) {
        KeepBestStateOnly();
        assert best != null;
        State st = GetCurrentState();
        st.setActionFailed(actionRef);
        executedAction.get(actionRef).setFailed();
    }

    /**
     * Set the action ending to the its real end time. It removes the duration
     * constraints between the starts and the end of the action (as given by the
     * duration anml variable). Adds a new constraint [realEndTime, realEndtime]
     * between the global start and the end of action time points.
     *
     * @param actionID
     * @param realEndTime
     */
    public void AddActionEnding(ActRef actionID, int realEndTime) {
        KeepBestStateOnly();
        State st = GetCurrentState();
        Action a = st.getAction(actionID);
        st.setActionSuccess(a, realEndTime);
        executedAction.get(actionID).setSuccess(realEndTime);
    }

    public void setActionExecuting(AtomicAction aa) {
        State st = GetCurrentState();
        Action a = st.getAction(aa.id);
        st.setActionExecuting(a, (int) aa.mStartTime);
        executedAction.put(aa.id, new ActionExecution(a, aa.params, aa.mStartTime));
    }

    /**
     * Pushes the earliest execution time point forward. Causes all pending
     * actions to be delayed
     *
     * @param earliestExecution
     */
    public void SetEarliestExecution(int earliestExecution) {
        this.currentTime = earliestExecution;
        KeepBestStateOnly();
        State s = GetCurrentState();
        if(s != null) {
            s.enforceDelay(pb.start(), pb.earliestExecution(), earliestExecution);
            // If the STN is not consistent after this addition, the the current plan is not feasible.
            // Full replan is necessary
            if (!s.isConsistent()) {
                this.best = null;
                this.queue.clear();
            }
        }
    }

    /**
     *
     */
    public enum EPlanState {

        TIMEOUT,
        /**
         *
         */
        CONSISTENT,
        /**
         *
         */
        INCONSISTENT,
        /**
         *
         */
        INFESSIBLE,
        /**
         *
         */
        UNINITIALIZED
    }
    /**
     * what is the current state of the plan
     */
    public EPlanState planState = EPlanState.UNINITIALIZED;

    //current best state
    private State best = null;

    /**
     * initializes the data structures of the planning problem
     *
     */
    public void Init() {
        queue = new PriorityQueue<State>(100, this.stateComparator());
        queue.add(new State(pb));
        best = queue.peek();
    }

    /**
     *
     * @return
     */
    public State GetCurrentState() {
        return best;
    }

    /**
     * Remove all states in the queues except for the best one (which is stored
     * in best). This is to be used when updating the problem to make sure we
     * don't keep any outdated states.
     */
    public void KeepBestStateOnly() {
        queue.clear();

        if (best == null) {
            TinyLogger.LogInfo("No known best state.");
        } else {
            queue.add(best);
        }

    }

    public final List<Resolver> GetResolvers(State st, Flaw f) {
        List<Resolver> candidates;
        if (f instanceof UnsupportedDatabase) {
            candidates = GetSupporters(((UnsupportedDatabase) f).consumer, st);
        } else if (f instanceof UndecomposedAction) {
            UndecomposedAction ua = (UndecomposedAction) f;
            candidates = new LinkedList<>();
            for (int decompositionID = 0; decompositionID < ua.action.decompositions().size(); decompositionID++) {
                candidates.add(new fape.core.planning.search.resolvers.Decomposition(ua.action, decompositionID));
            }
        } else if (f instanceof Threat) {
            candidates = GetResolvers(st, (Threat) f);
        } else if (f instanceof UnboundVariable) {
            candidates = GetResolvers(st, (UnboundVariable) f);
        } else if (f instanceof ResourceFlaw) {
            candidates = ((ResourceFlaw) f).resolvers;
        } else {
            throw new FAPEException("Unknown flaw type: " + f);
        }

        return st.retainValidOptions(f, candidates);
    }

    public final List<Resolver> GetResolvers(State st, UnboundVariable uv) {
        List<Resolver> bindings = new LinkedList<>();
        for (String value : st.domainOf(uv.var)) {
            bindings.add(new VarBinding(uv.var, value));
        }
        return bindings;
    }

    public final List<Resolver> GetResolvers(State st, Threat f) {
        List<Resolver> options = new LinkedList<>();
        options.add(new TemporalSeparation(f.db1, f.db2));
        options.add(new TemporalSeparation(f.db2, f.db1));
        for (int i = 0; i < f.db1.stateVariable.jArgs().size(); i++) {
            options.add(new BindingSeparation(
                    f.db1.stateVariable.jArgs().get(i),
                    f.db2.stateVariable.jArgs().get(i)));
        }
        return options;
    }

    /**
     * Finds all flaws of a given state. Currently, threats and unbound
     * variables are considered only if no other flaws are present.
     *
     * @return A list of flaws present in the system. The set of flaw might not
     * be exhaustive.
     */
    public List<Flaw> GetFlaws(State st) {
        List<Flaw> flaws = new LinkedList<>();
        for (TemporalDatabase consumer : st.consumers) {
            flaws.add(new UnsupportedDatabase(consumer));
        }
        for (Action refinable : st.getOpenLeaves()) {
            flaws.add(new UndecomposedAction(refinable));
        }
        if (flaws.isEmpty()) {
            List<TemporalDatabase> dbs = st.getDatabases();
            for (int i = 0; i < dbs.size(); i++) {
                TemporalDatabase db1 = dbs.get(i);
                for (int j = i + 1; j < dbs.size(); j++) {
                    TemporalDatabase db2 = dbs.get(j);
                    if (isThreatening(st, db1, db2)) {
                        flaws.add(new Threat(db1, db2));
                    }
                }
            }
        }
        //find the resource flaws
        flaws.addAll(st.resourceFlaws());
        if (flaws.isEmpty()) {
            for (VarRef v : st.getUnboundVariables()) {
                flaws.add(new UnboundVariable(v));
            }
        }
        return flaws;
    }

    /**
     * Implementation of search. An easy thing to do to forward this call to the
     * aStar method.
     *
     * @param forhowLong Max time the planner is allowed to run.
     * @return A solution state if the planner found one. null otherwise.
     */
    public abstract State search(TimeAmount forhowLong);

    /**
     * Provides a comparator that is used to sort flaws. THe first flaw will be
     * selected to be resolved.
     *
     * @param st State in which the flaws appear.
     * @return The comparator to use for ordering.
     */
    public final Comparator<Pair<Flaw, List<Resolver>>> flawComparator(State st) {
        return FlawCompFactory.get(st, flawSelStrategies);
    }

    /**
     * The comparator used to order the queue. THe first state in the queue
     * (according to this comparator, will be selected for expansion.
     *
     * @return The comparator to use for ordering the queue.
     */
    public final Comparator<State> stateComparator() {
        return PlanCompFactory.get(planSelStrategies);
    }

    /**
     * Checks if two temporal databases are threatening each others. It is the
     * case if: - both are not consuming databases (start with an assignment).
     * Otherwise, threat is naturally handled by looking for supporters. - their
     * state variables are unifiable. - they can overlap TODO: handle case where
     * multiple persitences are in the last part.
     *
     * @return True there is a threat.
     */
    protected boolean isThreatening(State st, TemporalDatabase db1, TemporalDatabase db2) {
        if (db1.isConsumer() || db2.isConsumer()) {
            return false;
        } else if (st.Unifiable(db1, db2)) {
            if (st.canBeBefore(
                    db1.GetChainComponent(0).getConsumeTimePoint(), // c1
                    db2.GetChainComponent(db2.chain.size() - 1).getSupportTimePoint()) && // s2
                    st.canBeBefore(
                            db2.GetChainComponent(db2.chain.size() - 1).getSupportTimePoint(), // s2
                            db1.GetChainComponent(db1.chain.size() - 1).getSupportTimePoint())) {    // s1
                return true;
            } else if (st.canBeBefore(
                    db2.GetChainComponent(0).getConsumeTimePoint(), // c2
                    db1.GetChainComponent(db1.chain.size() - 1).getSupportTimePoint()) && // s1
                    st.canBeBefore(
                            db1.GetChainComponent(0).getConsumeTimePoint(), // c1
                            db2.GetChainComponent(0).getConsumeTimePoint())) {                  // c2
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public static boolean optimal = false;

    protected State aStar(TimeAmount forHowLong) {
        ///Planner.logging = true;
        // first start by checking all the consistencies and propagating necessary constraints
        // those are irreversible operations, we do not make any decisions on them
        //State st = GetCurrentState();
        //
        //st.bindings.PropagateNecessary(st);
        //st.tdb.Propagate(st);
        long deadLine = System.currentTimeMillis() + forHowLong.val;
        //initializace heuristic
        /**
         * search
         */
        while (true) {
            if (System.currentTimeMillis() > deadLine) {
                TinyLogger.LogInfo("Timeout.");
                this.planState = EPlanState.INCONSISTENT;
                break;
            }
            if (queue.isEmpty()) {
                if (!APlanner.optimal) {
                    TinyLogger.LogInfo("No plan found.");
                    this.planState = EPlanState.INFESSIBLE;
                    break;
                } else if (GetFlaws(best).isEmpty()) {
                    this.planState = EPlanState.CONSISTENT;
                    return best;
                }
            }
            //get the best state and continue the search
            State st = queue.remove();
            OpenedStates++;

            if (APlanner.debugging) {
                st.exportTemporalNetwork("current-stn.dot");
            }

            List<Flaw> flaws = GetFlaws(st);

            TinyLogger.LogInfo(st);
            if (flaws.isEmpty()) {
                if(Planner.debugging)
                    st.assertConstraintNetworkGroundAndConsistent();
                if (!APlanner.optimal) {
                    this.planState = EPlanState.CONSISTENT;
                    TinyLogger.LogInfo("Plan found:");
                    TinyLogger.LogInfo(st);
                    return st;
                }else{
                    best = st;
                    //remove all suboptimal candidates from the queue
                    List<State> remove = new LinkedList<>();
                    for(State s:queue){
                        if(LMC.singleton.cost(s) <= LMC.singleton.cost(best)){
                            remove.add(s);
                        }
                    }
                    queue.removeAll(remove);
                    continue;
                }
            }
            //continue the search
            LinkedList<Pair<Flaw, List<Resolver>>> opts = new LinkedList<>();
            for (Flaw flaw : flaws) {
                opts.add(new Pair(flaw, GetResolvers(st, flaw)));
            }

            //do some sorting here - min domain
            //Collections.sort(opts, optionsComparatorMinDomain);
            Collections.sort(opts, this.flawComparator(st));

            if (opts.isEmpty()) {
                throw new FAPEException("Error: no flaws but state was not found to be a solution.");
            }

            if (opts.getFirst().value2.isEmpty()) {
                TinyLogger.LogInfo("Dead-end, flaw without resolvers: " + opts.getFirst().value1);
                //dead end
                continue;
            }

            //we just take the first option here as a tie breaker by min-domain
            Pair<Flaw, List<Resolver>> opt = opts.getFirst();

            TinyLogger.LogInfo(" Flaw:" + opt.value1.toString());

            for (Resolver o : opt.value2) {

                TinyLogger.LogInfo("   Res: " + o);

                State next = new State(st);
                boolean success = false;
                if (opt.value1 instanceof Threat
                        || opt.value1 instanceof UnboundVariable
                        || opt.value1 instanceof UndecomposedAction
                        || opt.value1 instanceof ResourceFlaw) {
                    success = ApplyOption(next, o, null);
                } else {
                    success = ApplyOption(next, o, next.GetDatabase(((UnsupportedDatabase) opt.value1).consumer.mID));
                }
                //TinyLogger.LogInfo(next.Report());
                if (success) {
                    queue.add(next);
                    GeneratedStates++;
                } else {
                    TinyLogger.LogInfo("   Dead-end reached for state: " + next.mID);
                    //inconsistent state, doing nothing
                }
            }

        }
        return null;
    }

    /**
     * starts plan repair, records the best plan, produces the best plan after
     * <b>forHowLong</b> miliseconds or null, if no plan was found
     *
     * @param forHowLong
     */
    public boolean Repair(TimeAmount forHowLong) {
        KeepBestStateOnly();
        best = search(forHowLong);
        if (best == null) {
            return false;
        }
        //dfs(forHowLong);

        //we empty the queue now and leave only the best state there
        KeepBestStateOnly();
        return true;
    }

    /**
     *
     * @param db
     * @param st
     * @return
     */
    public List<Resolver> GetSupporters(TemporalDatabase db, State st) {
        //here we need to find several types of supporters
        //1) chain parts that provide the value we need
        //2) actions that provide the value we need and can be added
        //3) tasks that can decompose into an action we need
        List<Resolver> ret = new LinkedList<>();

        //get chain connections
        for (TemporalDatabase b : st.getDatabases()) {
            if (db == b || !st.Unifiable(db, b)) {
                continue;
            }
            // if the database has a single persistence we try to integrate it with other persistences.
            // except if the state variable is constant, in which case looking only for the assignments saves search effort.
            if (db.HasSinglePersistence() && !db.stateVariable.func().isConstant()) {
                //we are looking for chain integration too
                int ct = 0;
                for (ChainComponent comp : b.chain) {
                    if (comp.change && st.unifiable(comp.GetSupportValue(), db.GetGlobalConsumeValue())
                            && st.canBeBefore(comp.getSupportTimePoint(), db.getConsumeTimePoint())) {
                        ret.add(new SupportingDatabase(b.mID, ct));
                    }
                    ct++;
                }

                // Otherwise, check for databases containing a change whose support value can
                // be unified with our consume value.
            } else if (st.unifiable(b.GetGlobalSupportValue(), db.GetGlobalConsumeValue())
                    && !b.HasSinglePersistence()
                    && st.canBeBefore(b.getSupportTimePoint(), db.getConsumeTimePoint())) {
                ret.add(new SupportingDatabase(b.mID));
            }
        }

        // adding actions
        // ... the idea is to decompose actions as long as they provide some support that I need, if they cant, I start adding actions
        //find actions that help me with achieving my value through some decomposition in the task network
        //they are those that I can find in the virtual decomposition tree
        //first get the action names from the abstract dtgs
        //StateVariable[] varis = null;
        //varis = db.domain.values().toArray(varis);
        ActionSupporterFinder supporters = getActionSupporterFinder();
        ActionDecompositions decompositions = new ActionDecompositions(pb);
        Collection<AbstractAction> potentialSupporters = supporters.getActionsSupporting(st, db);

        for (Action leaf : st.getOpenLeaves()) {
            for (Integer decID : decompositions.possibleDecompositions(leaf, potentialSupporters)) {
                ret.add(new fape.core.planning.search.resolvers.Decomposition(leaf, decID));
            }
        }

        //now we can look for adding the actions ad-hoc ...
        if (APlanner.actionResolvers) {
            for (AbstractAction aa : potentialSupporters) {
                // only considere action that are not marked motivated.
                // TODO: make it complete (consider a task hierarchy where an action is a descendant of unmotivated action)
                if (!aa.isMotivated()) {
                    ret.add(new SupportingAction(aa));
                }
            }
        }

        return ret;
    }

    /**
     * progresses in the plan up for howFarToProgress, returns either
     * AtomicActions that were instantiated with corresponding start times, or
     * null, if not solution was found in the given time
     *
     * @param howFarToProgress
     * @return
     */
    public List<AtomicAction> Progress(TimeAmount howFarToProgress) {
        State myState = best;
        Plan plan = new Plan(myState);

        List<AtomicAction> ret = new LinkedList<>();
        for (Action a : plan.GetNextActions()) {
            long startTime = myState.getEarliestStartTime(a.start());
            if (startTime > howFarToProgress.val) {
                continue;
            }
            if (a.status() != ActionStatus.PENDING) {
                continue;
            }
            AtomicAction aa = new AtomicAction(a, startTime, a.maxDuration(), best);
            ret.add(aa);
        }

        Collections.sort(ret, new Comparator<AtomicAction>() {
            @Override
            public int compare(AtomicAction o1, AtomicAction o2) {
                return (int) (o1.mStartTime - o2.mStartTime);
            }
        });

        // for all selecting actions, we set them as being executed and we bind their start time point
        // to the one we requested.

        for(AtomicAction aa : ret) {
            setActionExecuting(aa);
        }

        return ret;
    }

    public int numUnfinishedActions() {
        int cnt = 0;
        for (Action a : best.getAllActions()) {
            if (!a.decomposable() && (a.status() == ActionStatus.PENDING || a.status() == ActionStatus.EXECUTING)) {
                cnt++;
            }
        }
        return cnt;
    }

    public boolean hasPendingActions() {
        for (Action a : best.getAllActions()) {
            if (a.status() == ActionStatus.PENDING) {
                return true;
            }
        }
        return false;
    }

    /**
     * restarts the planning problem into its initial state
     */
    public boolean Replan(int forHowLong) {
        State st = new State(pb);

        for(ActionExecution ae : executedAction.values()) {
            Action a = ae.createNewGroundAction();
            if(ae.status == ActionStatus.EXECUTING || ae.status == ActionStatus.EXECUTED) {
                st.insert(a);
                st.setActionExecuting(a, (int) ae.startTime);
            }
            if(ae.status == ActionStatus.EXECUTED) {
                st.setActionSuccess(a, (int) ae.endTime);
            }
        }
        st.enforceDelay(pb.start(), pb.earliestExecution(), currentTime);
        st.isConsistent();
        best = st;
        queue.clear();
        queue.add(st);
        return Repair(new TimeAmount(forHowLong));
    }

    /**
     * enforces given facts into the plan (possibly breaking it) this is an
     * incremental step, if there was something already defined, the name
     * collisions are considered to be intentional
     *
     * @param anml An ANML AST to be integrated in the planner.
     * @param propagate If true, a propagation will be done in the constraint networks.
     *                  This should be avoided if the domain is not completely described yet.
     * @return True if the planner is applicable to resulting anml problem.
     */
    public boolean ForceFact(ParseResult anml, boolean propagate) {
        //read everything that is contained in the ANML block
        if (logging) {
            TinyLogger.LogInfo("Forcing new fact into best state.");
        }

        KeepBestStateOnly();

        //TODO: apply ANML to more states and choose the best after the application
        pb.addAnml(anml);
        this.dtg = new LiftedDTG(this.pb);

        // apply revisions to best state and check if it is consistent
        State st = GetCurrentState();

        st.update();
        if (propagate && !st.isConsistent()) {
            this.planState = EPlanState.INFESSIBLE;
        }

        return true;
    }

    public State extractCurrentState(int now) {
        State tmp = new State(best);
        for(Action a : tmp.getAllActions()) {
            if(a.status() == ActionStatus.PENDING)
                tmp.setActionFailed(a.id());
        }
        return tmp;
    }

}
