package fape.core.planning.heuristics.relaxed;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.planninggraph.GroundDTGs;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.NoSolutionException;
import fape.util.Utils;
import planstack.anml.model.concrete.TPRef;
import planstack.graph.GraphFactory;
import planstack.graph.core.LabeledEdge;
import planstack.graph.core.MultiLabeledDigraph;
import scala.collection.JavaConversions;

import java.util.*;
import java.util.stream.Collectors;

public class PartialPathDTG extends DomainTransitionGraph {

    public DTNode acceptingNode;
    public final Set<DTNode> entryPoints = new HashSet<>();
    public MultiLabeledDigraph<DTNode,DTEdge> transitions = GraphFactory.getMultiLabeledDigraph();

    /** A compulsory path that constitute the first levels of this DTG. */
    public final List<DTEdge> startPath = new LinkedList<>();

    /** The DTG that this partial path dtg was extracted from. The start path is in fact a path in this DTG */
    public final DomainTransitionGraph baseDTG;

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
        assert e.from.containerID == id && e.to.containerID == id;
        if(!transitions.contains(e.to))
            transitions.addVertex(e.to);
        transitions.addEdge(e.from, e.to, e);
    }

    public PartialPathDTG(List<DTEdge> initialPath, DomainTransitionGraph baseDTG) throws NoSolutionException {
        assert initialPath.size() > 0;

        if(baseDTG instanceof GroundDTGs.DTG)
            baseDTG = null;
        this.baseDTG = baseDTG;

        int lvl=1;

        // part of the graph matching the initial path
        DTNode lastOfPath = null;
        for(int i=0 ; i<initialPath.size() ; i++) {
            DTEdge e = initialPath.get(i);
            if(lastOfPath == null) { // first edge
                transitions.addVertex(convert(e.from, lvl));
                acceptingNode = convert(e.from, lvl);
            }
            if(e.from.containerID != e.to.containerID) {
                assert e.ga == null && e.act == null;
                assert e.from.hasSameFluent(e.to);
                assert baseDTG == null || e.from.containerID == baseDTG.id();
            } else {
                DTEdge newEdge = convert(e, lvl);
                addEdge(newEdge);
                startPath.add(newEdge);
                lastOfPath = e.to;
                lvl++;
            }
        }
        assert lastOfPath != null;

        // part of the graph matching the end of the timeline
        if(baseDTG != null && lastOfPath.containerID == baseDTG.id()) {
            Set<DTNode> nodesOfCurrentLevel = new HashSet<>();
            Set<DTNode> nodesOfNextLevel = new HashSet<>();
            nodesOfNextLevel.add(lastOfPath);
            do {
                nodesOfCurrentLevel = nodesOfNextLevel;
                nodesOfNextLevel = new HashSet<>();
                for(DTNode cur : nodesOfCurrentLevel) {
                    Iterator<DTEdge> it = baseDTG.outEdges(cur);
                    while(it.hasNext()) {
                        DTEdge e = it.next();
                        nodesOfNextLevel.add(e.to);
                        addEdge(convert(e, lvl));
                    }
                }
                lvl++;
            } while(!nodesOfNextLevel.isEmpty());
            assert !nodesOfCurrentLevel.isEmpty();
            // nodes of the last level are the entry points of the DTG
            final int l = lvl-1;
            entryPoints.addAll(nodesOfCurrentLevel.stream().map(n -> convert(n, l)).collect(Collectors.toList()));
        } else {
            // last node of the path is the last one. make it an entry point
            entryPoints.add(convert(lastOfPath, lvl));
        }

        assert !startPath.isEmpty();
        assert acceptingNode != null;
        for(DTNode n : entryPoints)
            assert n.containerID == id();
        if(baseDTG != null) {
            if(isExtensionOf(baseDTG)) {
                baseDTG.hasBeenExtended = true;
            } else {
                assert baseDTG instanceof PartialPathDTG && isSubPathOf((PartialPathDTG) baseDTG);
                this.hasBeenExtended = true;
            }

        }
    }

    public void extendWith(TimelineDTG dtg) {
        int lvl = entryPoints.iterator().next().lvl;
        Set<DTNode> next = new HashSet<>();
        Set<DTNode> current = new HashSet<>();
        for(DTNode n : entryPoints) {
            for(DTNode n2 : dtg.acceptingNodes) {
                if(n.hasSameFluent(n2)) {
                    next.add(n2);
                }
            }
        }

        while(!next.isEmpty()) {
            current = next;
            next = new HashSet<>();
            for(DTNode n2 : current) {
                Iterator<DTEdge> it = dtg.outEdges(n2);
                while (it.hasNext()) {
                    DTEdge e = it.next();
                    addEdge(convert(e, lvl));
                    next.add(e.to);
                }
            }
            lvl++;
        }
        assert !current.isEmpty();
        entryPoints.clear();
        for(DTNode n : current) {
            DTNode convertedEntry = convert(n, lvl-1);
            assert !outEdges(convertedEntry).hasNext();
            entryPoints.add(convertedEntry);
        }
    }

    /**
     * Returns true if the start path is a subpath the  one given a parameter.
     */
    public boolean isSubPathOf(PartialPathDTG ppd) {
        if(startPath.size() > ppd.startPath.size())
            return false;

        for(int i=0 ; i<startPath.size() ; i++) {
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

    public boolean isExtensionOf(DomainTransitionGraph dtg) {
        assert dtg != null;
        if(!Utils.eq(dtg, baseDTG))
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
        assert transitions.contains(n);
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
            assert nloc.containerID == id();
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
    public boolean isFree(DTEdge e) {
        return startPath.contains(e);
    }

    @Override
    public Collection<DTNode> getAllNodes() {
        return transitions.jVertices();
    }

    /** Returns the timeline on which this DTG is based. null if there is no such timelines. */
    public Timeline extendedTimeline() {
        if(baseDTG == null || baseDTG instanceof GroundDTGs.DTG)
            return null;
        if(baseDTG instanceof TimelineDTG)
            return ((TimelineDTG) baseDTG).tl;

        assert baseDTG instanceof PartialPathDTG;
        return ((PartialPathDTG) baseDTG).extendedTimeline();
    }

    /** Returns the state variable on which this DTG applies */
    public GStateVariable getStateVariable() {
        return startPath.get(0).to.value.sv;
    }

    public List<DTEdge> additionalEdges() {
        if(baseDTG != null && (baseDTG instanceof PartialPathDTG) && isSubPathOf((PartialPathDTG) baseDTG))
            return new LinkedList<>();

        int first = (baseDTG != null && (baseDTG instanceof PartialPathDTG)) ?
                ((PartialPathDTG) baseDTG).startPath.size() : 0;

        if(baseDTG != null && (baseDTG instanceof PartialPathDTG)) { //TODO: only for validation, remove when stable
            List<DTEdge> ext = ((PartialPathDTG) baseDTG).startPath;
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

    @Override public String toString() {
        return ""+id();
    }
}
