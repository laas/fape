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
package fape.core.planning.tasknetworks;

import fape.core.execution.model.ActionRef;
import fape.core.execution.model.TemporalConstraint;
import fape.core.planning.model.AbstractAction;
import fape.core.planning.model.Action;
import fape.core.planning.search.SupportOption;
import fape.util.Pair;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class TaskNetworkManager {

    //TaskNetwork net = new TaskNetwork();
    List<Action> roots = new LinkedList<>();
    //List<Action> openLeaves = new LinkedList<>();

    private void findLeaves(LinkedList<Action> leaves, Action current) {
        if (current.IsRefinable()) {
            leaves.add(current);
        } else if (current.decomposition != null) {
            for (Action a : current.decomposition) {
                findLeaves(leaves, a);
            }
        }
    }

    public List<Action> GetOpenLeaves() {
        LinkedList<Action> l = new LinkedList<>();
        for (Action a : roots) {
            findLeaves(l, a);
        }
        return l;
    }

    /**
     *
     * @param act
     */
    public void AddSeed(Action act) {
        roots.add(act);
    }

    /**
     *
     * @param a
     * @param abs
     * @param actions
     * @return which decompositions (numbers in the sequence) provides the
     * needed action
     */
    public List<Integer> DecomposesIntoDesiredAction(AbstractAction a, HashSet<String> abs, HashMap<String, AbstractAction> actions) {
        List<Integer> ret = new LinkedList<>();
        if (abs.contains(a.name)) {
            ret.add(0);
        } else {
            int ct = 0; //relative number of the decomposition
            for (Pair<List<ActionRef>, List<TemporalConstraint>> p : a.strongDecompositions) {
                for (ActionRef ref : p.value1) {
                    List<Integer> res = DecomposesIntoDesiredAction(actions.get(ref.name), abs, actions);
                    if (!res.isEmpty()) {
                        ret.add(ct);
                    }
                }
                ct++;
            }
        }
        return ret;
    }

    /**
     *
     * @param abs
     * @param actions
     * @return
     */
    public List<SupportOption> GetDecompositionCandidates(HashSet<String> abs, HashMap<String, AbstractAction> actions) {
        List<SupportOption> ret = new LinkedList<>();
        //lets run dfs to find the action names we like
        List<Action> openLeaves = GetOpenLeaves();
        for (Action a : openLeaves) {
            List<Integer> res = DecomposesIntoDesiredAction(actions.get(a.name), abs, actions);
            if (!res.isEmpty()) {
                for (Integer i : res) {
                    SupportOption o = new SupportOption();
                    o.actionToDecompose = a.mID;
                    o.decompositionID = i;
                    ret.add(o);
                }
            }
        }
        return ret;
    }

    /**
     *
     * @return
     */
    public TaskNetworkManager DeepCopy() {
        TaskNetworkManager tm = new TaskNetworkManager();
        for (Action a : this.roots) {
            tm.roots.add(a.DeepCopy());
        }
        return tm;
    }

    public String Report() {
        return "size: " + roots.size() + ", actions: " + roots.toString();
    }

    private float recCost(Action a) {
        float sum = a.GetCost();
        if (a.decomposition != null && !a.decomposition.isEmpty()) {
            for (Action b : a.decomposition) {
                sum += b.GetCost();
            }
        }
        return sum;
    }

    public float GetActionCosts() {
        float sum = 0;
        for (Action a : this.roots) {
            sum += recCost(a);
        }
        return sum;
    }

    public List<Action> GetAllActions() {
        LinkedList<Action> list = new LinkedList<>(roots);
        List<Action> ret = new LinkedList<>();
        while (!list.isEmpty()) {
            Action a = list.poll();
            if (a.decomposition == null || a.decomposition.isEmpty()) {
                ret.add(a);
            } else {
                for (Action b : a.decomposition) {
                    list.add(b);
                }
            }
        }
        return ret;
    }

    /**
     * returns the action with the given id
     *
     * @param actionToDecompose
     * @return
     */
    public Action GetAction(int actionToDecompose) {
        LinkedList<Action> qu = new LinkedList<>();
        for(Action a:roots){
            qu.add(a);
        }
        while(!qu.isEmpty()){
            Action a = qu.pop();
            if(a.mID == actionToDecompose){
                return a;
            }
            if(a.decomposition != null){
                for(Action b:a.decomposition){
                    qu.add(b);
                }
            }
        }
        return null;
    }
}
