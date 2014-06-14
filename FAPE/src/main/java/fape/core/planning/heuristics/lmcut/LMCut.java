/*
 * Author:  Filip Dvořák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package fape.core.planning.heuristics.lmcut;

import fape.core.planning.planninggraph.Fluent;
import fape.core.planning.planninggraph.GroundAction;
import fape.core.planning.planninggraph.GroundProblem;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * implements the landmark cut heuristic
 *
 * @author FD
 */
public final class LMCut {
    public static BitSet commonInit = null;
    
    
    public static LinkedHashMap<RelaxedGroundAtom, LinkedList<RelaxedGroundAction>> at2eff;
    public static LinkedHashSet<RelaxedGroundAtom> mAtoms;
    public static LinkedHashMap<Integer, RelaxedGroundAtom> at2index;
    public static Iterable<RelaxedGroundAction> mActions;
    public static LinkedHashMap<RelaxedGroundAtom, LinkedList<RelaxedGroundAction>> at2con;
    LinkedList<RelaxedGroundAction> latestUsefulActions = null;
    
    RelaxedGroundProblem problem = null;

    private static boolean isApplicable(HashSet<RelaxedGroundAtom> kb, RelaxedGroundAction a) {
        for (RelaxedGroundAtom at : a.pre) {
            if (!kb.contains(at)) {
                return false;
            }
        }
        return true;
    }

    private static void apply(HashSet<RelaxedGroundAtom> kb, RelaxedGroundAction a) {
        for (RelaxedGroundAtom at : a.eff) {
            kb.add(at);
        }
    }

    private static boolean triviallySatisfiable(HashSet<RelaxedGroundAtom> kb, List<RelaxedGroundAtom> goal) {
        for (RelaxedGroundAtom at : goal) {
            if (!kb.contains(at)) {
                return false;
            }
        }
        return true;
    }

    public LMCut(GroundProblem p) {
        RelaxedGroundProblem pr = new RelaxedGroundProblem();

        //translate from a planning graph
        for (GroundAction g : p.allActions()) {
            RelaxedGroundAction r = new RelaxedGroundAction();
            r.name = g.toString();
            for (Fluent f : g.add) {
                RelaxedGroundAtom a = new RelaxedGroundAtom(f.toString());
                r.eff.add(a);
            }
            for (Fluent f : g.pre) {
                RelaxedGroundAtom a = new RelaxedGroundAtom(f.toString());
                r.pre.add(a);
            }
            pr.actions.add(r);
        }

        for (Fluent f : p.initState.fluents) {
            RelaxedGroundAtom a = new RelaxedGroundAtom(f.toString());
            pr.init.add(a);
        }

        for (Fluent f : p.goalState.fluents) {
            RelaxedGroundAtom a = new RelaxedGroundAtom(f.toString());
            pr.goal.add(a);
        }

        //reduce the problem by kb-reachability
        List<RelaxedGroundAction> actionsToTry = new LinkedList<>(pr.actions);
        List<RelaxedGroundAction> appliedActions = new LinkedList<>();
        HashSet<RelaxedGroundAtom> addedAtoms = new HashSet<>();
        HashSet<RelaxedGroundAtom> kb = new HashSet<>(pr.init);
        int oldSize = -1;
        while (oldSize != kb.size()) {
            oldSize = kb.size();
            Iterator<RelaxedGroundAction> it = actionsToTry.iterator();
            while (it.hasNext()) {
                RelaxedGroundAction ac = it.next();
                if (isApplicable(kb, ac)) {
                    for (RelaxedGroundAtom att : ac.eff) {
                        kb.add(att);
                        addedAtoms.add(att);
                    }
                    it.remove();
                    appliedActions.add(ac);
                }
            }
        }
        if (!triviallySatisfiable(kb, pr.goal)) {
            throw new UnsupportedOperationException("The planning task failed the test of relaxed reachability of the goal.");
        }
        pr.actions = appliedActions;
        pr.init.retainAll(kb);
        LinkedHashSet<RelaxedGroundAtom> atomsToRemove = new LinkedHashSet<>(pr.init);
        atomsToRemove.removeAll(addedAtoms);
        pr.init.removeAll(atomsToRemove);
        for (RelaxedGroundAction a : pr.actions) {
            a.pre.removeAll(atomsToRemove);
            a.eff.removeAll(atomsToRemove);
        }

        RelaxedGroundAtom.ResetIndexes();
        for (RelaxedGroundAtom a : pr.init) {
            a.ReIndex();
        }
        for (RelaxedGroundAtom a : pr.goal) {
            a.ReIndex();
        }
        for (RelaxedGroundAction ac : pr.actions) {
            for (RelaxedGroundAtom a : ac.eff) {
                a.ReIndex();
            }
            for (RelaxedGroundAtom a : ac.pre) {
                a.ReIndex();
            }
        }
        for (RelaxedGroundAtom a : pr.atoms) {
            a.ReIndex();
        }

        pr.atoms = new LinkedList<>(pr.init);
        pr.atoms.addAll(addedAtoms);
        Init(new LinkedHashSet<>(pr.atoms), pr.actions);
        problem = pr;
    }
    
