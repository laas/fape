package fr.laas.fape.planning.core.inference;

import java.util.*;

@SuppressWarnings({"unchecked","rawtypes"})
public class LeveledReasoner {

    public static int defSize = 10;

    private boolean locked = false;

    class Enabler {
        final int level;
        final int clause;
        public Enabler(int level, int clause) {
            this.clause = clause;
            this.level = level;
        }
        public boolean isInitEnabler() { return clause == -1; }
    }

    int[] factsLevels = new int[defSize];
    int[] clausesLevels = new int[defSize];

    boolean[] initialFacts = new boolean[defSize];
    int[][] clausesConditions = new int[defSize][];
    int[][] clausesEffects = new int[defSize][];
    int[] clausesIds = new int[defSize];
    boolean[] allowedClauses = null;

    /** What are the clauses in which this fact appears as a precondition */
    ArrayList<Integer>[] factsAppearance = new ArrayList[defSize];
    int[] clausesPendingCount = new int[defSize];

    ArrayList<Enabler>[] enablers = new ArrayList[defSize];

    public LeveledReasoner() {
        Arrays.fill(factsLevels, -1);
        Arrays.fill(clausesLevels, -1);
    }

    public LeveledReasoner(LeveledReasoner toCopy, boolean[] allowedClauses) {
        toCopy.locked = true;
        nextClause = toCopy.nextClause;
        factsLevels = new int[toCopy.factsCapacity()];
        Arrays.fill(factsLevels, -1);
        clausesLevels = new int[toCopy.clausesCapacity()];
        Arrays.fill(clausesLevels, -1);
        initialFacts = new boolean[toCopy.factsCapacity()];
        clausesConditions = toCopy.clausesConditions;
        clausesEffects = toCopy.clausesEffects;
        clausesIds = toCopy.clausesIds;
        factsAppearance = toCopy.factsAppearance;
        clausesPendingCount = new int[toCopy.clausesCapacity()];
        enablers = new ArrayList[toCopy.factsCapacity()];
        this.allowedClauses = allowedClauses;
        for(int i=0 ; i<nextClause ; i++) {
            clausesPendingCount[i] = clausesConditions[i].length;
        }
    }

    private int factsCapacity() { return factsLevels.length; }
    private int clausesCapacity() { return clausesConditions.length; }

    int nextClause = 0;

    private void increaseClauseSize() {
        int prevSize = clausesLevels.length;
        int newSize = clausesLevels.length * 2;
        clausesLevels = Arrays.copyOf(clausesLevels, newSize);
        clausesConditions = Arrays.copyOf(clausesConditions, newSize);
        clausesEffects = Arrays.copyOf(clausesEffects, newSize);
        clausesIds = Arrays.copyOf(clausesIds, newSize);
        clausesPendingCount = Arrays.copyOf(clausesPendingCount, newSize);
        for(int i=prevSize ; i<newSize ; i++)
            clausesLevels[i] = -1;
    }
    private void ensureFactsCapacity(int atLeast) {
        if(factsCapacity() > atLeast)
            return;
        int prevSize = factsCapacity();
        int newSize = atLeast+1 > prevSize*2 ? atLeast +1 : prevSize*2;

        initialFacts = Arrays.copyOf(initialFacts, newSize);
        factsLevels = Arrays.copyOf(factsLevels, newSize);
        factsAppearance = Arrays.copyOf(factsAppearance, newSize);
        enablers = Arrays.copyOf(enablers, newSize);
        for(int i=prevSize ; i<newSize ; i++)
            factsLevels[i] = -1;
    }

