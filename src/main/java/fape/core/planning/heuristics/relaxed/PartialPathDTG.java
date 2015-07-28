package fape.core.planning.heuristics.relaxed;

import fape.core.planning.grounding.DisjunctiveFluent;
import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.FeasibilityReasoner;
import fape.core.planning.planninggraph.GroundDTGs;
import fape.core.planning.states.State;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.NoSolutionException;
import fape.util.Utils;
import planstack.anml.model.LStatementRef;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.statements.Assignment;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.graph.GraphFactory;
import planstack.graph.core.LabeledEdge;
import planstack.graph.core.MultiLabeledDigraph;
import scala.collection.JavaConversions;

import java.util.*;

public class PartialPathDTG extends DomainTransitionGraph {

    public DTNode acceptingNode;
    public final Set<DTNode> entryPoints = new HashSet<>();
    public MultiLabeledDigraph<DTNode,DTEdge> transitions = GraphFactory.getMultiLabeledDigraph();
    public final List<DTEdge> startPath;
    public final DomainTransitionGraph extendedDTG;
//    public final Timeline tl;
//    public final State st;
//    public final APlanner planner;
//    public final FeasibilityReasoner reas;

    Map<Integer, Map<Fluent, DTNode>> nodesByLevel = new HashMap<>();

    private DTNode convert(DTNode n, int lvl) {
        assert n.containerID != this.id;
        return new DTNode(n.value, lvl, this.id(), n.start, n.end);
    }

    private DTEdge convert(DTEdge e, int lvl) {
        DTNode orig = convert(e.from, lvl);
        DTNode dest = convert(e.to, lvl+1);
        return new DTEdge(orig, dest, e.act, e.ga);
    }

    public void addEdge(DTEdge e) {
        assert transitions.contains(e.from);
        if(!transitions.contains(e.to))
            transitions.addVertex(e.to);
    }

    public PartialPathDTG(Collection<DTEdge> initialPath, DomainTransitionGraph extendedDTG) throws NoSolutionException {
        assert initialPath.size() > 0;
        startPath = new LinkedList<>(initialPath);
        if(extendedDTG instanceof GroundDTGs.DTG)
            extendedDTG = null;
        this.extendedDTG = extendedDTG;

        int lvl=1;

        // part of the graph matching the initial path
        DTNode lastOfPath = null;
        for(DTEdge e : initialPath) {
            if(lastOfPath == null) { // first edge
                transitions.addVertex(convert(e.from, lvl));
                acceptingNode = convert(e.from, lvl);
            }
            if(e.from.containerID != e.to.containerID) {
                assert e.ga == null && e.act == null;
                assert e.from.hasSameFluent(e.to);
            } else {
                addEdge(convert(e, lvl));
                lastOfPath = e.to;
                lvl++;
            }
        }
        assert lastOfPath != null;

        // TODO : deal with transition edges from one DTG to another (remove them !)

        // part of the graph matching the end of the timeline
        if(extendedDTG != null && lastOfPath.containerID == extendedDTG.id()) {
            Set<DTNode> nodesOfCurrentLevel = new HashSet<>();
            nodesOfCurrentLevel.add(lastOfPath);
            while(nodesOfCurrentLevel.isEmpty()) {
                Set<DTNode> nodesOfNextLevel = new HashSet<>();
                for(DTNode cur : nodesOfCurrentLevel) {
                    Iterator<DTEdge> it = extendedDTG.outEdges(cur);
                    while(it.hasNext()) {
                        addEdge(convert(it.next(), lvl));
                    }
                }
                nodesOfCurrentLevel = nodesOfNextLevel;
                lvl++;
            }
            // nodes of the last level are the entry points
            entryPoints.addAll(nodesOfCurrentLevel);
        } else {
            // last node of the path is the last one. make it an entry point
            entryPoints.add(lastOfPath);
        }

        assert !startPath.isEmpty();
        assert acceptingNode != null;
        if(extendedDTG != null) {
            assert isExtensionOf(extendedDTG);
            extendedDTG.hasBeenExtended = true;
        }
    }

