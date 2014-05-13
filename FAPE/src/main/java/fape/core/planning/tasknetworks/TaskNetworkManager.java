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
import planstack.graph.GraphFactory;
import planstack.graph.core.UnlabeledDigraph;

import java.util.*;

public class TaskNetworkManager implements Reporter {

    final UnlabeledDigraph<TNNode> network;

    private int numActions;
    private int numOpenLeaves;
    private int numRoots;

    public TaskNetworkManager() {
        network = GraphFactory.getSimpleUnlabeledDigraph();
        numActions = 0;
        numOpenLeaves = 0;
        numRoots = 0;
    }

    public TaskNetworkManager(TaskNetworkManager base) {
        this.network = base.network.cc();
        this.numActions = base.numActions;
        this.numOpenLeaves = base.numOpenLeaves;
        this.numRoots = base.numRoots;
    }

    /**
     * O(1)
     * @return The number of undecomposed method.
     */
    public int getNumOpenLeaves() {
        return numOpenLeaves;
    }

    /**
     * O(1)
     * @return The number of actions in the task network (all actions are considered).
     */
    public int getNumActions() {
        return numActions;
    }

    /**
     * O(1)
     * @return Number of roots in the task network.
     */
    public int getNumRoots() {
        return numRoots;
    }

    /**
     * O(1)
     * @param a The action to check
     * @return True if the action is a root of the task network (e.g. it is not part
     *         of any decomposition.
     */
    public boolean isRoot(Action a) {
        return network.inDegree(new TNNode(a)) == 0;
    }

    /**
     * O(n)
     * @return All actions of the task network that are not issued from a decomposition
     *         (ie. roots of the task network).
     */
    public List<Action> roots() {
        List<Action> roots = new LinkedList<>();
        for(TNNode n : network.jVertices()) {
            if(n.isAction() && isRoot(n.asAction())) {
                roots.add(n.asAction());
            }
        }
        assert roots.size() == numRoots : "Error: wrong number of roots.";
        return roots;
    }

    /**
     * @param a Action to lookup
     * @return True if the action is decomposed
     */
    public boolean isDecomposed(Action a) {
        for(TNNode child : network.jChildren(new TNNode(a))) {
            if(child.isDecomposition()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the decomposition of an action.
     * @throws FAPEException if the action has no decomposition.
     * @param a The action for which to retrieve the decomposition.
     * @return The decomposition of the action.
     */
    private TNNode getDecomposition(Action a) {
        assert isDecomposed(a) : "Error: action "+a+" has no decomposition.";
        for(TNNode child : network.jChildren(new TNNode(a))) {
            if(child.isDecomposition()) {
                return child;
            }
        }
        throw new FAPEException("Action "+a+" has no known decomposition.");
    }

    /**
     * O(n)
     * @return All decomposable actions that are not decomposed yet.
     */
    public List<Action> GetOpenLeaves() {
        LinkedList<Action> l = new LinkedList<>();
        for (TNNode n : network.jVertices()) {
            if(n.isAction()) {
                Action a = n.asAction();
                if(a.decomposable() && !isDecomposed(a)) {
                    l.add(a);
                }
            }
        }
        assert l.size() == numOpenLeaves : "Error: wrong number of opened leaves.";
        return l;
    }

    /**
     * Inserts an action in the task network. If the action a has
     * a parent p, an edge from the decomposition of p to a is also added.
     * O(1)
     */
    public void insert(Action a) {
        network.addVertex(new TNNode(a));
        numActions++;
        if(a.decomposable())
            numOpenLeaves++;

        if(a.hasParent()) {
            assert isDecomposed(a.parent()) : "Error: adding an action as a child of a yet undecomposed action.";
            network.addEdge(getDecomposition(a.parent()), new TNNode(a));
        } else {
            numRoots++;
        }
    }

    /**
     * Adds a Decomposition of an action.
     * @param dec The decomposition.
     * @param parent The action containing the decomposition.
     */
    public void insert(Decomposition dec, Action parent) {
        network.addVertex(new TNNode(dec));
        network.addEdge(new TNNode(parent), new TNNode(dec));
        this.numOpenLeaves++;
    }

    /**
     * Checks if the action is a descendant of the decomposition (i.e. there
     * is a path from the decomposition to the action in the task network.)
     * @param child Descendant action.
     * @param dec Potential ancestor.
     * @return True if dec is an ancestor of child.
     */
    public boolean isDescendantOf(Action child, Decomposition dec) {
        return isDescendantOf(new TNNode(child), new TNNode(dec));
    }

    /**
     * Checks if the first node is a descendant of the second one.
     * (i.e. there is a path from n2 to n1).
     * @param n1 Descendant node.
     * @param n2 Potential ancestor.
     * @return True if n2 is an ancestor of n1.
     */
    private boolean isDescendantOf(TNNode n1, TNNode n2) {
        if(network.inDegree(n1) == 0) {
            return false;
        } else if(network.parents(n1).size() != 1) {
            throw new FAPEException("Error: node "+n1+" has more than one parent.");
        } else if(network.parents(n1).contains(n2)) {
            return true;
        } else {
            return isDescendantOf(network.parents(n1).head(), n2);
        }
    }

    /**
     * Performs recursively a deep copy on the task network manager and all its
     * actions.
     * @return A new TaskManager with the same content.
     */
    public TaskNetworkManager DeepCopy() {
        return new TaskNetworkManager(this);
    }

    @Override
    public String Report() {
        String str = "Num roots: " + roots().size() + ", roots: " + roots().toString();
        str += "\n\tLeaf actions" +  GetAllActions().toString();
        return str;
    }

    /**
     * O(n).
     * @return All actions of the task network.
     */
    public List<Action> GetAllActions() {
        List<Action> allActions = new LinkedList<>();
        for(TNNode n : network.jVertices()) {
            if(n.isAction()) {
                allActions.add(n.asAction());
            }
        }
        assert allActions.size() == numActions : "Error: wrong number of action.";
        return allActions;
    }

    /**
     * O(n)
     *
     * @param id Id of the action
     * @return the action with the given id
     */
    public Action GetAction(ActRef id) {
        for(Action a : GetAllActions()) {
            if(id.equals(a.id())) {
                return a;
            }
        }
        throw new FAPEException("No action with id: "+id);
    }

    /**
     * Lookup the action containing the given statement.
     * Implementations currently looks for all statements of all actions
     * O(n)
     * TODO: more efficient implementation
     *
     * @param e LogStatement to look for.
     * @return The action containing the statement. null if no action in the task network contains the statement.
     */
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
