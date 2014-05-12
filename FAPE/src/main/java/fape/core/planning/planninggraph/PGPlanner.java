package fape.core.planning.planninggraph;

import fape.core.planning.planner.APlanner;
import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.preprocessing.ActionSupporters;
import fape.core.planning.search.*;
import fape.core.planning.preprocessing.AbstractionHierarchy;
import fape.core.planning.search.strategies.flaws.FlawCompFactory;
import fape.core.planning.search.strategies.plans.BreadthFirst;
import fape.core.planning.search.strategies.plans.DepthFirst;
import fape.core.planning.search.strategies.plans.PlanCompFactory;
import fape.core.planning.search.strategies.plans.StateComparator;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.util.Pair;
import fape.util.TimeAmount;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.parser.ParseResult;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Planner that uses a relaxed planning graph for domain analysis (to select action resolvers).
 *
 * It does not cover the whole set of ANML problems for instance problems/concurrent-actions.anml
 * would fail with this planner.
 * It also produces a ground problem that might impractical in some domain with an important number of objects.
 */
public class PGPlanner extends APlanner {

    GroundProblem groundPB = null;
    RelaxedPlanningGraph pg = null;
    AbstractionHierarchy hierarchy = null;



    @Override
    public boolean ForceFact(ParseResult anml) {
        super.ForceFact(anml);

        if(GroundProblem.sizeEvaluation(pb) > 1000000)
            return false;

        groundPB = new GroundProblem(this.pb);
        pg = new RelaxedPlanningGraph(groundPB);
        pg.build();
        hierarchy = new AbstractionHierarchy(this.pb);

        return true;
    }



    @Override
    public String shortName() {
        return "rpg";
    }

    @Override
    public ActionSupporterFinder getActionSupporterFinder() {
        return new ActionSupporters(pb);
    }

    @Override
    public State search(TimeAmount forhowLong) {
        return aStar(forhowLong);
    }

    @Override
    public List<SupportOption> GetSupporters(TemporalDatabase db, State st) {
        // use the inherited GtSupporter method
        List<SupportOption> supportOptions = super.GetSupporters(db, st);
        if(false)
            return supportOptions;

        // remove all supporting actions, they will be replaced by out own ActionWithBindings
        List toRemove = new LinkedList();
        for(SupportOption o : supportOptions) {
            if(o.supportingAction != null) {
                toRemove.add(o);
            }
        }
        supportOptions.removeAll(toRemove);

        DisjunctiveFluent fluent = new DisjunctiveFluent(db.stateVariable,db.GetGlobalConsumeValue(), st.conNet, groundPB);
        DisjunctiveAction dAct = pg.enablers(fluent);
        List<Pair<AbstractAction, List<Set<String>>>> options = dAct.actionsAndParams(groundPB);


        for(Pair<AbstractAction, List<Set<String>>> supporter : options) {
            ActionWithBindings opt = new ActionWithBindings();
            opt.act = supporter.value1;//Factory.getStandaloneAction(groundPB.liftedPb, supporter.value1);

            assert opt.act.args().size() == supporter.value2.size() : "Problem: different number of parameters";

            for(int i=0 ; i<opt.act.args().size() ; i++) {
                opt.values.put(opt.act.args().get(i), supporter.value2.get(i));
            }
            SupportOption supportOption = new SupportOption();
            supportOption.actionWithBindings = opt;
            supportOptions.add(supportOption);
        }


        return supportOptions;
        //return super.GetSupporters(db, st);
    }
}
