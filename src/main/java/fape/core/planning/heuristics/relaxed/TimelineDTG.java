package fape.core.planning.heuristics.relaxed;

import fape.core.planning.grounding.DisjunctiveFluent;
import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.FeasibilityReasoner;
import fape.core.planning.states.State;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
import planstack.anml.model.LStatementRef;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.statements.Assignment;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.graph.GraphFactory;
import planstack.graph.core.LabeledEdge;
import planstack.graph.core.MultiLabeledDigraph;
import scala.collection.JavaConversions;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TimelineDTG extends DomainTransitionGraph {

    public final Set<DTNode> acceptingNodes = new HashSet<>();
    public final Set<DTNode> entryPoints = new HashSet<>();
    public MultiLabeledDigraph<DTNode,DTEdge> transitions = GraphFactory.getMultiLabeledDigraph();
    public final Timeline tl;
    public final State st;
    public final APlanner planner;
    public final FeasibilityReasoner reas;

    int currentLvl = 1;

    public TimelineDTG(Timeline tl, State st, APlanner planner, FeasibilityReasoner reas) {
        this.tl = tl;
        this.st = st;
        this.planner = planner;
        this.reas = reas;

//        if(tl.mID == 0) {
//            System.out.println("BREAK");
//        }
//
        boolean isFirstChange = true;
        int pendingChanges = tl.numChanges();
        for(ChainComponent cc : tl.getComponents()) {
            if(cc.change) {
                pendingChanges--;
                LogStatement s = cc.getFirst();
                Action container = st.getActionContaining(s);
                if(container == null) {
                    assert s instanceof Assignment;
                    assert s.endValue() instanceof InstanceRef;
                    Collection<Fluent> fluents = DisjunctiveFluent.fluentsOf(s.sv(), s.endValue(), st, false); //todo why false
                    DTNode from = new DTNode(null, currentLvl);
                    if(!transitions.contains(from))
                        transitions.addVertex(from);
                    acceptingNodes.add(from);
                    for(Fluent f : fluents) {
                        DTNode to = new DTNode(f, currentLvl+1);
                        if(!transitions.contains(to))
                            transitions.addVertex(to);
                        transitions.addEdge(from, to, new DTEdge(from, to, null, null));
                        if(pendingChanges == 0) // this is the last transition of this timeline, link to the DTG
                            if(!entryPoints.contains(to)) {
                                for(DTNode n : entryPoints)
                                    assert !n.hasSameFluent(to) : "Two different points with same value.";
                                entryPoints.add(to);
                            }
                    }
                } else {
                    Collection<GAction> acts = reas.getGroundActions(container, st);
                    LStatementRef statementRef = container.context().getRefOfStatement(s);
                    for(GAction ga : acts) {
                        GAction.GLogStatement gs = ga.statementWithRef(statementRef);
                        DTNode from;
                        DTNode to;
                        if(gs instanceof GAction.GTransition) {
                            from = new DTNode(new Fluent(gs.sv, ((GAction.GTransition) gs).from, true), currentLvl);
                            to = new DTNode(new Fluent(gs.sv, ((GAction.GTransition) gs).to, true), currentLvl+1);
                        } else {
                            assert gs instanceof GAction.GAssignement;
                            from = new DTNode(null, currentLvl);
                            to = new DTNode(new Fluent(gs.sv, ((GAction.GAssignement) gs).to, true), currentLvl+1);
                        }
                        if(isFirstChange) {
                            if(!transitions.contains(from))
                                transitions.addVertex(from);
                            acceptingNodes.add(from);
                        }
                        if(transitions.contains(from)) {
                            if(!transitions.contains(to))
                                transitions.addVertex(to);
                            DTEdge label = new DTEdge(from, to, container, ga);
                            transitions.addEdge(from, to, label);

                            if(pendingChanges == 0) { // this is the last transition of this timeline, link to the DTG
                                if(!entryPoints.contains(to)) {
                                    for(DTNode n : entryPoints)
                                        assert !n.hasSameFluent(to) : "Two different points with same value.";
                                    entryPoints.add(to);
                                }
                            }
                        }


                    }
                }
                currentLvl++;
                isFirstChange = false;
            }
        }
        assert !entryPoints.isEmpty();
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
    public boolean areEdgesFree() {
        return false;
    }
}
