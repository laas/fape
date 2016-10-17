package fr.laas.fape.planning.core.planning.grounding;

import fr.laas.fape.anml.model.concrete.InstanceRef;
import fr.laas.fape.planning.exceptions.FAPEException;
import fr.laas.fape.anml.model.LVarRef;

import java.util.*;

/**
 * This class is used to represent partial bindings of a set of variables.
 * It is used to take into account static constraint on variable instantiations, those only generatting
 * bindings that are likely to be valid.
 */
public class PartialBindings {
    /** All vars for which a value will be needed */
    final LVarRef[] allVars;

    /** All vars for which we provide values. Those are called focus vars. */
    LVarRef[] focusedVars;

    /** gives the position of focused vars into the allVars array. */
    int[] focusedVarsPositions;

    /** domain of each variable in allVars, in the same order. */
    final List<List<InstanceRef>> possibleValues;

    /** a list of bindings {[null, b1, c1, null], [null,b1,c2,null]}
     * Assuming that all vars = [A,B,C,D], it gives the possible bindings of (B,C), the values of
     * A and D are left unconstrained. in this example, B and C are the focused vars. */
    List<InstanceRef[]> partialBindings = new LinkedList<>();

    /** contains the positions of variable that are equals to each other */
    final List<SortedSet<Integer>> equalities = new LinkedList<>();

    /**
     *
     * @param allVars All vars that need to be binded.
     * @param focusedVars Vars for which this PartialBindings will provide values, focused vars are a subset of allVars.
     * @param possibleValues Associates a domain for each var in allVars (in the same order).
     */
    public PartialBindings(LVarRef[] allVars, LVarRef[] focusedVars, List<List<InstanceRef>> possibleValues) {
        this.allVars = allVars;
        this.focusedVars = focusedVars;
        this.possibleValues = possibleValues;

        assert focusedVars.length <= allVars.length;

        // get the positions of the focused vars in the allVars array.
        focusedVarsPositions = new int[focusedVars.length];
        for(int i=0 ; i< focusedVars.length ; i++) {
            focusedVarsPositions[i] = -1;
            for(int j=0 ; j<allVars.length ; j++)
                if(focusedVars[i].equals(allVars[j]))
                    focusedVarsPositions[i] = j;
            assert focusedVarsPositions[i] != -1;
        }

        if(focusedVars.length == 0)
            // no focus vars, add an empty binding [null,null,...]
            partialBindings.add(new InstanceRef[allVars.length]);
    }

    /** Adds a possible value sequence for all focus variables of this partial binding */
    public void addPartialBinding(InstanceRef[] myValues, GroundProblem pb) {
        assert myValues.length == focusedVars.length;
        InstanceRef[] binding = new InstanceRef[allVars.length];
        for(int i=0 ; i<myValues.length ; i++) {
            if(focusedVarsPositions[i] == -1) {
                if(pb.liftedPb.instance(focusedVars[i].id()).equals(myValues[i]))
                    continue;
                else
                    return;
            }
            binding[focusedVarsPositions[i]] = myValues[i];
        }
        partialBindings.add(binding);
    }

    public void bind(LVarRef var, InstanceRef value) {
        if(focusesOn(var)) {
            List<InstanceRef[]> toRemove = new LinkedList<>();
            int posV = pos(var);
            for(InstanceRef[] binding : partialBindings) {
                if(!binding[posV].equals(value))
                    toRemove.add(binding);
            }
            partialBindings.removeAll(toRemove);
        } else {
            addVar(var);
            int posV = pos(var);
            for(InstanceRef[] binding : partialBindings) {
                assert binding[posV] == null;
                binding[posV] = value;
            }
        }
    }

    /**
     * Merges the given partial binding into this one.
     * Hence, this PartialBindings will contain all constraints given by the other.
     */
    public void merge(PartialBindings o) {
        for(LVarRef oVar : o.focusedVars)
            if(!focusesOn(oVar))
                addVar(oVar);
        List<InstanceRef[]> newBindings = new LinkedList<>();
        for(InstanceRef[] myBinding : partialBindings) {
            for(InstanceRef[] oBinding : o.partialBindings) {
                InstanceRef[] newBinding = Arrays.copyOf(myBinding, myBinding.length);
                boolean valid = true;
                for(int i=0 ; i<newBinding.length ; i++) {
                    if(newBinding[i] == null) { // (null, x) -> x
                        newBinding[i] = oBinding[i];
                    } else if(oBinding[i] != null && !oBinding[i].equals(newBinding[i])) { // (x,y) -> invalid
                        valid = false;
                        break;
                    } // else (x, null) ou (x, x) -> x (already there)
                }
                if(valid)
                    newBindings.add(newBinding);
            }
        }

        partialBindings = newBindings;

        // get pending equalities from both and add them again
        equalities.addAll(o.equalities);
        processEqualities();
    }

    /**
     * Tries to enforce pending equalities constraints into the partial bindings.
     */
    private void processEqualities() {
        List<SortedSet<Integer>> eqSets = new LinkedList<>(equalities);
        equalities.clear();

        boolean integration = false;

        for(SortedSet<Integer> eqSet : eqSets) {
            // all variables in this set must be equals
            // look for a variable that is focused
            LVarRef pivot = null;
            for(int i : eqSet) {
                if(focusesOn(allVars[i]))
                    pivot = allVars[i];
            }

            if(pivot == null)
                // no variables in focus, just take the first
                pivot = allVars[eqSet.first()];

            for(int i : eqSet) {
                // enforce equality for all pars containing the pivot
                LVarRef o = allVars[i];
                if(!o.equals(pivot)) {
                    // remembers if the equality constraint was integrated
                    integration |= addEquality(pivot, o);
                }
            }
        }
        if(integration)
            processEqualities();
    }

