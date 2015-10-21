package fape.core.planning.heuristics.temporal;

import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.Value;

import java.util.*;

import fape.core.planning.heuristics.temporal.TempFluent.Fluent;

public class DepGraph {

    public interface Node {};
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

    Map<Node, Integer> optimisticEST = new HashMap<>();

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

    public DepGraph(Collection<RAct> actions, List<TempFluent> facts) {
        Set<Fluent> intantaneousEffects = new HashSet<>();
        FactAction init = new FactAction(facts);
        List<ActionNode> allActs = new LinkedList<>(actions);
        allActs.add(new FactAction(facts));

        for(ActionNode act : allActs) {
            actOut.put(act, new ArrayList<>());
            actIn.put(act, new ArrayList<>());
            for (TempFluent tf : act.getEffects()) {
                Fluent f = tf.fluent;
                actOut.get(act).add(new MinEdge(act, f, tf.getTime()));
                fluentIn.putIfAbsent(f, new ArrayList<>());
                fluentOut.putIfAbsent(f, new ArrayList<>());
                fluentIn.get(f).add(new MinEdge(act, f, tf.getTime()));
                if(tf.getTime() <= 0) {
                    assert tf.getTime() == 0 : "Effect with negative delay in: \n"+act;
                    intantaneousEffects.add(f);
                    System.out.println("instantaneous effect: "+f);
                }
            }
        }
        for(ActionNode act : allActs) {
            for(TempFluent tf : act.getConditions()) {
                Fluent f = tf.fluent;

                if(tf.getTime() > 0)
                    System.out.println(tf);

                // ignore potential zero length loops and potential negative loop
                if((intantaneousEffects.contains(f) && tf.getTime() >= 0) || tf.getTime() > 0) {
                    ignored.add(new MaxEdge(f, act, -tf.getTime()));
                    continue;
                }

                fluentOut.putIfAbsent(f, new ArrayList<>());
                fluentIn.putIfAbsent(f, new ArrayList<>());
                fluentOut.get(f).add(new MaxEdge(f, act, -tf.getTime()));
                actIn.get(act).add(new MaxEdge(f, act, -tf.getTime()));
            }
        }
        for(Fluent f : fluentIn.keySet())
            optimisticEST.put(f, -1);

        for(ActionNode act : allActs) {
            optimisticEST.put(act, -1);
            if(isEnabled(act)) {
//                System.out.println("[_start_] no conditions: "+act.act);
                setTime(act, 0);
            }
        }

    }

    public void propagate() {
        // run a dijkstra first to initialize everything
        dijkstra();
        assert queue.isEmpty();

        // prune
        for(MaxEdge e : ignored) {
            if(!isActive(e.fluent)) {
                System.out.println("deleting: "+e.act + " because of " + e);
                delete(e.act);
            }
        }

        for(int i=0 ; i<20 ; i++) {
            for (MaxEdge e : ignored) {
                if (isActive(e.act)) {
                    int dstTime = optimisticEST.get(e.fluent) + e.delay;
                    if (dstTime > optimisticEST.get(e.act)) {
                        System.out.println("Updating: " + optimisticEST.get(e.act) + " -> " + dstTime + " for " + e.act.toString()+
                        "    from: "+e);
                        labels.clear();
                        queue.clear();
                        setTime(e.act, dstTime);
                        dijkstra();
                    }
                }
            }
        }

        assert actOut.keySet().stream()
                .filter(a -> a instanceof FactAction)
                .allMatch(a -> a.getEffects().stream().allMatch(eff -> isActive(eff.fluent)));
        actOut.keySet().stream()
                .filter(a -> a instanceof FactAction)
                .forEach(a -> {
                    System.out.println(a);
                    a.getEffects().stream().forEach(eff -> {
                        System.out.println("  eff: "+eff+" "+optimisticEST.get(eff));
                    });
                });

        optimisticEST.entrySet().stream()
                .filter(e -> e.getValue() >= 0)
                .sorted((e1, e2) -> e1.getValue() - e2.getValue())
                .forEach(e -> System.out.println(e.getValue() + "  " + e.getKey()));

        System.out.println("-------------- tasks:");
        optimisticEST.entrySet().stream()
//                .filter(e -> e.getKey() instanceof Fluent && ((Fluent) e.getKey()).isTask())
                .filter(e -> e.getKey() instanceof ActionNode)
                .sorted((e1, e2) -> e1.getValue() - e2.getValue())
                .forEach(e -> System.out.println(e.getValue() + "  " + e.getKey()));

    }

    public void delete(Node n) {
        if(!labels.containsKey(n))
            return; // node has already been deleted

        System.out.println("Deleting: "+n);
        labels.remove(n);
        optimisticEST.remove(n);
        if(n instanceof Fluent) {
            fluentOut.get(n).stream()
                    .filter(e -> !isEnabled(e.act))
                    .forEach(e -> delete(e.act));
        } else {
            actOut.get(n).stream()
                    .filter(e -> !isEnabled(e.fluent))
                    .forEach(e -> delete(e.act));
        }
    }

    public void dijkstra() {
        while(!queue.isEmpty()) {
            Label cur = queue.poll();
            expandDijkstra(cur);
        }

        labels.values().stream().sorted().forEach(l -> {
            optimisticEST.put(l.n, l.time);
            if (l.n instanceof RAct)
                System.out.println(l.time + "  " + ((RAct) l.n).act);
            else
                System.out.println(l.time + ": " + l.n);
        });
        System.out.println("");
    }

    private void setTime(Node n, int newTime) {
        assert optimisticEST.containsKey(n);

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
        assert lbl.time >= optimisticEST.get(lbl.n);

        // only propagate if this increases our best known time
        if(lbl.time > optimisticEST.get(lbl.n)) {
            optimisticEST.put(lbl.n, lbl.time);

            if (lbl.n instanceof Fluent) {
                Fluent f = (Fluent) lbl.n;
                fluentOut.get(f).stream()
                        .filter(e -> !wasPruned(e.act))
                        .filter(e -> isEnabled(e.act))
                        .forEach(e -> setTime(e.act, earliestStart(e.act)));
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
}
