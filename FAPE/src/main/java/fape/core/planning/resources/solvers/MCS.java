/*
 * Author:  Filip Dvoøák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvoøák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package fape.core.planning.resources.solvers;

import fape.core.planning.resources.Reusable;
import fape.core.planning.states.State;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

/**
 *
 * @author FD
 */
public class MCS {

    static private void recursiveMCSSearch(float[] consumptionMap, float max, int root, float currentConsumption, 
                                    LinkedList<Integer> currentEvents, HashSet<Integer> candidates, 
                                    LinkedList<LinkedList<Integer>> criticalSets, ArrayList<LinkedList<Integer>> edges) {
        if (currentConsumption > max) {
            criticalSets.add(new LinkedList<>(currentEvents));
        } else {
            //the for cycle just runs over empty set, if we have run out of candidates, no arbitrary checking needed
            for (Integer i : candidates) {
                if (root > i) {
                    continue; //not processing the lower indexes, we have done those already previously
                }
                HashSet<Integer> newCandidates = new HashSet<>(candidates);
                newCandidates.retainAll(edges.get(i));
                currentEvents.addLast(i);
                recursiveMCSSearch(consumptionMap, max, root, currentConsumption + consumptionMap[i], currentEvents, newCandidates, criticalSets, edges);
                currentEvents.pollLast();
            }
        }
    }
    
    static public LinkedList<LinkedList<Integer>> FindSets(float[] consumptionMap, float max, ArrayList<LinkedList<Integer>> edges){
        LinkedList<LinkedList<Integer>> mcs = new LinkedList<>();
        for (int i = 0; i < edges.size(); i++) {
            HashSet<Integer> candidates = new HashSet<>(edges.get(i));
            LinkedList<Integer> clique = new LinkedList<>();
            clique.add(i);
            recursiveMCSSearch(consumptionMap, max, i, consumptionMap[i], clique, candidates, mcs, edges);
        }
        return mcs;
    }
}