    /** Gives the position of a variable in the array of all variables */
    private int pos(LVarRef v) {
        for(int i=0 ; i<allVars.length ; i++)
            if(allVars[i].equals(v))
                return i;
        throw new FAPEException("Error: unable to find var: "+v);
    }

    /**
     * Adds a new variable to focusedVars and focusedVarsPositions. This is used to provide a new focus var for
     * Which values will be present in the partial bindings
     */
    private void addVar(LVarRef v) {
        assert !focusesOn(v);
        focusedVars = Arrays.copyOf(focusedVars, focusedVars.length+1);
        focusedVarsPositions = Arrays.copyOf(focusedVarsPositions, focusedVarsPositions.length+1);
        focusedVars[focusedVars.length-1] = v;
        focusedVarsPositions[focusedVars.length-1] = pos(v);
        assert focusedVars.length <= allVars.length;
    }

    /** Enforces an equality constraint, returns true if it was integrated in the partialBindings.
     * Returns True if the variable was directly included, false if it was recorded for later. */
    public boolean addEquality(LVarRef a, LVarRef b) {
        if(focusesOn(a) && focusesOn(b)) {
            // a and b both have values in the partial bindings, remove any partial binding where a != b
            int posA = pos(a);
            int posB = pos(b);
            List<InstanceRef[]> toRemove = new LinkedList<>();
            for(InstanceRef[] binding : partialBindings) {
                if(!binding[posA].equals(binding[posB]))
                    toRemove.add(binding);
            }
            partialBindings.removeAll(toRemove);
            return true;
        } else if(focusesOn(a)) {
            // extend the current partial bindings with a new variable b whose value is always equal to a
            addVar(b);
            int posA = pos(a);
            int posB = pos(b);
            for(InstanceRef[] binding : partialBindings) {
                assert binding[posB] == null;
                binding[posB] = binding[posA];
            }
            return true;
        } else if(focusesOn(b)) {
            return addEquality(b, a);
        } else {
            // neitheir a or b have values in the partial bindings, record this constraint for later.
            int posA = pos(a);
            int posB = pos(b);
            boolean added = false;
            for(SortedSet<Integer> equalitySet : equalities) {
                if(equalitySet.contains(posA) || equalitySet.contains(posB)) {
                    equalitySet.add(posA);
                    equalitySet.add(posB);
                    added = true;
                    break;
                }
            }
            if(!added) {
                SortedSet<Integer> newEqualitySet = new TreeSet<>();
                newEqualitySet.add(posA);
                newEqualitySet.add(posB);
                equalities.add(newEqualitySet);
            }
            return false;
        }
    }

    /** Returns true if this variable is provided a value in the partial bindings */
    public boolean focusesOn(LVarRef v) {
        for(LVarRef v2 : focusedVars)
            if(v2.equals(v))
                return true;
        return false;
    }

    /** Gives the number of bindings that would be generated from this PartialBindings.
     * Note that pending equalities are not accounted in this calculus. */
    public int numPossibleBinding() {
        int n = 1;
        if(focusedVars.length > 0)
            n = n*partialBindings.size();
        for(int i=0 ; i<allVars.length ; i++) {
            if(!focusesOn(allVars[i]))
                n = n * possibleValues.get(i).size();
        }
        return n;
    }

    /**
     * Returns an array representing the different equalities constraints.
     * The value at index i in this array give the position of the variable that allVars[i] is equal to.
     * Hence: array[i] != -1 implies allVars[i] === allVars[array[i]]
     *
     * [-1, 0, -1, 1] means allVars(1) = allVars(0) and allVars(3) = allVars(1)
     */
    private int[] equalitiesPositions() {
        processEqualities();

        // [-1, 0, -1, 1] means v(1) = v(0) and v(3) = v(1)
        // hence it is the index of the variable it is equal to
        int[] equalitiesPos = new int[allVars.length];
        for(int i=0 ; i<equalitiesPos.length ; i++)
            equalitiesPos[i] = -1;

        for(SortedSet<Integer> eqSet : equalities) {
            int prev = -1;
            for(int cur : eqSet) {
                assert equalitiesPos[cur] == -1;
                if(prev != -1)
                    equalitiesPos[cur] = prev;
                prev = cur;
            }
        }
        return equalitiesPos;
    }

    /**
     * Get all possible instantiations fulfiling this partial bindings.
     * This is done by giving a value to all free variables.
     */
    public List<InstanceRef[]> instantiations() {
        int[] eqs = equalitiesPositions();

        List<InstanceRef[]> currentBindings = new LinkedList<>(partialBindings);
        for(int i=0 ; i<allVars.length ; i++) {
            if(focusesOn(allVars[i]))
                continue; // already contained in initial bindings
            List<InstanceRef[]> extendedBindings = new LinkedList<>();
            for(InstanceRef[] currentBinding : currentBindings) {
                // current binding is the possible bindings of all variables before i
                if(eqs[i] != -1) {
                    assert eqs[i] < i;
                    // variable at i is equal to variable at eqs[i], only one binding possible
                    InstanceRef[] extendedBinding = Arrays.copyOf(currentBinding, currentBinding.length);
                    extendedBinding[i] = extendedBinding[eqs[i]];
                    assert extendedBinding[i] != null;
                    extendedBindings.add(extendedBinding);
                } else {
                    // no equality constraints, create a new extendedBinding for each value in the domain of allVars[i]
                    for (InstanceRef val : possibleValues.get(i)) {
                        InstanceRef[] extendedBinding = Arrays.copyOf(currentBinding, currentBinding.length);
                        extendedBinding[i] = val;
                        extendedBindings.add(extendedBinding);
                    }
                }
            }
            //  bindings of all variables until i
            currentBindings = extendedBindings;
        }
        return currentBindings;
    }

}
