package fape.core.planning.planner;

import fape.core.planning.planninggraph.*;
import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.search.flaws.finders.FlawFinder;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractActionRef;
import planstack.anml.model.abs.AbstractDecomposition;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.ActionCondition;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.VarRef;
import planstack.structures.Pair;
import scala.collection.JavaConversions;
import sun.awt.image.ImageWatched;

import java.io.PrintWriter;
import java.lang.management.GarbageCollectorMXBean;
import java.util.*;
import java.util.concurrent.Future;

public class PGReachabilityPlanner extends TaskConditionPlanner {

    public Map<String, LVarRef[]> varsOfAction = new HashMap<>();
    final List<GAction> unfilteredActions;
    final Set<GAction> filteredActions;
    final GroundProblem base;
//    public RelaxedPlanningGraph rpg;

    public class FeasibilityChecker implements FlawFinder {
        @Override
        public List<Flaw> getFlaws(State st, APlanner planner) {
            Collection<GAction> acts = getAllActions(base, st);
            System.out.println("num: " + acts.size());
            for(GAction a : acts)
                System.out.println(a);

            return new LinkedList<>();
        }
    }

    public boolean checkFeasibility(State st) {
        Set<GAction> acts = getAllActions(base, st);
//        System.out.println("num: " + acts.size());
//        for(Action a : st.getAllActions())
//            System.out.println("a  "+Printer.action(st, a));
//        for(GAction a : acts)
//            System.out.println("b  "+a);

        for(Action a : st.getUnmotivatedActions()) {
            boolean derivable = false;
            for(GAction ga : groundedVersions(a, st)) {
                if (acts.contains(ga)) {
                    derivable = true;
                    break;
                }
            }
            if(!derivable) {
                System.out.println("NOT DERIVABLE     " + Printer.action(st, a) + "      !!!!!!!!!!!!!!!!!!");
                return false;
//                for (Action ac : st.getAllActions())
//                    System.out.println(Printer.action(st, ac));
            }
        }

        GroundProblem pb = new GroundProblem(base, st);
        RelaxedPlanningGraph pg = new RelaxedPlanningGraph(pb, acts);
        pg.build();

        for(TemporalDatabase cons : st.consumers) {
            DisjunctiveFluent df = new DisjunctiveFluent(cons.stateVariable, cons.GetGlobalConsumeValue(), st, pb);
            if(!pg.supported(df)) {
                System.out.println("NOT INFERABLE   "+Printer.inlineTemporalDatabase(st, cons));
                return false;
            }
        }

        for(Action a : st.getAllActions()) {
            boolean feasibleAct = false;
            for(GAction ga : groundedVersions(a, st)) {
                if(feasible(ga, acts, st))
                    feasibleAct = true;
            }
            if(!feasibleAct) {
                System.out.println("NOT FEASIBLE: " + a);
                return false;
            }
        }
        return true;
    }

    public Set<GAction> groundedVersions(Action a, State st) {
        Set<GAction> ret = new HashSet<>();
        for(GAction ga : getGrounded(a.abs())) {
            for(int i=0 ; i<a.args().size() ; i++) {
                if(!st.unifiable(a.args().get(i), ga.valueOf(a.abs().args().get(i))))
                    break;

                if(i == a.args().size()-1)
                    ret.add(ga);
            }
        }
        return ret;
    }

