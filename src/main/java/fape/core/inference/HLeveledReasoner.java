package fape.core.inference;

import java.util.*;

public class HLeveledReasoner<Clause, Fact> {

    private Map<Clause,Integer> clausesIds = new HashMap<>();
    private ArrayList<Clause>   clauses    = new ArrayList<>();

    private Map<Fact, Integer>  factsIds   = new HashMap<>();
    private ArrayList<Fact>     facts      = new ArrayList<>();

    int nextFact = 0;
    int nextClause = 0;

    LeveledReasoner lr = new LeveledReasoner();

    public void set(Fact f) {
        assert factsIds.containsKey(f);
        lr.set(factsIds.get(f));
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
}
