package fr.laas.fape.planning.core.planning.preprocessing.dtg;

import fr.laas.fape.anml.model.concrete.InstanceRef;
import fr.laas.fape.planning.core.planning.grounding.Fluent;
import fr.laas.fape.planning.core.planning.grounding.GAction;
import fr.laas.fape.planning.core.planning.grounding.GAction.GLogStatement;
import fr.laas.fape.planning.core.planning.grounding.GStateVariable;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.structures.Ident;
import fr.laas.fape.structures.Identifiable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;


public class TemporalDTG {
    public final Planner planner;
    public final GStateVariable sv;

    /** Stays false until the postProcess method has been called.
     * Until then, the DTG might not be in a consistent state. */
    private boolean postProcessed = false;
    private int numNodes = 0;

    @Getter @Ident(Node.class)
    public class Node implements Identifiable {
        public Node(Fluent fluent, int minStay, int maxStay, boolean isChangePossible) {
            this.fluent = fluent;
            this.minStay = minStay;
            this.maxStay = maxStay;
            this.isChangePossible = isChangePossible;
            this.id = planner.preprocessor.nextTemporalDTGNodeID++;
            planner.preprocessor.store.record(this);
        }
        final Fluent fluent;
        final int minStay;
        final int maxStay;
        final boolean isChangePossible;
        final int localNodeID = numNodes++;
        @Override public String toString() { return "["+minStay+","+(maxStay<Integer.MAX_VALUE?maxStay:"inf")+"] "+ fluent; }
        public boolean isUndefined() { return fluent == null; }

        public GStateVariable getStateVariable() { return getFluent().sv; }

        public List<Change> inChanges() { return getChangesTo(this); }
        public Iterable<Change> inChanges(Set<GAction> validActions) { return getChangesTo(this, validActions); }

        public int getMinDelayTo(Node target) {
            return getMinimalDelay(this, target);
        }

        private int id = -1;

        @Override public void setID(int i) { assert id == -1; id = i; }
        @Override public int getID() { return id; }
    }

    @AllArgsConstructor @Getter
    public class DurativeCondition {
        final Fluent f;
        final int minStay;
        @Override public String toString() { return "["+minStay+"] "+f; }
    }

    @AllArgsConstructor @Getter
    public class DurativeEffect {
        final Fluent f;
        final int minDuration;
        @Override public String toString() { return "["+minDuration+"] "+f; }
    }

    public abstract class Change {
        public abstract Node getFrom();
        public abstract Node getTo();
        public abstract int getDuration();
        public abstract List<DurativeCondition> getConditions();
        public abstract List<DurativeEffect> getSimultaneousEffects();
        public abstract GAction.GLogStatement getStatement();
        public abstract GAction getContainer();
        public abstract boolean isTransition();
        public  GStateVariable getStateVariable() { return getStatement().getStateVariable(); }
    }

    @AllArgsConstructor @Getter
    public class Transition extends Change {
        final Node from;
        final Node to;
        final int duration;
        final List<DurativeCondition> conditions;
        final List<DurativeEffect> simultaneousEffects;
        final GAction.GLogStatement statement;
        final GAction container;
        public boolean isTransition() { return true; }
        @Override public String toString() { return from+" -["+duration+"]-> "+to; }
    }

    @AllArgsConstructor @Getter
    public class Assignment extends Change {
        final Node to;
        final int duration;
        final List<DurativeCondition> conditions;
        final List<DurativeEffect> simultaneousEffects;
        final GAction.GLogStatement statement;
        final GAction container;
        public Node getFrom() { throw new UnsupportedOperationException("Trying to get the origin of an assignment change."); }
        public boolean isTransition() { return false; }
        @Override public String toString() { return " :=["+duration+"]:= "+to; }
    }

    private Map<InstanceRef, Node> baseNodes = new HashMap<>();
    private Map<Node, List<Change>> outTransitions = new HashMap<>();
    private Map<Node, List<Change>> inTransitions = new HashMap<>();
    private List<Assignment> allAssignments = new ArrayList<>();

    /** Matrix containing the All Pairs Shortest Path for this DTG. */
    int[] apsp;

    public TemporalDTG(GStateVariable sv, Collection<InstanceRef> domain, Planner pl) {
        this.planner = pl;
        this.sv = sv;
        for(InstanceRef val : domain) {
            Fluent f = pl.preprocessor.getFluent(sv, val);
            Node n = new Node(f, 0, Integer.MAX_VALUE, true);
            baseNodes.put(val, n);
            inTransitions.put(n, new ArrayList<>());
            outTransitions.put(n, new ArrayList<>());
        }
    }

