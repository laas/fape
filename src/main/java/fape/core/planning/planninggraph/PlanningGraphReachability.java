package fape.core.planning.planninggraph;

import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.*;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractActionRef;
import planstack.anml.model.abs.AbstractDecomposition;
import planstack.anml.model.concrete.*;
import planstack.constraints.bindings.ValuesHolder;

import java.util.*;

public class PlanningGraphReachability {

    final APlanner planner;
    public Map<String, LVarRef[]> varsOfAction = new HashMap<>();
    final Set<GAction> unfilteredActions;
    final Set<GAction> filteredActions;
    final GroundProblem base;
    /** Maps ground actions from their ID */
    public final HashMap<Integer, GAction> gactions = new HashMap<>();
    public final HashMap<ActRef, VarRef> groundedActVariable = new HashMap<>();

    /** Associate to a ground task condition all ground action (through their IDs) that can be derived from it */
    public final HashMap<GTaskCond, Set<Integer>> taskDerivabilities = new HashMap<>();


    public PlanningGraphReachability(APlanner planner, State initialState) {
        this.planner = planner;
        // this Problem contains all the ground actions
        base = new GroundProblem(initialState.pb);
        unfilteredActions = new HashSet<>(base.gActions);

        for(GAction ga : unfilteredActions) {
            initialState.csp.bindings().addPossibleValue(ga.id);
            assert(!gactions.containsKey(ga.id));
            gactions.put(ga.id, ga);
        }
        for(GAction act : unfilteredActions) {
            LinkedList<InstanceRef> args = new LinkedList<>();
            for(LVarRef var : act.abs.args())
                args.add(act.valueOf(var));
            GTaskCond tc = new GTaskCond(act.abs, args);
            if(!taskDerivabilities.containsKey(tc))
                taskDerivabilities.put(tc, new HashSet<Integer>());
            taskDerivabilities.get(tc).add(act.id);
        }

        for(GAction ga : unfilteredActions) {
            if(!varsOfAction.containsKey(ga.abs.name())) {
                varsOfAction.put(ga.abs.name(), ga.vars);
            }
        }

        Set<GAction> allFeasibleActions = getAllActions(initialState);

        this.filteredActions = allFeasibleActions;
        base.gActions.clear();
        base.gActions.addAll(filteredActions);

        for(GAction act : allFeasibleActions) {
            LinkedList<InstanceRef> args = new LinkedList<>();
            for(LVarRef var : act.abs.args())
                args.add(act.valueOf(var));
            GTaskCond tc = new GTaskCond(act.abs, args);
            if(!taskDerivabilities.containsKey(tc))
                taskDerivabilities.put(tc, new HashSet<Integer>());
            taskDerivabilities.get(tc).add(act.id);
        }

        for(GAction ga : allFeasibleActions) {
            if(!varsOfAction.containsKey(ga.abs.name())) {
                varsOfAction.put(ga.abs.name(), ga.vars);
            }

            List<String> values = new LinkedList<>();
            for(LVarRef var : varsOfAction.get(ga.abs.name()))
                values.add(ga.valueOf(var).instance());
            initialState.csp.bindings().addValuesToValuesSet(ga.abs.name(), values, ga.id);
        }
    }

