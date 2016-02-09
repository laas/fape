package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import lombok.Data;
import lombok.Value;

import java.util.*;
import java.util.stream.Collectors;

public class GoalNetwork {

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

    @Data public static class DisjunctiveGoal {
        final Set<GAction.GLogStatement> goals;
    }

    Map<DisjunctiveGoal, Set<DisjunctiveGoal>> precedences = new HashMap<>();
    Map<DisjunctiveGoal, DisjunctiveGoal> timelineFollower = new HashMap<>();

    public void addGoal(DisjunctiveGoal g, DisjunctiveGoal precedingLinkInTimeline) {
        precedences.put(g, new HashSet<>());
        if(precedingLinkInTimeline != null) {
            assert !timelineFollower.containsKey(precedingLinkInTimeline);
            timelineFollower.put(precedingLinkInTimeline, g);
            addPrecedence(precedingLinkInTimeline, g);
        }
    }

    public void addPrecedence(DisjunctiveGoal g1, DisjunctiveGoal g2) {
        assert !precedences.get(g2).contains(g1);
        precedences.get(g1).add(g2);
    }

    public Collection<DisjunctiveGoal> getActiveGoals() {
        return precedences.keySet().stream()
                .filter(g -> precedences.get(g).isEmpty())
                .collect(Collectors.toList());
    }

    public void setAchieved(DisjunctiveGoal g, int time) {
        assert precedences.get(g).isEmpty();
        precedences.remove(g);
        // TODO propagate times
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(DisjunctiveGoal dg : precedences.keySet()) {
            sb.append(dg.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
