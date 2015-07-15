package fape.core.planning.heuristics.relaxed;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.FAPEException;
import fape.exceptions.NoSolutionException;
import planstack.anml.model.concrete.Action;

import java.util.*;

public class OpenGoalTransitionFinder {

    Map<Integer, DomainTransitionGraph> dtgs = new HashMap<>();
    public List<DomainTransitionGraph.DTNode> startNodes = new LinkedList<>();

    public void addDTG(DomainTransitionGraph dtg) {
        assert !dtgs.containsKey(dtg.id());
        dtgs.put(dtg.id(), dtg);
    }

    private class NodeCost implements Comparable<NodeCost> {
        public final DomainTransitionGraph.DTNode n;
        public final int externalCost;
        public final int accumulatedCost;
        public final NodeCost pred;
        public final DomainTransitionGraph.DTEdge e;

        public NodeCost(DomainTransitionGraph.DTNode n, int extCost, int accCost, NodeCost pred, DomainTransitionGraph.DTEdge edge) {
            this.n = n;
            this.externalCost = extCost;
            this.accumulatedCost = accCost;
            this.pred = pred;
            this.e = edge;
        }

        public int cost() {
            return externalCost + accumulatedCost;
        }

        @Override
        public int compareTo(NodeCost o) {
            return cost() - o.cost();
        }
    }

    public interface CostEvaluator {
        public int cost(Action a, GAction ga);

//        public boolean usable(GAction ga);
    }

    public void addSources(Collection<Fluent> fluents) {
        for(DomainTransitionGraph dtg : dtgs.values()) {
            for (Fluent f : fluents) {
                DomainTransitionGraph.DTNode start = dtg.startNodeForFluent(f);
                if(start != null)
                    startNodes.add(start);
            }
        }
    }


    public TransitionSequence bestPath(CostEvaluator ce) throws NoSolutionException {
        Queue<NodeCost> q = new PriorityQueue<>();
        Map<DomainTransitionGraph.DTNode, NodeCost> costs = new HashMap<>();
        for (DomainTransitionGraph.DTNode n : startNodes) {
            NodeCost nc = new NodeCost(n, 0, 0, null, null);
            costs.put(n, nc);
            q.add(nc);
        }

        DomainTransitionGraph.DTNode dest = null;
        while (dest == null && !q.isEmpty()) {
            NodeCost nc = q.poll();
            DomainTransitionGraph current = dtgs.get(nc.n.containerID);
            if (current.isAccepting(nc.n)) {
                dest = nc.n;
            } else {
                Iterator<DomainTransitionGraph.DTEdge> it = current.inEdges(nc.n);
                while(it.hasNext()) {
                    DomainTransitionGraph.DTEdge e = it.next();
                    int accCost;
                    int extCost;
                    if(current.areEdgesFree()) {
                        accCost = nc.accumulatedCost;
                        extCost = nc.externalCost;
                    } else {
                        accCost = nc.accumulatedCost; //TODO + 1;
                        int actionCost = ce.cost(e.act, e.ga);
                        extCost = (actionCost > nc.externalCost) ? actionCost : nc.externalCost;
                    }
                    NodeCost ncNext = new NodeCost(e.from, extCost, accCost, nc, e);

                    if(!costs.containsKey(ncNext.n)) { // no existing label, put it in queue whatever
                        costs.put(ncNext.n, ncNext);
                        q.add(ncNext);
                    } else if(costs.get(ncNext.n).cost() > ncNext.cost()) { // existing label, update if better
                        q.remove(costs.get(ncNext.n));
                        costs.put(ncNext.n, ncNext);
                        q.add(ncNext);
                    }
                }
                for(DomainTransitionGraph dtg : dtgs.values()) {
                    if(dtg != current) {
                        DomainTransitionGraph.DTNode nNext = dtg.possibleEntryPointFrom(nc.n);
                        if(nNext != null) {
                            NodeCost ncNext = new NodeCost(nNext, nc.externalCost, nc.accumulatedCost, nc, null);
                            if(!costs.containsKey(ncNext.n)) { // no existing label, put it in queue whatever
                                costs.put(ncNext.n, ncNext);
                                q.add(ncNext);
                            } else if(costs.get(ncNext.n).cost() > ncNext.cost()) { // existing label, update if better
                                q.remove(costs.get(ncNext.n));
                                costs.put(ncNext.n, ncNext);
                                q.add(ncNext);
                            }
                        }
                    }
                }
            }
        }

        if(dest == null)
            throw new NoSolutionException();
//            throw new FAPEException("NO SOLUTION! !!!!");

        NodeCost cur = costs.get(dest);
        TransitionSequence seq = new TransitionSequence();
        DomainTransitionGraph containingDTG = dtgs.get(cur.n.containerID);
        if(containingDTG instanceof TimelineDTG) {
            seq.supporter = ((TimelineDTG) containingDTG).tl;
        } else if(containingDTG instanceof PathDTG) {
            seq.supporter = ((PathDTG) containingDTG).supporter;
            seq.extendedSolution = (PathDTG) containingDTG;
        }
        while(cur != null) {
            seq.nodes.add(cur);
            cur = cur.pred;
        }
        return seq;
    }

    public class TransitionSequence {
        public Timeline supporter = null;
        public PathDTG extendedSolution;
        public List<NodeCost> nodes = new LinkedList<>();

        public List<GAction> getActionsSequence() {
            List<GAction> solution = new LinkedList<>();
            for(NodeCost nc : nodes) {
                if(nc.e != null && nc.e.ga != null) {
                    solution.add(nc.e.ga);
                }
            }
            return solution;
        }

        public boolean hasGraphChange() {
            if(nodes.isEmpty()) return false;
            int containerID = nodes.get(0).n.containerID;
            for(int i = 1 ; i<nodes.size() ; i++) {
                NodeCost nc = nodes.get(i);
                if (nc.n.containerID != containerID && i+1 < nodes.size())
                    return true;
            }
            return false;
        }

        public PathDTG getDTG() {
            PathDTG dtg = new PathDTG(supporter, extendedSolution);
            for(NodeCost nc : nodes) {
                if(nc.e != null) {
                    dtg.addNextEdge(nc.e.from.value, nc.e.to.value, nc.e.ga, nc.e.act);
                }
            }
            return dtg;
        }
    }
}
