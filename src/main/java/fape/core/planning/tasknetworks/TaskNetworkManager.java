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

import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import fape.util.Reporter;
import planstack.anml.model.concrete.ActRef;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.ActionCondition;
import planstack.anml.model.concrete.Decomposition;
import planstack.anml.model.concrete.statements.IntegerAssignmentConstraint;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.graph.GraphFactory;
import planstack.graph.core.Edge;
import planstack.graph.core.SimpleUnlabeledDigraph;
import planstack.graph.core.UnlabeledDigraph;
import planstack.graph.printers.NodeEdgePrinter;
import planstack.structures.Pair;

import java.util.LinkedList;
import java.util.List;

public class TaskNetworkManager implements Reporter {

    final UnlabeledDigraph<TNNode> network;

    private int numActions;
    private int numOpenLeaves;
    private int numRoots;
    private int numOpenedActionConditions;
    private int numUnmotivatedActions;

    public TaskNetworkManager() {
        network = GraphFactory.getSimpleUnlabeledDigraph();
        numActions = 0;
        numOpenLeaves = 0;
        numRoots = 0;
        numOpenedActionConditions = 0;
        numUnmotivatedActions = 0;
    }

    public TaskNetworkManager(TaskNetworkManager base) {
        this.network = base.network.cc();
        this.numActions = base.numActions;
        this.numOpenLeaves = base.numOpenLeaves;
        this.numRoots = base.numRoots;
        this.numOpenedActionConditions = base.numOpenedActionConditions;
        this.numUnmotivatedActions = base.numUnmotivatedActions;
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
     * @return Number of opened action conditions.
     */
    public int getNumOpenActionConditions() { return numOpenedActionConditions; }

    /**
     * O(1)
     * @return Number of action with motivated keyword that are motivated yet.
     */
    public int getNumUnmotivatedActions() { return numUnmotivatedActions; }

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
     * @param ac ActionCondition to lookup
     * @return True if the action condition is supported (i.e. there is an edge
     *         from ac to an action.
     */
    public boolean isSupported(ActionCondition ac) {
        for(TNNode child : network.jChildren(new TNNode(ac))) {
            if(child.isAction()) {
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
     * O(n)
     * @return All action condition that are not supported yet.
     */
    public List<ActionCondition> getOpenTaskConditions() {
        LinkedList<ActionCondition> l = new LinkedList<>();
        for (TNNode n : network.jVertices()) {
            if(n.isActionCondition()) {
                ActionCondition ac = n.asActionCondition();
                if(!isSupported(ac)) {
                    l.add(ac);
                }
            }
        }
        assert l.size() == numOpenedActionConditions;
        return l;
    }

    /**
     * O(n)
     * @return All actions that have a motivated statements but are not yet
     *         part of any decomposition.
     */
    public List<Action> getUnmotivatedActions() {
        LinkedList<Action> l = new LinkedList<>();
        for (TNNode n : network.jVertices()) {
            if(n.isAction()) {
                Action a = n.asAction();
                if(a.mustBeMotivated() && !isMotivated(a)) {
                    l.add(a);
                }
            }
        }
        assert l.size() == numUnmotivatedActions;
        return l;
    }

    /**
     * @return True if this action is motivated: it is part of an action or a decomposition.
     */
    public boolean isMotivated(Action a) {
        assert a.mustBeMotivated();
        TNNode n = new TNNode(a);
        if(network.inDegree(n) == 0) {
            return false;
        } else {
            for(TNNode parent : network.jParents(n)) {
                if(true) return true;
                // it is part of an action or a decomposition
                if(parent.isAction() || parent.isDecomposition())
                    return true;
                // it is an action condition which it self is part of an action or decomposition
                if(network.inDegree(parent) > 0)
                    return true;
            }
            return false;
        }
    }

    /**
     * Add a task support link from an action condition to an action.
     * THis link means: the action condition cond is supported by the action a.
     *
     * An action condition should be supported by exactly one action.
     * @param cond An action condition already present in the task network.
     * @param a An action already present in the task network.
     */
    public void addSupport(ActionCondition cond, Action a) {
        assert network.contains(new TNNode(cond));
        assert network.contains(new TNNode(a));
        assert network.outDegree(new TNNode(cond)) == 0;
        network.addEdge(new TNNode(cond), new TNNode(a));
        if(network.inDegree(new TNNode(a)) == 1) {
            numRoots--;
            if(a.mustBeMotivated())
                numUnmotivatedActions--;
        }
        numOpenedActionConditions--;
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
            network.addEdge(new TNNode(a.parent()), new TNNode(a));
        } else {
            numRoots++;
        }
        if(a.mustBeMotivated() && !isMotivated(a))
            numUnmotivatedActions++;
    }

    /**
     * Adds a Decomposition of an action.
     * @param dec The decomposition.
     * @param parent The action containing the decomposition.
     */
    public void insert(Decomposition dec, Action parent) {
        network.addVertex(new TNNode(dec));
        network.addEdge(new TNNode(parent), new TNNode(dec));
        this.numOpenLeaves--;
    }

    /**
     * Adds an action condition to a decomposition.
     * @param ac The action condition.
     * @param parent The decomposition in which ac appears. This decomposition must be already
     *               present in the task network.
     */
    public void insert(ActionCondition ac, Decomposition parent) {
        network.addVertex(new TNNode(ac));
        network.addEdge(new TNNode(parent), new TNNode(ac));
        numOpenedActionConditions++;
    }

    /**
     * Adds an action condition to an action.
     * @param ac The action condition.
     * @param parent The action in which ac appears. This action must be already
     *               present in the task network.
     */
    public void insert(ActionCondition ac, Action parent) {
        network.addVertex(new TNNode(ac));
        network.addEdge(new TNNode(parent), new TNNode(ac));
        numOpenedActionConditions++;
    }

    /**
     * Adds an action condition in the task network.
     * @param ac The action condition.
     */
    public void insert(ActionCondition ac) {
        network.addVertex(new TNNode(ac));
        numOpenedActionConditions++;
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

    public Pair<Action, Integer> leastCommonAncestor(Action a1, Action a2) {
        List<Action> parents = new LinkedList<>();
        TNNode cur = new TNNode(a1);
        parents.add(a1);
        while(cur != null) {
            if(network.parents(cur).nonEmpty()) {
                assert network.parents(cur).size() == 1;
                cur = network.parents(cur).head();
                if(cur.isAction())
                    parents.add(cur.asAction());
            } else {
                cur = null;
            }
        }

        cur = new TNNode(a2);
        Action commonAncestor = null;
        int depth = -1;
        while(cur != null && commonAncestor == null) {
            if(cur.isAction()) {
                depth += 1;
                if(parents.contains(cur.asAction())) {
                    commonAncestor = cur.asAction();
                    break;
                }
            }
            if(network.parents(cur).nonEmpty()) {
                assert network.parents(cur).size() == 1;
                cur = network.parents(cur).head();

            } else {
                cur = null;
            }
        }

        if(commonAncestor != null)
            return new Pair<>(commonAncestor, depth);
        else
            return null;
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
     * TODO: account for decompositions?
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



    public void exportToDot(final State st, String filename) {
        class TNPrinter extends NodeEdgePrinter<TNNode, Object, Edge<TNNode>> {
            @Override
            public String printNode(TNNode n) {
                assert n.isAction();
                return Printer.action(st, n.asAction());
            }
        }

        SimpleUnlabeledDigraph<TNNode> g = GraphFactory.getSimpleUnlabeledDigraph();
        for(TNNode n : network.jVertices())
            if(n.isAction())
                g.addVertex(n);

        for(TNNode n : network.jVertices()) {
            if (n.isAction()) {
                for(TNNode child : network.jChildren(n)) {
                    if(child.isAction()) {
                        g.addEdge(n, child);
                    } else {
                        for(TNNode grandChild : network.jChildren(child)) {
                            if(grandChild.isAction()) {
                                g.addEdge(n, grandChild);
                            } else {
                                for(TNNode grandGrandChild : network.jChildren(grandChild)) {
                                    assert grandGrandChild.isAction();
                                    g.addEdge(n, grandGrandChild);
                                }
                            }
                        }
                    }
                }
            }
        }
        g.exportToDotFile(filename, new TNPrinter());
    }
}
