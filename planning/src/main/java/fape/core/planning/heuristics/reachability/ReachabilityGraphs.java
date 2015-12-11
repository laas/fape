package fape.core.planning.heuristics.reachability;

import fape.core.inference.HLeveledReasoner;
import fape.core.planning.grounding.DisjunctiveFluent;
import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GTask;
import fape.core.planning.preprocessing.Preprocessor;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.FeasibilityReasoner;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import fape.util.EffSet;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Task;
import planstack.constraints.bindings.Domain;

import java.util.HashSet;
import java.util.Set;

public class ReachabilityGraphs {

    final APlanner planner;
    final State st;

    final FeasibilityReasoner fr;
    final Preprocessor pp;

    HLeveledReasoner<GAction,GTask> derivGraph;
    HLeveledReasoner<GAction,Fluent> causalGraph;
    HLeveledReasoner<GAction,GTask> decompGraph;

    EffSet<GAction> addableActions = null;
    EffSet<GAction> inPlan = null;
    EffSet<GAction> unsupporting;
    EffSet<GAction> openTasksActs;
    EffSet<Fluent> inPlanFluents;

    public ReachabilityGraphs(APlanner pl, State st) {
        assert st.reachabilityGraphs == null : "Rebuilding a reachability graph.";
        this.planner = pl;
        this.st = st;
        this.fr = pl.preprocessor.getFeasibilityReasoner();
        this.pp = pl.preprocessor;
        initUnsupporting();
        initInPlanFluents();
        initGraphs(st.addableActions != null ? st.addableActions : pp.getAllActions());

        restrictTaskSupporters();
        restrictUnattachedActions();

        st.reachabilityGraphs = this;
        assert addableActions != null;
        st.addableActions = addableActions;
        st.addableTemplates = new HashSet<>();
        for(GAction ga : addableActions) {
            st.addableTemplates.add(ga.abs);
        }
        inPlan = new EffSet<GAction>(pp.groundActionIntRepresentation());
        for(Action a : st.getAllActions()) {
            inPlan.addAll(new EffSet<GAction>(pp.groundActionIntRepresentation(), st.csp.bindings().rawDomain(a.instantiationVar()).toBitSet()));
        }
    }

    public boolean isRefinableToSolution() {
        return st.isConsistent() && !hasUnreachableGoal();
    }

    private void initUnsupporting() {
        unsupporting = new EffSet<GAction>(pp.groundActionIntRepresentation());
        for(Action a : st.getAllActions())
            if(!st.taskNet.isSupporting(a))
                unsupporting.addAll(fr.getGroundActions(a, st));
    }

    private void initOpenTasksActs() {
        assert openTasksActs == null;
        openTasksActs = new EffSet<GAction>(pp.groundActionIntRepresentation());
        for(Task t : st.getOpenTasks()) {
            Domain dom = st.csp.bindings().rawDomain(t.groundSupportersVar());
            openTasksActs.addAll(new EffSet<GAction>(pp.groundActionIntRepresentation(), dom.toBitSet()));
        }
    }

    public void initInPlanFluents() {
        inPlanFluents = pp.getGroundProblem().allFluents(st);
    }

    private void initGraphs(EffSet<GAction> allowed) {
        boolean print = false;
        EffSet<GAction> restrictedAllowed = allowed.clone();

        if(print) System.out.println("\nInit: "+st.mID+"\n"+allowed);

        derivGraph = planner.preprocessor.getRestrictedDerivabilityReasoner(restrictedAllowed);
        for(Task t : st.getOpenTasks())
            for(GAction ga : fr.getPossibleSupporters(t, st))
                derivGraph.set(ga.task);
        derivGraph.infer();
        restrictedAllowed = derivGraph.validClauses();

        if(print) System.out.println(restrictedAllowed);

        causalGraph = planner.preprocessor.getRestrictedCausalReasoner(restrictedAllowed);
        causalGraph.setAll(inPlanFluents);
        causalGraph.infer();
        restrictedAllowed = causalGraph.validClauses();

        if(print) System.out.println(restrictedAllowed);

        decompGraph = planner.preprocessor.getRestrictedDecomposabilityReasoner(restrictedAllowed);
        for(GAction ga : unsupporting)
            decompGraph.set(ga.task);
        decompGraph.infer();
        restrictedAllowed = decompGraph.validClauses();

        if(print) System.out.println(restrictedAllowed);

        if(allowed.size() > restrictedAllowed.size())
            initGraphs(restrictedAllowed);
        else
            addableActions = restrictedAllowed;
    }

