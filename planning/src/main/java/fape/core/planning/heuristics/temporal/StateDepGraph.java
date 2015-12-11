package fape.core.planning.heuristics.temporal;

import fape.core.planning.grounding.GAction;
import fape.util.IteratorConcat;
import fr.laas.fape.structures.IR2IntMap;
import fr.laas.fape.structures.IRSet;

import java.util.*;
import java.util.stream.Collectors;

public class StateDepGraph implements DependencyGraph {

    static final int dbgLvl = 0;
    static final int dbgLvlDij = 0;

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

        if(dbgLvlDij > 3) {
            Propagator p = new BellmanFord(optimisticEST);
            earliestAppearances = p.getEarliestAppearances();

            Dijkstra dij = new Dijkstra(optimisticEST);
            IR2IntMap<Node> easDij = dij.getEarliestAppearances();

            for(Node n : earliestAppearances.keySet()) {
                assert easDij.containsKey(n);
            }
            for(Node n : easDij.keySet()) {
                if(!earliestAppearances.containsKey(n))
                    dij.display(n);
                assert earliestAppearances.containsKey(n);
                assert earliestAppearances.get(n).equals(easDij.get(n));
            }

        } else {
            Propagator p = new Dijkstra(optimisticEST);
            earliestAppearances = p.getEarliestAppearances();
        }

        addableActs = new IRSet<GAction>(core.store.getIntRep(GAction.class));
        for(Node n : earliestAppearances.keySet())
            if(n instanceof RAct)
                addableActs.add(((RAct) n).getAct());

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
        final IRSet<TempFluent.DGFluent> lateAchieved = new IRSet<>(core.store.getIntRep(TempFluent.DGFluent.class));
        IR2IntMap<Node> pendingForActivation = new IR2IntMap<Node>(core.store.getIntRep(Node.class));
        final IR2IntMap<Node> optimisticValues;

        IR2IntMap<Node> labelsCost = new IR2IntMap<Node>(core.store.getIntRep(Node.class));
        IR2IntMap<Node> labelsPred = new IR2IntMap<Node>(core.store.getIntRep(Node.class));

        PriorityQueue<Node> queue = new PriorityQueue<>((Node n1, Node n2) -> cost(n1) - cost(n2));
        List<MaxEdge> ignoredEdges = new ArrayList<>();

        private int cost(Node n) { return labelsCost.get(n.getID()); }
        private Node pred(Node n) { return (Node) core.store.get(Node.class, labelsPred.get(n)); }
        private void setPred(Node n, Node pred) { labelsPred.put(n, pred.getID()); }
        private boolean possible(Node n) { return labelsPred.containsKey(n); }
        private boolean optimisticallyPossible(Node n) { return optimisticValues.containsKey(n); }

        IRSet<Node> queueContent = new IRSet<Node>(core.store.getIntRep(Node.class));

        private void enqueue(Node n, int cost, Node pred) {
            if(!optimisticValues.containsKey(n))
                return; // ignore all non-possible node

            if(queueContent.contains(n)) {
//                assert queue.contains(n); //  O(n) !!!!
                if(cost < cost(n) && cost(n) > optimisticValues.get(n)) { // "cost" is an improvement and an improvement is possible
                    queue.remove(n);
                    labelsCost.put(n, Math.max(cost, optimisticValues.get(n)));
                    setPred(n, pred);
                    queue.add(n);
                }
            } else if(!labelsPred.containsKey(n)) { // node has not been settled yet
//                assert !queue.contains(n); //  O(n)
                labelsCost.put(n, Math.max(cost, optimisticValues.get(n)));
                setPred(n, pred);
                queue.add(n);
                queueContent.add(n);
            } else {
                assert cost >= cost(n) || cost(n) == optimisticValues.get(n);
            }
        }

