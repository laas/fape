package fape.core.planning.planner;

import fape.core.planning.Plan;
import fape.core.planning.Planner;
import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.preprocessing.LiftedDTG;
import fape.core.planning.search.flaws.finders.*;
import fape.core.planning.search.flaws.flaws.*;
import fape.core.planning.search.flaws.resolvers.*;
import fape.core.planning.search.strategies.flaws.FlawCompFactory;
import fape.core.planning.search.strategies.plans.PlanCompFactory;
import fape.core.planning.search.strategies.plans.SeqPlanComparator;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import fape.util.TinyLogger;
import fape.util.Utils;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.constraints.stnu.Controllability;

import java.util.*;

/**
 * Base for any planner in FAPE.
 * It is responsible for detecting and solving flaws and search procedures.
 *
 * Classes that inherit from it only have to implement the abstract methods to
 * provide a search policy. Overriding methods can also be done to change the
 * default behaviour.
 */
public abstract class APlanner {

    public APlanner(State initialState, PlanningOptions options) {
        filterOptions(options);
        this.options = options;
        this.pb = initialState.pb;
        assert pb.usesActionConditions() == this.useActionConditions() :
                "Difference between problem and planner in the handling action conditions";
        this.controllability = initialState.controllability;
        this.dtg = new LiftedDTG(this.pb);
        queue = new PriorityQueue<>(100, this.stateComparator());
        queue.add(initialState);
    }

    @Deprecated // we should always build from a state (maybe add a constructor from a problem)
    public APlanner(Controllability controllability, PlanningOptions options) {
        filterOptions(options);
        this.options = options;
        this.controllability = controllability;
        this.pb = new AnmlProblem(useActionConditions());
        this.dtg = new LiftedDTG(this.pb);
        this.queue = new PriorityQueue<>(100, this.stateComparator());
        queue.add(new State(pb, controllability));
    }

    public final PlanningOptions options;

    /**
     * Transforms the given options from planner independent to planner dependent.
     * <p/>
     * Currently it only removes unneeded flaw finders in case action conditions are not used.
     */
    private void filterOptions(PlanningOptions opts) {
        if (!useActionConditions()) {
            List<FlawFinder> flawFinders = new LinkedList<>();
            for (FlawFinder ff : opts.flawFinders) {
                if (!(ff instanceof UnmotivatedActionFinder) && !(ff instanceof UnsupportedTaskConditionFinder))
                    flawFinders.add(ff);
            }
            opts.flawFinders = flawFinders.toArray(new FlawFinder[flawFinders.size()]);
        }
    }


    @Deprecated //might not work in a general scheme were multiple planner instances are instantiated
    public static APlanner currentPlanner = null;

    public static boolean debugging = false;
    public static boolean logging = false;
    public static boolean actionResolvers = true; // do we add actions to resolve flaws?

    public int GeneratedStates = 1; //count the initial state
    public int OpenedStates = 0;
    public int numFastForwarded = 0;

    public final Controllability controllability;

    public final AnmlProblem pb;
    LiftedDTG dtg = null;

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
     * This method is invoked whenever a causal link is added and offers a way
     * to react to it for planner extending this class.
     *
     * @param st        State in which the causal link was added.
     * @param supporter Left side of the causal link.
     * @param consumer  Right side of the causal link.
     */
    public void causalLinkAdded(State st, LogStatement supporter, LogStatement consumer) {
    }

    /**
     * All possible state of the planner.
     */
    public enum EPlanState {
        TIMEOUT, CONSISTENT, INCONSISTENT, INFEASIBLE
    }

    /**
     * what is the current state of the plan
     */
    public EPlanState planState = EPlanState.INCONSISTENT;

