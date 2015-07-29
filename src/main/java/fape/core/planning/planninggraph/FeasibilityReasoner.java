package fape.core.planning.planninggraph;

import fape.core.inference.HReasoner;
import fape.core.inference.Predicate;
import fape.core.inference.Term;
import fape.core.planning.grounding.*;
import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.*;
import planstack.constraints.bindings.ValuesHolder;
import planstack.structures.Pair;

import java.util.*;

public class FeasibilityReasoner {

    final APlanner planner;
    public Map<String, LVarRef[]> varsOfAction = new HashMap<>();
    public Map<String, LVarRef[]> varsOfDecomposition = new HashMap<>();
    private Set<GAction> allActions;

    /** Maps ground actions from their ID */
    public final HashMap<Integer, GAction> gactions = new HashMap<>();
    public final HashMap<ActRef, VarRef> groundedActVariable = new HashMap<>();
    public final HashMap<ActRef, VarRef> decompositionVariable = new HashMap<>();

    final HReasoner<Term> baseReasoner;

    public FeasibilityReasoner(APlanner planner, State initialState) {
        this.planner = planner;
        // this Problem contains all the ground actions
        GroundProblem base = planner.preprocessor.getGroundProblem();
        allActions = new HashSet<>(base.gActions);

        baseReasoner = new HReasoner<>();
        for(GAction ga : allActions) {
            ga.addClauses(baseReasoner);
        }

        // all clauses have been added, lock the reasoner for a better sharing of data structures (clauses)
        baseReasoner.lock();

        for(GAction ga : allActions) {
            initialState.csp.bindings().addPossibleValue(ga.id);
            assert(!gactions.containsKey(ga.id));
            gactions.put(ga.id, ga);
        }

        for(GAction ga : allActions) {
            if(!varsOfAction.containsKey(ga.abs.name())) {
                varsOfAction.put(ga.abs.name(), ga.baseVars);
            }
        }

        allActions = getAllActions(initialState);
        base.gActions.clear();
        base.gActions.addAll(allActions);

        // get the maximum number of decompositions in the domain
        int maxNumDecompositions = 0;
        for(AbstractAction aa : initialState.pb.abstractActions()) {
            maxNumDecompositions = maxNumDecompositions > aa.jDecompositions().size() ?
                    maxNumDecompositions : aa.jDecompositions().size();
        }

        // create values for the decomposition variables
        for(int i=0 ; i<maxNumDecompositions ; i++) {
            initialState.csp.bindings().addPossibleValue(decCSPValue(i));
        }

        for(GAction ga : allActions) {
            if(!varsOfAction.containsKey(ga.abs.name())) {
                varsOfAction.put(ga.abs.name(), ga.baseVars);
            }
            if(ga.decID != -1 && !varsOfDecomposition.containsKey(new Pair<>(ga.baseName(), ga.decID))) {
                varsOfDecomposition.put(ga.decomposedName(), ga.decVars);
            }

            // all variables of this action
            List<String> values = new LinkedList<>();
            for(LVarRef var : varsOfAction.get(ga.abs.name()))
                values.add(ga.valueOf(var).instance());

            initialState.csp.bindings().addValuesToValuesSet(ga.abs.name(), values, ga.id);
        }
    }

    public static String decCSPValue(int decNumber) {
        return "decnum:"+decNumber;
    }

    public Set<GAction> getAllActions(State st) {
        if(st.addableGroundActions != null)
            return st.addableGroundActions;

        HReasoner<Term> r = getReasoner(st);
        HashSet<GAction> feasibles = new HashSet<>();
        for(Term t : r.trueFacts()) {
            if(t instanceof Predicate && ((Predicate) t).name.equals("possible_in_plan")) //TODO make a selector for this
                feasibles.add((GAction) ((Predicate) t).var);
        }

        List<Integer> allowedDomainOfActions = new LinkedList<>();
        for(GAction ga : feasibles) {
            allowedDomainOfActions.add(ga.id);
        }
        ValuesHolder dom = st.csp.bindings().intValuesAsDomain(allowedDomainOfActions);
        for(Action a : st.getAllActions()) {
            st.csp.bindings().restrictDomain(groundedActVariable.get(a.id()), dom);
        }

        st.addableGroundActions = feasibles;
        return feasibles;
    }