    public Iterable<RelaxedGroundAction> DiscoverRelevantActions(BitSet init, BitSet goal) {
        LinkedHashSet<RelaxedGroundAction> ret = new LinkedHashSet<>();
        //we now want to find all relevant actions, those are the actions that can contribute the search in some way
        //LinkedHashSet<RelaxedGroundAction> relevantAction = new LinkedHashSet<RelaxedGroundAction>();
        HashSet<RelaxedGroundAtom> processedAtoms = new HashSet<>();
        LinkedList<RelaxedGroundAtom> atomQueue = new LinkedList<>();
        for (int i = goal.nextSetBit(0); i >= 0; i = goal.nextSetBit(i + 1)) {
            if (!init.get(i)) {//for those goals, that were not achieved
                atomQueue.add(at2index.get(i));
                processedAtoms.add(at2index.get(i));
            }
        }
        while(!atomQueue.isEmpty()){
            RelaxedGroundAtom a = atomQueue.pop();
            LinkedList<RelaxedGroundAction> l = at2eff.get(a);
            for(RelaxedGroundAction ai : l){
                if(ret.contains(ai)){
                    continue;
                }
                ret.add(ai);
                for(RelaxedGroundAtom con : ai.pre){ //for each condition of the action
                    if(!processedAtoms.contains(con)){ //enqueue the condition, unless it was already processed
                        atomQueue.add(con);
                        processedAtoms.add(con);
                    }
                }
            }
        }
        return ret;
    }

    private void hMaxForwardQueue(HashMap<RelaxedGroundAtom, Float> atCost, HashMap<RelaxedGroundAction, Float> acCost, BitSet init) {
        //reset the actions's hmax params
        for (RelaxedGroundAction a : mActions) {
            a.ResetHmax();
        }
        //init atom costs to large vals
        for (RelaxedGroundAtom at : mAtoms) {
            atCost.put(at, Float.POSITIVE_INFINITY);
        }
        HashSet<RelaxedGroundAtom> base = new HashSet<>();
        HashSet<RelaxedGroundAction> seenActions = new HashSet<>();
        LinkedList<RelaxedGroundAction> queue = new LinkedList<>();
        for (int i = init.nextSetBit(0); i >= 0; i = init.nextSetBit(i + 1)) {
            RelaxedGroundAtom at = at2index.get(i);
            atCost.put(at, 0f);
            base.add(at);
            //for each action that i support
            for (RelaxedGroundAction ac : at2con.get(at)) {
                if (ac.IsApplicableClean(base)) {
                    queue.add(ac);
                    seenActions.add(ac);
                    ac.hMaxVal = 0f;
                    ac.hMaxSupporter = at;
                }
            }
        }
        while (!queue.isEmpty()) {
            //pop
            RelaxedGroundAction ac = queue.pop();
            float actCost = acCost.get(ac);
            for (RelaxedGroundAtom at : ac.eff) {
                base.add(at);
                if (atCost.get(at) > actCost + ac.hMaxVal) {
                    atCost.put(at, actCost + ac.hMaxVal);
                }
            }
            for (RelaxedGroundAtom at : ac.eff) {
                //float current = atCost.get(at);
                //if (current > ac.hMaxVal + actCost) { //better action found
                //atCost.put(at, ac.hMaxVal + actCost);
                for (RelaxedGroundAction changedAc : at2con.get(at)) { //for each action i support
                    if (changedAc.IsApplicableClean(base) && !seenActions.contains(changedAc)) {
                        //if the action is applicable and wasnt processed before
                        RelaxedGroundAtom maxSup = null;
                        float max = Float.NEGATIVE_INFINITY;
                        for (RelaxedGroundAtom sup : changedAc.pre) {
                            if (atCost.get(sup) > max) {
                                maxSup = sup;
                                max = atCost.get(sup);
                            }
                        }
                        changedAc.hMaxSupporter = maxSup;
                        changedAc.hMaxVal = max;
                        queue.add(changedAc);
                        seenActions.add(changedAc);
                    } else if (seenActions.contains(changedAc) && changedAc.hMaxSupporter == at && ac.hMaxVal + actCost < changedAc.hMaxVal) {
                        //OR the action was seen and current atom improves its reachability
                        //find its new maximal supporter now
                        RelaxedGroundAtom maxSup = null;
                        float max = Float.NEGATIVE_INFINITY;
                        for (RelaxedGroundAtom sup : changedAc.pre) {
                            if (atCost.get(sup) > max) {
                                maxSup = sup;
                                max = atCost.get(sup);
                            }
                        }
                        changedAc.hMaxSupporter = maxSup;
                        changedAc.hMaxVal = max;
                        //and enqueue the action again
                        queue.add(changedAc);
                    }
                }
            }
        }
    }

