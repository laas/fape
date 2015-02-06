package fape.core.planning.planner;

import fape.core.execution.model.AtomicAction;
import fape.core.planning.Plan;
import fape.core.planning.Planner;
import fape.core.planning.preprocessing.ActionDecompositions;
import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.preprocessing.LiftedDTG;
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
import fape.util.ActionsChart;
import fape.util.Pair;
import fape.util.TinyLogger;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.LActRef;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractDecomposition;
import planstack.anml.model.concrete.*;
import planstack.anml.model.concrete.Decomposition;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.ResourceStatement;
import planstack.anml.model.concrete.statements.Statement;
import planstack.anml.parser.ParseResult;
import planstack.constraints.stnu.Controllability;
import scala.Tuple2;
import scala.Tuple3;

import java.util.*;

/**
 * Base for any planner in FAPE. It defines all basic operations useful for
 * planning such as alterations of search states, inclusions of ANML blocks ...
 *
 * Classes that inherit from it only have to implement the abstract methods to
 * provide a search policy. Overriding methods can also be done to change the
 * default behaviour.
 */
public abstract class APlanner {

    public APlanner(State initialState, String[] planSelStrategies, String[] flawSelStrategies, Map<ActRef, ActionExecution> actionsExecutions) {
        this.pb = initialState.pb;
        assert pb.usesActionConditions() == this.useActionConditions() :
                "Difference between problem and planner in the handling action conditions";
        this.planSelStrategies = planSelStrategies;
        this.flawSelStrategies = flawSelStrategies;
        this.executedAction = actionsExecutions;
        this.controllability = initialState.controllability;
        this.dtg = new LiftedDTG(this.pb);
        queue = new PriorityQueue<>(100, this.stateComparator());
        queue.add(initialState);
        best = queue.peek();
    }

    public APlanner(Controllability controllability, String[] planSelStrategies, String[] flawSelStrategies) {
        this.planSelStrategies = planSelStrategies;
        this.flawSelStrategies = flawSelStrategies;
        this.controllability = controllability;
        this.executedAction = new HashMap<>();
        this.pb = new AnmlProblem(useActionConditions());
        this.dtg = new LiftedDTG(this.pb);
        this.queue = new PriorityQueue<>(100, this.stateComparator());
        queue.add(new State(pb, controllability));
        best = queue.peek();
    }



    public final Map<ActRef, ActionExecution> executedAction;
    public int currentTime = 0;

    @Deprecated //might not work in a general scheme were pultiple planner instances are intantiated
    public static APlanner currentPlanner = null;

    public static boolean debugging = true;
    public static boolean logging = true;
    public static boolean actionResolvers = true; // do we add actions to resolve flaws?

    public int GeneratedStates = 1; //count the initial state
    public int OpenedStates = 0;

    public final Controllability controllability;

    public final AnmlProblem pb;
    LiftedDTG dtg = null;

    /**
     * Used to build comparators for flaws. Default to a least commiting first.
     */
    public final String[] flawSelStrategies;

    /**
     * Used to build comparators for partial plans.
     */
    public final String[] planSelStrategies;

    /**
     * A short identifier for the planner.
     *
     * @return THe planner ID.
     */
    public abstract String shortName();

    public abstract ActionSupporterFinder getActionSupporterFinder();

    /**
     * If this returns true, decomposition will yield ActionConditions instead of Actions.
     * ActionConditions results in flaws that can be solved by unifying the action condition with
     * an action.
     */
    public boolean useActionConditions() {
        return false;
    }

    /**
     *
     */
    protected final PriorityQueue<State> queue;