    private void recordTransition(Transition tr) {
        assert !postProcessed;
        if(!outTransitions.containsKey(tr.getFrom()))
            outTransitions.put(tr.getFrom(), new ArrayList<>());
        if(!outTransitions.containsKey(tr.getTo()))
            outTransitions.put(tr.getTo(), new ArrayList<>());
        if(!inTransitions.containsKey(tr.getFrom()))
            inTransitions.put(tr.getFrom(), new ArrayList<>());
        if(!inTransitions.containsKey(tr.getTo()))
            inTransitions.put(tr.getTo(), new ArrayList<>());

        outTransitions.get(tr.getFrom()).add(tr);
        inTransitions.get(tr.getTo()).add(tr);
    }

    private void recordAssignment(Assignment ass) {
        assert !postProcessed;
        if(!outTransitions.containsKey(ass.getTo()))
            outTransitions.put(ass.getTo(), new ArrayList<>());
        if(!inTransitions.containsKey(ass.getTo()))
            inTransitions.put(ass.getTo(), new ArrayList<>());

        allAssignments.add(ass);
    }

    public Iterable<Assignment> getAssignments(Set<GAction> validActions) {
        return allAssignments.stream()
                .filter(ass -> validActions.contains(ass.getContainer()))
                ::iterator;
    }

    public Iterable<Change> getChangesFrom(Node n, Set<GAction> validActions) {
        assert postProcessed;
        return outTransitions.get(n).stream()
                .filter(ch -> validActions.contains(ch.getContainer()))
                ::iterator;
    }

    public Iterable<Change> getChangesTo(Node n, Set<GAction> validActions) {
        assert postProcessed;
        return inTransitions.get(n).stream()
                .filter(ch -> validActions.contains(ch.getContainer()))
                ::iterator;
    }

    public List<Change> getChangesTo(Node n) {
        assert postProcessed;
        return inTransitions.get(n);
    }

    public Node getBaseNode(InstanceRef value) {
        return baseNodes.get(value);
    }

    /** returns the minimal delay between those two nodes. */
    public int getMinimalDelay(Node from, Node to) {
        assert from.getStateVariable() == sv && to.getStateVariable() == sv;
        assert postProcessed;
        return dist(from, to);
    }

    public void extendWith(GAction act) {
        assert !postProcessed;
        List<GAction.GLogStatement> changes = act.getStatements().stream()
                .filter(s -> s.getStateVariable() == sv && s.isChange())
                .collect(Collectors.toList());
        if(changes.size() == 1) {
            GLogStatement s = changes.get(0);
            Node to = baseNodes.get(s.endValue());
            int duration = s.getMinDuration();
            List<DurativeCondition> conds = act.conditionsAt(s.start(), planner).stream()
                    .map(cond -> {
                        Fluent f = planner.preprocessor.getFluent(cond.sv, cond.startValue());
                        if(cond.isTransition())
                            return new DurativeCondition(f, 0);
                        else // persistence
                            return new DurativeCondition(f, act.minDuration(cond.start(), cond.end()));
                    })
                    .collect(Collectors.toList());
            List<DurativeEffect> effs = act.changesStartingAt(s.start(), planner).stream()
                    .map(eff -> new DurativeEffect(planner.preprocessor.getFluent(eff.sv, eff.endValue()), eff.getMinDuration()))
                    .collect(Collectors.toList());
            if(s.isTransition()) {
                Node from = baseNodes.get(s.startValue());
                Transition tr = new Transition(from, to, duration, conds, effs, s, act);
                recordTransition(tr);
            } else { // assignement
                allAssignments.add(new Assignment(to, duration, conds, effs, s, act));
            }
        } else { // multiple changes on this state variable
            // sort all statements by increasing time
            changes.sort((s1, s2) -> Integer.compare(act.minDuration(act.abs.start(), s1.start()), act.minDuration(act.abs.start(), s2.start())));

            //split the chain of changes into subchains in which the effect of a change is compatible with the condition of the following one
            LinkedList<LinkedList<GLogStatement>> subchains = new LinkedList<>();
            subchains.add(new LinkedList<>());
            for(GLogStatement s : changes) {
                if(s.isAssignement())
                    // start new subchain on assignments
                    subchains.add(new LinkedList<>());
                if(s.isTransition() &&
                        (subchains.getLast().isEmpty() || !s.startValue().equals(subchains.getLast().getLast().endValue())))
                    // start new subchain on transitions whose value is incompatible with the previous one
                    subchains.add(new LinkedList<>());
                subchains.getLast().add(s);
            }
            for(List<GLogStatement> chain : subchains) {
                Node lastNode = null;
                for(int i=0 ; i<chain.size() ; i++) {
                    GLogStatement s = chain.get(i);
                    Optional<Node> from;
                    if(s.isAssignement())
                        from = Optional.empty();
                    else if(i == 0)
                        from = Optional.of(baseNodes.get(s.startValue()));
                    else
                        from = Optional.of(lastNode);
                    Node to;
                    if(i == chain.size()-1) {
                        to = baseNodes.get(s.endValue());
                    } else {
                        GLogStatement nextChange = chain.get(i+1);
                        int minStay = act.minDuration(s.end(), nextChange.start());
                        int maxStay = act.maxDuration(s.end(), nextChange.start());
                        // try to find out if there is a persistence condition until the next change
                        boolean isChangePossible = act.getStatements().stream()
                                .filter(pers -> pers.isPersistence())
                                .filter(pers -> pers.getStateVariable() == s.getStateVariable())
                                .filter(pers -> pers.endValue().equals(s.endValue()))
                                .filter(pers -> act.maxDuration(s.end(), pers.start()) <= 1)
                                .filter(pers -> nextChange.isAssignement() && act.maxDuration(pers.end(), nextChange.start()) == 0
                                        || nextChange.isTransition() && act.maxDuration(pers.end(), nextChange.start()) <= 1)
                                .count() == 0;

                        to = new Node(
                                planner.preprocessor.getFluent(s.sv, s.endValue()),
                                minStay, maxStay, isChangePossible);
                    }
                    int duration = act.minDuration(s.start(), s.end());
                    List<DurativeCondition> conds = act.conditionsAt(s.start(), planner).stream()
                            .map(cond -> {
                                Fluent f = planner.preprocessor.getFluent(cond.sv, cond.startValue());
                                if(cond.isTransition())
                                    return new DurativeCondition(f, 0);
                                else // persistence
                                    return new DurativeCondition(f, act.minDuration(cond.start(), cond.end()));
                            })
                            .collect(Collectors.toList());
                    List<DurativeEffect> effs = act.changesStartingAt(s.start(), planner).stream()
                            .map(eff -> new DurativeEffect(planner.preprocessor.getFluent(eff.sv, eff.endValue()), eff.getMinDuration()))
                            .collect(Collectors.toList());

                    if(s.isTransition())
                        recordTransition(new Transition(from.get(), to, duration, conds, effs, s, act));
                    else
                        allAssignments.add(new Assignment(to, duration, conds, effs, s, act));
                    lastNode = to;
                }
            }
        }
    }

