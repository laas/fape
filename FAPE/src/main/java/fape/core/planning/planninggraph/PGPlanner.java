package fape.core.planning.planninggraph;

import fape.core.planning.planner.APlanner;
import fape.core.planning.planner.ActionExecution;
import fape.core.planning.preprocessing.AbstractionHierarchy;
import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.preprocessing.LiftedDTG;
import fape.core.planning.search.flaws.resolvers.SupportingAction;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.util.Pair;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.ActRef;
import planstack.anml.parser.ParseResult;
import planstack.constraints.stnu.Controllability;

import java.util.*;

/**
 * Planner that uses a relaxed planning graph for domain analysis (to select action resolvers).
 *
 * It does not cover the whole set of ANML problems for instance problems/concurrent-actions.anml
 * would fail with this planner.
 * It also produces a ground problem that might impractical in some domain with an important number of objects.
 */
public class PGPlanner extends APlanner {

    protected GroundProblem groundPB = null;
    protected RelaxedPlanningGraph pg = null;
    AbstractionHierarchy hierarchy = null; //TODO why?
    LiftedDTG dtg = null;

    public PGPlanner(State initialState, String[] planSelStrategies, String[] flawSelStrategies, Map<ActRef, ActionExecution> actionsExecutions) {
        super(initialState, planSelStrategies, flawSelStrategies, actionsExecutions);

        groundPB = new GroundProblem(this.pb);
        pg = new RelaxedPlanningGraph(groundPB);
        pg.build();
        hierarchy = new AbstractionHierarchy(this.pb);
        dtg = new LiftedDTG(pb);
    }

    public PGPlanner(Controllability controllability, String[] planSelStrategies, String[] flawSelStrategies) {
        super(controllability, planSelStrategies, flawSelStrategies);
    }

    @Override
    public boolean ForceFact(ParseResult anml, boolean propagate) {
        super.ForceFact(anml, propagate);

        if(GroundProblem.sizeEvaluation(pb) > 1000000)
            return false;

        groundPB = new GroundProblem(this.pb);
        pg = new RelaxedPlanningGraph(groundPB);
        pg.build();
        if(APlanner.debugging)
            pg.graph.exportToDotFile("rpg.dot");
        hierarchy = new AbstractionHierarchy(this.pb);
        dtg = new LiftedDTG(pb);

        return true;
    }


    @Override
    public String shortName() {
        return "rpg";
    }

    @Override
    public ActionSupporterFinder getActionSupporterFinder() {
        return dtg;
    }

    /** TODO: This should be implemented by overriding the OpenGoalFinder.
    @Override
    public List<Resolver> GetSupporters(TemporalDatabase db, State st) {
        // use the inherited GtSupporter method
        List<Resolver> supportOptions = super.GetSupporters(db, st);

        // remove all supporting actions, they will be replaced by out own ActionWithBindings
        List<Resolver> toRemove = new LinkedList<>();
        for(Resolver o : supportOptions) {
            if(o.hasActionInsertion()) {
                toRemove.add(o);
            }
        }
        supportOptions.removeAll(toRemove);


        for(SupportingAction opt : rpgActionSupporters(db, st)) {
            supportOptions.add(opt);
        }


        return supportOptions;
        //return super.GetSupporters(db, st);
    }
    */

    public List<SupportingAction> rpgActionSupporters(TemporalDatabase db, State st) {
        List<SupportingAction> supporters = new LinkedList<>();
        DisjunctiveFluent fluent = new DisjunctiveFluent(db.stateVariable,db.GetGlobalConsumeValue(), st, groundPB);
        DisjunctiveAction dAct = pg.enablers(fluent);
        List<Pair<AbstractAction, List<Set<String>>>> options = dAct.actionsAndParams(groundPB);


        for(Pair<AbstractAction, List<Set<String>>> supporter : options) {
            AbstractAction act = supporter.value1;//Factory.getStandaloneAction(groundPB.liftedPb, supporter.value1);
            Map<LVarRef, Collection<String>> values = new HashMap<>();
            assert act.args().size() == supporter.value2.size() : "Problem: different number of parameters";

            for(int i=0 ; i<act.args().size() ; i++) {
                values.put(act.args().get(i), supporter.value2.get(i));
            }
            supporters.add(new SupportingAction(act, values, db));
        }
        return supporters;
    }
}
