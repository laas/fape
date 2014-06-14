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

import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

/**
 *
 * @author F
 */
public class JustificationGraph {

    private class Vertex {

        RelaxedGroundAtom at;
        LinkedHashSet<Edge> toMe, fromMe;
        boolean goalZone, examined, examined2;

        @Override
        public boolean equals(Object obj) {
            return at.equals(((Vertex) obj).at);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + (this.at != null ? this.at.hashCode() : 0);
            return hash;
        }

        @Override
        public String toString() {
            return at.toString();
        }

        public Vertex(RelaxedGroundAtom atm) {
            examined = false;
            goalZone = false;
            at = atm;
            toMe = new LinkedHashSet<>();
            fromMe = new LinkedHashSet<>();
        }
    }

    private class Edge {

        RelaxedGroundAction orig;
        Vertex from, to;
        float cost;

        public Edge(Vertex _from, Vertex _to, float _cost, RelaxedGroundAction ai) {
            from = _from;
            to = _to;
            orig = ai;
            cost = _cost;
        }

        @Override
        public String toString() {
            return orig.toString();
        }
    }
    Vertex[] vertices;
    Vertex[] init, goal;
    float cutMin;

    JustificationGraph(HashMap<RelaxedGroundAtom, Float> caCost, HashMap<RelaxedGroundAction, Float> cActCost, Iterable<RelaxedGroundAction> actions, BitSet _init, BitSet _goal) {
        vertices = new Vertex[caCost.size()];

        for (RelaxedGroundAction a : actions) {
            float maxVal = -1;
            RelaxedGroundAtom hMaxAtom = null;
            for (RelaxedGroundAtom at : a.pre) {
                if (caCost.get(at) > maxVal) {
                    hMaxAtom = at;
                    maxVal = caCost.get(at);
                }
            }
            for (RelaxedGroundAtom at : a.eff) {
                addEdge(hMaxAtom, at, cActCost.get(a), a);
            }
        }

        init = new Vertex[_init.cardinality()];
        goal = new Vertex[_goal.cardinality()];
        int ct = 0;
        for (int i = _init.nextSetBit(0); i >= 0; i = _init.nextSetBit(i + 1)) {
            init[ct++] = vertices[i];
        }
        ct = 0;
        for (int i = _goal.nextSetBit(0); i >= 0; i = _goal.nextSetBit(i + 1)) {
            goal[ct++] = vertices[i];
        }
    }

    private void addEdge(RelaxedGroundAtom _from, RelaxedGroundAtom _to, float cost, RelaxedGroundAction ai) {
        if (vertices[_from.mID] == null) {
            vertices[_from.mID] = new Vertex(_from);
        }
        if (vertices[_to.mID] == null) {
            vertices[_to.mID] = new Vertex(_to);
        }
        Edge e = new Edge(vertices[_from.mID], vertices[_to.mID], cost, ai);
        vertices[_from.mID].fromMe.add(e);
        vertices[_to.mID].toMe.add(e);
    }

    private void spreadGoalZone(Vertex v) {
        try{
        v.examined2 = true;
        }catch(Exception e){
            int xx= 0;
        }
        v.goalZone = true;
        for (Edge e : v.toMe) {
            if (e.cost < 0.001f && !e.from.examined2) { //TODO check this, looks unstable
                spreadGoalZone(e.from);
            }
        }
    }

    private void gatherCut(LinkedHashSet<RelaxedGroundAction> cut, Vertex v) {
        if (v == null) {
            return;
        }
        v.examined = true;


        for (Edge e : v.fromMe) {
            if (e.to.goalZone) {
                if (!v.goalZone) {
                    cutMin = Math.min(cutMin, e.cost);
                    cut.add(e.orig);
                }
            } else if (!e.to.examined) {
                gatherCut(cut, e.to);
            }
        }
    }

    public LinkedHashSet<RelaxedGroundAction> FindCut() {
        //find atoms that lead to an atom with a zero cost path
        for (Vertex v : goal) {
            spreadGoalZone(v);
        }

        boolean quickerEnd = false;
        for (Vertex v : init) {
            if (v != null && v.goalZone) {
                quickerEnd = true;
            }
        }
        if (quickerEnd) {
            return new LinkedHashSet<>();
        }

        //generate cut actions
        LinkedHashSet<RelaxedGroundAction> cut = new LinkedHashSet<>();
        cutMin = Float.POSITIVE_INFINITY;
        for (Vertex v : init) {
            gatherCut(cut, v);
        }
        return cut;
    }

    public float GetCutMin() {
        return cutMin;
    }
}
