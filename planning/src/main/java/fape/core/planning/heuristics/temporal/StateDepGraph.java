package fape.core.planning.heuristics.temporal;

import fape.core.planning.grounding.GAction;
import fape.util.IteratorConcat;
import fr.laas.fape.structures.IR2IntMap;
import fr.laas.fape.structures.IRSet;

import java.util.*;
import java.util.stream.Collectors;

public class StateDepGraph implements DependencyGraph {

    static final int dbgLvl = 0;

    public DepGraphCore core;
    public final FactAction facts;
    List<MinEdge> initMinEdges = new ArrayList<>();
    Map<TempFluent.DGFluent, List<MinEdge>> initFluents = new HashMap<>();
    int dmax;

    IR2IntMap<Node> earliestAppearances = null;
    IRSet<GAction> addableActs = null;

    public StateDepGraph(DepGraphCore core, List<TempFluent> initFacts) {
        this.core = core;

        this.facts = core.store.getFactAction(initFacts);
        dmax = core.dmax;

        for(TempFluent tf : facts.getEffects()) {
            MinEdge e = new MinEdge(facts, tf.fluent, tf.getTime());
            initMinEdges.add(e);
            initFluents.putIfAbsent(tf.fluent, new ArrayList<>());
            initFluents.get(tf.fluent).add(e);
        }
    }

    private void printActions() {
        // complete action: show start
        System.out.println("\nactions: " +
                earliestAppearances.entrySet().stream()
                        .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                        .filter(n -> n.getKey() instanceof RAct)
                        .filter(n -> ((RAct) n.getKey()).tp.toString().equals("ContainerStart"))
                        .map(a -> "\n  [" + a.getValue() + "] " + ((RAct) a.getKey()).act)
                        .collect(Collectors.toList()));
    }

    @Override
    public Iterator<MaxEdge> inEdgesIt(ActionNode n) {
        if(n != facts)
            return core.inEdgesIt(n);
        else
            return Collections.emptyIterator(); // fact action has no incoming edges
    }

    @Override
    public Iterator<MinEdge> outEdgesIt(ActionNode n) {
        if(n != facts)
            return core.outEdgesIt(n);
        else
            return initMinEdges.iterator();

    }

    @Override
    public Iterator<MinEdge> inEdgesIt(TempFluent.DGFluent f) {
        if(initFluents.containsKey(f))
            return new IteratorConcat<>(core.inEdgesIt(f), initFluents.get(f).iterator());
        else
            return core.inEdgesIt(f);
    }

    @Override
    public Iterator<MaxEdge> outEdgesIt(TempFluent.DGFluent f) {
        return core.outEdgesIt(f); // no such edge can be added by a fact action
    }


    public IR2IntMap<Node> propagate(Optional<StateDepGraph> ancestorGraph) {
        IR2IntMap<Node> optimisticEST;
        if(ancestorGraph.isPresent()) {
            optimisticEST = ancestorGraph.get().earliestAppearances.clone();
            optimisticEST.remove(ancestorGraph.get().facts);
            optimisticEST.put(facts, 0);
        } else {
            optimisticEST = core.getDefaultEarliestApprearances();
            optimisticEST.put(facts, 0);
            for(TempFluent.DGFluent f : initFluents.keySet())
                optimisticEST.putIfAbsent(f, 0);
        }
        new Dijkstra(optimisticEST);
        Propagator p = new BellmanFord(optimisticEST);
        earliestAppearances = p.getEarliestAppearances();

        if(!ancestorGraph.isPresent()) {
            List<RAct> feasibles = earliestAppearances.keySet().stream()
                    .filter(n -> n instanceof RAct)
                    .map(n -> (RAct) n)
                    .collect(Collectors.toList());
            DepGraphCore prevCore = core;
            core = new DepGraphCore(feasibles, core.store);
            if(dbgLvl >= 1) System.out.println("Shrank core graph to: "+core.getDefaultEarliestApprearances().size()
                    +" nodes. (Initially: "+prevCore.getDefaultEarliestApprearances().size()+")");
        }

        if(dbgLvl >= 2) printActions();

        return earliestAppearances;
    }


