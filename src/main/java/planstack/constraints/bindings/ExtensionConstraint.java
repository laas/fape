package planstack.constraints.bindings;


import java.util.*;

/**
 * Represents a n-ary constraint defined in extension.
 *
 * It consists of a n-tuple of variables and a set of n-tuples of values.
 */
public class ExtensionConstraint {
    public LinkedList<LinkedList<Integer>> values = new LinkedList<>();

    public final boolean isLastVarInteger;

    public ExtensionConstraint(boolean isLastVarInteger) {
        this.isLastVarInteger = isLastVarInteger;
    }

    public ExtensionConstraint DeepCopy() {
        return this;
    }

    LinkedList<Map<Integer, BitSet>> relevantConstraints = new LinkedList<>();

    public void addValues(List<Integer> vals) {
        assert values.isEmpty() || values.get(0).size() == vals.size();
        this.values.add(new LinkedList<Integer>(vals));

        if(relevantConstraints.size() == 0) {
            for(int i=0 ; i<vals.size() ; i++)
                relevantConstraints.add(new HashMap<Integer, BitSet>());
        }

        for(int i=0 ; i<vals.size() ; i++) {
            if(!relevantConstraints.get(i).containsKey(vals.get(i)))
                relevantConstraints.get(i).put(vals.get(i), new BitSet());
            relevantConstraints.get(i).get(vals.get(i)).set(this.values.size()-1);
        }
    }

    public Set<Integer> valuesUnderRestriction(int wanted, Map<Integer, Set<Integer>> constraints) {
        BitSet toConsider = new BitSet(values.size());
        toConsider.set(0, values.size());
        for(int var : constraints.keySet()) {
            BitSet local = new BitSet(values.size());
            for(int val : constraints.get(var)) {
                if(relevantConstraints.get(var).containsKey(val)) {
                    local.or(relevantConstraints.get(var).get(val));
                }
            }
            toConsider.and(local);
        }

        Set<Integer> ret = new HashSet<>();
        int i =0;
        for(List<Integer> vals : this.values) {
            if(toConsider.get(i)) {
                ret.add(vals.get(wanted));
            }
            i++;
        }

        return ret;
    }
}