    public int addClause(int[] clauseConditions, int[] clauseEffects, int id) {
        assert !locked : "Trying to add a clause to a locked LeveledReasoner.";
        if(nextClause >= clausesCapacity())
            increaseClauseSize();

        clausesConditions[nextClause] = clauseConditions;
        clausesEffects[nextClause] = clauseEffects;

        // make sure we have enough space
        for(int eff : clauseEffects)
            ensureFactsCapacity(eff);
        for(int cond : clauseConditions) {
            ensureFactsCapacity(cond);

            // record that this fact appears as a precondition of this clause
            if(factsAppearance[cond] == null)
                factsAppearance[cond] = new ArrayList<>();
            factsAppearance[cond].add(nextClause);
        }
        clausesPendingCount[nextClause] = clauseConditions.length;
        clausesIds[nextClause] = id;
        clausesLevels[nextClause] = -1;
        return nextClause++;
    }

    public void set(int fact) {
        ensureFactsCapacity(fact);
        initialFacts[fact] = true;
        if(enablers[fact] == null)
            enablers[fact] = new ArrayList<>();
        enablers[fact].add(new Enabler(0, -1));
        factsLevels[fact] = 0;
    }

    private boolean inferenceMade = true;
    public void infer() {
        int nextLevel = 1;
        boolean[] currentFacts = initialFacts;

        while(inferenceMade) {
            inferenceMade = false;
            currentFacts = getNextLevel(currentFacts, nextLevel);
            nextLevel++;
        }
    }

    private boolean allowed(int clauseID) {
        return allowedClauses == null || allowedClauses[clauseID];
    }

    protected boolean[] getNextLevel(boolean[] facts, int lvl) {
        boolean[] nextLevel = Arrays.copyOfRange(facts, 0, facts.length);
        assert lvl > 0;

        // deal with clauses with no conditions
        if(lvl == 1) {
            for (int clause = 0; clause < nextClause; clause++) {
                if (clausesConditions[clause].length == 0 && allowed(clause)) {
                    clausesLevels[clause] = 1;
                    for(int eff : clausesEffects[clause]) {
                        if(enablers[eff] == null)
                            enablers[eff] = new ArrayList<>();

                        enablers[eff].add(new Enabler(lvl, clause));

                        factsLevels[eff] = factsLevels[eff] == -1 ? lvl : factsLevels[eff];
                        nextLevel[eff] = true;
                        inferenceMade = true;
                    }
                }
            }
        }

        for(int i=0 ; i<facts.length ; i++) {
            if(facts[i] && (factsLevels[i] == lvl-1) && factsAppearance[i] != null) {
                assert enablers != null && !enablers[i].isEmpty();
                for(int clause : factsAppearance[i]) {
                    assert clausesPendingCount[clause] > 0;
                    clausesPendingCount[clause]--;
                    if(clausesPendingCount[clause] == 0 && allowed(clause)) {

                        // just became true, set clause level
                        assert clausesLevels[clause] == -1;
                        clausesLevels[clause] = lvl;

                        for(int eff : clausesEffects[clause]) {
                            if(enablers[eff] == null)
                                enablers[eff] = new ArrayList<>();

                            enablers[eff].add(new Enabler(lvl, clause));

                            factsLevels[eff] = factsLevels[eff] == -1 ? lvl : factsLevels[eff];
                            nextLevel[eff] = true;
                            inferenceMade = true;
                        }
                    }
                }
            }
        }
        return nextLevel;
    }

    public int levelOfClause(int clause) {
        return clausesLevels[clause];
    }

    public int levelOfFact(int fact) {
        return factsLevels[fact];
    }

    public Collection<Integer> getPathTo(int fact) {
        return getPathTo(fact, new HashSet<Integer>());
    }

    public Collection<Integer> getPathTo(int fact, Collection<Integer> alreadyAchievedClauses) {
        assert levelOfFact(fact) != -1 : "Requesting path to a fact that is not true.";
        Set<Integer> clauses = new HashSet<>(alreadyAchievedClauses);
        Queue<Integer> opened = new LinkedList<>();
        opened.add(fact);
        while(!opened.isEmpty()) {
            int next = opened.remove();
            if(factsLevels[next] == 0)
                continue;

            int bestClause = -1;
            int bestLevel = Integer.MAX_VALUE;
            for(Enabler e : enablers[next]) {
                if(clauses.contains(e.clause)) {
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
}