    interface Propagator {
        IR2IntMap<Node> getEarliestAppearances();
    }

    class BellmanFord implements Propagator {
        final IRSet<Node> possible;
        final IR2IntMap<Node> eas;

        private int ea(Node n) { return ea(n.getID()); }
        private int ea(int nid) { return eas.get(nid); }
        private void setEa(Node n, int t) { setEa(n.getID(), t); }
        private void setEa(int nid, int t) { assert ea(nid) <= t; eas.put(nid, t); }
        private boolean possible(Node n) { return possible(n.getID()); }
        private boolean possible(int nid) { return possible.contains(nid); }
        private void setImpossible(int nid) { possible.remove(nid); eas.remove(nid); }
        private void setImpossible(Node n) { setImpossible(n.getID()); }

        public BellmanFord(IR2IntMap<Node> optimisticValues) {
            possible = new IRSet<Node>(core.store.getIntRep(Node.class));
            eas = optimisticValues.clone();
            earliestAppearances = eas;

            PrimitiveIterator.OfInt it = optimisticValues.keysIterator();
            while(it.hasNext())
                possible.add(it.nextInt());

            int numIter = 0; int numCut = 0;
            boolean updated = true;
            while(updated) {
                if(dbgLvl >= 3) printActions();
                numIter ++;
                updated = false;
                final PrimitiveIterator.OfInt nodesIt = possible.primitiveIterator();
                while (nodesIt.hasNext()) {
                    updated |= update(nodesIt.nextInt());
                }

                if(dbgLvl >= 3) printActions();

                // nix late nodes
                List<Integer> easOrdered = new ArrayList<Integer>(eas.values());
                Collections.sort(easOrdered);
                int prevValue = 0; int cut_threshold = Integer.MAX_VALUE;
                for(int val : easOrdered) {
                    if(val - prevValue > dmax) {
                        cut_threshold = val;
                        break;
                    }
                    prevValue = val;
                }
                if(cut_threshold != Integer.MAX_VALUE) {
                    PrimitiveIterator.OfInt nodes = possible.primitiveIterator();
                    while(nodes.hasNext()) {
                        int nid = nodes.nextInt();
                        if(ea(nid) > cut_threshold) {
                            setImpossible(nid);
                            numCut++;
                        }
                    }
                }
                if(dbgLvl >= 3) printActions();
            }
            if(dbgLvl >= 1) System.out.println(String.format("Num iterations: %d\tNum removed: %d", numIter, numCut));

            addableActs = new IRSet<GAction>(core.store.getIntRep(GAction.class));
            for(Node n : possible)
                if(n instanceof RAct)
                    addableActs.add(((RAct) n).getAct());
        }

        private boolean update(int nid) {
            Node n = (Node) core.store.get(Node.class, nid); //TODO: keep to primitive types
            if(n instanceof TempFluent.DGFluent)
                return updateFluent((TempFluent.DGFluent) n);
            else
                return updateAction((ActionNode) n);
        }

        private boolean updateFluent(TempFluent.DGFluent f) {
            if(!possible(f)) return false;
            assert eas.containsKey(f.getID()) : "Possible fluent with no earliest appearance time.";

            int min = Integer.MAX_VALUE;
            for(MinEdge e : inEdges(f)) {
                if(possible(e.act) && ea(e.act) + e.delay < min) {
                    min = ea(e.act) + e.delay;
                }
            }
            if(min == Integer.MAX_VALUE) {
                setImpossible(f);
                return true;
            } else if(min > ea(f)) {
                setEa(f, min);
                return true;
            } else {
                return false;
            }
        }