    public HReasoner<Term> getReasoner(State st) {
        return getReasoner(st, allActions);
    }

    public Collection<GTaskCond> getGroundedTasks(ActionCondition liftedTask, State st) {
        List<GTaskCond> tasks = new LinkedList<>();
        LinkedList<List<InstanceRef>> varDomains = new LinkedList<>();
        for(VarRef v : liftedTask.args()) {
            varDomains.add(new LinkedList<InstanceRef>());
            for(String value : st.domainOf(v)) {
                varDomains.getLast().add(st.pb.instance(value));
            }
        }
        List<List<InstanceRef>> instantiations = PGUtils.allCombinations(varDomains);
        for(List<InstanceRef> instantiation : instantiations) {
            GTaskCond task = new GTaskCond(liftedTask.abs(), instantiation);
            tasks.add(task);
        }
        return tasks;
    }

    public Iterable<GTaskCond> getDerivableTasks(State st) {
        List<GTaskCond> derivableTasks = new LinkedList<>();
        for(ActionCondition ac : st.getOpenTaskConditions()) {
            derivableTasks.addAll(getGroundedTasks(ac, st));
        }

        for(Action a : st.getOpenLeaves()) {
            for(Integer gActID : st.csp.bindings().domainOfIntVar(groundedActVariable.get(a.id()))) {
                GAction ga = gactions.get(gActID);
                for(GTaskCond tc : ga.subTasks) {
                    derivableTasks.add(tc);
                }
            }
        }
        return derivableTasks;
    }

    private HReasoner<Term> getReasoner(State st, Collection<GAction> acceptable) {
        if(st.reasoner != null)
            return st.reasoner;

        HReasoner<Term> r = new HReasoner<>(baseReasoner, true);
        for(Fluent f : GroundProblem.allFluents(st)) {
            // if the fluent is not already recorded, it does not appear in any clause and we can safely ignore it
            if(r.hasTerm(f))
                r.set(f);
        }

        for(GAction acc : acceptable)
            r.set(new Predicate("acceptable", acc));

        for(GTaskCond tc : getDerivableTasks(st)) {
            r.set(new Predicate("derivable_task", tc));
        }

        for(Action a : st.getAllActions()) {
            for(GAction ga : getGroundActions(a, st)) {
                r.set(new Predicate("in_plan", ga));
            }
        }

        Set<GAction> feasibles = new HashSet<>();

        for(Term t : r.trueFacts()) {
            if(t instanceof Predicate && ((Predicate) t).name.equals("possible_in_plan")) //TODO make a selector for this
                feasibles.add((GAction) ((Predicate) t).var);
        }

        // continue until a fixed point is reached
        if(feasibles.size() < acceptable.size()) {
            // number of possible actions was reduced, keep going
            return getReasoner(st, feasibles);
        } else {
            // fixed point reached
            st.reasoner = r;
            return r;
        }
    }

