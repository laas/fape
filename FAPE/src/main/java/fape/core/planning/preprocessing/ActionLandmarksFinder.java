package fape.core.planning.preprocessing;

import fape.core.planning.planninggraph.*;
import fape.util.Utils;
import planstack.anml.model.Function;
import planstack.anml.model.abs.AbstractAction;
import planstack.graph.GraphFactory;
import planstack.graph.core.DirectedGraph;
import planstack.graph.core.Edge;
import planstack.graph.core.SimpleUnlabeledDigraph;
import planstack.graph.printers.NodeEdgePrinter;

import java.util.*;

public class ActionLandmarksFinder {

    final GroundProblem pb;
    final RelaxedPlanningGraph rpg;

    final Queue<DisjunctiveFluent> toProcess = new LinkedList<>();
    public List<DisjunctiveAction> landmarks = new LinkedList<>();

    public SimpleUnlabeledDigraph<Landmark> landmarkGraph = GraphFactory.getSimpleUnlabeledDigraph();

    public ActionLandmarksFinder(GroundProblem pb) {
        this.pb = pb;
        this.rpg = new RelaxedPlanningGraph(pb);
        rpg.build();
    }

    public void addGoal(DisjunctiveFluent goal) {
        landmarkGraph.addVertex(goal);
        toProcess.add(goal);
    }

    /**
     * Returns true if a landmark encoding da is already known
     * @param da
     * @return
     */
    public boolean hasLandmark(DisjunctiveAction da) {
        for(DisjunctiveAction o : landmarks) {
            if(da.contains(o)) {
                return true;
            }
        }
        return false;
    }

    public void getLandmarks() {
        for(int qsdkjsdf=0 ; /*qsdkjsdf<2000 &&*/ !toProcess.isEmpty() ; qsdkjsdf++) {

            DisjunctiveFluent fluent = toProcess.remove();
            DisjunctiveAction da = getActionLandmarks(fluent);

            AbstractAction aa = null;
            boolean invalid = false;
            for(GroundAction a : da.actions) {
                if(aa == null) {
                    aa = a.act.abs();
                } else if(aa != a.act.abs()) {
                    //invalid = true;
                }
            }
            if(da.actions.isEmpty())
                invalid = true;
            if(!invalid && !hasLandmark(da)) {
                landmarkGraph.addVertex(da);
                landmarkGraph.addEdge(da, fluent);

                for(DisjunctiveFluent f : preconditions(da)) {
                    landmarkGraph.addVertex(f);
                    landmarkGraph.addEdge(f, da);
                    toProcess.add(f);
                }
                for(GroundAction a : da.actions) {
                    System.out.println(a);
                }
                System.out.println();

                landmarks.add(da);
            }
        }
    }

    boolean removeRedundantLandmarksOnce() {
        boolean modified = false;

        List<DisjunctiveAction>  notContaining = new LinkedList<>();
        for(int i=0 ; i<landmarks.size() ; i++) {
            DisjunctiveAction landmark = landmarks.get(i);
            boolean isContainer = false;
            for(int j=0 ; j<landmarks.size() ; j++) {
                DisjunctiveAction oLandmark = landmarks.get(j);
                if(i == j) {
                    continue;
                } else if(landmark.contains(oLandmark)) {
                    if(oLandmark.contains(landmark) && i<j) {
                        // identical
                    } else {
                        isContainer = true;
                        modified = true;
                        break;
                    }
                }
            }
            if(!isContainer)
                notContaining.add(landmark);
        }

        List<DisjunctiveAction>  notRedundant = new LinkedList<>();
        for(int i=0 ; i<notContaining.size() ; i++) {
            boolean intersects = false;
            for(int j=i+1 ; j<notContaining.size() ; j++) {
                DisjunctiveAction la = notContaining.get(i);
                DisjunctiveAction lb = notContaining.get(j);
                if(Utils.nonEmptyIntersection(la.actions, lb.actions)) {
                    intersects = true;
                    DisjunctiveAction unionLandmark = new DisjunctiveAction(la.actions);
                    unionLandmark.actions.addAll(lb.actions);
                    notRedundant.add(unionLandmark);
                    break;
                }
            }
            if(!intersects) {
                notRedundant.add(notContaining.get(i));
            }
        }
        landmarks = notRedundant;
        return modified;
    }

    public void removeRedundantLandmarks() {
        while(removeRedundantLandmarksOnce());
    }

    public DisjunctiveAction getActionLandmarks(DisjunctiveFluent df) {
        DisjunctiveAction da = new DisjunctiveAction();

        RestrictedRelaxedPlanningGraph rrpg = new RestrictedRelaxedPlanningGraph(pb, df);
        rrpg.build();

        if(rpg.isInitFact(df))
            return da;

        for(GroundAction ga : rpg.enablers(df).actions) {
            if(rrpg.applicable(ga)) {
                da.actions.add(ga);
            }
        }

        return da;
    }

    public Set<DisjunctiveFluent> preconditions(DisjunctiveAction da) {
        List<List<Fluent>> allPreconditions = new LinkedList<>();
        for(GroundAction a : da.actions) {
            allPreconditions.add(a.pre);
        }
        Set<DisjunctiveFluent> disjunctiveFluents = new HashSet<>();
        for(List<Fluent> disfluent : PGUtils.allCombinations(allPreconditions)) {
            boolean onSameFunction = true;
            if(disfluent.isEmpty()) {
                break;
            }
            Function f = disfluent.get(0).f;
            for(Fluent fluent : disfluent) {
                if(f != fluent.f) {
                    onSameFunction = false;
                    break;
                }
            }
            //if(onSameFunction)
                disjunctiveFluents.add(new DisjunctiveFluent(disfluent));
        }

        return disjunctiveFluents;
    }

    public String report() {
        StringBuilder builder = new StringBuilder();
        for(DisjunctiveAction landmark : landmarks) {
            for(GroundAction a : landmark.actions) {
                builder.append(a);
                builder.append('\n');
            }
            builder.append('\n');
        }
        return builder.toString();
    }


    class LandmarkGraphPrinter extends NodeEdgePrinter<Landmark, Object, Edge<Landmark>> {
        @Override
        public boolean excludeNode(Landmark l) {
            if(l instanceof DisjunctiveFluent)
                return landmarkGraph.inDegree(l) == 0;
            else
                return false;
        }
    }

    public void exportToDot(String file) {
        landmarkGraph.exportToDotFile(file, new LandmarkGraphPrinter());
    }
}