    /**
     * Finds all flaws of a given state. Currently, threats and unbound
     * variables are considered only if no other flaws are present.
     *
     * @return A list of flaws present in the system. The list of flaws might not
     * be exhaustive.
     */
    public List<Flaw> getFlaws(State st) {
        List<Flaw> flaws = new LinkedList<>();
        for (FlawFinder fd : options.flawFinders)
            flaws.addAll(fd.getFlaws(st, this));


        //find the resource flaws
        flaws.addAll(st.resourceFlaws());

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
     * @param deadline             Absolute time (in ms) at which the planner must stop.
     * @param maxDepth             Will discard any partial plan which depth is greater than that.
     *                             Note that the depth of a partial plan is computed with respect to
     *                             initial state (i.e. repairing a state starts with a depth > 0)
     * @param incrementalDeepening If set to true, the planner will increase the maximum
     *                             allowed depth from 1 until maxDepth until a plan is found
     *                             or the planner times out.
     * @return A solution plan if the planner founds one. null otherwise.
     * Also check the "planState" field for more detailed information.
     */
    public State search(final long deadline, final int maxDepth, final boolean incrementalDeepening) {
        if (options.useAEpsilon) {
            return aEpsilonSearch(deadline, maxDepth, incrementalDeepening);
        } else {
            return bestFirstSearch(deadline, maxDepth, incrementalDeepening);
        }
    }

    public State bestFirstSearch(final long deadline, final int maxDepth, final boolean incrementalDeepening){

        List<State> toRestore = new LinkedList<>(queue);

        int currentMaxDepth;
        if(incrementalDeepening)
        currentMaxDepth=1;
        else
        currentMaxDepth=maxDepth;

        State solution = null;
        while(currentMaxDepth<=maxDepth&&solution==null&&planState!=EPlanState.TIMEOUT)

        {
            queue.clear();
            queue.addAll(toRestore);
            solution = depthBoundedAStar(deadline, currentMaxDepth);

            if (solution != null) {
                // here we check that the plan is indeed a solution
                // it might not be the case if are looking for DC plans (Plan.makeDispatchable)
                // and the STNU does not check dynamic controllability
                Plan plan = new Plan(solution);
                if (!plan.isConsistent()) {
                    System.err.println("Returned state is not consistent or not DC. We keep searching.");
                    planState = EPlanState.INCONSISTENT;
                    solution = null;
                }
            }

            if (currentMaxDepth == Integer.MAX_VALUE) // make sure we don't overflow
                break;
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
    public final Comparator<Flaw> flawComparator(State st) {
        return FlawCompFactory.get(st, this, options.flawSelStrategies);
    }

    SeqPlanComparator stateComparator = null;

    /**
     * The comparator used to order the queue. THe first state in the queue
     * (according to this comparator, will be selected for expansion.
     *
     * @return The comparator to use for ordering the queue.
     */
    public final SeqPlanComparator stateComparator() {
        if(stateComparator == null)
            stateComparator = PlanCompFactory.get(this, options.planSelStrategies);
        return stateComparator;
    }

    /**
     * Checks if two temporal databases are threatening each others. It is the
     * case if: - both are not consuming databases (start with an assignment).
     * Otherwise, threat is naturally handled by looking for supporters. - their
     * state variables are unifiable. - they can overlap
     *
     * @return True there is a threat.
     */

    protected State depthBoundedAStar(final long deadLine, final int maxDepth) {
        while (true) {
            if (System.currentTimeMillis() > deadLine) {
                TinyLogger.LogInfo("Timeout.");
                this.planState = EPlanState.TIMEOUT;
                return null;
            }
            if (queue.isEmpty()) {
                this.planState = EPlanState.INFEASIBLE;
                return null;
            }

            //get the best state and continue the search
            State st = queue.remove();

            if(!st.isConsistent())
                break;
            OpenedStates++;

            if (APlanner.debugging) {
                st.exportTemporalNetwork("current-stn.dot");
            }

            if(this instanceof PGReachabilityPlanner)
                if(!(((PGReachabilityPlanner) this).checkFeasibility(st))) {
                    TinyLogger.LogInfo(st, "\nDead End State: [%s]", st.mID);
                    continue;
                }

            List<Flaw> flaws = getFlaws(st);

            TinyLogger.LogInfo(st, "\nCurrent state: [%s]", st.mID);
            if (flaws.isEmpty()) {
                if(Planner.debugging)
                    st.assertConstraintNetworkGroundAndConsistent();

                this.planState = EPlanState.CONSISTENT;
                TinyLogger.LogInfo("Plan found:");
                TinyLogger.LogInfo(st);
                return st;
            }

            if(st.depth == maxDepth) //we are not interested in its children
                continue;

            // sort the flaws, higher priority come first
            try {
                Collections.sort(flaws, this.flawComparator(st));
            } catch (java.lang.IllegalArgumentException e) {
                // problem with the sort function, try to find an problematic example and exit
                System.err.println("The flaw comparison function is not legal (for instance, it might not be transitive).");
                Utils.showExampleProblemWithFlawComparator(flaws, this.flawComparator(st), st, this);
                System.exit(1);
            }

            if (flaws.isEmpty()) {
                throw new FAPEException("Error: no flaws but state was not found to be a solution.");
            }

            //we just take the first flaw and its resolvers
            Flaw f;
            if(options.chooseFlawManually) {
                System.out.print("STATE :" + st.mID + "\n");
                for(int i=0 ; i<flaws.size() ; i++)
                    System.out.println("["+i+"] "+Printer.p(st, flaws.get(i)));
                int choosen = Utils.readInt();
                f = flaws.get(choosen);
            } else {
                f = flaws.get(0);
            }
            List<Resolver> resolvers = f.getResolvers(st, this);

            if (resolvers.isEmpty()) {
                // dead end, keep going
                TinyLogger.LogInfo(st, "  Dead-end, flaw without resolvers: %s", flaws.get(0));
                continue;
            }

            TinyLogger.LogInfo(st, " Flaw: %s", f);

            // Append the possibles fixed state to the queue
            for (Resolver res : resolvers) {
                TinyLogger.LogInfo(st, "   Res: %s", res);

                State next = st.cc();
                TinyLogger.LogInfo(st, "     [%s] Adding %s", next.mID, res);
                boolean success = applyResolver(next, res);

                if (success) {
                    if(options.useFastForward) {
                        if(fastForward(next)) {
                            queue.add(next);
                            GeneratedStates++;
                        } else {
                            TinyLogger.LogInfo(st, "     ff: Dead-end reached for state: %s", next.mID);
                        }
                    } else {
                        queue.add(next);
                        GeneratedStates++;
                    }
                } else {
                    TinyLogger.LogInfo(st, "     Dead-end reached for state: %s", next.mID);
                    //inconsistent state, doing nothing
                }
            }

        }
        return null;
    }

    /**
     * This function looks at flaws and resolvers in the state and fixes flaws with a single resolver.
     * @param st
     * @return
     */
    public boolean fastForward(State st) {
        List<Flaw> flaws = getFlaws(st);

        if (flaws.isEmpty()) {
            return true;
        }

        //do some sorting here - min domain
        //Collections.sort(opts, optionsComparatorMinDomain);
        Collections.sort(flaws, this.flawComparator(st));

        //we just take the first flaw and its resolvers
        Flaw f = flaws.get(0);
        List<Resolver> resolvers = f.getResolvers(st, this);
        if (resolvers.isEmpty()) {
            // dead end, keep going
            TinyLogger.LogInfo(st, "  Dead-end, flaw without resolvers: %s", flaws.get(0));
            return false;
        }

        if(resolvers.size() == 1) {
            Resolver res = resolvers.get(0);
            TinyLogger.LogInfo(st, "     [%s] ff: Adding %s", st.mID, res);
            if(applyResolver(st, res)) {
                numFastForwarded++;
                return fastForward(st);
            } else {
                return false;
            }
        } else {
            // nothing was done
            return true;
        }
    }

    /**
     * Applies a resolver to the given state. This state will be modified to integrate the resolver.
     *
     * @param st State to modify
     * @param resolver Resolver to apply
     * @return True if the resolver was successfully applied and the resulting state is consistent.
     *         False otherwise.
     */
    public boolean applyResolver(State st, Resolver resolver) {
        return resolver.apply(st, this) &&
                st.isConsistent();
    }

    protected Float AXg = 0.5F;
    protected Float AXh = 0.5F;
    protected Float Openg = 0.5F;
    protected Float Openh = 0.5F;
    protected Float Sonsg = 0.5F;
    protected Float Sonh = 0.5F;

    protected int espilon = 1;

    private PriorityQueue<State>open ; //state list
    private List<State> closed ;
    private List<State> solved ;      //at this moment we don't need a list because we don't want the optimum optimorum
    private float fthreshold;
    private boolean persevere = true; //at this moment useless; i don't know how use it and where

    private PriorityQueue<State> AX;  //list of sons of the last state selected which are acceptable ( for fthreshold )

    public State aEpsilonSearch(final long deadline, final int maxDepth, final boolean incrementalDeepening){
        assert maxDepth >= 999999 : "Max depth is not used in A-Epsilon.";
        assert !incrementalDeepening : "Incremental deepening is not available in A-Epsilon.";
        open = new PriorityQueue<>(100,new AEComparator(this, Openg, Openh));
        open.add(queue.peek());

        closed = new ArrayList<>();
        solved = new ArrayList<>();
        fthreshold = (1 + espilon) * h(open.peek());

        AX = new PriorityQueue<>(100,new AEComparator(this, AXg, AXh));

        PriorityQueue<State> sons = expand(open.peek());
        if (sons == null ){
            return null;
        }
        State temp ;
        while (! sons.isEmpty() ){
            temp = sons.remove();
            if((g(temp) + h(temp)) < fthreshold) {
                AX.add(temp);
            }
        }

        State n ;
        while( !open.isEmpty() && solved.isEmpty()) {
            if (System.currentTimeMillis() > deadline) {
                TinyLogger.LogInfo("Timeout.");
                this.planState = EPlanState.TIMEOUT;
                return null;
            }

            if ( !AX.isEmpty()) {
                n = AX.remove();
                open.remove(n);
            } else {
                n = open.peek();
            }
            if(!n.isConsistent())
                break;
            OpenedStates++;
            sons = expand(n);
            if (solved.isEmpty()) {
                while ((solved.isEmpty()) && (sons != null) && (!atLeastOneIsAcceptable(sons) || !open.isEmpty() || !persevere)) {

                    if (System.currentTimeMillis() > deadline) {
                        TinyLogger.LogInfo("Timeout.");
                        this.planState = EPlanState.TIMEOUT;
                        return null;
                    }

                    if (! sons.isEmpty() ){
                        OpenedStates++;
                        sons = expand(sons.peek());
                    } else {
                        sons =null;
                    }
                }
                AX.clear();
                if (sons != null){
                    while (!sons.isEmpty()) {
                        temp = sons.remove();
                        if ((g(temp) + h(temp)) < fthreshold) {
                            AX.add(temp);
                        }
                    }
                }
            }
        }
        if (open.isEmpty()){
            System.err.println("failure");
            return null;
        } else {
            return solved.get(0);
        }
    }
    /**
     *
     * @param st State whose son have to be calculated
     * @return sons of st if he got at least one resolver
     * This fonction can also modifie open, closed, solved and fthreshold
     */
    private PriorityQueue<State> expand(State st) {
        open.remove(st);
        closed.add(st);

        PriorityQueue<State> sons = new PriorityQueue<>(100,new AEComparator(this, Sonsg, Sonh));

        List<Flaw> flaws = getFlaws(st);

        TinyLogger.LogInfo(st, "\nCurrent state: [%s]", st.mID);

        //if (st.depth == maxDepth) //we are not interested in its children
        //return null;

        // sort the flaws, higher priority come first
        try {
            Collections.sort(flaws, this.flawComparator(st));
        } catch (java.lang.IllegalArgumentException e) {
            // problem with the sort function, try to find an problematic example and exit
            System.err.println("The flaw comparison function is not legal (for instance, it might not be transitive).");
            Utils.showExampleProblemWithFlawComparator(flaws, this.flawComparator(st), st, this);
            System.exit(1);
        }

        if (flaws.isEmpty()) {
            throw new FAPEException("Error: no flaws but state was not found to be a solution.");
        }

        //we just take the first flaw and its resolvers
        Flaw f;
        if (options.chooseFlawManually) {
            System.out.print("STATE :" + st.mID + "\n");
            for (int i = 0; i < flaws.size(); i++)
                System.out.println("[" + i + "] " + Printer.p(st, flaws.get(i)));
            int choosen = Utils.readInt();
            f = flaws.get(choosen);
        } else {
            f = flaws.get(0);
        }
        List<Resolver> resolvers = f.getResolvers(st, this);

        if (resolvers.isEmpty()) {
            // dead end, keep going
            TinyLogger.LogInfo(st, "  Dead-end, flaw without resolvers: %s", flaws.get(0));
            //continue;
        }

        TinyLogger.LogInfo(st, " Flaw: %s", f);

        // Append the possibles fixed state to the queue
        for (Resolver res : resolvers) {
            TinyLogger.LogInfo(st, "   Res: %s", res);

            State next = st.cc();
            TinyLogger.LogInfo(st, "     [%s] Adding %s", next.mID, res);
            boolean success = applyResolver(next, res);

            if (success) {
                if (getFlaws(next).isEmpty()){
                    TinyLogger.LogInfo("Plan found:");
                    TinyLogger.LogInfo(st);
                    solved.add(next);
                } else if (!(open.contains(next) && !closed.contains(next))) {
                    GeneratedStates++;
                    open.add(next);
                    sons.add(next);
                }
            }else {
                TinyLogger.LogInfo(st, "     Dead-end reached for state: %s", next.mID);
                //inconsistent state, doing nothing
            }

        }
        if (!sons.isEmpty()) {
            if (fthreshold < (1 + espilon) * (g(sons.peek()) + h(sons.peek()))) {
                fthreshold = (1 + espilon) * (g(sons.peek()) + h(sons.peek()));
            }
        }
        return sons;
    }

    /**
     * @return true if at least one of the state in sons if acceptable (for fthreshold )
     */
    private boolean atLeastOneIsAcceptable(PriorityQueue<State> sons){
        State temp;
        if (!sons.isEmpty()) {
            Iterator<State> it = sons.iterator();
            while (it.hasNext()) {
                temp = it.next();
                if (open.contains(temp) && (g(temp) + h(temp)) < fthreshold) {
                    return true;
                }
            }
        }
        return false;
    }

    public int h(State st){
        return (int) stateComparator().h(st);
    }

    public int g(State st){
        return (int) stateComparator().g(st);
    }
}
