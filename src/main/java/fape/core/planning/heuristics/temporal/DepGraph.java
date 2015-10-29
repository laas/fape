package fape.core.planning.heuristics.temporal;

import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GTaskCond;
import fape.core.planning.grounding.GroundProblem;
import fape.core.planning.heuristics.Preprocessor;
import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.util.EffSet;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.Value;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fape.core.planning.heuristics.temporal.TempFluent.Fluent;
import planstack.anml.model.concrete.Task;

public class DepGraph {

    private static boolean isInfty(int num) { return num > 99999; }

    public interface Node {}
    public interface ActionNode extends Node {
        List<TempFluent> getConditions();
        List<TempFluent> getEffects();
    }

    @Value public static class FactAction implements ActionNode {
        public final List<TempFluent> effects;
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

    // graph structure
    Map<ActionNode, List<MinEdge>> actOut = new HashMap<>();
    Map<Fluent, List<MaxEdge>> fluentOut = new HashMap<>();
    Map<ActionNode, List<MaxEdge>> actIn = new HashMap<>();
    Map<Fluent, List<MinEdge>> fluentIn = new HashMap<>();

    final Map<Node, Integer> optimisticEST;

    List<MaxEdge> ignored = new ArrayList<>();

    // heap
    Map<Node, Label> labels = new HashMap<>();
    PriorityQueue<Label> queue = new PriorityQueue<>();

    @AllArgsConstructor @ToString public class Label implements Comparable<Label> {
        public final Node n;
        public int time;
        public boolean finished;

        @Override
        public int compareTo(Label label) { return time - label.time; }
    }

    public DepGraph(Collection<RAct> actions, List<TempFluent> facts, Optional<Map<Node,Integer>> earliestStarts) {
        Set<Fluent> instantaneousEffects = new HashSet<>();
        FactAction init = new FactAction(facts);
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
                if(tf.getTime() <= 0) {
                    assert tf.getTime() == 0 : "Effect with negative delay in: \n"+act;
                    instantaneousEffects.add(f);
                }
            }
        }
        for(ActionNode act : allActs) {
            for(TempFluent tf : act.getConditions()) {
                Fluent f = tf.fluent;

                // ignore potential zero length loops and potential negative loop
                if((instantaneousEffects.contains(f) && tf.getTime() >= 0) || tf.getTime() > 0) {
                    ignored.add(new MaxEdge(f, act, -tf.getTime()));
                    continue;
                }

                fluentOut.putIfAbsent(f, new ArrayList<>());
                fluentIn.putIfAbsent(f, new ArrayList<>());
                fluentOut.get(f).add(new MaxEdge(f, act, -tf.getTime()));
                actIn.get(act).add(new MaxEdge(f, act, -tf.getTime()));
            }
        }