    public void postProcess() {
        Map<Fluent, Set<Node>> nodesByValue = new HashMap<>();
        for(Node n : inTransitions.keySet()) {
            if(!nodesByValue.containsKey(n.getFluent()))
                nodesByValue.put(n.getFluent(), new HashSet<>());
            nodesByValue.get(n.getFluent()).add(n);
        }

        for(Assignment ass : allAssignments) {
            for(Node from : outTransitions.keySet()) {
                if(from.isChangePossible())
                    outTransitions.get(from).add(ass);
            }
            inTransitions.get(ass.getTo()).add(ass);
        }
        floydWarshall();

        postProcessed = true;
    }

    private int index(int x, int y) { return x + y*numNodes; }
    private int dist(int x, int y) { return apsp[index(x,y)]; }
    private void setDist(int x, int y, int d) { apsp[index(x,y)] = d; }
    private void updateIfLower(int x, int y, int d) {
        if(d < dist(x,y))
            setDist(x, y, d);
    }
    private int dist(Node x, Node y) { return apsp[index(x.localNodeID, y.localNodeID)]; }
    private void setDist(Node x, Node y, int dist) { apsp[index(x.localNodeID, y.localNodeID)] = dist; }
    private void updateIfLower(Node x, Node y, int dist) {
        if(dist < dist(x,y))
            setDist(x,y,dist);
    }
    private static int INF = Integer.MAX_VALUE /2-1;

    private void floydWarshall() {
        apsp = new int[numNodes*numNodes];
        for(int i=0 ; i<numNodes ; i++) {
            for(int j=0 ; j<numNodes ; j++) {
                apsp[index(i,j)] = INF;
            }
        }

        for(Node n : outTransitions.keySet()) {
            setDist(n, n, 0);
            for(Change ch : outTransitions.get(n)) {
                updateIfLower(n, ch.getTo(), ch.getDuration());
            }
        }

        for(int k=0 ; k<numNodes ; k++) {
            for(int i=0; i<numNodes ; i++) {
                for(int j=0 ; j<numNodes ; j++) {
                    updateIfLower(i, j, dist(i,k) + dist(k,j));
                }
            }
        }
//        System.out.println("All pairs shortest path: "+sv);
//        for(Node n1 : outTransitions.keySet()) {
//            System.out.println("  "+n1);
//            for(Node n2 : outTransitions.keySet()) {
//                System.out.println("    "+ (dist(n1,n2)==INF ? "inf  " : dist(n1,n2)+"  ")+ n2);
//            }
//        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(sv);
        for(Node n : outTransitions.keySet()) {
            sb.append("\n  "); sb.append(n);
            for(Change tr : outTransitions.get(n)) {
                sb.append("\n    "); sb.append(tr);
            }
        }
        return sb.toString();
    }
}
