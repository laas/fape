package fr.laas.fape.constraints.bindings;


import java.util.*;

/**
 * Represents a n-ary constraint defined in extension.
 *
 * It consists of a n-tuple of variables and a set of n-tuples of values.
 */
public class ExtensionConstraint {

    final String name;

    /** A set of n-tuples of values { <a1,b1,c1>,<a2,b2,c2> }.
     * If a tuple of variable <A,B,C> are constrained this, their final value must match one of those
     * tuples of values.
     */
    public int[][] bindings = new int[100][];
    final int numVariables;
    int numBindings = 0;

    public final boolean isLastVarInteger;

    public ExtensionConstraint(String name, boolean isLastVarInteger, int numVariables) {
        this.name = name;
        this.isLastVarInteger = isLastVarInteger;
        this.numVariables = numVariables;
    }

    public boolean isEmpty() { return numBindings == 0; }

    /** Number of variables involved in this constraint */
    public int numVars() {
        return numVariables;
    }

    /** Matches the possible values of a variable to the bindings they appear in. For instance:
     * [ { a1 -> {0,1}, a2 -> {2,3} },
     *   { b1 -> {0,2}, b2 -> {1,3} } ]
     *  means:
     *   - the value a1 of the first variable appears in the 0th and 1st bindings
     *   - the value a2 of the first variable appears in the 2nd and 3rd bindings
     *   - the value b1 of the second variable appears in the 0th and 2nd bindings
     *   - the value b2 of the second variable appears in the 1st and 3rd bindings
     */
    LinkedList<Map<Integer, BitSet>> relevantConstraints = new LinkedList<>();

    /** Add a possible binding <a,b,c...> to this constraint */
    public void addValues(List<Integer> vals) {
        assert vals.size() == numVariables;

        if(numBindings == bindings.length)
            bindings = Arrays.copyOf(bindings, bindings.length*2);

        int[] valsArray = new int[vals.size()];
        for(int i=0 ; i<vals.size() ; i ++)
            valsArray[i] = vals.get(i);
        this.bindings[numBindings++] = valsArray;

        if(relevantConstraints.size() == 0) {
            for(int i=0 ; i<vals.size() ; i++)
                relevantConstraints.add(new HashMap<Integer, BitSet>());
        }

        for(int i=0 ; i<vals.size() ; i++) {
            if(!relevantConstraints.get(i).containsKey(vals.get(i)))
                relevantConstraints.get(i).put(vals.get(i), new BitSet());
            relevantConstraints.get(i).get(vals.get(i)).set(numBindings-1);
        }
    }

    /**
     * @param domains Initial domains of the variables.
     * @return Domains restricted do values that can fulfill at least one complete binding.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public Set<Integer>[] restrictedDomains(Set<Integer>[] domains) {
        assert domains.length == numVars();

        if(numBindings == 0) { // no possible value in this constraint, all domains are empty
            Set<Integer>[] emptyDoms = new Set[domains.length];
            for(int i=0 ; i<emptyDoms.length ; i++)
                emptyDoms[i] = new HashSet<>();
            return emptyDoms;
        }


        // will contain a booleans stating if the ith binding is valid according to the given domains
        BitSet toConsider = new BitSet(numBindings);
        // at first they are all interesting
        toConsider.set(0, numBindings);

        for(int var=0 ; var<domains.length ; var++) {
            // for all variables
            BitSet local = new BitSet(numBindings);
            for(int val : domains[var]) {
                if(relevantConstraints.get(var).containsKey(val)) {
                    // add all values that are flagged are relevant for the (var, val) pair
                    local.or(relevantConstraints.get(var).get(val));
                }
            }
            // relevant constraints are those that were flagged interesting for this variable and all previous ones
            toConsider.and(local);
        }

        Set<Integer>[] finalDomains = new Set[domains.length];
        for(int i=0 ; i<finalDomains.length ; i++)
            finalDomains[i] = new HashSet<>();

        // get all the valid bindings and augment the domain with those
        for(int bindingId=0 ; bindingId<numBindings ; bindingId++) {
            if(toConsider.get(bindingId)) {
                for(int var=0 ; var<numVars() ; var++)
                    finalDomains[var].add(bindings[bindingId][var]);
            }
        }

        return finalDomains;
    }
}