package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.GAction;
import fape.util.Pair;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.TPRef;

import static fape.util.Pair.*;

import java.util.*;
import java.util.stream.Collectors;

public class GoalNetwork {

    public static void log(String s) { if(false) System.out.println(s); }

//    public interface Statement {
//
//    }
//    @Value public static class Condition implements Statement {
//        final Fluent f;
//        final int duration;
//    }
//    @Value public static class Transition implements Statement {
//        final Fluent from;
//        final Fluent to;
//        final int duration;
//    }
//    @Value public static class Assignement implements Statement {
//        final Fluent to;
//        final int duration;
//    }

    @RequiredArgsConstructor @Getter @ToString
    public static class DisjunctiveGoal {
        final Set<GAction.GLogStatement> goals;
        final TPRef start;
        final TPRef end;
    }

    Map<DisjunctiveGoal, Set<DisjunctiveGoal>> inPrecLinks = new HashMap<>();
    Map<DisjunctiveGoal, Set<DisjunctiveGoal>> outPrecLinks = new HashMap<>();
    Map<DisjunctiveGoal, DisjunctiveGoal> timelineFollower = new HashMap<>();

    public void addGoal(DisjunctiveGoal g, DisjunctiveGoal precedingLinkInTimeline) {
        inPrecLinks.put(g, new HashSet<>());
        outPrecLinks.put(g, new HashSet<>());
        if(precedingLinkInTimeline != null) {
            assert !timelineFollower.containsKey(precedingLinkInTimeline);
            timelineFollower.put(precedingLinkInTimeline, g);
            addPrecedence(precedingLinkInTimeline, g);
        }
    }

    public void addPrecedence(DisjunctiveGoal g1, DisjunctiveGoal g2) {
        assert !inPrecLinks.get(g2).contains(g1);
        outPrecLinks.get(g1).add(g2);
        inPrecLinks.get(g2).add(g1);
    }

    public Iterable<DisjunctiveGoal> getAllGoals() {
        return new HashSet<>(inPrecLinks.keySet());
    }

    public Collection<DisjunctiveGoal> getActiveGoals() {
        return inPrecLinks.keySet().stream()
                .filter(g -> inPrecLinks.get(g).isEmpty())
                .collect(Collectors.toList());
    }

    public void setAchieved(DisjunctiveGoal g, GAction.GLogStatement achievedDisjunct) {
        log("Achieved: "+achievedDisjunct);
        assert inPrecLinks.get(g).isEmpty();
        for(DisjunctiveGoal descendant : outPrecLinks.get(g)) {
            inPrecLinks.get(descendant).remove(g);
        }
        inPrecLinks.remove(g);
        outPrecLinks.remove(g);
        timelineFollower.remove(g);
        // TODO propagate times
    }

    public List<Pair<GAction.GLogStatement, DisjunctiveGoal>> satisfiable(PartialState ps) {
        List<Pair<GAction.GLogStatement, DisjunctiveGoal>> res = new ArrayList<>();
        for(DisjunctiveGoal dg : getActiveGoals()) {
            for(GAction.GLogStatement gls : dg.getGoals()) {
                if(gls instanceof GAction.GAssignment) {
                    res.add(pair(gls, dg));
                } else if(ps.labels.containsKey(gls.sv)) {
                    InstanceRef condVal = gls instanceof GAction.GPersistence ?
                            ((GAction.GPersistence) gls).value : ((GAction.GTransition) gls).from;
                    if(ps.labels.get(gls.sv).getVal().equals(condVal))
                        res.add(pair(gls, dg));
                }
            }
        }
        return res;
    }

    public boolean isEmpty() { return inPrecLinks.isEmpty(); }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(DisjunctiveGoal dg : inPrecLinks.keySet()) {
            sb.append(dg.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
