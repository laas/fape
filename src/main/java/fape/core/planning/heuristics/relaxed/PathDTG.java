package fape.core.planning.heuristics.relaxed;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.timelines.Timeline;
import fape.util.Pair;
import planstack.anml.model.concrete.Action;

import java.util.*;

public class PathDTG extends DomainTransitionGraph {

    Map<DTNode, DTEdge> revEdges = new HashMap<>();
    Map<DTNode, DTEdge> edges = new HashMap<>();
    DTNode startNode = null;
    DTNode endNode = null;
    final Timeline supporter;

    /** if not null, this is an path in a DTG that was extended to get this path */
    public final PathDTG extendedSolution;

    int size = 0;

    public PathDTG(Timeline tl, PathDTG extendedPath) {
        supporter = tl;
        this.extendedSolution = extendedPath;
    }

    public GStateVariable getStateVariable() {
        return endNode.value.sv;
    }

    private boolean eq(Object o1, Object o2) {
        if(o1 == null && o2 == null)
            return true;
        else if(o1 == null && o2 != null)
            return false;
        else if(o2 == null && o1 != null)
            return false;
        else
            return o1.equals(o2);
    }

    public boolean contains(PathDTG path) {
        if(size < path.size)
            return false;
        if (!getStateVariable().equals(path.getStateVariable()))
            return false;
        if(!eq(supporter, path.supporter))
            return false;
        if(supporter != null && !supporter.equals(path.supporter))
            return false;

        DTNode myCur = startNode;
        DTNode oCur = path.startNode;
        while(oCur != null) {
            if(!oCur.equals(myCur))
                return false;
            if(path.edges.containsKey(oCur)) {
                DTEdge myEdge = edges.get(myCur);
                DTEdge oEdge = path.edges.get(oCur);
                assert myEdge.act == oEdge.act;
                if (!eq(myEdge.ga, oEdge.ga))
                    return false;
                myCur = myEdge.to;
                oCur = oEdge.to;
            } else {
                // reached the end
                oCur = null;
            }
        }
        return true;
    }

    public List<Pair<Action, GAction>> actionBindings() {
        List<Pair<Action,GAction>> actionBindings = new LinkedList<>();
        DTNode cur = startNode;
        while(edges.containsKey(cur)) {
            DTEdge e = edges.get(cur);
            if(e.act != null) {
                assert e.ga != null;
                actionBindings.add(new Pair<Action, GAction>(e.act, e.ga));
            }
            cur = e.to;
        }
        return actionBindings;
    }

    public List<GAction> allGroundActions() {
        List<GAction> groundActions = new LinkedList<>();
        DTNode cur = startNode;
        for(DTEdge e : edges.values())
            if(e.ga != null)
                groundActions.add(e.ga);

        return groundActions;
    }

    public String toString() {
        assert endNode != null;
        DTNode cur = endNode;
        String out = "";
        while(!cur.equals(startNode)) {
            out = cur + " > " + out;
            cur = revEdges.get(cur).from;
        }
        out = startNode+" > "+out;
        return out;
    }

    public void addNextEdge(Fluent from, Fluent to, GAction gAction, Action liftedAct) {
        DTNode src = new DTNode(from, size);
        DTNode dst = new DTNode(to, size+1);

        assert !revEdges.containsKey(dst);
        if(size == 0) {
            assert revEdges.isEmpty();
            assert edges.isEmpty();
            assert startNode == null;
            startNode = src;
        } else {
            assert revEdges.containsKey(src) : "Edges must be added in topological order.";
        }
        revEdges.put(dst, new DTEdge(src, dst, liftedAct, gAction));
        edges.put(src, new DTEdge(src, dst, liftedAct, gAction));
        endNode = dst;
        size++;
        assert edges.size() == revEdges.size();
    }

    @Override
    public Iterator<DTEdge> inEdges(final DTNode n) {
        return new Iterator<DTEdge>() {
            boolean isFirst = true;

            @Override public boolean hasNext() {
                return isFirst;
            }

            @Override public DTEdge next() {
                if(isFirst) {
                    isFirst = false;
                    return revEdges.get(n);
                } else {
                    return null;
                }
            }

            @Override public void remove() { throw new UnsupportedOperationException("Not supported yet."); }
        };
    }

    @Override
    public DTNode startNodeForFluent(Fluent f) {
        assert endNode != null;
        if(endNode.hasFluent(f))
            return endNode;
        else
            return null;
    }

    @Override
    public boolean isAccepting(DTNode n) {
        return n.equals(startNode);
    }

    @Override
    public DTNode possibleEntryPointFrom(DTNode n) {
        if(n.hasSameFluent(endNode))
            return endNode;
        else
            return null;
    }

    @Override
    public boolean areEdgesFree() {
        return true;
    }
}
