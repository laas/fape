package fape.core.planning.constraints;


import planstack.anml.model.concrete.VarRef;

import java.util.*;

/**
 * Represents a n-ary constraint defined in extension.
 *
 * It consists of a n-tuple of variables and a set of n-tuples of values.
 */
public class ExtensionConstraint {
    LinkedList<VarRef> variables = new LinkedList<>();
    LinkedList<LinkedList<Integer>> values = new LinkedList<>();

    Map<VarRef, Map<VarRef,Map<Integer, List<Integer>>>> processed = null;

    public ExtensionConstraint(Collection<VarRef> vars, Collection<LinkedList<Integer>> values) {
        variables.addAll(vars);
        for(Collection<Integer> valueSeq : values) {
            this.values.add(new LinkedList<>(valueSeq));
        }
    }

    /**
     * @return True if this variable appears in the constraint.
     */
    public boolean isAbout(VarRef v) {
        return variables.contains(v);
    }

    public ExtensionConstraint DeepCopy() {
        return this;
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
    public Map<VarRef, Map<VarRef,Map<Integer, List<Integer>>>> processed() {
        if(processed != null)
            return processed;

        processed = new HashMap<>();
        for(int i=0 ; i<variables.size() ; i++) {
            Map<VarRef,Map<Integer, List<Integer>>> currentVar = new HashMap<>();
            for(int j=0 ; j<variables.size() ; j++) {
                if(i == j) continue;

                Map<Integer, List<Integer>> possibleValues = new HashMap<>();

                for(LinkedList<Integer> valuesSeq : values) {
                    Integer currVal = valuesSeq.get(i);
                    if(!possibleValues.containsKey(currVal))
                        possibleValues.put(currVal, new LinkedList<Integer>());

                    if(!possibleValues.get(currVal).contains(valuesSeq.get(j))) {
                        possibleValues.get(currVal).add(valuesSeq.get(j));
                    }
                }
                currentVar.put(variables.get(j), possibleValues);
            }
            processed.put(variables.get(i), currentVar);
        }
        return processed;
    }
}