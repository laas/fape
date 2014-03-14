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

import fape.exceptions.FAPEException;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.TemporalStatement;
import planstack.graph.core.SimpleUnlabeledDigraph;
import planstack.graph.core.SimpleUnlabeledDigraph$;
import planstack.graph.core.UnlabeledDigraph;

import java.util.*;

/**
 *
 * @author FD
 */
public class TaskNetworkManager {

    final UnlabeledDigraph<Action> network;

    public TaskNetworkManager() {
        network = SimpleUnlabeledDigraph$.MODULE$.apply();
    }

    public TaskNetworkManager(UnlabeledDigraph<Action> network) {
        this.network = network;
    }

    public boolean isRoot(Action a) {
        return network.inDegree(a) == 0;
    }

    public List<Action> roots() {
        List<Action> roots = new LinkedList<>();
        for(Action a : network.jVertices()) {
            if(isRoot(a)) {
                roots.add(a);
            }
        }
        return roots;
    }

    public boolean isDecomposed(Action a) {
        return network.outDegree(a) != 0;
    }

    public List<Action> GetOpenLeaves() {
        LinkedList<Action> l = new LinkedList<>();
        for (Action a : network.jVertices()) {
            if(a.decomposable() && isDecomposed(a)) {
                l.add(a);
            }
        }
        return l;
    }
//
//    /**
//     *
//     * @param act
//     */
//    public void AddSeed(Action act) {
//        roots.add(act);
//    }
//
//    /**
//     *
//     * @param a
//     * @param abs
//     * @param actions
//     * @return which decompositions (numbers in the sequence) provides the
//     * needed action
//     */
//    public List<Integer> DecomposesIntoDesiredAction(AbstractAction a, HashSet<String> abs, HashMap<String, AbstractAction> actions) {
//        List<Integer> ret = new LinkedList<>();
//        if (abs.contains(a.name)) {
//            ret.add(0);
//        } else {
//            int ct = 0; //relative number of the decomposition
//            for (Pair<List<ActionRef>, List<TemporalConstraint>> p : a.strongDecompositions) {
//                for (ActionRef ref : p.value1) {
//                    List<Integer> res = DecomposesIntoDesiredAction(actions.get(ref.name), abs, actions);
//                    if (!res.isEmpty()) {
//                        ret.add(ct);
//                    }
//                }
//                ct++;
//            }
//        }
//        return ret;
//    }
//
//    /**
//     *
//     * @param abs
//     * @param actions
//     * @return
//     */
//    public List<SupportOption> GetDecompositionCandidates(HashSet<String> abs, HashMap<String, AbstractAction> actions) {
//        List<SupportOption> ret = new LinkedList<>();
//        //lets run dfs to find the action names we like
//        List<Action> openLeaves = GetOpenLeaves();
//        for (Action a : openLeaves) {
//            List<Integer> res = DecomposesIntoDesiredAction(actions.get(a.name), abs, actions);
//            if (!res.isEmpty()) {
//                for (Integer i : res) {
//                    SupportOption o = new SupportOption();
//                    o.actionToDecompose = a.mID;
//                    o.decompositionID = i;
//                    ret.add(o);
//                }
//            }
//        }
//        return ret;
//    }
//
//
    /**
     * Performs recursively a deep copy on the task network manager and all its
     * actions.
     * @return A new TaskManager with the same content.
     */
    public TaskNetworkManager DeepCopy() {
        return new TaskNetworkManager(network.cc());
    }

    public String Report() {
        String str = "Num roots: " + roots().size() + ", roots: " + roots().toString();
        str += "\n\tLeaf actions" +  GetAllActions().toString();
        return str;
    }

    public float GetActionCosts() {
        float sum = 0;
        for (Action a : network.jVertices()) {
            sum += a.cost();
        }
        return sum;
    }

    public List<Action> GetAllActions() {
        return network.jVertices();
    }

    /**
     * returns the action with the given id
     *
     * @param id Id of the action
     * @return
     */
    public Action GetAction(String id) {
        for(Action a : GetAllActions()) {
            if(id.equals(a.id())) {
                return a;
            }
        }
        throw new FAPEException("No action with id: "+id);
    }

    public Action getActionContainingStatement(LogStatement e) {
        for(Action a : GetAllActions()) {
            for(TemporalStatement ts : a.jStatements()) {
                if(ts.statement().equals(e)) {
                    return a;
                }
            }
        }
        return null;
    }

//    /**
//     * flips the "removed" switch on an action, since the action failed we do
//     * not consider it to ba a part of plan anymore
//     *
//     * @param id the id of the action
//     */
//    public void FailAction(Integer id) {
//        Action a = GetAction(id);
//        if(a != null)
//            a.status = Action.Status.FAILED;
//        else
//            throw new FAPEException("Unable to fail action: id "+id+" does not exist.");
//    }
//
//    public void SetActionSuccess(Integer id) {
//        Action a = GetAction(id);
//        if(a != null) {
//            if(a.status != Action.Status.EXECUTING)
//                TinyLogger.LogInfo("WARNING: setting action' status to EXECUTED while its current status is not EXECUTING");
//            a.status = Action.Status.EXECUTED;
//        } else
//            throw new FAPEException("Unable to report success of action: id "+id+" does not exist.");
//
//    }
//
//    public void SetActionExecuting(Integer id) {
//        Action a = GetAction(id);
//        if(a != null) {
//            if(a.status != Action.Status.PENDING)
//                TinyLogger.LogInfo("WARNING: setting action' status to EXECUTING while its current status is not PENDING");
//            a.status = Action.Status.EXECUTING;
//        } else
//            throw new FAPEException("Unable to report execution of action: id "+id+" does not exist.");
//
//    }
}
