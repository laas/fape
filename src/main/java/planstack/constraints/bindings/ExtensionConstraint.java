package planstack.constraints.bindings;


import java.util.*;

/**
 * Represents a n-ary constraint defined in extension.
 *
 * It consists of a n-tuple of variables and a set of n-tuples of values.
 */
public class ExtensionConstraint {

    public int[][] bindings = new int[100][];
    int numBindings = 0;

    public final boolean isLastVarInteger;

    public ExtensionConstraint(boolean isLastVarInteger) {
        this.isLastVarInteger = isLastVarInteger;
    }

    public ExtensionConstraint DeepCopy() {
        return this;
    }

    public boolean isEmpty() { return numBindings == 0; }

    public int numVars() {
        assert numBindings > 0;
        return bindings[0].length;
    }

    LinkedList<Map<Integer, BitSet>> relevantConstraints = new LinkedList<>();

    public void addValues(List<Integer> vals) {
        assert numBindings == 0 || bindings[0].length == vals.size();

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

    public Set<Integer> valuesUnderRestriction(int wanted, Set<Integer>[] domains) {
        assert domains.length == numVars();
        BitSet toConsider = new BitSet(numBindings);
        toConsider.set(0, numBindings);
        for(int var=0 ; var<domains.length ; var++) {
            BitSet local = new BitSet(numBindings);
            for(int val : domains[var]) {
                if(relevantConstraints.get(var).containsKey(val)) {
                    local.or(relevantConstraints.get(var).get(val));
                }
            }
            toConsider.and(local);
        }

        Set<Integer> ret = new HashSet<>();
        int i =0;
        for(int[] binding : this.bindings) {
            if(toConsider.get(i)) {
                ret.add(binding[wanted]);
            }
            i++;
        }

        return ret;
    }
}