    public PGReachabilityPlanner(State initialState, PlanningOptions options) {
        super(initialState, options);
        // this Problem contains all the ground actions
        base = new GroundProblem(initialState.pb);
        unfilteredActions = base.gActions;
//        GroundProblem fromState = new GroundProblem(base, initialState);
//
//        System.out.println();
//
//        rpg = new RelaxedPlanningGraph(fromState);
//        rpg.build();
//        System.out.println("rpg before: " + rpg.getAllActions().size());
//
//        System.out.println("before: "+ fromState.gActions.size());
//        Set<GAction> groundPossible = derivableFromInitialTaskNetwork(initialState, new HashSet<GAction>(rpg.getAllActions()));
//        fromState.gActions.clear();
//        fromState.gActions.addAll(groundPossible);
//        System.out.println("after: " + fromState.gActions.size());
//
//        rpg = new RelaxedPlanningGraph(fromState);
//        rpg.build();
//        System.out.println("rpg after: "+rpg.getAllActions().size());
//        for(GAction ga : rpg.getAllActions()) {
//            System.out.println(ga);
//        }

        Set<GAction> allFeasibleActions = getAllActions(base, initialState);
//        for(GAction ga : allFeasibleActions)
//            System.out.println(ga.toString()+"                       aaaaaa");

        this.filteredActions = allFeasibleActions;
        base.gActions.clear();
        base.gActions.addAll(filteredActions);

        for(GAction ga : allFeasibleActions) {
            if(!varsOfAction.containsKey(ga.abs.name())) {
                varsOfAction.put(ga.abs.name(), ga.vars);
            }

            List<String> values = new LinkedList<>();
            for(LVarRef var : varsOfAction.get(ga.abs.name()))
                values.add(ga.valueOf(var).instance());
            initialState.addValuesToValuesSet(ga.abs.name(), values);
        }

        // add all other actions with nothing
//        for(AbstractAction aa : pb.abstractActions()) {
//            if(!varsOfAction.containsKey(aa.name())) {
//                varsOfAction.put(aa.name(), aa.args().toArray(new LVarRef[aa.args().size()]));
//            }
//        }

        toASP(initialState);
    }


