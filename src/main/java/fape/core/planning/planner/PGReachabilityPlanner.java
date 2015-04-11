package fape.core.planning.planner;

import fape.core.planning.planninggraph.*;
import fape.core.planning.search.flaws.finders.FlawFinder;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractActionRef;
import planstack.anml.model.abs.AbstractDecomposition;
import planstack.anml.model.concrete.*;
import planstack.constraints.bindings.ValuesHolder;

import java.io.PrintWriter;
import java.util.*;

public class PGReachabilityPlanner extends TaskConditionPlanner {

    public Map<String, LVarRef[]> varsOfAction = new HashMap<>();
    final Set<GAction> unfilteredActions;
    final Set<GAction> filteredActions;
    final GroundProblem base;
    public final HashMap<Integer, GAction> gactions = new HashMap<>();
    public final HashMap<ActRef, VarRef> groundedActVariable = new HashMap<>();
//    public final HashMap<Integer, Set<Integer>> derivabilities = new HashMap<>();
    public final HashMap<GTaskCond, Set<Integer>> taskDerivabilities = new HashMap<>();
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
//                System.out.println("NOT DERIVABLE     " + Printer.action(st, a) + "      !!!!!!!!!!!!!!!!!!");
                return false;
//                for (Action ac : st.getAllActions())
//                    System.out.println(Printer.action(st, ac));
            }
        }

        GroundProblem pb = new GroundProblem(base, st);
        RelaxedPlanningGraph pg = new RelaxedPlanningGraph(pb, acts);
        pg.build();



        for(Action a : st.getAllActions()) {
            boolean feasibleAct = false;
            for(GAction ga : groundedVersions(a, st)) {
//                if(feasible(ga, acts, st))
                if(acts.contains(ga)) {
                    feasibleAct = true;
                    break;
                }
            }
            if(!feasibleAct) {
//                System.out.println("NOT FEASIBLE: " + a);
                return false;
            }
        }

        Set<GAction> derivableOnly = derivableFromInitialTaskNetwork(st, acts);
