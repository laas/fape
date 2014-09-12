package planstack.constraints.bindings;


import java.util.*;

/**
 * Represents a n-ary constraint defined in extension.
 *
 * It consists of a n-tuple of variables and a set of n-tuples of values.
 */
public class ExtensionConstraint {
    protected LinkedList<LinkedList<Integer>> values = new LinkedList<>();

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

    /**
     * Gives a binary representation of the constraint.
     *
     * Example: if the constraint is:
     * <code>variables:(A, B, C)</code>,
     * <code>values: [(1, 2, 2), (1,3, 4)]</code>.
     *
     * The binary representation will be:
     * <code>
     *     {
     *         A : {
     *             B : 1 -> [2, 3]  // A=1 => B=2 or B=3
     *             C : 1 -> [3, 4]  // A=1 => C=3 or C=4
     *         },
     *         B : {
     *             A : 2 -> [1]     // B=2 => A=1
     *                 3 -> [1]
     *             C : 2 -> [2]
     *                 3 -> [4]
     *         },
     *         C : { ... }
     *     }
     * </code>
     * @return
     */
//    public Map<VarRef, Map<VarRef,Map<Integer, List<Integer>>>> processed() {
//        if(processed != null)
//            return processed;
//
//        processed = new HashMap<>();
//        for(int i=0 ; i<variables.size() ; i++) {
//            Map<VarRef,Map<Integer, List<Integer>>> currentVar = new HashMap<>();
//            for(int j=0 ; j<variables.size() ; j++) {
//                if(i == j) continue;
//
//                Map<Integer, List<Integer>> possibleValues = new HashMap<>();
//
//                for(LinkedList<Integer> valuesSeq : values) {
//                    Integer currVal = valuesSeq.get(i);
//                    if(!possibleValues.containsKey(currVal))
//                        possibleValues.put(currVal, new LinkedList<Integer>());
//
//                    if(!possibleValues.get(currVal).contains(valuesSeq.get(j))) {
//                        possibleValues.get(currVal).add(valuesSeq.get(j));
//                    }
//                }
//                currentVar.put(variables.get(j), possibleValues);
//            }
//            processed.put(variables.get(i), currentVar);
//        }
//        return processed;
//    }
}