    public float Eval(BitSet init, BitSet goal) {

        float totalHVal = 0;
        HashMap<RelaxedGroundAtom, Float> caCost = new HashMap<>();
        HashMap<RelaxedGroundAction, Float> cActCost = new HashMap<>();

        //calculate the initial costs
        for (RelaxedGroundAction a : mActions) {
            cActCost.put(a, a.cost);
        }
        
        //hMaxForward(caCost, cActCost, init);
        hMaxForwardQueue(caCost, cActCost, init);

        boolean allZeroCost = true;
        for (int i = goal.nextSetBit(0); i >= 0; i = goal.nextSetBit(i + 1)) {
            Float f = caCost.get(at2index.get(i));
            if(f == null){
                return Float.MAX_VALUE;
            }else if (caCost.get(at2index.get(i)) > 0) {
                allZeroCost = false;
            }
        }

        //save the cut actions for later usage
        latestUsefulActions = new LinkedList<>();

        boolean end = allZeroCost;
        while (!end) {
            //construct the justification graph
            JustificationGraph g = new JustificationGraph(caCost, cActCost, mActions, init, goal);

            //get results
            LinkedHashSet<RelaxedGroundAction> l = g.FindCut();
            if (l.isEmpty()) {
                break; //we have derived all we could
            }
            float min = g.GetCutMin();
            latestUsefulActions.addAll(l);

            //apply the results to actions costs
            for (RelaxedGroundAction a : l) {
                cActCost.put(a, cActCost.get(a) - min);
            }

            //update h value
            totalHVal += min;

            //reset atom costs
            caCost = new HashMap<>();

            //recalculate the h max estimates
            hMaxForwardQueue(caCost, cActCost, init);

            //check if all goals are reachable with no cost
            allZeroCost = true;
            for (int i = goal.nextSetBit(0); i >= 0; i = goal.nextSetBit(i + 1)) {
                if (caCost.get(at2index.get(i)) > 0) {
                    allZeroCost = false;
                }
            }

            //end if there is no more cost to extract
            end = allZeroCost;
        }
        return totalHVal;
    }

    public void Init(LinkedHashSet<RelaxedGroundAtom> atoms, Iterable<RelaxedGroundAction> actions) {
        mAtoms = atoms;
        at2eff = new LinkedHashMap<>();
        for (RelaxedGroundAtom at : mAtoms) {
            at2eff.put(at, new LinkedList<RelaxedGroundAction>());
        }
        for (RelaxedGroundAction a : actions) {
            for (RelaxedGroundAtom at : a.eff) {
                LinkedList<RelaxedGroundAction> l = at2eff.get(at);
                if (l == null) {
                    l = new LinkedList<>();
                }
                l.add(a);
                at2eff.put(at, l);
            }
        }
        at2con = new LinkedHashMap<>();
        for (RelaxedGroundAtom at : mAtoms) {
            at2con.put(at, new LinkedList<RelaxedGroundAction>());
        }
        for (RelaxedGroundAction a : actions) {
            for (RelaxedGroundAtom at : a.pre) {
                LinkedList<RelaxedGroundAction> l = at2con.get(at);
                l.add(a);
                at2con.put(at, l);
            }
        }

        mAtoms = atoms;
        at2index = new LinkedHashMap<>();
        for (RelaxedGroundAtom a : mAtoms) {
            at2index.put(a.mID, a);
        }
        mActions = actions;
    }

}