    public String toASP(State st) {
        StringBuilder sb = new StringBuilder();
        GroundProblem pb = new GroundProblem(base, st);

        sb.append("\n");
        for(GAction ga : pb.allActions()) {
            sb.append("% "); sb.append(ga.toString()); sb.append("\n");
            if(!ga.abs.motivated()) {
                sb.append("deriv(");
                sb.append(ga.toASP());
                sb.append(").\n");
            }
            if(ga.pre.isEmpty()) {
                sb.append("sup(");
                sb.append(ga.toASP());
                sb.append(").\n");
            } else {
                sb.append("sup(");
                sb.append(ga.toASP());
                sb.append(") :- ");
                for (int i = 0; i < ga.pre.size(); i++) {
                    sb.append(ga.pre.get(i).toASP());
                    if (i == ga.pre.size() - 1)
                        sb.append(".\n");
                    else
                        sb.append(", ");
                }
            }

            for(Fluent add : ga.add) {
                sb.append(add.toASP());
                sb.append(" :- usable(");
                sb.append(ga.toASP());
                sb.append(").\n");
            }
            sb.append("\n");
        }

        for(GAction a : actionsInState(st, new HashSet<GAction>(pb.allActions()))) {
            sb.append("deriv(");
            sb.append(a.toASP());
            sb.append(").\n");
        }


        sb.append("\n% Refinements\n");
        for(GAction a : pb.allActions()) {
            for(Pair<String, List<InstanceRef>> actRef : a.getActionRefs()) {
                AbstractAction abs = st.pb.getAction(actRef.v1());
                List<GAction> grounded = getGrounded(abs);
                boolean oneFeasible = false;
                for(GAction subTask : grounded) {
                    for (int i = 0; i < actRef.v2().size(); i++) {
                        if(!actRef.v2().get(i).equals(subTask.valueOf(abs.args().get(i)))) {
                            break; // at least one arg different
                        }
                        if(i == actRef.v2().size()-1) {
                            sb.append("deriv(");
                            sb.append(subTask.toASP());
                            sb.append(") :- deriv(");
                            sb.append(a.toASP());
                            sb.append(").\n");
                        }
                    }
                }
            }
        }

        for(GAction a : pb.allActions()) {
            if(a.getActionRefs().isEmpty()) {
                sb.append("decomposable(");
                sb.append(a.toASP());
                sb.append(").\n");
            } else {
                sb.append("decomposable(");
                sb.append(a.toASP());
                sb.append(") :- ");
                Iterator<Pair<String, List<InstanceRef>>> actRefIt = a.getActionRefs().iterator();
                while (actRefIt.hasNext()) {
                    Pair<String, List<InstanceRef>> actRef = actRefIt.next();

                    AbstractAction abs = st.pb.getAction(actRef.v1());
                    List<GAction> grounded = getGrounded(abs);
                    boolean oneFeasible = false;
                    sb.append("1 { usable(");
                    Iterator<GAction> subTasksIt = grounded.iterator();
                    while(subTasksIt.hasNext()) {
                        GAction subTask = subTasksIt.next();

                        sb.append(subTask.toASP());
                        if(subTasksIt.hasNext())
                            sb.append("; ");
                        else
                            sb.append(")");

                    }
                    sb.append("}");
                    if(actRefIt.hasNext())
                        sb.append(", ");
                    else
                        sb.append(".\n");
                }
            }
        }

        sb.append("\nusable(X) :- deriv(X), sup(X), decomposable(X).\n");

        StringBuilder pbSb = new StringBuilder();

        pbSb.append("%% Initial task network\n");
        for(ActionCondition ac : st.getOpenTaskConditions()) {
            pbSb.append("% "); pbSb.append(Printer.taskCondition(st, ac)); pbSb.append("\n");
            List<GAction> grounded = getGrounded(ac.abs());
            for(GAction ga : grounded) {
                for (int i = 0; i < ac.args().size(); i++) {
                    if (!st.unifiable(ac.args().get(i), ga.valueOf(ac.abs().args().get(i)))) {
                        break;
                    }
                    if(i == ac.args().size()-1) {
                        pbSb.append("deriv(");
                        pbSb.append(ga.toASP());
                        pbSb.append(").\n");
                    }

                }
            }
        }



        pbSb.append("% initial states (fluents)\n");
        for(Fluent f : pb.initState.fluents) {
            pbSb.append(f.toASP()+".\n");
        }


        pbSb.append("#show usable/1.\n");
        try {
            PrintWriter domPW = new PrintWriter("dom.lp", "UTF-8");
            domPW.write(sb.toString());
            domPW.close();

            PrintWriter pbPW = new PrintWriter("pb.pl", "UTF-8");
            pbPW.write(pbSb.toString());
            pbPW.close();
        } catch (Exception e) {
            System.err.println("ERROR, file not found");
        }
        return sb.toString();
    }

