package fape.core.planning.planninggraph;

import planstack.graph.GraphFactory;
import planstack.graph.core.LabeledDigraph;

import java.util.*;

public class RelaxedPlanningGraph {

    final LabeledDigraph<PGNode, PGEdgeLabel> graph = GraphFactory.getLabeledDigraph();

    final GroundProblem pb;

    final Map<PGNode, Integer> distances = new HashMap<>();

    final List<GAction> baseActions;

    public RelaxedPlanningGraph(GroundProblem pb) {
        this.pb = pb;
        this.baseActions = new LinkedList<>(pb.allActions());
    }

    public RelaxedPlanningGraph(GroundProblem pb, Collection<GAction> acts) {
        this.pb = pb;
        this.baseActions = new LinkedList<>(acts);
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
//        updateDistances(pb.initState);
    }

    private int level(PGNode n) {
        assert graph.contains(n);
        assert distances.containsKey(n);
        return distances.get(n);
    }

    public Set<GAction> buildRelaxedPlan(DisjunctiveFluent disjunctiveOpenGoal, Collection<GAction> initialRelaxedPlan) {
        Set<GAction> relaxedPlan = new HashSet<>();
        Queue<Fluent> openGoals = new LinkedList<>();
        Set<Fluent> done = new HashSet<>();

        for(GAction ga : initialRelaxedPlan) {
            done.addAll(ga.add);
            relaxedPlan.add(ga);
        }

        Fluent bestFluent = null;
        int bestLevel = Integer.MAX_VALUE;
        for(Fluent f : disjunctiveOpenGoal.fluents) {
            if(graph.contains(f) && distances.get(f) < bestLevel) {
                bestFluent = f;
                bestLevel = distances.get(f);
            }
        }
        if(bestFluent == null) {
            // infeasible
            return null;
        }

        openGoals.add(bestFluent);
        while(!openGoals.isEmpty()) {
            Fluent og = openGoals.remove();
            if(level(og) == 0 || done.contains(og)) {
                done.add(og);
            } else {
                GAction bestEnabler = null;
                int bestEnablerLevel = Integer.MAX_VALUE;
                for(PGNode n : graph.jParents(og)) {
                    assert n instanceof GAction;
                    if(level(n) < bestEnablerLevel) {
                        bestEnabler = (GAction) n;
                        bestEnablerLevel = level(n);
                    }
                }

                assert bestEnabler != null;
                assert !relaxedPlan.contains(bestEnabler);
                relaxedPlan.add(bestEnabler);
                openGoals.addAll(bestEnabler.pre);
                done.addAll(bestEnabler.add);
            }
        }
        return relaxedPlan;
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
        assert a != null;
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
            distances.put(f, 0);
            graph.addEdge(s, f, new PGEdgeLabel());
        }
    }

    public void insertAction(GAction a) {
        assert applicable(a);
        assert !graph.contains(a);

        graph.addVertex(a);
        int maxPreLevel = -1;
        for(Fluent precondition : a.pre) {
            graph.addEdge(precondition, a, new PGEdgeLabel());
            maxPreLevel = maxPreLevel > distances.get(precondition) ? maxPreLevel : distances.get(precondition);
        }
        distances.put(a, maxPreLevel +1);

        for(Fluent addition : a.add) {
            if(!graph.contains(addition)) {
                graph.addVertex(addition);
                distances.put(addition, distances.get(a)+1);
            }
            graph.addEdge(a, addition, new PGEdgeLabel());
        }
    }

    public boolean expandOneLevel() {
        List<GAction> toInsert = new LinkedList<>();
        for(GAction a : baseActions) {
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
        }

        return toInsert.size() >0;
    }

    public boolean expandOnce() {
        int numInsertedActions = 0;
        for(GAction a : baseActions) {
            assert a != null;
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

    public List<GAction> getAllActions() {
        LinkedList<GAction> actions = new LinkedList<>();
        for(PGNode n : graph.jVertices()) {
            if(n instanceof GAction) {
                actions.add((GAction) n);
            }
        }
        return actions;
    }
}
