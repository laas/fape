package fape.core.planning.planninggraph;

import fape.exceptions.FAPEException;
import planstack.graph.GraphFactory;
import planstack.graph.core.LabeledDigraph;

import java.util.*;

public class RelaxedPlanningGraph {

    final LabeledDigraph<PGNode, PGEdgeLabel> graph = GraphFactory.getLabeledDigraph();

    final GroundProblem pb;

    final Map<PGNode, Integer> distances = new HashMap<>();

    public RelaxedPlanningGraph(GroundProblem pb) {
        this.pb = pb;
    }

    public int buildUntil(DisjunctiveFluent df) {
        setInitState(pb.initState);
        distances.put(pb.initState, 0);

        int depth = 0;
        while(!supported(df)) {
//            System.out.println("Depth: "+depth);
            if (expandOneLevel())
                depth += 1;
            else
                return 99999;
        }
        return depth;
    }

    public boolean supported(DisjunctiveFluent df) {
        for(Fluent f : df.fluents) {
            if(graph.contains(f))
                return true;
        }
        return false;
    }

    public void build() {
        setInitState(pb.initState);
        distances.put(pb.initState, 0);

        while(expandOnce());
        updateDistances(pb.initState);
    }

    /**
     * Provides a way to exclude actions in subclasses.
     * This is typically used in restricted planning graphs
     * where enablers for a fluent must be excluded.
     * @param ga
     * @return True if the action is excluded from the current model.
     */
    public boolean isExcluded(GAction ga) {
        return false;
    }

    /**
     * Checks if all preconditions of this action are met (i.e. are in the planning graph).
     * Note that even excluded action can be applicable.
     * @param a
     * @return True if the action is applicable.
     */
    public boolean applicable(GAction a) {
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

    public void insertAction(GAction a) {
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

    public boolean expandOneLevel() {
        List<GAction> toInsert = new LinkedList<>();
        for(GAction a : pb.allActions()) {
            if(graph.contains(a)) {
                continue;
            } else if(isExcluded(a)) {
                continue;
            } else if(applicable(a)) {
                toInsert.add(a);
            }
        }

        for(GAction a : toInsert) {
            insertAction(a);
//            System.out.println("  Inserting: "+a);
        }

        return toInsert.size() >0;
    }

    public boolean expandOnce() {
        int numInsertedActions = 0;
        for(GAction a : pb.allActions()) {
            if(graph.contains(a)) {
                continue;
            } else if(isExcluded(a)) {
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

    public DisjunctiveAction enablers(DisjunctiveFluent df) {
        DisjunctiveAction supporters = new DisjunctiveAction();
        for(Fluent f : df.fluents) {
            supporters.actions.addAll(enablers(f).actions);
        }
        return supporters;
    }

    public DisjunctiveAction enablers(Fluent f) {
        List<GroundAction> actions = new LinkedList<>();

        // fluent f is not achievable, hence an empty disjunctive action
        if(!graph.contains(f))
            return new DisjunctiveAction(actions);

        for(PGNode n : graph.jParents(f)) {
            if(n instanceof GroundAction) {
                actions.add((GroundAction) n);
            } else if(n instanceof GroundState) {
                //actions.add(null);
            } else {
                throw new FAPEException("There should be no fluent parent of another fluent.");
            }
        }

        return new DisjunctiveAction(actions);
    }

    /**
     * @param f FLuent to lookup.
     * @return True if the fluent is supported by the initial state.
     */
    public boolean isInitFact(Fluent f) {
        for(PGNode n : graph.jParents(f)) {
            if(n instanceof GroundState)
                return true;
        }
        return false;
    }

    /**
     *
     * @param df A disjunctive fluent.
     * @return True if any of fluent in the disjunction is supported by the initial state
     *         or if the disjunction is empty (always true).
     */
    public boolean isInitFact(DisjunctiveFluent df) {
        if(df.fluents.isEmpty())
            return true;

        for(Fluent f : df.fluents) {
            if(isInitFact(f)) {
                return true;
            }
        }
        return false;
    }
}
