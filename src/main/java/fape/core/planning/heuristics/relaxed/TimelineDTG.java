package fape.core.planning.heuristics.relaxed;

import fape.core.planning.grounding.DisjunctiveFluent;
import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.FeasibilityReasoner;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.NoSolutionException;
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

public class TimelineDTG extends DomainTransitionGraph {

    public final Set<DTNode> acceptingNodes = new HashSet<>();
    public final Set<DTNode> entryPoints = new HashSet<>();
    public MultiLabeledDigraph<DTNode,DTEdge> transitions = GraphFactory.getMultiLabeledDigraph();
    public final Timeline tl;
    public final State st;
    public final APlanner planner;
    public final FeasibilityReasoner reas;

    Map<Integer, Map<Fluent, DTNode>> nodesByLevel = new HashMap<>();

    private boolean hasNode(int lvl, Fluent f) {
        assert nodesByLevel.containsKey(lvl);
        return nodesByLevel.get(lvl).containsKey(f);
    }

    private DTNode getNode(int lvl, Fluent f) {
        assert nodesByLevel.containsKey(lvl);
        return nodesByLevel.get(lvl).get(f);
    }

    private DTNode addNode(int lvl, Fluent f, TPRef start, TPRef end) {
        if(!nodesByLevel.containsKey(lvl))
            nodesByLevel.put(lvl, new HashMap<Fluent, DTNode>());
        if(nodesByLevel.get(lvl).containsKey(f)) {
            DTNode ret = nodesByLevel.get(lvl).get(f);
            assert ret.start == start && ret.end == end;
            return ret;
        } else {
            assert !nodesByLevel.get(lvl).containsKey(f);
            nodesByLevel.get(lvl).put(f, new DTNode(f, lvl, start, end));
            return nodesByLevel.get(lvl).get(f);
        }
    }

    public TimelineDTG(Timeline tl, State st, APlanner planner, FeasibilityReasoner reas) throws NoSolutionException {

        this.tl = tl;
        this.st = st;
        this.planner = planner;
        this.reas = reas;

        for(int i=0 ; i<tl.numChanges() ; i++) {
            ChainComponent cc = tl.getChangeNumber(i);

            LogStatement s = cc.getFirst();
            if(i == 0) { // first statement
                if(s instanceof Assignment) {
                    addNode(i, null, null, s.start());
                } else {
                    Collection<Fluent> fluents = DisjunctiveFluent.fluentsOf(s.sv(), s.startValue(), st, false); //todo why false
                    for(Fluent f : fluents) {
                        addNode(i, f, null, s.start());
                    }
                }
                for(DTNode n : nodesByLevel.get(0).values()) {
                    acceptingNodes.add(n);
                    transitions.addVertex(n);
                }
            }
            Action container = st.getActionContaining(s);

            if(container == null) { // statement was not added as part of an action
                assert s instanceof Assignment;
                assert s.endValue() instanceof InstanceRef;
                assert i == 0;
                assert nodesByLevel.get(i).size() == 1;
                DTNode from = nodesByLevel.get(i).get(null);
                Collection<Fluent> fluents = DisjunctiveFluent.fluentsOf(s.sv(), s.endValue(), st, false); //todo why false

                TPRef start = s.end();
                TPRef end = i+1 < tl.numChanges() ? tl.getChangeNumber(i+1).getConsumeTimePoint() : null;
                for(Fluent f : fluents) {
                    DTNode to = addNode(i+1, f, start, end);
                    if(!transitions.contains(to))
                        transitions.addVertex(to);
                    transitions.addEdge(from, to, new DTEdge(from, to, null, null));
                    if(i == tl.numChanges() -1) // this is the last transition of this timeline, link to the DTG
                        if(!entryPoints.contains(to)) {
                            for(DTNode n : entryPoints)
                                assert !n.hasSameFluent(to) : "Two different points with same value.";
                            entryPoints.add(to);
                        }
                }
            } else { // statement was added as part of an action
                Collection<GAction> acts = reas.getGroundActions(container, st);
                LStatementRef statementRef = container.context().getRefOfStatement(s);
                for(GAction ga : acts) {
                    GAction.GLogStatement gs = ga.statementWithRef(statementRef);
                    DTNode from;
                    DTNode to;
                    TPRef start = s.end();
                    TPRef end = i+1 < tl.numChanges() ? tl.getChangeNumber(i+1).getConsumeTimePoint() : null;
                    if(gs instanceof GAction.GTransition) {
                        Fluent fromFluent = new Fluent(gs.sv, ((GAction.GTransition) gs).from, true);
                        Fluent toFluent = new Fluent(gs.sv, ((GAction.GTransition) gs).to, true);
                        from = hasNode(i, fromFluent) ? getNode(i, fromFluent) : null;
                        to =  addNode(i+1, toFluent, start, end);
                    } else {
                        assert gs instanceof GAction.GAssignement;
                        from = hasNode(i, null) ? getNode(i, null) : null;
                        Fluent toFluent = new Fluent(gs.sv, ((GAction.GAssignement) gs).to, true);
                        to = addNode(i+1, toFluent, start, end);
                    }

                    if(from != null && transitions.contains(from)) {
                        if(!transitions.contains(to))
                            transitions.addVertex(to);
                        DTEdge label = new DTEdge(from, to, container, ga);
                        transitions.addEdge(from, to, label);

                        if(i == tl.numChanges()-1) { // this is the last transition of this timeline, link to the DTG
                            if(!entryPoints.contains(to)) {
                                for(DTNode n : entryPoints)
                                    assert !n.hasSameFluent(to) : "Two different points with same value.";
                                entryPoints.add(to);
                            }
                        }
                    }


                }
            }
        }
        if(entryPoints.isEmpty())
            // There exist no path
            throw new NoSolutionException();
    }

    @Override
    public Iterator<DTEdge> inEdges(DTNode n) {
        final Iterator<LabeledEdge<DTNode,DTEdge>> it = JavaConversions.asJavaIterator(transitions.inEdges(n).iterator());
        return new Iterator<DTEdge>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public DTEdge next() {
                return it.next().l();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }

    @Override
    public DTNode startNodeForFluent(Fluent f) {
        assert !entryPoints.isEmpty();
        for(DTNode nloc : entryPoints) {
            if(nloc.hasFluent(f))
                return nloc;
        }
        return null; // no entry point with identical value
    }

    @Override
    public boolean isAccepting(DTNode n) {
        return acceptingNodes.contains(n);
    }

    @Override
    public DTNode possibleEntryPointFrom(DTNode n) {
        assert n.containerID != id();
        assert !entryPoints.isEmpty();
        for(DTNode nloc : entryPoints) {
            if(nloc.value.equals(n.value))
                return nloc;
        }
        return null; // no entry point with identical value
    }

    @Override
    public Collection<DTNode> unifiableNodes(Fluent f, TPRef start, TPRef end, State st) {
        List<DTNode> mergeableNodes = new LinkedList<>();
        for(DTNode cur : transitions.jVertices()) {
            if(!acceptingNodes.contains(cur) && cur.canSupportValue(f, start, end, st))
                mergeableNodes.add(cur);
        }
        return mergeableNodes;
    }

    @Override
    public boolean areEdgesFree() {
        return false;
    }
}
