//package fape.core.planning.heuristics.relaxed;
//
//import fape.core.planning.grounding.DisjunctiveFluent;
//import fape.core.planning.grounding.Fluent;
//import fape.core.planning.grounding.GAction;
//import fape.core.planning.planninggraph.FeasibilityReasoner;
//import fape.core.planning.planninggraph.GroundDTGs;
//import fape.core.planning.states.State;
//import fape.core.planning.timelines.ChainComponent;
//import fape.core.planning.timelines.Timeline;
//import fape.exceptions.FAPEException;
//import fape.util.Pair;
//import planstack.anml.model.LStatementRef;
//import planstack.anml.model.concrete.Action;
//import planstack.anml.model.concrete.InstanceRef;
//import planstack.anml.model.concrete.statements.Assignment;
//import planstack.anml.model.concrete.statements.LogStatement;
//import planstack.graph.GraphFactory;
//import planstack.graph.core.LabeledEdge;
//import planstack.graph.core.MultiLabeledDigraph;
//import scala.collection.JavaConversions;
//
//import java.util.*;
//
//public class DomainTransitions {
//
//    public class Node {
//        public final int lvl;
//        public final Fluent value;
//        public Node(Fluent value, int lvl) {
//            this.value = value;
//            this.lvl = lvl;
//        }
//        @Override public int hashCode() {
//            return (value != null ? value.hashCode() : 0) + lvl;
//        }
//        @Override public boolean equals(Object o) {
//            if(o instanceof Node) {
//                if(((Node) o).lvl == lvl) {
//                    if(value != null && ((Node) o).value != null)
//                        return ((Node) o).value.equals(value);
//                    else
//                        return value == ((Node) o).value;
//                }
//            }
//            return false;
//        }
//
//        @Override public String toString() {
//            String base = acceptingNodes.contains(this) ? "(acc) " : "";
//            base = base + (startingNodes.contains(this) ? "(start)" : "");
//            if(value != null) return base+value.toString()+" "+lvl;
//            else return base+"null "+lvl;
//        }
//    }
//    public class Edge {
//        public final Action act;
//        public final GAction ga;
//        public Edge(Action act, GAction ga) {
//            this.act = act;
//            this.ga = ga;
//        }
//        @Override public String toString() {
//            if(act != null) return "ag";
//            if(ga != null) return "g"; //return ga.toString();
//            else return "";
//        }
//    }
//
//    public final FeasibilityReasoner reas;
//    public final State st;
//    public final Set<Node> acceptingNodes = new HashSet<>();
//    public final Set<Node> startingNodes = new HashSet<>();
//    public MultiLabeledDigraph<Node,Edge> transitions = GraphFactory.getMultiLabeledDigraph();
//
//    public DomainTransitions(FeasibilityReasoner reas, State st) {
//        this.reas = reas;
//        this.st = st;
//    }
//
//    boolean timelineAdded = false;
//
////    public void addStartNodes(Collection<Fluent> starts) {
////        for(Fluent f : starts) {
////            startingNodes.add(baseNode(f));
////        }
////    }
//
////    public void addDTG(GroundDTGs.DTG dtg) {
////        assert !timelineAdded;
////
////        for(GroundDTGs.Edge e : dtg.edges()) {
////
////            Node s = baseNode(e.from);
////            Node d = baseNode(e.to);
////            if(!transitions.contains(s))
////                transitions.addVertex(s);
////            if(!transitions.contains(d))
////                transitions.addVertex(d);
////            Edge label = new Edge(null, e.act);
////            transitions.addEdge(s, d, label);
////            if(e.from == null) // unconditional transition
////                acceptingNodes.add(s); // s is an accepting node
////        }
////    }
//
//    int currentLvl = 1;
//
//    /*
//    public Node baseNode(Fluent f) {
//        return new Node(f, 0);
//    }
//
//    public void addTimeline(Timeline tl) {
//        currentLvl += 10;
//        timelineAdded = true;
//        boolean isFirstChange = true;
//        int pendingChanges = tl.numChanges();
//        for(ChainComponent cc : tl.getComponents()) {
//            if(cc.change) {
//                pendingChanges--;
//                LogStatement s = cc.getFirst();
//                Action container = st.getActionContaining(s);
//                if(container == null) {
//                    assert s instanceof Assignment;
//                    assert s.endValue() instanceof InstanceRef;
//                    Collection<Fluent> fluents = DisjunctiveFluent.fluentsOf(s.sv(), s.endValue(), st, false); //todo why false
//                    Node from = new Node(null, currentLvl);
//                    if(!transitions.contains(from))
//                        transitions.addVertex(from);
//                    acceptingNodes.add(from);
//                    for(Fluent f : fluents) {
//                        Node to = new Node(f, currentLvl+1);
//                        if(!transitions.contains(to))
//                            transitions.addVertex(to);
//                        transitions.addEdge(from, to, new Edge(null, null));
//                        if(pendingChanges == 0) // this is the last transition of this timeline, link to the DTG
//                            if(transitions.contains(baseNode(to.value))) {
//                                transitions.addEdge(to, baseNode(to.value), new Edge(null, null));
//                            }
//                    }
//                } else {
//                    Collection<GAction> acts = reas.getGroundActions(container, st);
//                    LStatementRef statementRef = container.context().getRefOfStatement(s);
//                    for(GAction ga : acts) {
//                        GAction.GLogStatement gs = ga.statementWithRef(statementRef);
//                        Node from;
//                        Node to;
//                        if(gs instanceof GAction.GTransition) {
//                            from = new Node(new Fluent(gs.sv, ((GAction.GTransition) gs).from, true), currentLvl);
//                            to = new Node(new Fluent(gs.sv, ((GAction.GTransition) gs).to, true), currentLvl+1);
//                        } else {
//                            assert gs instanceof GAction.GAssignement;
//                            from = new Node(null, currentLvl);
//                            to = new Node(new Fluent(gs.sv, ((GAction.GAssignement) gs).to, true), currentLvl+1);
//                        }
//                        if(isFirstChange) {
//                            if(!transitions.contains(from))
//                                transitions.addVertex(from);
//                            acceptingNodes.add(from);
//                        }
//                        if(transitions.contains(from)) {
//                            if(!transitions.contains(to))
//                                transitions.addVertex(to);
//                            Edge label = new Edge(container, ga);
//                            transitions.addEdge(from, to, label);
//
//                            if(pendingChanges == 0) { // this is the last transition of this timeline, link to the DTG
//                                Node b = baseNode(to.value);
//                                if(to.value.sv.f.name().contains("Rover.at"))
//                                    assert transitions.contains(b);
//                                if (transitions.contains(baseNode(to.value))) {
//                                    transitions.addEdge(to, baseNode(to.value), new Edge(null, null));
//                                }
//                            }
//                        }
//
//
//                    }
//                }
//                currentLvl++;
//                isFirstChange = false;
//            }
//        }
//    }*/
//
//    private class NodeCost implements Comparable<NodeCost> {
//        public final DomainTransitionGraph.DTNode n;
//        public final int externalCost;
//        public final int accumulatedCost;
//        public final NodeCost pred;
//        public final DomainTransitionGraph.DTEdge e;
//
//        public NodeCost(DomainTransitionGraph.DTNode n, int extCost, int accCost, NodeCost pred, DomainTransitionGraph.DTEdge edge) {
//            this.n = n;
//            this.externalCost = extCost;
//            this.accumulatedCost = accCost;
//            this.pred = pred;
//            this.e = edge;
//        }
//
//        public int cost() {
//            return externalCost + accumulatedCost;
//        }
//
//        @Override
//        public int compareTo(NodeCost o) {
//            return cost() - o.cost();
//        }
//    }
//
//    public List<GAction> bestPath(CostEvaluator ce) {
//        Queue<NodeCost> q = new PriorityQueue<>();
//        Map<DomainTransitionGraph.DTNode, NodeCost> costs = new HashMap<>();
//        for(DomainTransitionGraph.DTNode n : startingNodes) {
//            NodeCost nc = new NodeCost(n, 0, 0, null, null);
//            costs.put(n, nc);
//            q.add(nc);
//        }
//
//        Node dest = null;
//        while(dest == null && !q.isEmpty()) {
//            NodeCost nc = q.poll();
//            if(acceptingNodes.contains(nc.n)) {
//                dest = nc.n;
//            } else {
//                for(LabeledEdge<Node,Edge> e : JavaConversions.asJavaIterable(transitions.inEdges(nc.n))) {
//                    int accCost = nc.accumulatedCost +1;
//                    int actionCost = ce.cost(e.l().act, e.l().ga);
//                    int extCost = (actionCost > nc.externalCost) ? actionCost : nc.externalCost;
//                    NodeCost ncNext = new NodeCost(e.u(), extCost, accCost, nc, e.l());
//
//                    if(!costs.containsKey(ncNext.n)) { // no existing label, put it in queue qhatever
//                        costs.put(ncNext.n, ncNext);
//                        q.add(ncNext);
//                    } else if(costs.get(ncNext.n).cost() > ncNext.cost()) { // existing label, update if better
//                        q.remove(costs.get(ncNext.n));
//                        costs.put(ncNext.n, ncNext);
//                        q.add(ncNext);
//                    }
//                }
//            }
//        }
//        if(dest == null)
//            throw new FAPEException("NO SOLUTION! !!!!");
//
//        List<GAction> solution = new LinkedList<>();
//        NodeCost cur = costs.get(dest);
//        while(cur != null) {
//            if(cur.e != null && cur.e.ga != null)
//                solution.add(cur.e.ga);
//            cur = cur.pred;
//        }
//        return solution;
//    }
//
//    public void print(String filename) {
//        transitions.exportToDotFile(filename);
//    }
//
//    public interface CostEvaluator {
//        public int cost(Action a, GAction ga);
//
////        public boolean usable(GAction ga);
//    }
//}
