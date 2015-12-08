package fape.core.planning.heuristics.temporal;

import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GroundProblem;
import fape.core.planning.heuristics.Preprocessor;
import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.core.planning.states.StateExtension;
import fape.util.EffSet;
import fr.laas.fape.structures.*;
import lombok.*;

import java.util.*;
import java.util.stream.Collectors;

import fape.core.planning.heuristics.temporal.TempFluent.Fluent;
import planstack.anml.model.concrete.Task;

public class DepGraph {
    private static int dbgLvl = 2;

    private static boolean isInfty(int num) { return num > 99999; }

    @Ident(Node.class)
    public abstract static class Node extends AbsIdentifiable {}
    public abstract static class ActionNode extends Node {
        public abstract List<TempFluent> getConditions();
        public abstract List<TempFluent> getEffects();
    }

    @Getter @Ident(Node.class)
    public static class FactAction extends ActionNode {
        public final List<TempFluent> effects;

        @ValueConstructor @Deprecated
        public FactAction(List<TempFluent> effects) { this.effects = effects; }

        @Override public List<TempFluent> getConditions() { return new ArrayList<>(); }
    }

    @Value class MaxEdge {
        public final TempFluent.Fluent fluent;
        public final ActionNode act;
        public final int delay;
    }
    @Value class MinEdge {
        public final ActionNode act;
        public final TempFluent.Fluent fluent;
        public final int delay;
    }

    private GStore store;

    // graph structure
    Map<ActionNode, List<MinEdge>> actOut = new HashMap<>();
    Map<Fluent, List<MaxEdge>> fluentOut = new HashMap<>();
    Map<ActionNode, List<MaxEdge>> actIn = new HashMap<>();
    Map<Fluent, List<MinEdge>> fluentIn = new HashMap<>();

    IR2IntMap<Node> optimisticEST;
    int dmax = Integer.MIN_VALUE;

    public DepGraph(Collection<RAct> actions, List<TempFluent> facts, State st) {
        store = st.pl.preprocessor.store;
        Set<Fluent> instantaneousEffects = new HashSet<>();
        FactAction init = (FactAction) st.pl.preprocessor.store.get(FactAction.class, Arrays.asList(facts)); // new FactAction(facts);
        List<ActionNode> allActs = new LinkedList<>(actions);
        allActs.add(init);

        for(ActionNode act : allActs) {
            actOut.put(act, new ArrayList<>());
            actIn.put(act, new ArrayList<>());
            for (TempFluent tf : act.getEffects()) {
                Fluent f = tf.fluent;
                assert !isInfty(tf.getTime()) : "Effect with infinite delay.";
                actOut.get(act).add(new MinEdge(act, f, tf.getTime()));
                fluentIn.putIfAbsent(f, new ArrayList<>());
                fluentOut.putIfAbsent(f, new ArrayList<>());
                fluentIn.get(f).add(new MinEdge(act, f, tf.getTime()));
                dmax = Math.max(dmax, tf.getTime());
            }
        }
        for(ActionNode act : allActs) {
            for(TempFluent tf : act.getConditions()) {
                final Fluent f = tf.fluent;
                fluentOut.putIfAbsent(f, new ArrayList<>());
                fluentIn.putIfAbsent(f, new ArrayList<>());
                fluentOut.get(f).add(new MaxEdge(f, act, -tf.getTime()));
                actIn.get(act).add(new MaxEdge(f, act, -tf.getTime()));
                dmax = Math.max(dmax, -tf.getTime());
            }
        }

        if(st.hasExtension(StateExt.class)) {
            optimisticEST = st.getExtension(StateExt.class).getDepGraphEarliestAppearances();
            // remove any existing fact action
            optimisticEST.keySet().stream().collect(Collectors.toSet()).stream()
                    .filter(n -> n instanceof FactAction)
                    .forEach(optimisticEST::remove);

            // add new fact action
            optimisticEST.put(init, 0);
        } else {
            optimisticEST = new IR2IntMap<Node>(st.pl.preprocessor.store.getIntRep(Node.class)); //new HashMap<>();
            for (Fluent f : fluentIn.keySet())
                optimisticEST.put(f, 0);

            for (ActionNode act : allActs) {
                optimisticEST.put(act, 0);
            }
        }
    }

    public void propagate() {
        Propagator p = new BellmanFord(optimisticEST);
        optimisticEST = p.getEarliestAppearances();

        if(dbgLvl >= 2) printActions();
    }