    public boolean isExtensionOf(DomainTransitionGraph dtg) {
        assert dtg != null;
        if(!Utils.eq(dtg, extendedDTG))
            return false;
        if(dtg instanceof TimelineDTG)
            return true;
        assert dtg instanceof PartialPathDTG;
        PartialPathDTG ppd = (PartialPathDTG) dtg;
        if(startPath.size() < ppd.startPath.size())
            return false;

        for(int i=0 ; i<ppd.startPath.size() ; i++) {
            DTEdge e1 = startPath.get(i);
            DTEdge e2 = ppd.startPath.get(i);
            if(!e1.from.hasSameFluent(e2.from))
                return false;
            if(!e1.to.hasSameFluent(e2.to))
                return false;
            if(!Utils.eq(e1.act, e2.act))
                return false;
            if(!Utils.eq(e1.ga, e2.ga))
                return false;
        }
        return true;
    }

    @Override
    public Iterator<DTEdge> inEdges(DTNode n) {
        final Iterator<LabeledEdge<DTNode,DTEdge>> it = JavaConversions.asJavaIterator(transitions.inEdges(n).iterator());
        return new Iterator<DTEdge>() {
            @Override public boolean hasNext() { return it.hasNext(); }
            @Override public DTEdge next() { return it.next().l(); }
            @Override public void remove() { throw new UnsupportedOperationException("Not supported yet."); }
        };
    }

    @Override
    public Iterator<DTEdge> outEdges(DTNode n) {
        final Iterator<LabeledEdge<DTNode,DTEdge>> it = JavaConversions.asJavaIterator(transitions.outEdges(n).iterator());
        return new Iterator<DTEdge>() {
            @Override public boolean hasNext() { return it.hasNext(); }
            @Override public DTEdge next() { return it.next().l(); }
            @Override public void remove() { throw new UnsupportedOperationException("Not supported yet."); }
        };
    }

    @Override
    public DTNode startNodeForFluent(Fluent f) {
        // nodes should always start at the DTG level
        return null; // no entry point with identical value
    }

    @Override
    public boolean isAccepting(DTNode n) {
        return acceptingNode.equals(n);
    }

    @Override
    public DTNode possibleEntryPointFrom(DTNode n) {
        assert n.containerID != id();
        assert !entryPoints.isEmpty();
        for(DTNode nloc : entryPoints) {
            if(nloc.hasSameFluent(n))
                return nloc;
        }
        return null; // no entry point with identical value
    }

    @Override
    public Collection<DTNode> unifiableNodes(Fluent f, TPRef start, TPRef end, State st) {
        List<DTNode> mergeableNodes = new LinkedList<>();
        for(DTNode cur : transitions.jVertices()) {
            if(!acceptingNode.equals(cur) && cur.canSupportValue(f, start, end, st))
                mergeableNodes.add(cur);
        }
        return mergeableNodes;
    }

    @Override
    public boolean areEdgesFree() {
        return false;
    }

    @Override
    public Collection<DTNode> getAllNodes() {
        return transitions.jVertices();
    }

    public Timeline extendedTimeline() {
        if(extendedDTG == null || extendedDTG instanceof GroundDTGs.DTG)
            return null;
        if(extendedDTG instanceof TimelineDTG)
            return ((TimelineDTG) extendedDTG).tl;

        assert extendedDTG instanceof PartialPathDTG;
        return ((PartialPathDTG) extendedDTG).extendedTimeline();
    }

    public GStateVariable getStateVariable() {
        return startPath.get(0).to.value.sv;
    }

    public List<DTEdge> additionalEdges() {
        int first = (extendedDTG != null && (extendedDTG instanceof PartialPathDTG)) ?
                ((PartialPathDTG) extendedDTG).startPath.size() : 0;

        if(extendedDTG != null && (extendedDTG instanceof PartialPathDTG)) { ///TODO: only for validation, remove when stable
            List<DTEdge> ext = ((PartialPathDTG) extendedDTG).startPath;
            for (int i = 0; i < first; i++) {
                DTEdge e1 = startPath.get(i);
                DTEdge e2 = ext.get(i);
                assert e1.from.hasSameFluent(e2.from);
                assert e1.to.hasSameFluent(e2.to);
                assert Utils.eq(e1.act, e2.act);
                assert Utils.eq(e1.ga, e2.ga);
            }

        }
        return startPath.subList(first, startPath.size());
    }
}
