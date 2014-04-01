package fape.core.planning.planninggraph;

import fape.core.planning.Planner;
import fape.core.planning.search.SupportOption;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import planstack.anml.parser.ParseResult;

import java.util.List;

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

        DisjunctiveFluent fluent = new DisjunctiveFluent(db.stateVariable,db.GetGlobalConsumeValue(), st.conNet.domains, groundPB);

        return super.GetSupporters(db, st);
    }
}
