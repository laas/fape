package fr.laas.fape.planning.core.inference;

import fr.laas.fape.planning.core.planning.heuristics.DefaultIntRepresentation;
import fr.laas.fape.planning.core.planning.heuristics.IntRepresentation;
import fr.laas.fape.planning.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class HReasoner<T> {
    private static final int baseNumVars = 100;
    private static final int baseNumClause = 1000;

    /** This allows a one to one mapping between terms and integers. Those are used for faster access using primitive type. */
    private final IntRepresentation<T> termIntRep;

    /** Maps a termID to its identifier into its identifier in the underlying reasoner */
    private int[] varsIds;

    /** Maps a identifiers of the underlying reasoner into termIDs. */
    private int[] vars;
    private final Reasoner res;

    private int numVars = 0;

    private boolean locked = false;

    public HReasoner(IntRepresentation<T> termIntRep) {
        this.termIntRep = termIntRep;
        vars = new int[baseNumVars];
        varsIds = new int[100];
        Arrays.fill(varsIds, -1);
        res = new Reasoner(baseNumVars, baseNumClause);
    }

    public HReasoner(HReasoner<T> toCopy, boolean lock) {
        locked = lock;
        this.termIntRep = toCopy.termIntRep;
        this.numVars = toCopy.numVars;
        this.res = new Reasoner(toCopy.res, lock);

        if(locked && toCopy.locked) {
            this.varsIds = toCopy.varsIds;
            this.vars = toCopy.vars;
        } else {
            this.varsIds = Arrays.copyOf(toCopy.varsIds, toCopy.numVars);
            this.vars = Arrays.copyOfRange(toCopy.vars, 0, numVars);
        }
    }

    public void lock() {
        this.locked = true;
        res.lock();
    }

    public final boolean hasTerm(T term) {
        return termIntRep.hasRepresentation(term) && varsIds[termIntRep.asInt(term)] != -1;
    }

    @SafeVarargs
    public final void addHornClause(final T left, final T... right) {
        assert !locked;
        addVarIfAbsent(left);
        int[] rightIds = new int[right.length];
        for(int i=0 ; i<right.length ; i++) {
            addVarIfAbsent(right[i]);
            rightIds[i] = inReasonerID(right[i]);
        }

        res.addHornClause(inReasonerID(left), rightIds);
    }

    public void set(T o) {
        if(!hasTerm(o))
            addVar(o);
        res.set(inReasonerID(o));
    }

    public Collection<T> trueFacts() {
        List<T> facts = new ArrayList<>();
        for(int var=0 ; var< numVars; var++) {
            if(res.varsStatus[var]) {
                facts.add(fromReasonerID(var));
            }
        }
        return facts;
    }

    public boolean isTrue(T term) {
        assert hasTerm(term) : "Term "+term+" is not a recorded variable.";
        return res.varsStatus[inReasonerID(term)];
    }

    /** Returns the identifier of a term in the underlying reasoner */
    private int inReasonerID(T o) {
        return varsIds[termIntRep.asInt(o)];
    }

    private T fromReasonerID(int reasonerTermID) {
        return termIntRep.fromInt(vars[reasonerTermID]);
    }

    public void addVarIfAbsent(T term) {
        if(!hasTerm(term))
            addVar(term);
    }

    public void addVar(T o) {
        assert !locked : "Error adding variable to locked Reasoner: "+o;
        assert !hasTerm(o) : "Var already registered";
        if(vars.length <= numVars) {
            vars = Arrays.copyOf(vars, vars.length*2);
        }
        int id = numVars++;
        vars[id] = termIntRep.asInt(o);
        final int intRepOfTerm = termIntRep.asInt(o);
        if(intRepOfTerm >= varsIds.length) {
            varsIds = Utils.copyIntoBigger(varsIds, Math.max(intRepOfTerm, varsIds.length*2), -1);
        }
        varsIds[termIntRep.asInt(o)] =  id;
        res.addVar(id);
    }


    public static void main(String[] args) {
        HReasoner<String> res = new HReasoner<>(new DefaultIntRepresentation<String>());
        res.addHornClause("A", "a", "b");
        res.addHornClause("c", "A");
        res.addHornClause("B", "c", "b");
        res.addHornClause("d", "B");

        System.out.println(res.trueFacts());
        res.set("c");
        System.out.println(res.trueFacts());
        res.set("b");
        System.out.println(res.trueFacts());
        res.set("e");
    }
}
