package fape.core.inference;

import fape.exceptions.NoSolutionException;

import java.util.*;

public class HLeveledReasoner<Clause, Fact> {

    private Map<Clause,Integer> clausesIds = new HashMap<>();
    private ArrayList<Clause>   clauses    = new ArrayList<>();

    private Map<Fact, Integer>  factsIds   = new HashMap<>();
    private ArrayList<Fact>     facts      = new ArrayList<>();

    int nextFact = 0;
    int nextClause = 0;

    public HLeveledReasoner() {}

    public HLeveledReasoner(HLeveledReasoner<Clause,Fact> toClone, Collection<Clause> allowed) {
        this.clausesIds = toClone.clausesIds;
        this.clauses = toClone.clauses;
        this.facts = toClone.facts;
        this.factsIds = toClone.factsIds;
        boolean[] allowedClause = null;
        if(allowed != null) {
            allowedClause = new boolean[toClone.lr.nextClause];
            for(Clause cl : allowed) {
                allowedClause[clausesIds.get(cl)] = true;
            }
        }
        this.lr = new LeveledReasoner(toClone.lr, allowedClause);
    }

    public HLeveledReasoner<Clause,Fact> clone() { return new HLeveledReasoner<>(this, null); }

    LeveledReasoner lr = new LeveledReasoner();

    /** Set this fact to true. If the fact is not known, nothing is done */
    public void set(Fact f) {
        if(factsIds.containsKey(f))
            lr.set(factsIds.get(f));
    }

    /** Returns true if this fact has been recorded (part of a clause previously added). */
    public boolean knowsFact(Fact f) {
        return factsIds.containsKey(f);
    }

    public void addClause(Fact[] conditions, Fact[] effects, Clause clause) {
        addClause(Arrays.asList(conditions), Arrays.asList(effects), clause);
    }

    public void infer() { lr.infer(); }

    public void addClause(List<Fact> conditions, List<Fact> effects, Clause clause) {
        int[] conds = new int[conditions.size()];
        for(int i=0 ; i<conditions.size() ; i++) {
            Fact f = conditions.get(i);
            if(!factsIds.containsKey(f)) {
                factsIds.put(f, nextFact);
                facts.add(f);
                assert facts.get(nextFact) == f;
                nextFact++;
            }
            conds[i] = factsIds.get(conditions.get(i));
        }
        int[] effs = new int[effects.size()];
        for(int i=0 ; i<effects.size() ; i++) {
            Fact f = effects.get(i);
            if(!factsIds.containsKey(f)) {
                factsIds.put(f, nextFact);
                facts.add(f);
                assert facts.get(nextFact) == f;
                nextFact++;
            }
            effs[i] = factsIds.get(effects.get(i));
        }
        assert !clausesIds.containsKey(clause);
        clausesIds.put(clause, nextClause);
        clauses.add(clause);
        assert clauses.get(nextClause) == clause;
        nextClause++;

        int cId = lr.addClause(conds, effs, clausesIds.get(clause));
        assert cId == clausesIds.get(clause);
    }

    public Collection<Clause> getSteps(Fact f) {
        assert factsIds.containsKey(f);
        Collection<Integer> pathWithIds = lr.getPathTo(factsIds.get(f));
        Collection<Clause> path = new LinkedList<>();
        for(Integer i : pathWithIds) {
            path.add(clauses.get(i));
        }
        return path;
    }

    public Collection<Clause> getStepsToAnyOf(Collection<Fact> disjunctionOfFacts, Collection<Clause> alreadyUsedClauses) throws NoSolutionException {
        List<Integer> disFacts = new LinkedList<>();
        for(Fact f : disjunctionOfFacts) {
//            assert factsIds.containsKey(f);
            if(factsIds.containsKey(f))
                disFacts.add(factsIds.get(f));
        }
        if(disFacts.isEmpty())
            throw new NoSolutionException("");

        List<Integer> usedClauses = new LinkedList<>();
        for(Clause c : alreadyUsedClauses) {
            assert clausesIds.containsKey(c);
            usedClauses.add(clausesIds.get(c));
        }
        Collection<Integer> stepsIds = lr.getPathToAnyOf(disFacts, usedClauses);
        List<Clause> steps = new LinkedList<>();
        for(Integer c : stepsIds) {
            steps.add(clauses.get(c));
        }
        return steps;
    }

    /**
     * Returns all possible clause achieving this fact.
     * The value null means that it was set as an initial fact.
     * @throws NoSolutionException If no clause ever achieve this fact. (this means its level is -1)
     */
    public Collection<Clause> candidatesFor(Fact f) throws NoSolutionException {
        List<Clause> candidates = new LinkedList<>();
        for(LeveledReasoner.Enabler enabler : lr.enablers[factsIds.get(f)]) {
            if(enabler.isInitEnabler()) {
                candidates.add(null);
            } else {
                candidates.add(clauses.get(enabler.clause));
            }
        }
        if(candidates.isEmpty())
            throw new NoSolutionException("");
        return candidates;
    }

    /** Returns all conditions of this clause */
    public Collection<Fact> conditionsOf(Clause clause) {
        assert clause != null;
        List<Fact> conditions = new LinkedList<>();
        for(int f : lr.clausesConditions[clausesIds.get(clause)]) {
            conditions.add(facts.get(f));
        }
        return conditions;
    }

    /**
     *  Returns the minimum level in which this facts appears
     * 0 meaning it is initially set. -1 meaning it was not inferred
     */
    public int levelOfFact(Fact f) {
        assert f != null && knowsFact(f);
        return lr.levelOfFact(factsIds.get(f));
    }

    /**
     *  Returns the minimum level in which this clause is valid
     *  -1 meaning it is never valid
     */
    public int levelOfClause(Clause c) {
        assert c != null && clausesIds.containsKey(c);
        return lr.levelOfClause(clausesIds.get(c));
    }

    public String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("Facts: (lvl - fact):\n");
        int currentLvl = -1;
        while(currentLvl < 20) {
            for (Fact f : factsIds.keySet()) {
                if(levelOfFact(f) == currentLvl) {
                    sb.append(currentLvl);
                    sb.append(" ");
                    sb.append(f);
                    sb.append("\n");
                }
            }
            currentLvl++;
        }
        sb.append("\n\nClauses: (lvl - clause");
        currentLvl = -1;
        while(currentLvl < 20) {
            for (Clause c : clausesIds.keySet()) {
                if(levelOfClause(c) == currentLvl) {
                    sb.append(currentLvl);
                    sb.append(" ");
                    sb.append(c);
                    sb.append("\n");
                }
            }
            currentLvl++;
        }
        return sb.toString();
    }
}