    public boolean checkFeasibility(State st) {
        Set<GAction> acts = getAllActions(st);

        for(Action a : st.getUnmotivatedActions()) {
            boolean derivable = false;
            for(GAction ga : getGroundActions(a, st)) {
                if (acts.contains(ga)) {
                    derivable = true;
                    break;
                }
            }
            if(!derivable) {
                // this unmotivated action cannot be derived from the current HTN
                return false;
            }
        }

        for(Action a : st.getAllActions()) {
            boolean feasibleAct = false;
            for(GAction ga : getGroundActions(a, st)) {
                if(st.reasoner.isTrue(new Predicate("possible_in_plan", ga))) {
                    feasibleAct = true;
                    break;
                }

            }
            if(!feasibleAct) {
                // there is no feasible ground versions of this action
                return false;
            }
        }

        // check that all open goals has at least one achievable fluent
        for(Timeline cons : st.tdb.getConsumers()) {
            boolean supported = false;
            for(Fluent f : DisjunctiveFluent.fluentsOf(cons.stateVariable, cons.getGlobalConsumeValue(), st, false)) {
                if(st.reasoner.hasTerm(f) && st.reasoner.isTrue(f)) {
                    supported = true;
                    break;
                }
            }
            if(!supported)
                return false;
        }

        // computes the set of non addable actions (used to prune resolvers)
        // TODO: use derivable? (here we are implicitly using all possible_in_plan)
        Set<AbstractAction> addableActions = new HashSet<>();
        for(GAction ga : getAllActions(st))
            addableActions.add(ga.abs);
        Set<AbstractAction> nonAddable = new HashSet<>(st.pb.abstractActions());
        nonAddable.removeAll(addableActions);
        st.notAddable = nonAddable;

        return true;
    }

    public Set<GAction> getGroundActions(Action liftedAction, State st) {
        Set<GAction> ret = new HashSet<>();
        assert(groundedActVariable.containsKey(liftedAction.id()));
        for(Integer i : st.csp.bindings().domainOfIntVar(this.groundedActVariable.get(liftedAction.id()))) {
            if(gactions.containsKey(i)) // the domain might contain any int variable
                ret.add(gactions.get(i));
        }

        return ret;
    }

    /** This will associate with an action a variable in the CSP representing its
     * possible ground versions.
     * @param act Action for which we need to create the variable.
     * @param st  State in which the action appears (needed to update the CSP)
     */
    public void createGroundActionVariables(Action act, State st) {
        assert !groundedActVariable.containsKey(act.id()) : "The action already has a variable for its ground version.";

        // all ground versions of this actions (represented by their ID)
        LVarRef[] vars = varsOfAction.get(act.abs().name());
        List<VarRef> values = new LinkedList<>();
        for(LVarRef v : vars) {
            if(v.id().equals("__dec__")) {
                VarRef decVar = new VarRef();
                List<String> domain = new LinkedList<>();
                for(int i=0 ; i< act.decompositions().size() ; i++)
                    domain.add(decCSPValue(i));
                st.csp.bindings().AddVariable(decVar, domain, "decomposition_variable");
                decompositionVariable.put(act.id(), decVar);
                values.add(decVar);
            } else {
                values.add(act.context().getDefinition(v)._2());
            }
        }
        // Variable representing the ground versions of this action
        VarRef gAction = new VarRef();
        st.csp.bindings().AddIntVariable(gAction);
        values.add(gAction);
        groundedActVariable.put(act.id(), gAction);
        st.addValuesSetConstraint(values, act.abs().name());
    }

    private Map<AbstractAction, List<GAction>> groundedActs = new HashMap<>();

    public List<GAction> getGrounded(AbstractAction abs) {
        if(!groundedActs.containsKey(abs)) {
            List<GAction> grounded = new LinkedList<>();
            for (GAction a : allActions)
                if (a.abs == abs)
                    grounded.add(a);
            groundedActs.put(abs, grounded);
        }
        return groundedActs.get(abs);
    }

    public Set<GAction> actionsInState(State st, Set<GAction> rpgFeasibleActions) {
        Set<GAction> ret = new HashSet<>();
        ValuesHolder current = new ValuesHolder(new LinkedList<Integer>());
        for(Action a : st.getAllActions()) {
            assert(groundedActVariable.containsKey(a.id()));
            ValuesHolder toAdd = st.csp.bindings().rawDomain(groundedActVariable.get(a.id()));
            current = current.union(toAdd);
        }
        for(Integer gaRawID : current.values()) {
            Integer gaID = st.csp.bindings().intValueOfRawID(gaRawID);
            assert(gactions.containsKey(gaID));
            GAction ga = gactions.get(gaID);
            assert ga != null;
            if(rpgFeasibleActions.contains(ga))
                ret.add(gactions.get(gaID));
        }
        return ret;
    }
}
