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
import fape.util.Reporter;
import planstack.anml.model.concrete.*;
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
public class TaskNetworkManager implements Reporter {

    final UnlabeledDigraph<Action> network;

    public TaskNetworkManager() {
        network = SimpleUnlabeledDigraph$.MODULE$.apply(); //TODO: More java-friendly factory in graphs
    }

    public TaskNetworkManager(UnlabeledDigraph<Action> network) {
        this.network = network;
    }

    public boolean isRoot(Action a) {
        return network.inDegree(a) == 0;
    }

    /**
     * @return All actions of the task network that are not issued from a decomposition
     *         (ie. roots of the task network).
     */
    public List<Action> roots() {
        List<Action> roots = new LinkedList<>();
        for(Action a : network.jVertices()) {
            if(isRoot(a)) {
                roots.add(a);
            }
        }
        return roots;
    }

    /**
     * @param a Action to lookup
     * @return True if the action is decomposed
     */
    public boolean isDecomposed(Action a) {
        return network.outDegree(a) != 0;
    }

    /**
     * @return All decomposable actions that are not decomposed yet.
     */
    public List<Action> GetOpenLeaves() {
        LinkedList<Action> l = new LinkedList<>();
        for (Action a : network.jVertices()) {
            if(a.decomposable() && !isDecomposed(a)) {
                l.add(a);
            }
        }
        return l;
    }

    /**
     * Inserts an action in the task network. If the action a has
     * a parent p, an edge from p to a is also added.
     */
    public void insert(Action a) {
        network.addVertex(a);
        if(a.hasParent()) {
            network.addEdge(a.parent(), a);
        }
    }

    /**
     * Performs recursively a deep copy on the task network manager and all its
     * actions.
     * @return A new TaskManager with the same content.
     */
    public TaskNetworkManager DeepCopy() {
        return new TaskNetworkManager(network.cc());
    }

    @Override
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
    public Action GetAction(ActRef id) {
        for(Action a : GetAllActions()) {
            if(id.equals(a.id())) {
                return a;
            }
        }
        throw new FAPEException("No action with id: "+id);
    }

    public Action getActionContainingStatement(LogStatement e) {
        for(Action a : GetAllActions()) {
            for(LogStatement s : a.logStatements()) {
                if(s.equals(e)) {
                    return a;
                }
            }
        }
        return null;
    }
}
