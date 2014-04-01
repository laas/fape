package fape.core.planning.planninggraph;

import planstack.anml.model.concrete.VarRef;

import java.util.LinkedList;
import java.util.List;

public class PGUtils {

    /**
     * [[a1, a2], [b], [c1, C2]]
     *  => [ [a1, b, c1], [a1, b, c2], [a2, b, c1], [a2, b, c2]]
     * @param valuesSets
     * @return
     */
    public static <T> List<List<T>> allCombinations(List<List<T>> valuesSets) {
        return allCombinations(valuesSets, 0, new LinkedList<T>());
    }

    private static <T> List<List<T>> allCombinations(List<List<T>> valuesSets, int startWith, List<T> baseValues) {
        List<List<T>> ret = new LinkedList<>();

        if(startWith >= valuesSets.size()) {
            ret.add(baseValues);
            return ret;
        }
        for(T val : valuesSets.get(startWith)) {
            List<T> newBaseValues = new LinkedList<>(baseValues);
            newBaseValues.add(val);
            ret.addAll(allCombinations(valuesSets, startWith+1, newBaseValues));
        }

        return ret;
    }
}