    /**
     * applies a resolver to the state
     *
     * TODO: rewrite to a polymorphic switch instead of checking nulls in variables
     *
     * @param next State which we need to apply the changes
     * @param o Resolver to apply
     * @param consumer if non null, this is the database that must be supported by the resolver.
     * @return True if the resulting state is consistent.
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

        TinyLogger.LogInfo(next, "     [%s] Adding %s",next.mID, o);

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
                for (LogStatement second : ((TemporalSeparation) o).second.chain.getFirst().contents) {
                    next.enforceStrictlyBefore(first.end(), second.start());
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
                    assert theSupport == null : "Distinguishing resource events upon the same resource in one action " +
                            "needs to be implemented.";
                    theSupport = s;
                }
            }
            assert theSupport != null : "Could not find a supporting resource statement in the action.";
            next.addUnificationConstraint(theSupport.sv(), opt.unifyingResourceVariable);

            //add temporal constraint
            if (opt.before) {
                //the supporting statement must occur before the given time point
                next.enforceStrictlyBefore(theSupport.end(), opt.when);
            } else {
                //vice-versa
                next.enforceStrictlyBefore(opt.when, theSupport.start());
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

            //TODO(fdvorak): here we should add the binding between the statevariable of supporting resource event in one
            // of the decomposed actions for now we leave it to search
        } else if (o instanceof TemporalConstraint) {
            TemporalConstraint tc = (TemporalConstraint) o;
            next.enforceConstraint(tc.first, tc.second, tc.min, tc.max);
        } else if(o instanceof NewTaskSupporter) {
            ActionCondition ac = ((NewTaskSupporter) o).condition;
            // create new action with the same arguments
            Action act = Factory.getInstantiatedAction(pb, ((NewTaskSupporter) o).abs, ac.args());
            next.insert(act);

            // enforce equality of time points and add support to task network
            next.addSupport(ac, act);
        } else if(o instanceof ExistingTaskSupporter) {
            ActionCondition ac = ((ExistingTaskSupporter) o).condition;
            Action act = ((ExistingTaskSupporter) o).act;

            // add equality constraint between all args
            for (int i = 0; i < ac.args().size(); i++) {
                next.addUnificationConstraint(act.args().get(i), ac.args().get(i));
            }
            //enforce equality of time points and add support to task network
            next.addSupport(ac, act);
        } else if(o instanceof MotivatedSupport) {
            assert useActionConditions() : "Error: looking for motivated support in a planner that does not use action conditions.";
            MotivatedSupport ms = (MotivatedSupport) o;

            // action that will be decomposed. Either it is already in the plan or we add it now
            Action act;
            if(ms.act == null) {
                act = Factory.getStandaloneAction(pb, ms.abs);
                next.insert(act);
            } else {
                act = ms.act;
            }

            ActionCondition ac;
            if(ms.decID == -1) {
                // the action condition is directly in the main body
                ac = act.context().actionConditions().apply(ms.actRef);
            } else {
                // we need to make one decomposition
                // decompose the action with the given decomposition ID
                AbstractDecomposition absDec = act.decompositions().get(ms.decID);
                Decomposition dec = Factory.getDecomposition(pb, act, absDec);
                next.applyDecomposition(dec);

                // Get the action condition we wanted
                ac = dec.context().actionConditions().apply(ms.actRef);
            }
            // add equality constraint between all args
            for (int i = 0; i < ac.args().size(); i++) {
                next.addUnificationConstraint(ms.toSupport.args().get(i), ac.args().get(i));
            }
            //enforce equality of time points and add the support in the task network
            next.addSupport(ac, ms.toSupport);
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
     * @param st State in which the causal link was added.
     * @param supporter Left side of the causal link.
     * @param consumer Right side of the causal link.
     */
    public void causalLinkAdded(State st, LogStatement supporter, LogStatement consumer) {
    }

    /**
     * Removes the action results from the system.
     *
     * @param actionRef The action that failed.
     */
    public void FailAction(ActRef actionRef, int endTime) {
        KeepBestStateOnly();
        assert best != null;
        State st = GetCurrentState();
        st.setActionFailed(actionRef, endTime);
        executedAction.get(actionRef).setFailed();
    }