    public Set<GAction> getAllActions(GroundProblem base, State st) {
        GroundProblem pb = new GroundProblem(base, st);
        RelaxedPlanningGraph rpg = new RelaxedPlanningGraph(pb);
        rpg.build();
        Set<GAction> feasibles = new HashSet<>(rpg.getAllActions());

        boolean improved = true;
        while (improved) {
//            System.out.println("Orig feasible: "+feasibles.size());
            Set<GAction> feasibleAndDerivable = derivableFromInitialTaskNetwork(st, feasibles);
            feasibleAndDerivable.addAll(actionsInState(st, feasibles));

            assert feasibleAndDerivable.size() <= feasibles.size();
            if(feasibleAndDerivable.size() == feasibles.size())
                improved = false;
            else {
                pb.gActions.clear();
                pb.gActions.addAll(feasibleAndDerivable);
                rpg = new RelaxedPlanningGraph(pb);
                rpg.build();
                feasibles = new HashSet<>(rpg.getAllActions());
//                System.out.println("new derivable: "+feasibleAndDerivable.size());
            }
        }
        return feasibles;
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

        for(Pair<String, List<InstanceRef>> actRef : ga.getActionRefs()) {
            AbstractAction abs = st.pb.getAction(actRef.v1());
            List<GAction> grounded = getGrounded(abs);
            boolean oneFeasible = false;
            for(GAction subTask : grounded) {
                if(!rpgFeasibleActions.contains(subTask)) // not feasible
                    continue;
                for (int i = 0; i < actRef.v2().size(); i++) {
                    if(!actRef.v2().get(i).equals(subTask.valueOf(abs.args().get(i)))) {
                        break; // at least one arg different
                    }
                    if(i == actRef.v2().size()-1) {
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
        for(Action a : st.getAllActions()) {
            for(GAction ga : groundedVersions(a, st)) {
                if(feasible(ga, rpgFeasibleActions, st))
                    ret.add(ga);
            }
        }
        if(ret.size() == 0 && st.getAllActions().size() > 0) {
            System.out.println("["+st.mID+"] INFEAS?  Strange, no actions in state.");
//            actionsInState(st, rpgFeasibleActions);
        }
//        for(GAction a : ret) {
//            System.out.println("ccc   "+a);
//        }
        return ret;
    }
//    int count = 0;

    public Set<GAction> derivableFromInitialTaskNetwork(State st, Set<GAction> rpgFeasibleActions) {
        Set<GAction> possible = new HashSet<>();

        for(AbstractAction abs : st.pb.abstractActions())
            if(!abs.motivated())
                for(GAction ga : getGrounded(abs))
                    if(feasible(ga, rpgFeasibleActions, st))
                        possible.add(ga);

        for(ActionCondition ac : st.getOpenTaskConditions()) {
            List<GAction> grounded = getGrounded(ac.abs());
            for(GAction ga : grounded) {
                for (int i = 0; i < ac.args().size(); i++) {
                    if (!st.unifiable(ac.args().get(i), ga.valueOf(ac.abs().args().get(i)))) {
                        break;
                    }
                    if(i == ac.args().size()-1 && feasible(ga, rpgFeasibleActions, st))
                        possible.add(ga);
                }
            }
        }

        Set<GAction> pendingPossible = new HashSet<>(possible);

        for(Action a : st.getOpenLeaves()) {
            // if an action is not decomposed, it can produce new task conditions
            List<GAction> groundedVersions = new LinkedList<>();

            for(GAction ga : getGrounded(a.abs())) {
                if(!feasible(ga, rpgFeasibleActions, st))
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
                        Set<GAction> supported = supportedByTaskCond(name, args, st);
                        for(GAction sup : supported) {
                            if(feasible(sup, rpgFeasibleActions, st)) {
                                pendingPossible.add(sup);
                                possible.add(sup);
                            }
                        }
                    }
                }
            }

//            possible.addAll(groundedVersions);
        }


        while(!pendingPossible.isEmpty()) {
            GAction act = pendingPossible.iterator().next();
            pendingPossible.remove(act);

            for(Pair<String, List<InstanceRef>> actRef : act.getActionRefs()) {
                AbstractAction abs = st.pb.getAction(actRef.v1());
                List<GAction> grounded = getGrounded(abs);
                Set<GAction> supportedByTaskCond = supportedByTaskCond(actRef.v1(), actRef.v2(), st);
                for(GAction supported : supportedByTaskCond)
                    if(!possible.contains(supported) && feasible(supported, rpgFeasibleActions, st)) {
                        possible.add(supported);
                        pendingPossible.add(supported);
                    }
            }
        }

//        for(GAction ga : possible) {
//            System.out.println(ga);
//        }

        return possible;
    }

    public Set<GAction> supportedByTaskCond(String actName, List<InstanceRef> args, State st) {
        Set<GAction> supported = new HashSet<>();
        AbstractAction abs = st.pb.getAction(actName);
        List<GAction> grounded = getGrounded(abs);
        for(GAction ga : grounded) {
//            if(possible.contains(ga))
//                continue;
            for (int i = 0; i < args.size(); i++) {
                if(!args.get(i).equals(ga.valueOf(abs.args().get(i)))) {
                    break;
                }
                if(i == args.size()-1) {
                    supported.add(ga);
                }
            }
        }
        return supported;
    }

    @Override
    public String shortName() {
        return "pgr";
    }
}