        public Dijkstra(IR2IntMap<Node> optimisticValues) {
            this.optimisticValues = optimisticValues.clone();
            for(Node n : optimisticValues.keySet()) {
                if(n instanceof TempFluent.DGFluent) {
                    TempFluent.DGFluent f = (TempFluent.DGFluent) n;
                    for(MinEdge e : inEdges(f)) {
                        if(e.delay <= 0) {
                            lateAchieved.add(f);
                        }
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
                        if(e.delay >= 0 && !lateAchieved.contains(e.fluent))
                            numReq++;
                        else
                            ignoredEdges.add(e);
                    }
                    pendingForActivation.put(n, numReq);
                    if(numReq == 0) {
                        enqueue(n, 0, n);
                    }
                }
            }

            boolean firstPropagationFinished = false;

            while(!queue.isEmpty()) {
                if (!firstPropagationFinished) {
                    originalDijkstra();
                    firstPropagationFinished = true;
                } else {
                    incrementalDijkstra();
                }


                // process ignored edges and enqueue modified nodes
                for (MaxEdge e : ignoredEdges) {
                    if (possible(e.act)) {
                        if (!possible(e.fluent)) {
                            // the action is not possible
                            delete(e.act);
                            if(dbgLvlDij >2) System.out.println(String.format(">del %s", e.act));
                        } else {
                            if (cost(e.fluent) + e.delay > cost(e.act)) {
                                delayEnqueue(e.act, cost(e.fluent) + e.delay, e.fluent);
                                if(dbgLvlDij >2) System.out.println(String.format(">[%d] %s", cost(e.act), e.act));
                            }
                        }
                    }
                }

                // nix late nodes
                List<Integer> easOrdered = new ArrayList<Integer>(labelsCost.values());
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
                    for(Node n : labelsCost.keySet()) {
                        if(possible(n) && cost(n) > cut_threshold) {
                            delete(n);
                        }
                    }
                }
            }
        }

        private void incrementalDijkstra() {
            while(!queue.isEmpty()) {
                Node n = queue.poll();
                queueContent.remove(n);
                if(dbgLvlDij >2) System.out.println(String.format("+[%d] %s     <<--- %s", cost(n), n, pred(n)!=n ? pred(n) : " self"));
                if(n instanceof ActionNode) {
                    ActionNode a = (ActionNode) n;
                    for (MinEdge e : outEdges(a)) {
                        if(pred(e.fluent) == a) {
                            int bestCost = Integer.MAX_VALUE;
                            Node bestPred = null;
                            for(MinEdge minE : inEdges(e.fluent)) {
                                if(possible(minE.act) && cost(minE.act) +minE.delay < bestCost) {
                                    bestCost = cost(minE.act) + minE.delay;
                                    bestPred = minE.act;
                                }
                            }
                            assert bestPred != null;
                            delayEnqueue(e.fluent, bestCost, bestPred);
                        }
                    }
                } else {
                    TempFluent.DGFluent f = (TempFluent.DGFluent) n;
                    if(!lateAchieved.contains(f)) { // late achieved are ignored in the count of pending requirements
                        for(MaxEdge e : outEdges(f)) {
                            if(optimisticallyPossible(e.act) && e.delay >= 0) {
                                assert pendingForActivation.get(e.act) == 0;
                                delayEnqueue(e.act, cost(e.fluent) + e.delay, e.fluent);
                            }
                        }
                    }
                }
            }
        }

        private void originalDijkstra() {
            // main dijkstra loop
            while(!queue.isEmpty()) {
                Node n = queue.poll();
                queueContent.remove(n);
                if(dbgLvlDij >2) System.out.println(String.format(" [%d] %s     <<--- %s", cost(n), n, pred(n)!=n ? pred(n) : " self"));
                if(n instanceof ActionNode) {
                    ActionNode a = (ActionNode) n;
                    for (MinEdge e : outEdges(a)) {
                        enqueue(e.fluent, cost(a) + e.delay, a);
                    }
                } else {
                    TempFluent.DGFluent f = (TempFluent.DGFluent) n;
                    if(!lateAchieved.contains(f)) { // late achieved are ignored in the count of pending requirements
                        for(MaxEdge e : outEdges(f)) {
                            if(optimisticallyPossible(e.act) && e.delay >= 0) {
                                int prevCost = labelsCost.getOrDefault(e.act, optimisticValues.get(n));
                                labelsCost.put(e.act, Math.max(cost(f) + e.delay, prevCost));
                                pendingForActivation.put(e.act, pendingForActivation.get(e.act) - 1);
                                if(dbgLvlDij >2) System.out.println(String.format(" [%d] %s     <<--- %s", cost(n), n, pred(n) != n ? pred(n) : " self"));
                                if(pendingForActivation.get(e.act) == 0) {
                                    enqueue(e.act, labelsCost.get(e.act), f);
                                }
                            }
                        }
                    }
                }
            }
            // clean up label cost
            for(Node n : labelsCost.keySet())
                if(!possible(n))
                    labelsCost.remove(n);
        }

        private void display(Node n) {
            System.out.println();
            System.out.print(possible(n) ? "["+cost(n)+"] " : "[--] ");
            System.out.println(n);
            if(n instanceof TempFluent.DGFluent) {
                for(MinEdge e : inEdges((TempFluent.DGFluent) n))
                    System.out.println("  "+possible(e.act)+" "+e);
            } else {
                for(MaxEdge e : inEdges((ActionNode) n))
                    System.out.println("  "+possible(e.fluent)+"  "+e);
            }
        }

        private void delete(Node n) {
            if(!possible(n))
                return;

            labelsCost.remove(n);
            labelsPred.remove(n);
            queue.remove(n);
            if(queueContent.contains(n))
                queueContent.remove(n);

            if(n instanceof TempFluent.DGFluent) {
                TempFluent.DGFluent f = (TempFluent.DGFluent) n;
                for(MaxEdge e : outEdges(f)) {
                    delete(e.act);
                }
            } else {
                ActionNode a = (ActionNode) n;
                for(MinEdge e : outEdges(a)) {
                    assert !possible(e.fluent) || pred(e.fluent) == a || possible(pred(e.fluent));
                    if(possible(e.fluent) && pred(e.fluent) == a) {
                        int bestCost = Integer.MAX_VALUE;
                        Node bestPred = null;
                        for(MinEdge minE : inEdges(e.fluent)) {
                            if(possible(minE.act) && cost(minE.act) +minE.delay < bestCost) {
                                bestCost = cost(minE.act) + minE.delay;
                                bestPred = minE.act;
                            }
                        }
                        if(bestPred == null) {
                            delete(e.fluent);
                            assert !possible(e.fluent);
                        } else {
                            delayEnqueue(e.fluent, bestCost, bestPred);
                            assert pred(e.fluent) != a;
                        }
                    }
                }
            }
        }

        private void delayEnqueue(Node n, int newCost, Node newPred) {
            assert possible(n);
            assert cost(n) >= optimisticValues.get(n);

            setPred(n, newPred); // always change the predecessor
            if(cost(n) < newCost) {
                if(queue.contains(n))
                    queue.remove(n);
                labelsCost.put(n, newCost);
                setPred(n, newPred);
                queue.add(n);
                queueContent.add(n);
            }

        }

        @Override
        public IR2IntMap<Node> getEarliestAppearances() {
            return labelsCost;
        }
    }
}