    /**
     * Set the action ending to the its real end time. It removes the duration
     * constraints between the starts and the end of the action (as given by the
     * duration anml variable). Adds a new constraint [realEndTime, realEndtime]
     * between the global start and the end of action time points.
     *
     * @param actionID Id of the action that finished.
     * @param realEndTime Observed end time.
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
     */
    public void SetEarliestExecution(int earliestExecution) {
        if(Plan.showChart)
            ActionsChart.setCurrentTime(earliestExecution);
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
     * All possible state of the planner.
     */
    public enum EPlanState {
        TIMEOUT, CONSISTENT, INCONSISTENT, INFEASIBLE, UNINITIALIZED
    }
    /**
     * what is the current state of the plan
     */
    public EPlanState planState = EPlanState.UNINITIALIZED;

    //current best state
    private State best = null;

    /**
     * @return The current best state. Null if no consistent state was found.
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
        } else if(f instanceof UnsupportedTaskCond) {
            candidates = GetResolvers(st, (UnsupportedTaskCond) f);
        } else if(f instanceof UnmotivatedAction) {
            candidates = GetResolvers(st, (UnmotivatedAction) f);
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

        if(st.canBeStrictlyBefore(f.db1.getLastTimePoints().getFirst(), f.db2.getFirstTimePoints().getFirst()))
            options.add(new TemporalSeparation(f.db1, f.db2));
        if(st.canBeStrictlyBefore(f.db2.getLastTimePoints().getFirst(), f.db1.getFirstTimePoints().getFirst()))
            options.add(new TemporalSeparation(f.db2, f.db1));
        for (int i = 0; i < f.db1.stateVariable.jArgs().size(); i++) {
            if(st.separable(f.db1.stateVariable.jArgs().get(i), f.db2.stateVariable.jArgs().get(i)))
                options.add(new BindingSeparation(
                        f.db1.stateVariable.jArgs().get(i),
                        f.db2.stateVariable.jArgs().get(i)));
        }
        return options;
    }

    /**
     * Resolvers for an action conditions of finding or inserting an action that can be
     * unified with the action condition.
     */
    public final List<Resolver> GetResolvers(State st, UnsupportedTaskCond utc) {
        List<Resolver> resolvers = new LinkedList<>();

        // inserting a new action is always a resolver.
        resolvers.add(new NewTaskSupporter(utc.actCond, utc.actCond.abs()));

        for(Action act : st.getAllActions()) {
            if(act.abs() == utc.actCond.abs()) {
                boolean unifiable = true;
                for(int i=0 ; i<act.args().size() ; i++) {
                    unifiable &= st.unifiable(act.args().get(i), utc.actCond.args().get(i));
                }
                unifiable &= st.canBeBefore(act.start(), utc.actCond.start());
                unifiable &= st.canBeBefore(utc.actCond.start(), act.start());
                unifiable &= st.canBeBefore(act.end(), utc.actCond.end());
                unifiable &= st.canBeBefore(utc.actCond.end(), act.end());
                if(unifiable)
                    resolvers.add(new ExistingTaskSupporter(utc.actCond, act));
            }
        }
        return resolvers;
    }

    public List<Resolver> GetResolvers(State st, UnmotivatedAction ua) {
        List<Resolver> resolvers = new LinkedList<>();
        // action that must be matched with a task conditions
        Action act = ua.act;

        // any task condition unifiable with act
        for(ActionCondition ac : st.getOpenTaskConditions()) {
            boolean unifiable = true;
            if(ac.abs() != act.abs())
                break;
            for(int i=0 ; i<act.args().size() ; i++) {
                unifiable &= st.unifiable(act.args().get(i), ac.args().get(i));
            }
            unifiable &= st.canBeBefore(act.start(), ac.start());
            unifiable &= st.canBeBefore(ac.start(), act.start());
            unifiable &= st.canBeBefore(act.end(), ac.end());
            unifiable &= st.canBeBefore(ac.end(), act.end());
            if(unifiable)
                resolvers.add(new ExistingTaskSupporter(ac, act));
        }

        ActionDecompositions preproc = new ActionDecompositions(pb);

        // resolvers: any any action we add to the plan and that might provide (through decomposition)
        // a task condition
        for(Tuple3<AbstractAction, Integer, LActRef> insertion : preproc.supporterForMotivatedAction(act)) {
            resolvers.add(new MotivatedSupport(act, insertion._1(), insertion._2(), insertion._3()));
        }

        // resolvers: any action in the plan that can be refined to a task condition
        for(Action a : st.getOpenLeaves()) {
            for (Tuple2<Integer, LActRef> insertion : preproc.supporterForMotivatedAction(a, act)) {
                resolvers.add(new MotivatedSupport(act, a, insertion._1(), insertion._2()));
            }
        }

        return resolvers;
    }

    /**
     * Finds all flaws of a given state. Currently, threats and unbound
     * variables are considered only if no other flaws are present.
     *
     * @return A list of flaws present in the system. The list of flaws might not
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
        for (ActionCondition ac : st.getOpenTaskConditions()) {
            flaws.add(new UnsupportedTaskCond(ac));
        }
        for (Action unmotivated : st.getUnmotivatedActions()) {
            flaws.add(new UnmotivatedAction(unmotivated));
        }
        //find the resource flaws
        flaws.addAll(st.resourceFlaws());

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

        if (flaws.isEmpty()) {
            for (VarRef v : st.getUnboundVariables()) {
                assert !st.typeOf(v).equals("integer");
                flaws.add(new UnboundVariable(v));
            }
        }

        return flaws;
    }

    /**
     * Implementation of search. An easy thing to do to forward this call to the
     * depthBoundedAStar method.
     *
     * @param deadline Absolute time (in ms) at which the planner must stop.
     * @return A solution state if the planner found one. null otherwise.
     */
    public State search(final long deadline) {
        return search(deadline, Integer.MAX_VALUE, false);
    }

    /**
     *
     * @param deadline Absolute time (in ms) at which the planner must stop.
     * @param maxDepth Will discard any partial plan which depth is greater than that.
     *                 Note that the depth of a partial plan is computed with respect to
     *                 initial state (i.e. repairing a state starts with a depth > 0)
     * @param incrementalDeepening If set to true, the planner will increase the maximum
     *                             allowed depth from 1 until maxDepth until a plan is found
     *                             or the planner times out.
     * @return A solution plan if the planner founds one. null otherwise.
     *         Also check the "planState" field for more detailed information.
     */
    public State search(final long deadline, final int maxDepth, final boolean incrementalDeepening) {
        List<State> toRestore = new LinkedList<>(queue);

        int currentMaxDepth;
        if(incrementalDeepening)
            currentMaxDepth = 1;
        else
            currentMaxDepth = maxDepth;

        State solution = null;
        while(currentMaxDepth <= maxDepth && solution == null && planState != EPlanState.TIMEOUT) {
            queue.clear();
            queue.addAll(toRestore);
            solution = depthBoundedAStar(deadline, currentMaxDepth);
            currentMaxDepth += 1;
        }
        return solution;
    }

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
     * state variables are unifiable. - they can overlap
     *
     * @return True there is a threat.
     */
    protected boolean isThreatening(State st, TemporalDatabase db1, TemporalDatabase db2) {
        // if they are not both consumers, it is dealt by open goal reasoning
        if (db1.isConsumer() || db2.isConsumer())
            return false;

        // if their state variables are not unifiable
        if(!st.Unifiable(db1, db2))
            return false;

        // if db1 cannot start before db2 ends
        boolean db1AfterDB2 = true;
        for(TPRef start1 : db1.getFirstTimePoints()) {
            for (TPRef end2 : db2.getLastTimePoints()) {
                if (st.canBeBefore(start1, end2)) {
                    db1AfterDB2 = false;
                    break;
                }
            }
        }
        // if db2 cannot start before db1 ends
        boolean db2AfterDB1 = true;
        for(TPRef end1 : db1.getLastTimePoints())
            for(TPRef start2 : db2.getFirstTimePoints())
                if(st.canBeBefore(start2, end1)) {
                    db2AfterDB1 = false;
                    break;
                }

        // true if they can overlap
        return !(db1AfterDB2 || db2AfterDB1);

    }

    public static boolean optimal = false;

    protected State depthBoundedAStar(final long deadLine, final int maxDepth) {
        /**
         * search
         */
        while (true) {
            if (System.currentTimeMillis() > deadLine) {
                TinyLogger.LogInfo("Timeout.");
                this.planState = EPlanState.TIMEOUT;
                return null;
            }
            if (queue.isEmpty()) {
                if (!APlanner.optimal) {
                    TinyLogger.LogInfo("No plan found.");
                    this.planState = EPlanState.INFEASIBLE;
                    return null;
                } else if(best.isConsistent() && GetFlaws(best).isEmpty()) {
                    this.planState = EPlanState.CONSISTENT;
                    return best;
                } else {
                    this.planState = EPlanState.INFEASIBLE;
                    return null;
                }
            }
            //get the best state and continue the search
            State st = queue.remove();

            if(!st.isConsistent())
                break;
            OpenedStates++;

            if (APlanner.debugging) {
                st.exportTemporalNetwork("current-stn.dot");
            }

            List<Flaw> flaws = GetFlaws(st);

            TinyLogger.LogInfo(st, "\nCurrent state: [%s]", st.mID);
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

            if(st.depth == maxDepth) //we are not interested in its children
                continue;

            //continue the search
            LinkedList<Pair<Flaw, List<Resolver>>> opts = new LinkedList<>();
            for (Flaw flaw : flaws) {
                opts.add(new Pair<>(flaw, GetResolvers(st, flaw)));
            }

            //do some sorting here - min domain
            //Collections.sort(opts, optionsComparatorMinDomain);
            Collections.sort(opts, this.flawComparator(st));

            if (opts.isEmpty()) {
                throw new FAPEException("Error: no flaws but state was not found to be a solution.");
            }

            if (opts.getFirst().value2.isEmpty()) {
                TinyLogger.LogInfo(st, "  Dead-end, flaw without resolvers: %s", opts.getFirst().value1);
                //dead end
                continue;
            }

            //we just take the first option here as a tie breaker by min-domain
            Pair<Flaw, List<Resolver>> opt = opts.getFirst();

            TinyLogger.LogInfo(st, " Flaw: %s", opt.value1);

            for (Resolver o : opt.value2) {

                TinyLogger.LogInfo(st, "   Res: %s", o);

                State next = new State(st);
                boolean success = false;
                if (opt.value1 instanceof Threat
                        || opt.value1 instanceof UnboundVariable
                        || opt.value1 instanceof UndecomposedAction
                        || opt.value1 instanceof ResourceFlaw
                        || opt.value1 instanceof UnsupportedTaskCond
                        || opt.value1 instanceof UnmotivatedAction) {
                    success = ApplyOption(next, o, null);
                } else {
                    success = ApplyOption(next, o, next.GetDatabase(((UnsupportedDatabase) opt.value1).consumer.mID));
                }
                //TinyLogger.LogInfo(next.Report());
                if (success) {
                    queue.add(next);
                    GeneratedStates++;
                } else {
                    TinyLogger.LogInfo(st, "     Dead-end reached for state: %s", next.mID);
                    //inconsistent state, doing nothing
                }
            }

        }
        return null;
    }

    /**
     * Keeps the current best plan and tries to repair it until the absolute deadline.
     * @param deadline Absolute deadline in ms (to compare with currentTimeMillis())
     * @return True if a consistent state was found, false otherwise.
     */
    @Deprecated // we should build a planner with one state and search from that
    public boolean Repair(long deadline) {
        return Repair(deadline, Integer.MAX_VALUE, false);
    }

    /**
     * Cleans up the queue of everything except the best state and starts searching from this one.
     *
     * @param deadline Absolute deadline in ms (to compare with currentTimeMillis())
     * @param maxDepth Maximum depth of a solution any state beyond that will be discarded.
     *                 Note that the current best state might have a depth > 0.
     * @param incrementalDeepening Gradually increments the maximum depth until maxDepth is reached.
     * @return True if a solution is found, False otherwise.
     */
    @Deprecated  // we should build a planner with one state and search from that
    public boolean Repair(final long deadline, final int maxDepth, final boolean incrementalDeepening) {
        KeepBestStateOnly();

        do {
            planState = EPlanState.INCONSISTENT;

            best = search(deadline, maxDepth, incrementalDeepening);
            assert best == null || best.isConsistent() : "Search returned an inconsistent plan.";

            if (planState == EPlanState.TIMEOUT && best == null)
                return false;
            else if (planState == EPlanState.INFEASIBLE) {
                assert best == null;
                return false;
            } else if(planState == EPlanState.CONSISTENT) {
                assert best != null;
                // it seems consistent, check if it is dynamically controllable
                Plan plan = new Plan(best);
                if (!plan.isConsistent()) {
                    System.err.println("Returned state is not consistent or not DC. We keep searching.");
                    planState = EPlanState.INCONSISTENT;
                    search(deadline); //TODO: here incremental deepening is abandoned, this whole part should be in search
                } else {
                    planState = EPlanState.CONSISTENT;
                }
            }
        } while(planState != EPlanState.CONSISTENT);

        //we empty the queue now and leave only the best state there
        KeepBestStateOnly();
        return true;
    }

    /**
     * Finds all resolvers for an unsupported temporal database.
     * This includes causal links from an other database and actions insertions.
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
                if (useActionConditions() || !aa.mustBeMotivated()) {
                    ret.add(new SupportingAction(aa));
                }
            }
        }

        return ret;
    }

    /**
     * Progresses in the plan up for howFarToProgress.
     * Returns either AtomicActions that were instantiated with corresponding start times, or
     * null, if not solution was found in the given time
     */
    public List<AtomicAction> Progress(long currentTime) {
        this.SetEarliestExecution((int) currentTime);
        this.Repair(System.currentTimeMillis() + 500);
        State myState = best;
        if(best == null)
            return new LinkedList<>();

        Plan plan = new Plan(myState);

        if(Plan.showChart)
            ActionsChart.setCurrentTime((int) currentTime);


        List<AtomicAction> ret = new LinkedList<>();
        for (Action a : plan.getExecutableActions((int) currentTime)) {
            long startTime = myState.getEarliestStartTime(a.start());
            assert a.status() == ActionStatus.PENDING : "Action "+a+" is not pending but "+a.status();
            assert startTime >= currentTime : "Cannot start an action at a time "+startTime+" lower that "+
                    "current time: "+currentTime;
            long latestEnd = myState.getLatestStartTime(a.end());
            AtomicAction aa = new AtomicAction(a, startTime, plan.getMinDuration(a), plan.getMaxDuration(a), best);
            ret.add(aa);
        }

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
    public boolean Replan(long deadline) {
        State st = new State(pb, controllability);

        for(ActionExecution ae : executedAction.values()) {
            Action a = ae.createNewGroundAction(pb);
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
        return Repair(deadline);
    }

    /**
     * Enforces given facts into the plan (possibly breaking it) this is an
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
            this.planState = EPlanState.INFEASIBLE;
        }

        return true;
    }
}
