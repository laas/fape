package fape.core.planning.planninggraph;

import planstack.graph.core.LabeledDigraph;
import planstack.graph.core.LabeledDigraph$;
import planstack.graph.printers.NodeEdgePrinter;

public class RelaxedPlanningGraph {

    final LabeledDigraph<PGNode, PGEdgeLabel> graph = LabeledDigraph$.MODULE$.apply();

    final GroundProblem pb;

    public RelaxedPlanningGraph(GroundProblem pb) {
        this.pb = pb;

        for(Fluent f : pb.initState.fluents) {
            graph.addVertex(f);
        }

        while(expandOnce());
    }

    public boolean applicable(GroundAction a) {
        for(Fluent precondition : a.pre) {
            if(!graph.contains(precondition)) {
                return false;
            }
        }
        return true;
    }

    public void insertAction(GroundAction a) {
        assert applicable(a);
        assert !graph.contains(a);

        graph.addVertex(a);
        for(Fluent precondition : a.pre) {
            graph.addEdge(precondition, a, new PGEdgeLabel());
        }

        for(Fluent addition : a.add) {
            if(!graph.contains(addition))
                graph.addVertex(addition);
            graph.addEdge(a, addition, new PGEdgeLabel());
        }

    }

    public boolean expandOnce() {
        int numInsertedActions = 0;
        for(GroundAction a : pb.allActions()) {
            if(graph.contains(a)) {
                continue;
            } else if(applicable(a)) {
                insertAction(a);
                numInsertedActions++;
            }
        }

        return numInsertedActions >0;
    }


}
