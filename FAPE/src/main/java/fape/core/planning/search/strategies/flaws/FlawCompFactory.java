package fape.core.planning.search.strategies.flaws;

import fape.core.planning.preprocessing.AbstractionHierarchy;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;

import java.util.LinkedList;
import java.util.List;

public class FlawCompFactory {

    public static FlawComparator get(State st, String... comparators ) {
        List<FlawComparator> compList = new LinkedList<>();
        for(String compID : comparators) {
            switch (compID) {
                case "abs":
                    compList.add(new AbsHierarchyComp(st));
                    break;
                case "lcf":
                    compList.add(new LeastCommitingFirst());
                    break;
                default:
                    throw new FAPEException("Unrecognized flaw comparator option: "+compID);
            }
        }
        return new SeqFlawComparator(compList);
    }
}