//        System.out.println(derivableOnly.size()+" "+acts.size());
//        if(derivableOnly.size() == 0) {
//            System.out.println(st.depth+"\t      "+ SOCA.f(st));
//        } else {
//            System.out.println("                      aaaaaaaaaaaaaaa");
//        }
        for(TemporalDatabase cons : st.consumers) {/*
            DisjunctiveFluent df = new DisjunctiveFluent(cons.stateVariable, cons.getGlobalConsumeValue(), st, pb);
            if(!pg.supported(df)) {
//                System.out.println("NOT INFERABLE   "+Printer.inlineTemporalDatabase(st, cons));
                return false;
            }*/
            GroundProblem subpb = new GroundProblem(pb, st, cons);
            RelaxedPlanningGraph rpg = new RelaxedPlanningGraph(subpb, derivableOnly);
            int depth = rpg.buildUntil(new DisjunctiveFluent(cons.stateVariable, cons.getGlobalConsumeValue(), st));
            if(depth > 1000) {
//                System.out.println("CUT OFF: "+Printer.inlineTemporalDatabase(st, cons));
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

    public Set<GAction> oldGroundedVersions(Action a, State st) {
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

    public Set<GAction> groundedVersions(Action a, State st) {
        Set<GAction> ret = new HashSet<>();
        assert(groundedActVariable.containsKey(a.id()));
        for(Integer i : st.csp.bindings().domainOfIntVar(this.groundedActVariable.get(a.id())))
            ret.add(gactions.get(i));

        return ret;
    }


    public PGReachabilityPlanner(State initialState, PlanningOptions options) {
        super(initialState, options);
        // this Problem contains all the ground actions
        base = new GroundProblem(initialState.pb);
        unfilteredActions = new HashSet<>(base.gActions);

        for(GAction ga : unfilteredActions) {
            initialState.csp.bindings().addPossibleValue(ga.id);
            assert(!gactions.containsKey(ga.id));
            gactions.put(ga.id, ga);
        }
        int cnt = 0;
        for(GAction act : unfilteredActions) {

            LinkedList<InstanceRef> args = new LinkedList<>();
            for(LVarRef var : act.abs.args())
                args.add(act.valueOf(var));
            GTaskCond tc = new GTaskCond(act.abs, args);
            if(!taskDerivabilities.containsKey(tc))
                taskDerivabilities.put(tc, new HashSet<Integer>());
            taskDerivabilities.get(tc).add(act.id);

            /*
            derivabilities.put(act.id, new HashSet<Integer>());
            for(Pair<String, List<InstanceRef>> actRef : act.getActionRefs()) {
                AbstractAction abs = pb.getAction(actRef.v1());
                LVarRef[] args = new LVarRef[abs.args().size()];
                for(int i=0 ; i<args.length ; i++) args[i] = abs.args().get(i);
                List<GAction> grounded = getGrounded(abs);
                for (GAction subTask : grounded) {
                    cnt ++;
                    for (int i = 0; i < actRef.v2().size(); i++) {
                        if (!unfilteredActions.contains(subTask))
                            continue;
                        if (!actRef.v2().get(i).equals(subTask.valueOf(args[i])))
                            break; // at least one arg different
                        if (i == actRef.v2().size() - 1) {
                            derivabilities.get(act.id).add(subTask.id);
                        }
                    }
                }
            }*/
        }
//        System.out.println(unfilteredActions.size() + " " + cnt);

        Set<GAction> allFeasibleActions = getAllActions(base, initialState);
//        for(GAction ga : allFeasibleActions)
//            System.out.println(ga.toString()+"                       aaaaaa");

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
            /*
            derivabilities.put(act.id, new HashSet<Integer>());

            for(Pair<String, List<InstanceRef>> actRef : act.getActionRefs()) {
                AbstractAction abs = pb.getAction(actRef.v1());
                List<GAction> grounded = getGrounded(abs);
                for (GAction subTask : grounded) {
                    for (int i = 0; i < actRef.v2().size(); i++) {
                        if (!allFeasibleActions.contains(subTask))
                            continue;
                        if (!actRef.v2().get(i).equals(subTask.valueOf(abs.args().get(i))))
                            break; // at least one arg different
                        if (i == actRef.v2().size() - 1) {
                            derivabilities.get(act.id).add(subTask.id);
                        }
                    }
                }
            }*/
//                Set<GAction> supportedByTaskCond = supportedByTaskCond(actRef.v1(), actRef.v2());
//                for(GAction supported : supportedByTaskCond)
//                    if(allFeasibleActions.contains(supported)) {
//                        derivabilities.get(act.id).add(supported.id);
//                    }
//            }
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
/*
        for(Map.Entry<String, ExtensionConstraint> entry : initialState.csp.bindings().exts.entrySet()) {
            System.out.println(entry.getKey());
            for(List<Integer> vals : entry.getValue().values) {
                System.out.print(" ");
                for(int i=0 ; i<vals.size() ; i++) {
                    if(i != vals.size()-1)
                        System.out.print(initialState.csp.bindings().values.get(vals.get(i))+" ");
                    else
                        System.out.println(initialState.csp.bindings().intValues.get(vals.get(i))+":"+
                                gactions.get(initialState.csp.bindings().intValues.get(vals.get(i))));
                }
            }
        }*/

        // add all other actions with nothing
//        for(AbstractAction aa : pb.abstractActions()) {
//            if(!varsOfAction.containsKey(aa.name())) {
//                varsOfAction.put(aa.name(), aa.args().toArray(new LVarRef[aa.args().size()]));
//            }
//        }

//        toASP(initialState);
//        for(Map.Entry<AbstractAction, Integer> e : callCount.entrySet()) {
//            System.out.println(e.getKey()+" "+e.getValue());
//        }
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
            for(GTaskCond actRef : a.getActionRefs()) {
                List<GAction> grounded = getGrounded(actRef.act);
                boolean oneFeasible = false;
                for(GAction subTask : grounded) {
                    for (int i = 0 ; i < actRef.args.length ; i++) {
                        if(!actRef.args[i].equals(subTask.valueOf(actRef.act.args().get(i)))) {
                            break; // at least one arg different
                        }
                        if(i == actRef.args.length-1) {
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
                Iterator<GTaskCond> actRefIt = a.getActionRefs().iterator();
                while (actRefIt.hasNext()) {
                    GTaskCond actRef = actRefIt.next();

                    List<GAction> grounded = getGrounded(actRef.act);
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

    public Set<GAction> getAllActions(GroundProblem base, State st) {
        GroundProblem pb = new GroundProblem(base, st);
        RelaxedPlanningGraph rpg = new RelaxedPlanningGraph(pb);
        rpg.build();
        Set<GAction> feasibles = new HashSet<>(rpg.getAllActions());
        List<Integer> feasiblesIDs = new LinkedList<>();
        for(GAction ga : feasibles)
            feasiblesIDs.add(ga.id);

        ValuesHolder dom = st.csp.bindings().intValuesAsDomain(feasiblesIDs);
        for(Action a : st.getAllActions()) {
            st.csp.bindings().restrictDomain(groundedActVariable.get(a.id()), dom);
        }

        Set<GAction> feasibleAndDecomposable = decomposable(feasibles);
        assert feasibleAndDecomposable.size() <= feasibles.size();

        Set<GAction> strictest = feasibleAndDecomposable;

        boolean improved = true;
        while (improved) {
//            System.out.println("Orig feasible: "+feasibles.size());
            Set<GAction> andDerivable = derivableFromInitialTaskNetwork(st, strictest);
            assert andDerivable.size() <= strictest.size();

            andDerivable.addAll(actionsInState(st, strictest));
            assert andDerivable.size() <= strictest.size();

            strictest = andDerivable;

//            Set<GAction> feasibleDerivableDecomposable = new HashSet<>();
//            for(GAction a : feasibleAndDerivable) {
//                if(feasible(a, feasibleAndDerivable, st))
//                    feasibleDerivableDecomposable.add(a);
//            }

//            assert feasibleDerivableDecomposable.size() <= feasibles.size();
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
        count++;
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

    public Set<GAction> OLDactionsInState(State st, Set<GAction> rpgFeasibleActions) {
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
    int count = 0;


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

//            possible.addAll(groundedVersions);
        }


        while(!pendingPossible.isEmpty()) {
            GAction act = pendingPossible.iterator().next();
            pendingPossible.remove(act);

            for(GTaskCond gtc : act.getActionRefs()) {
                if(!taskDerivabilities.containsKey(gtc))
                    continue;
                for (Integer i : taskDerivabilities.get(gtc)) {
                    GAction derivable = gactions.get(i);
                    if (!possible.contains(derivable) && allowed.contains(derivable)) {
                        possible.add(derivable);
                        pendingPossible.add(derivable);
                    }
                }
            }
/*
            for(Pair<String, List<InstanceRef>> actRef : act.getActionRefs()) {
                AbstractAction abs = st.pb.getAction(actRef.v1());
                List<GAction> grounded = getGrounded(abs);
                Set<GAction> supportedByTaskCond = supportedByTaskCond(actRef.v1(), actRef.v2());
                for(GAction supported : supportedByTaskCond)
                    if(!possible.contains(supported) && feasible(supported, rpgFeasibleActions, st)) {
                        possible.add(supported);
                        pendingPossible.add(supported);
                    }
            }*/
        }
//        for(GAction poss : possible)
//            assert allowed.contains(poss);

//        for(GAction ga : possible) {
//            System.out.println(ga);
//        }

//        System.out.println(count+" "+possible.size());
//        count = 0;
        return possible;
    }

    private Map<AbstractAction, Map<LVarRef, Map<VarRef, BitSet>>> as = null;
    private Map<AbstractAction, Integer> callCount = new HashMap<>();

    public Set<GAction> supportedByTaskCond(String actName, List<InstanceRef> args) {
        /*if(as == null) {
            as = new HashMap<>();
            for(AbstractAction a : pb.abstractActions()) {
                callCount.put(a, 0);
                as.put(a, new HashMap<LVarRef, Map<VarRef, BitSet>>());
                for(LVarRef arg : a.args())
                    as.get(a).put(arg, new HashMap<VarRef, BitSet>());
                for(GAction ga : getGrounded(a)) {
                    for(LVarRef arg : a.args()) {
                        InstanceRef val = ga.valueOf(arg);
                        if(!as.get(a).get(arg).containsKey(val))
                            as.get(a).get(arg).put(val, new BitSet());
                        as.get(a).get(arg).get(val).set(ga.id);
                    }
                }
            }
        }

        AbstractAction abs = pb.getAction(actName);
        callCount.put(abs, callCount.get(abs)+1);
        BitSet supported = null;
        if(args.size() == 0) {
            supported = new BitSet();
            for(GAction ga : getGrounded(abs))
                supported.set(ga.id);
        }

        for(int i=0 ; i<args.size() ; i++) {
            LVarRef var = abs.args().get(i);
            InstanceRef val = args.get(i);
            assert as.containsKey(abs) : "Abstract action "+abs+" is not recorded.";
            assert as.get(abs).containsKey(var);
            if(!as.get(abs).get(var).containsKey(val))
                supported = new BitSet(); // no ground action of this value
            else if(supported == null) {
                supported = new BitSet(as.get(abs).get(var).get(val).size());
                supported.or(as.get(abs).get(var).get(val));
            }
            else {
                supported.and(as.get(abs).get(var).get(val));
            }
        }*/

//        Set<GAction> supported = new HashSet<>();
//        AbstractAction abs = pb.getAction(actName);
//        List<GAction> grounded = getGrounded(abs);
//        System.out.println(abs.name()+"  "+grounded.size());
//        for(GAction ga : grounded) {
////            if(possible.contains(ga))
////                continue;
//            for (int i = 0; i < args.size(); i++) {
//                if(!args.get(i).equals(ga.valueOf(abs.args().get(i)))) {
//                    break;
//                }
//                if(i == args.size()-1) {
//                    supported.add(ga);
//                }
//            }
//        }
        Set<Integer> sup = taskDerivabilities.get(new GTaskCond(pb.getAction(actName), args));
        HashSet<GAction> all = new HashSet<>();
        if(sup == null) // no action derivable from this task cond
            return all;
        for(int s : sup)
            all.add(gactions.get(s));
//        for(int i=0 ; i<supported.size() ; i++)
//            if(supported.get(i))
//                all.add(gactions.get(i));
        return all;
    }

    @Override
    public String shortName() {
        return "pgr";
    }
}