        private boolean updateAction(ActionNode a) {
            int max = Integer.MIN_VALUE;
            for(MaxEdge e : inEdges(a)) {
                if(!possible(e.fluent))
                    max = Integer.MAX_VALUE;
                else
                    max = Math.max(max, ea(e.fluent) + e.delay);
            }
            if(max == Integer.MAX_VALUE) {
                setImpossible(a);
                return true;
            } else if(max > ea(a)) {
                setEa(a, max);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public IR2IntMap<Node> getEarliestAppearances() {
            return eas;
        }
    }

    public class Dijkstra implements Propagator {
        // contains all fluents that can be achieved be an action earlier or concurrently to it (i.e. with
        // an incoming null or negative edge)
        IRSet<TempFluent.DGFluent> lateAchieved = new IRSet<>(core.store.getIntRep(TempFluent.DGFluent.class));
        IR2IntMap<Node> pendingForActivation = new IR2IntMap<Node>(core.store.getIntRep(Node.class));
        final IR2IntMap<Node> optimisticValues;

        IR2IntMap<Node> labelsCost = new IR2IntMap<Node>(core.store.getIntRep(Node.class));

        PriorityQueue<Node> queue = new PriorityQueue<>((Node n1, Node n2) -> cost(n1) - cost(n2));

        private int cost(Node n) { return labelsCost.get(n); }
        private boolean optimisticallyPossible(Node n) { return optimisticValues.containsKey(n); }
        private void enqueue(Node n, int cost) {
            if(!optimisticValues.containsKey(n))
                return; // ignore all non-possble node

            if(queue.contains(n)) {
                if(cost < cost(n) && cost(n) > optimisticValues.get(n)) { // "cost" is an improvement and an improvement is possible
                    queue.remove(n);
                    labelsCost.put(n, Math.max(cost, optimisticValues.get(n)));
                    queue.add(n);
                }
            } else if(!labelsCost.containsKey(n)){
                labelsCost.put(n, Math.max(cost, optimisticValues.get(n)));
                queue.add(n);
            } else {
                assert cost >= cost(n) || cost(n) == optimisticValues.get(n);
            }
        }

        public Dijkstra(IR2IntMap<Node> optimisticValues) {
            System.out.println();
            this.optimisticValues = optimisticValues.clone();
            for(Node n : optimisticValues.keySet()) {
                if(n instanceof TempFluent.DGFluent) {
                    for(MinEdge e : inEdges((TempFluent.DGFluent) n)) {
                        if(e.delay <= 0)
                            lateAchieved.add((TempFluent.DGFluent) n);
                    }
                }
            }
            for(Node n : optimisticValues.keySet()) {
                if(n instanceof TempFluent.DGFluent) {
                    pendingForActivation.put(n, 1);
                } else { // action node
                    ActionNode a = (ActionNode) n;
                    int numReq = 0;
                    for(MaxEdge e : inEdges(a)) {
                        if(e.delay <= 0 && !lateAchieved.contains(e.fluent))
                            numReq++;
                    }
                    pendingForActivation.put(n, numReq);
                    if(numReq == 0) {
                        enqueue(n, 0);
                    }
                }
            }

            while(!queue.isEmpty()) {
                Node n = queue.poll();
                System.out.println(String.format("[%d] %s", cost(n), n));
                if(n instanceof ActionNode) {
                    ActionNode a = (ActionNode) n;
                    for (MinEdge e : outEdges(a)) {
                        enqueue(e.fluent, cost(a) + e.delay);
                    }
                } else {
                    TempFluent.DGFluent f = (TempFluent.DGFluent) n;
                    if(!lateAchieved.contains(f)) { // late achieved are ignored in the count of pending requirements
                        for(MaxEdge e : outEdges(f)) {
                            if(optimisticallyPossible(e.act) && e.delay >= 0) {
                                int prevCost = labelsCost.getOrDefault(e.act, optimisticValues.get(n));
                                labelsCost.put(e.act, Math.max(cost(f) + e.delay, prevCost));
                                pendingForActivation.put(e.act, pendingForActivation.get(e.act) - 1);
                                if(pendingForActivation.get(e.act) == 0) {
                                    enqueue(e.act, labelsCost.get(e.act));
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public IR2IntMap<Node> getEarliestAppearances() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
