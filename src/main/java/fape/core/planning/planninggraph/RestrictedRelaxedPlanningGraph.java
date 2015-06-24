package fape.core.planning.planninggraph;

import fape.core.planning.grounding.DisjunctiveFluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GroundProblem;
import fape.util.Utils;

public class RestrictedRelaxedPlanningGraph extends RelaxedPlanningGraph {

    final DisjunctiveFluent df;

    public RestrictedRelaxedPlanningGraph(GroundProblem pb, DisjunctiveFluent df) {
        super(pb);
        this.df = df;
    }

    @Override
    public boolean isExcluded(GAction ga) {
        return Utils.nonEmptyIntersection(ga.add, df.fluents);
    }
}