    public boolean checkFeasibility(State st) {
        Set<GAction> acts = getAllActions(st);

        for(Action a : st.getUnmotivatedActions()) {
            boolean derivable = false;
            for(GAction ga : groundedVersions(a, st)) {
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

        GroundProblem pb = new GroundProblem(base, st);
        RelaxedPlanningGraph pg = new RelaxedPlanningGraph(pb, acts);
        pg.build();



        for(Action a : st.getAllActions()) {
            boolean feasibleAct = false;
            for(GAction ga : groundedVersions(a, st)) {
                if(acts.contains(ga)) {
                    feasibleAct = true;
                    break;
                }
            }
            if(!feasibleAct) {
                // there is no feasible ground versions of this action
                return false;
            }
        }

        Set<GAction> derivableOnly = derivableFromInitialTaskNetwork(st, acts);

        for(Timeline cons : st.consumers) {
            GroundProblem subpb = new GroundProblem(pb, st, cons);
            RelaxedPlanningGraph rpg = new RelaxedPlanningGraph(subpb, derivableOnly);
            int depth = rpg.buildUntil(new DisjunctiveFluent(cons.stateVariable, cons.getGlobalConsumeValue(), st));
            if(depth > 1000) {
                // this consumer cannot be derived (none of its ground versions appear in the planning graph)
                return false;
            }
        }
        Set<AbstractAction> addableActions = new HashSet<>();
        for(GAction ga : derivableOnly)
            addableActions.add(ga.abs);
        Set<AbstractAction> nonAddable = new HashSet<>(st.pb.abstractActions());
        nonAddable.removeAll(addableActions);
        st.notAddable = nonAddable;

        return true;
    }

    public Set<GAction> groundedVersions(Action a, State st) {
        Set<GAction> ret = new HashSet<>();
        assert(groundedActVariable.containsKey(a.id()));
        for(Integer i : st.csp.bindings().domainOfIntVar(this.groundedActVariable.get(a.id())))
            ret.add(gactions.get(i));

        return ret;
    }

    public Set<GAction> decomposable(Set<GAction> feasibles) {
        Set<GAction> ret = new HashSet<>();
        for(GAction a : feasibles) {
            boolean applicableSubTasks[] = new boolean[a.getActionRefs().size()];
            for(int i=0 ; i<applicableSubTasks.length ; i++)
                applicableSubTasks[i] = false;

            for(int i=0 ; i<applicableSubTasks.length ; i++) {
                GTaskCond gtc = a.getActionRefs().get(i);
                if(!taskDerivabilities.containsKey(gtc))
                    break; // no action supporting task cond in this problem
                for(Integer sub : taskDerivabilities.get(gtc)) {
                    if(feasibles.contains(gactions.get(sub))) {
                        applicableSubTasks[i] = true;
                        break;
                    }
                }
            }
            boolean applicable = true;
            for(boolean applicableSubTask : applicableSubTasks)
                applicable = applicable && applicableSubTask;

            if(applicable)
                ret.add(a);
        }
        return ret;
    }

    public Set<GAction> getAllActions(State st) {
        if(st.addableGroundActions != null)
            return st.addableGroundActions;

        GroundProblem pb = new GroundProblem(base, st);
        RelaxedPlanningGraph rpg = new RelaxedPlanningGraph(pb);
        rpg.build();
        Set<GAction> feasibles = new HashSet<>(rpg.getAllActions());
        List<Integer> feasiblesIDs = new LinkedList<>();
        for(GAction ga : feasibles)
            feasiblesIDs.add(ga.id);

        ValuesHolder dom = st.csp.bindings().intValuesAsDomain(feasiblesIDs);
        for(Action a : st.getAllActions()) {
            if(!groundedActVariable.containsKey(a.id()))
                createGroundActionVariable(a, st);
            st.csp.bindings().restrictDomain(groundedActVariable.get(a.id()), dom);
        }

        Set<GAction> feasibleAndDecomposable = decomposable(feasibles);
        assert feasibleAndDecomposable.size() <= feasibles.size();

        Set<GAction> strictest = feasibleAndDecomposable;

        boolean improved = true;
        while (improved) {
            Set<GAction> andDerivable = derivableFromInitialTaskNetwork(st, strictest);
            assert andDerivable.size() <= strictest.size();

            andDerivable.addAll(actionsInState(st, strictest));
            assert andDerivable.size() <= strictest.size();

            strictest = andDerivable;

            if(strictest.size() == feasibles.size())
                improved = false;
            else {
                pb.gActions.clear();
                pb.gActions.addAll(andDerivable);
                rpg = new RelaxedPlanningGraph(pb);
                rpg.build();
                feasibles = new HashSet<>(rpg.getAllActions());
                assert feasibles.size() <= strictest.size();
                strictest = feasibles;
            }
        }
        st.addableGroundActions = feasibles;
        return feasibles;
    }

    /** This will associate with an action a variable in the CSP representing its
     * possible ground versions.
     * @param act Action for which we need to create the variable.
     * @param st  State in which the action appears (needed to update the CSP)
     */
    public void createGroundActionVariable(Action act, State st) {
        assert !groundedActVariable.containsKey(act.id()) : "The action already has a variable for its ground version.";

        // all ground versions of this actions (represented by their ID)
        LVarRef[] vars = varsOfAction.get(act.abs().name());
        List<VarRef> values = new LinkedList<>();
        for(LVarRef v : vars)
            values.add(act.context().getDefinition(v)._2());
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
            for (GAction a : (filteredActions != null) ? filteredActions : unfilteredActions)
                if (a.abs == abs)
                    grounded.add(a);
            groundedActs.put(abs, grounded);
        }
        return groundedActs.get(abs);
    }

    public boolean feasible(GAction ga, Set<GAction> rpgFeasibleActions, State st) {
        if(!rpgFeasibleActions.contains(ga))
            return false;

        for(GTaskCond actRef : ga.getActionRefs()) {
            List<GAction> grounded = getGrounded(actRef.act);
            boolean oneFeasible = false;
            for(GAction subTask : grounded) {
                if(!rpgFeasibleActions.contains(subTask)) // not feasible
                    continue;
                for (int i = 0; i < actRef.args.length; i++) {
                    if(!actRef.args[i].equals(subTask.valueOf(actRef.act.args().get(i)))) {
                        break; // at least one arg different
                    }
                    if(i == actRef.args.length-1) {
                        oneFeasible = true; // rpg feasible and unfiable with subtask
                    }
                }
            }
            if(!oneFeasible)
                return false;
        }
        return true;
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

    public Set<GAction> derivableFromInitialTaskNetwork(State st, Set<GAction> allowed) {
        Set<GAction> possible = new HashSet<>();

        for(AbstractAction abs : st.pb.abstractActions())
            if(!abs.motivated())
                for(GAction ga : getGrounded(abs))
                    if(allowed.contains(ga))
                        possible.add(ga);

        for(ActionCondition ac : st.getOpenTaskConditions()) {
            List<GAction> grounded = getGrounded(ac.abs());
            for(GAction ga : grounded) {
                for (int i = 0; i < ac.args().size(); i++) {
                    if (!st.unifiable(ac.args().get(i), ga.valueOf(ac.abs().args().get(i)))) {
                        break;
                    }
                    if(i == ac.args().size()-1 && allowed.contains(ga))
                        possible.add(ga);
                }
            }
        }
        Set<GAction> pendingPossible = new HashSet<>(possible);

        for(Action a : st.getOpenLeaves()) {
            // if an action is not decomposed, it can produce new task conditions
            List<GAction> groundedVersions = new LinkedList<>();

            for(GAction ga : getGrounded(a.abs())) {
                if(!allowed.contains(ga))
                    continue;
                for(int i=0 ; i<a.args().size() ; i++) {
                    if(!st.unifiable(a.args().get(i), ga.valueOf(a.abs().args().get(i))))
                        break;

                    if(i == a.args().size()-1)
                        groundedVersions.add(ga);
                }
            }
            for(AbstractDecomposition dec : a.decompositions()) {
                for(AbstractActionRef ref : dec.jActions()) {
                    String name = ref.name();
                    for(GAction ga : groundedVersions) {
                        List<InstanceRef> args = new LinkedList<>();
                        for(LVarRef v : ref.jArgs())
                            args.add(ga.valueOf(v));
                        Set<GAction> supported = supportedByTaskCond(name, args);
                        for(GAction sup : supported) {
                            if(allowed.contains(sup)) {
                                pendingPossible.add(sup);
                                possible.add(sup);
                            }
                        }
                    }
                }
            }
        }


        while(!pendingPossible.isEmpty()) {
            GAction act = pendingPossible.iterator().next();
            pendingPossible.remove(act);

            for (GTaskCond gtc : act.getActionRefs()) {
                if (!taskDerivabilities.containsKey(gtc))
                    continue;
                for (Integer i : taskDerivabilities.get(gtc)) {
                    GAction derivable = gactions.get(i);
                    if (!possible.contains(derivable) && allowed.contains(derivable)) {
                        possible.add(derivable);
                        pendingPossible.add(derivable);
                    }
                }
            }
        }

        return possible;
    }

    public Set<GAction> supportedByTaskCond(String actName, List<InstanceRef> args) {
        Set<Integer> sup = taskDerivabilities.get(new GTaskCond(planner.pb.getAction(actName), args));
        HashSet<GAction> all = new HashSet<>();
        if(sup == null) // no action derivable from this task cond
            return all;

        for(int s : sup)
            all.add(gactions.get(s));

        return all;
    }
}
