package fape.core.planning.planninggraph;

import fape.exceptions.FAPEException;
import planstack.graph.core.LabeledDigraph;
import planstack.graph.core.LabeledDigraph$;
import planstack.graph.printers.NodeEdgePrinter;

import java.util.*;

public class RelaxedPlanningGraph {

    final LabeledDigraph<PGNode, PGEdgeLabel> graph = LabeledDigraph$.MODULE$.apply();

    final GroundProblem pb;

    final Map<PGNode, Integer> distances = new HashMap<>();

    public RelaxedPlanningGraph(GroundProblem pb) {
        this.pb = pb;

        setInitState(pb.initState);
        distances.put(pb.initState, 0);

        while(expandOnce());
        updateDistances(pb.initState);
    }

    public boolean applicable(GroundAction a) {
        for(Fluent precondition : a.pre) {
            if(!graph.contains(precondition)) {
                return false;
            }
        }
        return true;
    }

    public void setInitState(GroundState s) {
        graph.addVertex(s);
        for(Fluent f : s.fluents) {
            graph.addVertex(f);
            graph.addEdge(s, f, new PGEdgeLabel());
        }
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

    int distance(PGNode node) {
        if(distances.containsKey(node)) {
            return distances.get(node);
        } else {
            return Integer.MAX_VALUE;
        }
    }

    int maxDist(Collection<PGNode> nodes) {
        if(nodes.isEmpty()) {
            return Integer.MAX_VALUE;
        } else {
            int max = Integer.MIN_VALUE;
            for(PGNode n : nodes) {
                max = Math.max(max, distance(n));
            }
            return max;
        }
    }

    int minDist(Collection<PGNode> nodes) {
        if(nodes.isEmpty()) {
            return Integer.MIN_VALUE;
        } else {
            int min = Integer.MAX_VALUE;
            for(PGNode n : nodes) {
                min = Math.min(min, distance(n));
            }
            return min;
        }
    }

    void updateDistances(PGNode source) {
        assert distances.containsKey(source);
        int d = distances.get(source);

        for(PGNode child : graph.jChildren(source)) {
            if(child instanceof GroundAction) {
                int dChild = maxDist(graph.jParents(child));
                if(dChild != Integer.MAX_VALUE) {
                    distances.put(child, dChild+1);
                    updateDistances(child);
                }
            } else if(child instanceof Fluent) {
                int dChild = minDist(graph.jParents(child));
                assert dChild != Integer.MAX_VALUE;
                if(distance(child) > dChild) {
                    distances.put(child, dChild);
                    updateDistances(child);
                }
            }
        }
    }

    public DisjunctiveAction enablers(Fluent f) {
        List<GroundAction> actions = new LinkedList<>();
        for(PGNode n : graph.jParents(f)) {
            if(n instanceof GroundAction) {
                actions.add((GroundAction) n);
            } else if(n instanceof GroundState) {
                actions.add(null);
            } else {
                throw new FAPEException("There should be no fluent parent of another fluent.");
            }
        }

        return new DisjunctiveAction(actions);
    }
}