    private void restrictTaskSupporters() {
        EffSet<GAction> potentialSupporters = unsupporting.clone();
        potentialSupporters.addAll(addableActions);
        Domain dom = new Domain(potentialSupporters.toBitSet());
        for(Task t : st.getOpenTasks())
            st.csp.bindings().restrictDomain(t.groundSupportersVar(), dom);
        st.isConsistent();
    }

    private boolean hasUnreachableGoal() { //TODO: should just restrict the domain?
        for(Timeline og : st.tdb.getConsumers()) {
            boolean doable = false;
            for(Fluent f : DisjunctiveFluent.fluentsOf(og.stateVariable, og.getGlobalConsumeValue(), st, planner)) {
                if(!causalGraph.knowsFact(f))
                    continue;
                if (causalGraph.levelOfFact(f) >= 0) {
                    doable = true;
                    break;
                }
            }
            if(!doable)
                return true;
        }
        return false;
    }

    private void restrictUnattachedActions() { // TODO: this method could be optimized
        initOpenTasksActs();
        EffSet<GAction> attachable = openTasksActs.clone();
        // a dependent action
        Set<GTask> tasks = new HashSet<>();
        for(GAction ga : addableActions) {
            tasks.addAll(ga.subTasks);
        }
        for(GAction ga : pp.getAllActions()) {
            if(tasks.contains(ga.task))
                attachable.add(ga);
        }

        Domain unattachedDomain = new Domain(attachable.toBitSet());
        for(Action a : st.getUnmotivatedActions()) {
            st.csp.bindings().restrictDomain(a.instantiationVar(), unattachedDomain);
        }
    }

    private boolean hasNonAttachableAction() { // TODO: this should just restrict the domain
        for(Action a : st.getUnmotivatedActions()) {
            boolean motivable = false;
            for(GAction ga : fr.getGroundActions(a, st)) {
                if(derivGraph.levelOfFact(ga.task) >= 0) {
                    motivable = true;
                    break;
                }
            }
            if(!motivable)
                return true;
        }
        return false;
    }


    public boolean isAddable(GAction ga) {
        return addableActions.contains(ga);
    }

    public int causalLevelOf(GAction ga) {
        if(addableActions.contains(ga)) {
            return causalGraph.levelOfClause(ga);
        } else {
            int max = 0;
            for(int precondition : ga.preconditions) {
                int lvl = causalGraph.levelOfFact(precondition);
                if(lvl == -1)
                    return -1;
                max = max > lvl ? max : lvl;
            }

            return max;
        }
    }

    public int causalLevelOfFluent(Fluent f) {
        return causalGraph.levelOfFact(f);
    }

    public int derivLevelOf(GAction ga) {
        if(!ga.abs.mustBeMotivated())
            return 0;

        if(addableActions.contains(ga)) {
            return derivGraph.levelOfClause(ga);
        } else {
            return derivGraph.levelOfFact(ga.task);
        }
    }

    public int decompLevelOf(GAction ga) {
        if(addableActions.contains(ga)) {
            return decompGraph.levelOfClause(ga);
        } else {
            int max = 0;
            for(GTask subtask : ga.subTasks) {
                int lvl = decompGraph.levelOfFact(subtask);
                if(lvl == -1)
                    return -1;
                max = max > lvl ? max : lvl;
            }
            return max;
        }
    }

    public Set<GAction> addableAndInPlanSupporters(Fluent f) {
        EffSet<GAction> all = addableActions.clone();
        all.addAll(inPlan);

        Set<GAction> supporters = new HashSet<>();

        for(GAction ga : all) {
            if(ga.add.contains(f) && causalLevelOf(ga) >= 0) {
                supporters.add(ga);
            }
        }
        return supporters;
    }
}
