package fape.core.planning.planninggraph;

import fape.core.planning.Planner;
import fape.core.planning.search.ActionWithBindings;
import fape.core.planning.search.SupportOption;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.util.Pair;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Factory;
import planstack.anml.parser.ParseResult;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class PGPlanner extends Planner {

    GroundProblem groundPB = null;
    RelaxedPlanningGraph pg = null;


    @Override
    public void ForceFact(ParseResult anml) {
        super.ForceFact(anml);

        groundPB = new GroundProblem(this.pb);
        pg = new RelaxedPlanningGraph(groundPB);
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

        DisjunctiveFluent fluent = new DisjunctiveFluent(db.stateVariable,db.GetGlobalConsumeValue(), st.conNet.domains, groundPB);
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