    public static DepGraph of(State st, APlanner pl) {
        final Preprocessor pp = st.pl.preprocessor;
        final GroundProblem gpb = pp.getGroundProblem();


        List<TempFluent> tempFluents = gpb.tempsFluents(st).stream()
                .flatMap(tfs -> tfs.fluents.stream().map(f -> new TempFluent(
                        st.getEarliestStartTime(tfs.timepoints.iterator().next()),
                        TempFluent.Fluent.from(f, pp.store))))
                .collect(Collectors.toList());

        Set<TempFluent> tasks = new HashSet<>();
        for(Task t : st.getOpenTasks()) {
            int est = st.getEarliestStartTime(t.start());
            for(GAction ga : new EffSet<>(pp.groundActionIntRepresentation(), st.csp.bindings().rawDomain(t.groundSupportersVar()).toBitSet())) {
                tasks.add(new TempFluent(est, TempFluent.Fluent.from(ga.task, st.pb, pp.store)));
            }
        }

        List<TempFluent> allFacts = new LinkedList<>();
        allFacts.addAll(tempFluents);
        allFacts.addAll(tasks);

        Optional<StateExt> stateExtOptional = st.hasExtension(StateExt.class) ? Optional.of(st.getExtension(StateExt.class)) : Optional.empty();
        DepGraph dg = new DepGraph(pl.preprocessor.getRelaxedActions(), allFacts, st);
        dg.propagate();
        if(!st.hasExtension(StateExt.class))
            st.addExtension(new StateExt(dg.optimisticEST));

        if(dbgLvl >= 1) {
            long newNumAct = st.getExtension(StateExt.class).depGraphEarliestAppearances.keySet().stream().filter(k -> k instanceof RAct).count();
            System.out.println("Num actions:  " + newNumAct);
        }
//        if(dbgLvl >= 2) dg.printActions();
        return dg;
    }

    private void printActions() {
//        // actions split at each controllable timepoint
//        System.out.println("\nactions: " +
//                optimisticEST.entrySet().stream()
//                        .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
//                        .filter(n -> n.getKey() instanceof RAct)
////                        .filter(n -> n.getKey().toString().contains("at") && n.getKey().toString().contains("tru1") && !(n.getKey() instanceof FactAction))
//                        .map(a -> "\n  [" + a.getValue() + "] " + a.getKey())
//                        .collect(Collectors.toList()));

        // complete action: show start
        System.out.println("\nactions: " +
                optimisticEST.entrySet().stream()
                        .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                        .filter(n -> n.getKey() instanceof RAct)
                        .filter(n -> ((RAct) n.getKey()).tp.toString().equals("ContainerStart"))
                        .map(a -> "\n  [" + a.getValue() + "] " + ((RAct) a.getKey()).act)
                                .collect(Collectors.toList()));
    }

    /**
     * An instance of this class is to be attached to
     */
    @Value public static class StateExt implements StateExtension {
        public final IR2IntMap<Node> depGraphEarliestAppearances;

        @Override
        public StateExt clone() {
            return new StateExt(depGraphEarliestAppearances.clone());
        }
    }

    public static class Handler implements fape.core.planning.search.Handler {

        @Override
        public void apply(State st, StateLifeTime time, APlanner planner) {
            if(time == StateLifeTime.SELECTION) {
                DepGraph.of(st, planner);
            }
        }
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
        private void setImpossible(int nid) {
            possible.remove(nid); eas.remove(nid); }
        private void setImpossible(Node n) { setImpossible(n.getID()); }

        public BellmanFord(IR2IntMap<Node> optimisticValues) {
            possible = new IRSet<Node>(store.getIntRep(Node.class));
            eas = optimisticValues.clone();

            PrimitiveIterator.OfInt it = optimisticValues.keysIterator();
            while(it.hasNext())
                possible.add(it.nextInt());

            int numIter = 0; int numCut = 0;
            boolean updated = true;
            while(updated) {
                numIter ++;
                updated = false;
                final PrimitiveIterator.OfInt nodesIt = possible.primitiveIterator();
                while (nodesIt.hasNext()) {
                    updated |= update(nodesIt.nextInt());
                }

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
            }
            if(dbgLvl >= 1) System.out.println(String.format("Num iterations: %d\tNum removed: %d", numIter, numCut));
        }

        private boolean update(int nid) {
            Node n = (Node) store.get(Node.class, nid); //TODO: keep to primitive types
            if(n instanceof Fluent)
                return updateFluent((Fluent) n);
            else
                return updateAction((ActionNode) n);
        }

        private boolean updateFluent(Fluent f) {
            if(!possible(f)) return false;
            assert eas.containsKey(f.getID()) : "Possible fluent with no earliest appearance time.";

            int min = Integer.MAX_VALUE;
            for(MinEdge e : fluentIn.get(f)) {
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
            for(MaxEdge e : actIn.get(a)) {
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
}
