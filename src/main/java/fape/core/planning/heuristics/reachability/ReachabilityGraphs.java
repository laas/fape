package fape.core.planning.heuristics.reachability;

import fape.core.inference.HLeveledReasoner;
import fape.core.planning.grounding.DisjunctiveFluent;
import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GTaskCond;
import fape.core.planning.heuristics.Preprocessor;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.FeasibilityReasoner;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import fape.util.EffSet;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Task;
import planstack.constraints.bindings.Domain;

public class ReachabilityGraphs {

    final APlanner planner;
    final State st;

    final FeasibilityReasoner fr;
    final Preprocessor pp;

    HLeveledReasoner<GAction,GTaskCond> derivGraph;
    HLeveledReasoner<GAction,Fluent> causalGraph;
    HLeveledReasoner<GAction,GTaskCond> decompGraph;

    EffSet<GAction> addableActions = null;
    EffSet<GAction> unsupporting;
    EffSet<Fluent> inPlanFluents;

    public ReachabilityGraphs(APlanner pl, State st) {
        this.planner = pl;
        this.st = st;
        this.fr = pl.preprocessor.getFeasibilityReasoner();
        this.pp = pl.preprocessor;
        initUnsupporting();
        initInPlanFluents();
        initGraphs(pp.getAllActions()); // TODO: reuse from set of previous state
    }

    private void initUnsupporting() {
        unsupporting = new EffSet<GAction>(pp.groundActionIntRepresentation());
        for(Action a : st.getAllActions())
            if(!st.taskNet.isSupporting(a))
                unsupporting.addAll(fr.getGroundActions(a, st));
    }

    public void initInPlanFluents() {
        inPlanFluents = pp.getGroundProblem().allFluents(st);
    }

    private void initGraphs(EffSet<GAction> allowed) {
        boolean print = true;
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

    public void restrictTaskSupporters() {
        EffSet<GAction> potentialSupporters = unsupporting.clone();
        potentialSupporters.addAll(addableActions);
        Domain dom = new Domain(potentialSupporters.toBitSet());
        for(Task t : st.getOpenTasks())
            st.csp.bindings().restrictDomain(t.groundSupportersVar(), dom);
    }

    public boolean hasUnreachableGoal() { //TODO: should just restrict the domain?
        for(Timeline og : st.tdb.getConsumers()) {
            boolean doable = false;
            for(Fluent f : DisjunctiveFluent.fluentsOf(og.stateVariable, og.getGlobalConsumeValue(), st, planner)) {
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

    public boolean hasNonMotivableAction() { // TODO: this should just restrict the domain
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
}
