package fape.core.inference;

import java.util.*;

public class LeveledReasoner {

    public static int defSize = 10;

    class Enabler {
        final int level;
        final int clause;
        public Enabler(int level, int clause) {
            this.clause = clause;
            this.level = level;
        }
    }

    int[] factsLevels = new int[defSize];
    int[] clausesLevels = new int[defSize];

    boolean[] initialFacts = new boolean[defSize];
    int[][] clausesConditions = new int[defSize][];
    int[][] clausesEffects = new int[defSize][];
    int[] clausesIds = new int[defSize];

    /** What are the clauses in which this fact appears as a precondition */
    ArrayList<Integer>[] factsAppearance = new ArrayList[defSize];
    int[] clausesPendingCount = new int[defSize];

    ArrayList<Enabler>[] enablers = new ArrayList[defSize];

    public LeveledReasoner() {
        Arrays.fill(factsLevels, -1);
        Arrays.fill(clausesLevels, -1);
    }

    private int factsCapacity() { return factsLevels.length; }
    private int clausesCapacity() { return clausesConditions.length; }

    int nextClause = 0;

    private void increaseClauseSize() {
        int newSize = clausesLevels.length * 2;
        clausesLevels = Arrays.copyOf(clausesLevels, newSize);
        clausesConditions = Arrays.copyOf(clausesConditions, newSize);
        clausesEffects = Arrays.copyOf(clausesEffects, newSize);
        clausesIds = Arrays.copyOf(clausesIds, newSize);
        clausesPendingCount = Arrays.copyOf(clausesPendingCount, newSize);
    }
    private void increaseFactsSize() {
        int newSize = initialFacts.length*2;
        initialFacts = Arrays.copyOf(initialFacts, newSize);
        factsLevels = Arrays.copyOf(factsLevels, newSize);
        factsAppearance = Arrays.copyOf(factsAppearance, newSize);
        enablers = Arrays.copyOf(enablers, newSize);
    }

    public int addClause(int[] clauseConditions, int[] clauseEffects, int id) {
        if(nextClause >= clausesCapacity())
            increaseClauseSize();

        clausesConditions[nextClause] = clauseConditions;
        clausesEffects[nextClause] = clauseEffects;

        // make sure we ahve enough space
        for(int eff : clauseEffects)
            if(eff >= factsCapacity())
                increaseFactsSize();
        for(int cond : clauseConditions) {
            if(cond >= factsCapacity())
                increaseFactsSize();

            // record that this fact appears as a precondition of this clause
            if(factsAppearance[cond] == null)
                factsAppearance[cond] = new ArrayList<>();
            factsAppearance[cond].add(nextClause);
        }
        clausesPendingCount[nextClause] = clauseConditions.length;
        clausesIds[nextClause] = id;
        return nextClause++;
    }

    public void set(int fact) {
        if(fact < factsCapacity())
            increaseFactsSize();
        initialFacts[fact] = true;
        if(enablers[fact] == null)
            enablers[fact] = new ArrayList<>();
        enablers[fact].add(new Enabler(0, -1));
        factsLevels[fact] = 0;
    }

    public boolean[] getNextLevel(boolean[] facts, int lvl) {
        boolean[] nextLevel = Arrays.copyOfRange(facts, 0, facts.length);

        for(int i=0 ; i<facts.length ; i++) {
            if(facts[i] && (factsLevels[i] == lvl-1) && factsAppearance[i] != null) {
                assert enablers != null && !enablers[i].isEmpty();
                for(int clause : factsAppearance[i]) {
                    assert clausesPendingCount[clause] > 0;
                    clausesPendingCount[clause]--;
                    if(clausesPendingCount[clause] == 0) {
                        for(int eff : clausesEffects[clause]) {

                            if(enablers[eff] == null) enablers[eff] = new ArrayList<>();
                            enablers[eff].add(new Enabler(lvl, clause));
                            assert clausesLevels[clause] == -1;
                            clausesLevels[clause] = lvl;
                            factsLevels[eff] = factsLevels[eff] == -1 ? lvl : factsLevels[eff];
                            nextLevel[eff] = true;
                        }
                    }
                }
            }
        }
        return nextLevel;
    }

    private static int[] arr(int... values) {
        return values.clone();
    }

    public Collection<Integer> getPathTo(int fact) {
        Set<Integer> clauses = new HashSet<>();
        Queue<Integer> opened = new LinkedList<>();
        opened.add(fact);
        while(!opened.isEmpty()) {
            int next = opened.remove();
            if(factsLevels[next] == 0)
                continue;

            int bestClause = -1;
            int bestLevel = Integer.MAX_VALUE;
            for(Enabler e : enablers[next]) {
                if(clauses.contains(Integer.valueOf(e.clause))) {
                    bestClause = e.clause;
                    break;
                } else {
                    if(bestLevel > clausesLevels[e.clause]) {
                        bestLevel = clausesLevels[e.clause];
                        bestClause = e.clause;
                    }
                }
            }
            assert bestClause != -1;
            if(!clauses.contains(bestClause)) {
                for (int cond : clausesConditions[bestClause]) {
                    opened.add(cond);
                }
                clauses.add(bestClause);
            }
        }
        return clauses;
    }


    public static void main(String[] args) {
        LeveledReasoner r = new LeveledReasoner();
        r.addClause(arr(1,2), arr(3,6), 0);
        r.addClause(arr(4,5), arr(6), 1);
        r.addClause(arr(2,3), arr(4), 2);

        r.set(1);
        r.set(2);
        r.set(5);

        boolean[] lvl1 = r.getNextLevel(r.initialFacts, 1);

        boolean[] lvl2 = r.getNextLevel(lvl1, 2);
        boolean[] lvl3 = r.getNextLevel(lvl2, 3);

        System.out.println(lvl2);
        System.out.println(r.getPathTo(6));
    }
}
