package fr.laas.fape.planning.core.planning.search.strategies.plans.tsp;

import fr.laas.fape.anml.model.concrete.TPRef;
import fr.laas.fape.planning.core.planning.grounding.GAction;
import fr.laas.fape.planning.util.Pair;
import lombok.*;

import java.util.*;
import java.util.stream.Collectors;

import static fr.laas.fape.planning.util.Pair.pair;

public class GoalNetwork {

    private static int debugLvl = 0;
    public static void log(String s) { assert debugLvl > 0; System.out.println(s); }

    @RequiredArgsConstructor @Getter @ToString
    public static class DisjunctiveGoal {
        final Set<GAction.GLogStatement> goals;
        final TPRef start;
        final TPRef end;
        int earliest = -1;
        public void setEarliest(int time) {
            assert time >= earliest;
            earliest = time;
        }
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
        enforceDelay(g1,g2);
    }

    private void enforceDelay(DisjunctiveGoal pred, DisjunctiveGoal succ) {
        if(pred.getEarliest() +1 > succ.getEarliest())
            succ.setEarliest(pred.getEarliest()+1);
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
        if(debugLvl > 0)
            log("Achieved: "+achievedDisjunct);
        assert inPrecLinks.get(g).isEmpty();
        for(DisjunctiveGoal descendant : outPrecLinks.get(g)) {
            inPrecLinks.get(descendant).remove(g);
            enforceDelay(g, descendant);
        }
        inPrecLinks.remove(g);
        outPrecLinks.remove(g);
        timelineFollower.remove(g);
    }

    public List<Pair<GAction.GLogStatement, DisjunctiveGoal>> satisfiable(PartialState ps) {
        List<Pair<GAction.GLogStatement, DisjunctiveGoal>> res = new ArrayList<>();
        for(DisjunctiveGoal dg : getActiveGoals()) {
            for(GAction.GLogStatement gls : dg.getGoals()) {
                if(gls instanceof GAction.GAssignment) {
                    res.add(pair(gls, dg));
                } else if(ps.labels.containsKey(gls.sv)) {
                    if(ps.latestLabel(gls.sv).getVal().equals(gls.startValue()))
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
