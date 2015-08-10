package fape.core.inference;

import fape.core.planning.heuristics.DefaultIntRepresentation;
import fape.core.planning.heuristics.IntRepresentation;
import fape.exceptions.NoSolutionException;
import fape.util.Utils;

import javax.rmi.CORBA.Util;
import java.util.*;

public class HLeveledReasoner<Clause, Fact> {

    private int[] clausesIds;
    private int[] clauses;

    private int[]  factsIds;
    private int[] facts;

    int nextFact = 0;
    int nextClause = 0;

    private final IntRepresentation<Clause> clauseIntRep;
    private final IntRepresentation<Fact>   factIntRep;

    public HLeveledReasoner() {
        clausesIds = Utils.copyIntoBigger(new int[0], 100, -1);
        clauses = Utils.copyIntoBigger(new int[0], 100, -1);
        factsIds = Utils.copyIntoBigger(new int[0], 100, -1);
        facts = Utils.copyIntoBigger(new int[0], 100, -1);
        clauseIntRep = new DefaultIntRepresentation<>();
        factIntRep = new DefaultIntRepresentation<>();
    }

    public HLeveledReasoner(HLeveledReasoner<Clause,Fact> toClone, Collection<Clause> allowed) {
        this.clausesIds = toClone.clausesIds;
        this.clauses = toClone.clauses;
        this.clauseIntRep = toClone.clauseIntRep;
        this.factIntRep = toClone.factIntRep;
        this.facts = toClone.facts;
        this.factsIds = toClone.factsIds;
        boolean[] allowedClause = null;
        if(allowed != null) {
            allowedClause = new boolean[toClone.lr.nextClause];
            for(Clause cl : allowed) {
                allowedClause[inReasonerClauseId(cl)] = true;
            }
        }
        this.lr = new LeveledReasoner(toClone.lr, allowedClause);
    }

    public HLeveledReasoner<Clause,Fact> clone() { return new HLeveledReasoner<>(this, null); }

    private int inReasonerClauseId(Clause cl) { return clausesIds[clauseIntRep.asInt(cl)]; }
    private int inReasonerFactId(Fact f) { return factsIds[factIntRep.asInt(f)]; }
    private Clause clauseFromReasonerId(int clauseReasID) { return clauseIntRep.fromInt(clauses[clauseReasID]); }
    private Fact factFromReasId(int factReasID) { return factIntRep.fromInt(facts[factReasID]); }

    LeveledReasoner lr = new LeveledReasoner();

    /** Set this fact to true. If the fact is not known, nothing is done */
    public void set(Fact f) {
        if(knowsFact(f))
            lr.set(inReasonerFactId(f));
    }

    /** Returns true if this fact has been recorded (part of a clause previously added). */
    public boolean knowsFact(Fact f) {
        return factIntRep.hasRepresentation(f) && factIntRep.asInt(f) < factsIds.length && factsIds[factIntRep.asInt(f)] != -1;
    }

    public boolean knowsClause(Clause cl) {
        return clauseIntRep.asInt(cl) < clausesIds.length && clausesIds[clauseIntRep.asInt(cl)] != -1;
    }

    public void addClause(Fact[] conditions, Fact[] effects, Clause clause) {
        addClause(Arrays.asList(conditions), Arrays.asList(effects), clause);
    }

    private void addFactIfUnknown(Fact f) {
        if(!knowsFact(f)) {
            final int fact = factIntRep.asInt(f);
            if(fact >= factsIds.length)
                factsIds = Utils.copyIntoBigger(factsIds, Math.max(fact+1, factsIds.length*2), -1);
            factsIds[fact] = nextFact;
            if(nextFact >= facts.length)
                facts = Utils.copyIntoBigger(facts, facts.length*2, -1);
            facts[nextFact++] = fact;
        }
    }

    private void recordClause(Clause cl) {
        final int clause = clauseIntRep.asInt(cl);
        assert clauseIntRep.asInt(cl) >= clausesIds.length || clausesIds[clauseIntRep.asInt(cl)] == -1;
        if(clause >= clausesIds.length)
            clausesIds = Utils.copyIntoBigger(clausesIds, Math.max(clause+1, clausesIds.length*2), -1);
        clausesIds[clause] = nextClause;
        if(nextClause >= clauses.length)
            clauses = Utils.copyIntoBigger(clauses, clauses.length*2, -1);
        clauses[nextClause++] = clause;

    }

    public void infer() { lr.infer(); }

    public void addClause(List<Fact> conditions, List<Fact> effects, Clause clause) {
        int[] conds = new int[conditions.size()];
        for(int i=0 ; i<conditions.size() ; i++) {
            Fact f = conditions.get(i);
            addFactIfUnknown(f);
            conds[i] = inReasonerFactId(f);
        }
        int[] effs = new int[effects.size()];
        for(int i=0 ; i<effects.size() ; i++) {
            Fact f = effects.get(i);
            addFactIfUnknown(f);
            effs[i] = inReasonerFactId(f);
        }
        recordClause(clause);

        int cId = lr.addClause(conds, effs, inReasonerClauseId(clause));
        assert cId == inReasonerClauseId(clause);
    }

    public Collection<Clause> getSteps(Fact f) {
        assert knowsFact(f);
        Collection<Integer> pathWithIds = lr.getPathTo(inReasonerFactId(f));
        Collection<Clause> path = new LinkedList<>();
        for(Integer i : pathWithIds) {
            path.add(clauseFromReasonerId(i));
        }
        return path;
    }

    /**
     * Returns all possible clause achieving this fact.
     * The value null means that it was set as an initial fact.
     * @throws NoSolutionException If no clause ever achieve this fact. (this means its level is -1)
     */
    public Collection<Clause> candidatesFor(Fact f) throws NoSolutionException {
        List<Clause> candidates = new LinkedList<>();
        for(LeveledReasoner.Enabler enabler : lr.enablers[inReasonerFactId(f)]) {
            if(enabler.isInitEnabler()) {
                candidates.add(null);
            } else {
                candidates.add(clauseFromReasonerId(enabler.clause));
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
        for(int f : lr.clausesConditions[inReasonerClauseId(clause)]) {
            conditions.add(factFromReasId(f));
        }
        return conditions;
    }

    /**
     *  Returns the minimum level in which this facts appears
     * 0 meaning it is initially set. -1 meaning it was not inferred
     */
    public int levelOfFact(Fact f) {
        assert f != null && knowsFact(f);
        return lr.levelOfFact(inReasonerFactId(f));
    }

    /**
     *  Returns the minimum level in which this clause is valid
     *  -1 meaning it is never valid
     */
    public int levelOfClause(Clause c) {
        assert c != null && knowsClause(c);
        return lr.levelOfClause(inReasonerClauseId(c));
    }
/*
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
    }*/
}
