package fape.core.planning.planninggraph;

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