        if(earliestStarts.isPresent()) {
            optimisticEST = earliestStarts.get();

            // remove any existing fact action
            optimisticEST.keySet().stream().collect(Collectors.toSet()).stream()
                    .filter(n -> n instanceof FactAction)
                    .forEach(optimisticEST::remove);

            // add new fact action
            optimisticEST.put(init, 0);
        } else {
            optimisticEST = new HashMap<>();
            for (Fluent f : fluentIn.keySet())
                optimisticEST.put(f, -1);

            for (ActionNode act : allActs) {
                optimisticEST.put(act, -1);
            }
        }
        for (ActionNode act : allActs) {
            if (isEnabled(act)) {
                setTime(act, 0);
            }
        }
    }

    private int extractMaxLabel() {
        return Stream.concat(
                actOut.values().stream().flatMap(List::stream)
                        .filter(minEdge -> !isInfty(-minEdge.delay))
                        .map(e -> Math.abs(e.delay)),
                Stream.concat(ignored.stream(), actIn.values().stream().flatMap(List::stream))
                        .filter(maxEdge -> !isInfty(-maxEdge.delay))
                        .map(e -> Math.abs(e.delay)))
                .max(Integer::compare).get();
    }

    boolean isFirstDijkstraFinished = false;

    public void propagate() {
        // run a dijkstra first to initialize everything
        dijkstra();
        isFirstDijkstraFinished = true;
        assert queue.isEmpty();
        // delete everything that was not marked by dijkstra
        Set<Node> nodes = new HashSet<>(optimisticEST.keySet());
        for(Node n : nodes) {
            if(!labels.containsKey(n))
                delete(n);
        }

        // prune
        for(MaxEdge e : ignored) {
            if(!isActive(e.fluent)) {
                assert !optimisticEST.containsKey(e.fluent);
                delete(e.act);
                assert !optimisticEST.containsKey(e.act);
                assert !isActive(e.act);
            }
        }
        final int D = extractMaxLabel();
        assert !isInfty(D);

        for(int i=0 ; i<2 ; i++) {
            for (MaxEdge e : ignored) {
                if (isActive(e.act)) {
                    assert isEnabled(e.act);
                    assert isActive(e.fluent);
                    assert optimisticEST.containsKey(e.fluent);
                    int dstTime = optimisticEST.get(e.fluent) + e.delay;
                    if (dstTime > optimisticEST.get(e.act)) {
//                        System.out.println("Updating: " + optimisticEST.get(e.act) + " -> " + dstTime + " for " + e.act.toString()+
//                        "    from: "+e);
                        labels.clear();
                        queue.clear();
                        setTime(e.act, dstTime);
                        dijkstra();
                    }
                }
            }
            Function<Stream<Integer>, Optional<Integer>> lateThreshold = s -> {
                Optional<Integer> prev = Optional.empty();
                for (Integer v : s.sorted().collect(Collectors.toList())) {
                    if (prev.isPresent() && (v - prev.get()) > D) {
                        // gap of at least D between v and its predecessor, v is the late threshold
                        return Optional.of(v);
                    }
                    prev = Optional.of(v);
                }
                return Optional.empty();
            };
            Optional<Integer> cutThreshold = lateThreshold.apply(optimisticEST.values().stream());
            if(cutThreshold.isPresent()) {
                for(Node n : new ArrayList<>(optimisticEST.keySet())) {
                    if(optimisticEST.containsKey(n) && optimisticEST.get(n) > cutThreshold.get()) {
                        System.out.println("Late cutting: "+n);
                        delete(n);
                    }
                }
            }
        }
    }

    public void delete(Node n) {
        if(!isActive(n)) {
            optimisticEST.remove(n);
            assert !labels.containsKey(n);
            return;
        }

        labels.remove(n);
        optimisticEST.remove(n);
        if(n instanceof Fluent) {
            fluentOut.get(n).stream()
                    .forEach(e -> delete(e.act));

            ignored.stream()
                    .filter(e -> e.fluent.equals(n))
                    .forEach(e -> delete(e.act));
        } else {
            actOut.get((ActionNode) n).stream()
                    .filter(e -> !isEnabled(e.fluent))
                    .forEach(e -> delete(e.fluent));
        }
        assert !isActive(n);
    }

    public void dijkstra() {
        while(!queue.isEmpty()) {
            Label cur = queue.poll();
            expandDijkstra(cur);
        }
    }

    private void setTime(Node n, int newTime) {
        if(!optimisticEST.containsKey(n))
            return; // only enqueue node that are "optimistically feasible"

        // real time is the max of optimistic and newTime
        int time = newTime > optimisticEST.get(n) ? newTime : optimisticEST.get(n);

        if(labels.containsKey(n)) {
            Label lbl = labels.get(n);
            assert !lbl.finished || time >= lbl.time : "Updating a finished label";
            if(time < lbl.time) {
                queue.remove(lbl);
                lbl.time = time;
                queue.add(lbl);
            }
        } else {
            Label lbl = new Label(n, time, false);
            labels.put(n, lbl);
            queue.add(lbl);
        }
    }

    private void expandDijkstra(Label lbl) {
        assert !wasPruned(lbl.n);
        assert !lbl.finished;
        assert !queue.contains(lbl);
        lbl.finished = true;
        assert optimisticEST.containsKey(lbl.n) : "not possible (according to optiEST): "+lbl.n;
        assert lbl.time >= optimisticEST.get(lbl.n);

        // only propagate if this increases our best known time
        if(!isFirstDijkstraFinished || lbl.time > optimisticEST.get(lbl.n)) {
            optimisticEST.put(lbl.n, lbl.time);

            if (lbl.n instanceof Fluent) {
                Fluent f = (Fluent) lbl.n;
                fluentOut.get(f).stream()
                        .map(e -> e.act)
                        .filter(a -> !wasPruned(a))
                        .filter(a -> actIn.get(a).stream().allMatch(e -> labels.containsKey(e.fluent)))
                        .filter(a -> isEnabled(a))
                        .forEach(a -> setTime(a, earliestStart(a)));
            } else {
                ActionNode act = (ActionNode) lbl.n;
                actOut.get(act).stream()
                        .filter(e -> !wasPruned(e.fluent))
                        .forEach(e -> setTime(e.fluent, earliestStart(e.fluent)));
            }
        }
    }

    private boolean wasPruned(Node n) {
        return !optimisticEST.containsKey(n);
    }

    private boolean isActive(Node n) {
        return optimisticEST.containsKey(n) && optimisticEST.get(n) >= 0;
    }

    private boolean isEnabled(ActionNode act) {
        return actIn.get(act).stream().allMatch(e -> isActive(e.fluent));
    }

    private boolean isEnabled(Fluent f) {
        return fluentIn.get(f).stream().anyMatch(e -> isActive(e.act));
    }

    private int earliestStart(ActionNode act) {
        assert isEnabled(act);
        return actIn.get(act).stream()
                .map(e -> optimisticEST.get(e.fluent) + e.delay)
                .max(Integer::compare)
                .orElse(0);
    }

    private int earliestStart(Fluent f) {
        assert isEnabled(f);
        return fluentIn.get(f).stream()
                .filter(e -> isActive(e.act))
                .map(e -> optimisticEST.get(e.act)+ e.delay)
                .min(Integer::compare)
                .get();
    }

    public static DepGraph of(State st, APlanner pl) {
        final Preprocessor pp = pl.preprocessor;
        final GroundProblem gpb = pl.preprocessor.getGroundProblem();
        List<TempFluent> tempFluents = gpb.tempsFluents(st).stream()
                .flatMap(tfs -> tfs.fluents.stream().map(f -> new TempFluent(
                        st.getEarliestStartTime(tfs.timepoints.iterator().next()),
                        TempFluent.Fluent.from(f))))
                .collect(Collectors.toList());

        Set<TempFluent> tasks = new HashSet<>();
        for(Task t : st.getOpenTasks()) {
            int est = st.getEarliestStartTime(t.start());
            for(GAction ga : new EffSet<>(pp.groundActionIntRepresentation(), st.csp.bindings().rawDomain(t.groundSupportersVar()).toBitSet())) {
                tasks.add(new TempFluent(est, TempFluent.Fluent.from(ga.task, st.pb)));
            }
        }

        List<TempFluent> allFacts = new LinkedList<>();
        allFacts.addAll(tempFluents);
        allFacts.addAll(tasks);

        DepGraph dg = new DepGraph(pl.preprocessor.getGroundProblem().relaxedActions, allFacts, st.depGraphESTs);
        dg.propagate();
        if (!st.depGraphESTs.isPresent())
            st.depGraphESTs = Optional.of(dg.optimisticEST);

//        System.out.println("\nall: " + dg.optimisticEST.keySet());
        System.out.println("\nactions: " +
                dg.optimisticEST.entrySet().stream()
                        .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                        .filter(n -> n.getKey() instanceof RAct)
//                        .filter(n -> n.getKey().toString().contains("at") && n.getKey().toString().contains("tru1") && !(n.getKey() instanceof FactAction))
                        .map(a -> "\n  [" + a.getValue() + "] " + a.getKey())
                        .collect(Collectors.toList()));

        return dg;
    }
}
