/*
 * This file is part of Filuta.
 *
 * Filuta  - AI planning system with time and resources
 *
 * Author: Filip Dvořák (filip.dvorak@runbox.com)
 * (C) Copyright 2008-2009 Filip Dvořák
 *
 */
package fape.core.planning.dtgs;

import fape.core.execution.model.Instance;
import fape.core.execution.model.Reference;
import fape.core.planning.Planner;
import fape.core.planning.model.*;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.events.propositional.TransitionEvent;
import fape.exceptions.FAPEException;

import java.util.*;

/**
 * Domain transition graph, contains methods for its creation and for providing
 * ordered paths.
 *
 * this version is abstracted, the edges are labeled with uninstatiated abstract
 * actions
 *
 * @author Filip Dvořák
 */
public class ADTG {

    /**
     * array of names of values, parsed from original representation
     */
    //public String names[];
    /**
     * DTG id
     */
    /**
     * amount of values in DTG
     */
    public int var_size;

    //maps value names to their indexes in the system    
    /*public HashMap<String, Integer> valueIndexes = new HashMap<>();
     int valueIndexCounter = 0;

     public void AddValue(String nm) {
     if (valueIndexes.containsKey(nm)) {
     throw new FAPEException("Type already defined.");
     }
     valueIndexes.put(nm, valueIndexCounter);
     valueIndexCounter++;
     }*/
    public Type mType;

    /**
     * Map every object of type mType to an int id used for
     * indexing in th graph.
     */
    private HashMap<String, Integer> instances = new HashMap<>();
    private int nextId = 0;

    private final Planner planner;

    /**
     *
     */
    public String var_id;
    /**
     * the graph itself, graph[i][j] contains actions that change i-th value to
     * j-th value
     */
    public DTGEdge graph[][];
    /**
     * weighted graph, measures operators
     */
    public float op_cost[][];
    /**
     * weighted graph, measures time
     */
    public float time_cost[][];

    /**
     * weighted graph, measures resource usage
     */
    //public float res_cost[][];
    /**
     * reads names.
     *
     * @param v
     */
    /*void rewrite_names(Variable v){
     names = v.val_names;
     }*/
    /**
     *
     */
    /*void csv_export(){
     String out;// = new String();
     out = "";
     for(int i = 0; i < this.var_size; i++){
     for(int j = 0; j < this.var_size; j++){
     if(this.graph[i][j] != null)
     out += i+", "+j+"\n";
     }
     }
     SimpleStructures.FileHandling.file_output(out, "test.csv");

     }*/
    /**
     * represents comparison according to (op,time) measure
     *
     * @param from1 state varibale value
     * @param from2 state varibale value
     * @param to state varibale value
     * @return true if path from <b>from1</b> to <b>to</b> is shorter than from
     * <b>from2</b>, false otherwise
     */
    public boolean shorter(int from1, int from2, int to) {
        return (op_cost[from1][to] < op_cost[from2][to]
                || (op_cost[from1][to] == op_cost[from2][to]
                && time_cost[from1][to] < time_cost[from2][to]));
    }

    /**
     * computes path according to measure (op,time)
     *
     * @param from state varibale value
     * @param to state varibale value
     * @return weigth of the path
     */
    public float path_len(int from, int to) {
        return op_cost[from][to] * 100000 + time_cost[from][to];
    }

    /**
     * finds all reachable values vi from <b>from</b> and sorts them according
     * to (op,time) upon (vi, <b>to</b>)
     *
     * @param from state varibale value
     * @param to state varibale value
     * @param all_paths false if only shorter paths than current one should be
     * considered
     * @return set of values
     */
    public int[] get_sorted_one_way(int from, int to, boolean all_paths) {
        int pm[] = new int[this.var_size], pm_top = 0, ret[], pom;
        for (int i = 0; i < this.var_size; i++) {
            if (graph[from][i] != null && graph[from][i].act.size() > 0) {
                if (all_paths || shorter(i, from, to)) //
                {
                    pm[pm_top++] = i;
                }
            }
        }
        //sort
        for (int i = 0; i < pm_top; i++) {
            for (int j = 1; j < pm_top; j++) {
                if (shorter(pm[j], pm[j - 1], to)) {
                    pom = pm[j];
                    pm[j] = pm[j - 1];
                    pm[j - 1] = pom;
                }
            }
        }
        ret = new int[pm_top];
        System.arraycopy(pm, 0, ret, 0, pm_top);
        return ret;
    }

    /**
     * Performs Floyd-Warshal for all weighted graphs.
     */
    public void op_all_paths() { //fw - shortest paths
        float min;
        op_cost = new float[var_size][];
        time_cost = new float[var_size][];
        //res_cost = new float[var_size][];
        for (int i = 0; i < var_size; i++) {
            op_cost[i] = new float[var_size];
            time_cost[i] = new float[var_size];
            //res_cost[i] = new float[var_size];
            // edge inic
            for (int j = 0; j < var_size; j++) {
                if (graph[i][j] == null) {
                    op_cost[i][j] = 1000000;
                    time_cost[i][j] = 1000000;
                    //res_cost[i][j] = 1000000;

                } else {
                    op_cost[i][j] = 1;
                    min = 1000000;
                    for (int k = 0; k < graph[i][j].act.size(); k++) {
                        if (min > graph[i][j].act.get(k).GetDuration()) {
                            min = graph[i][j].act.get(k).GetDuration();
                        }
                    }
                    time_cost[i][j] = min;
                    /*if(graph[i][j].act.getFirst().res_events != null && graph[i][j].act.getFirst().res_events.length > 0)
                     res_cost[i][j] = graph[i][j].act.getFirst().res_events[0].value;*/
                }
            }
        }
        for (int k = 0; k < var_size; k++) {
            for (int i = 0; i < var_size; i++) {
                for (int j = 0; j < var_size; j++) {
                    if ((op_cost[i][j] > op_cost[i][k] + op_cost[k][j])
                            || ((op_cost[i][j] == op_cost[i][k] + op_cost[k][j])
                            && (time_cost[i][j] > time_cost[i][k] + time_cost[k][j]))) {

                        op_cost[i][j] = op_cost[i][k] + op_cost[k][j];
                        time_cost[i][j] = time_cost[i][k] + time_cost[k][j];
                        //res_cost[i][j] = res_cost[i][k] + res_cost[k][j];

                    }
                }
            }
        }
        //clear diagonal
        for (int i = 0; i < var_size; i++) {
            op_cost[i][i] = 0;
            time_cost[i][i] = 0;
            //res_cost [i][i] = 0;
        }

    }

    /**
     * Resource demand of the path (from,to).
     *
     * @param from
     * @param to
     * @return
     */
    /*public int resource_demand(int from, int to){
     return res_cost[from][to];
     }*/
    /**
     * Abstract Domain Transition Graphs
     *
     * @param t
     * @param actions
     */
    public ADTG(Planner planner, Type t, Collection<AbstractAction> actions) {
        this.planner = planner;
        var_id = t.name;
        List<String> vars = planner.types.instances(t.name);
        var_size = vars.size();
        mType = t;

        int i, j;
        graph = new DTGEdge[var_size][];
        for(String var : vars) {
            instances.put(var, nextId);
            graph[nextId] = new DTGEdge[var_size];
            nextId++;
        }

        // TODO: this is not really usefull, we just repeat the same thing n^2 times
        // we might need to rethink the whole DTG thing
        for (AbstractAction a : actions) {
            for (j = 0; j < a.events.size(); j++) {
                if (a.events.get(j).isTransitionEvent() && a.events.get(j).SupportsStateVariable(var_id)) {
                    //TransitionEvent te = (TransitionEvent) a.events.get(j).event;
                    for (int ii = 0; ii < graph.length; ii++) {
                        for (int jj = ii; jj < graph.length; jj++) {
                            if (graph[ii][jj] == null) {
                                graph[ii][jj] = new DTGEdge();
                            }
                            graph[ii][jj].push(a);
                        }
                    }
                }
            }
        }
    }


    /**
     *
     * @param db
     * @return
     */
    public HashSet<String> GetActionSupporters(Planner pl, State st, TemporalDatabase db) { //TODO remove reference to planner
        VariableRef var = db.GetGlobalConsumeValue();
        List<Integer> mValues = new LinkedList<>();

        for (String val : st.parameterBindings.get(var).domain) {
            mValues.add(instances.get(val));
        }

        // All actions that may be enablers for this dtb
        HashSet<AbstractAction> potentialSupporterActions = new HashSet<>();

        // Get all actions that might be supporting this database according to the DTG
        for (DTGEdge[] graph1 : graph) {
            for (Integer i : mValues) {
                DTGEdge e = null;
                e = graph1[i];
                if (e != null && e.act != null) {
                    for (AbstractAction a : e.act) {
                        potentialSupporterActions.add(a);
                    }
                }
            }
        }



        // all actions that definitly support the database
        HashSet<String> supporterActionNames = new HashSet<>();

        // for all actions, check if there is a transition event supporting the database
        for(AbstractAction a : potentialSupporterActions) {
            for (AbstractTemporalEvent eve : a.events) {
                /*
                List<StateVariable> list = new LinkedList<>(eve.stateVariableDomain);
                list.retainAll(db.domain);
                if (!list.isEmpty() && eve.event instanceof TransitionEvent) {
                    if (((TransitionEvent) eve.event).to.Unifiable(db.GetGlobalConsumeValue())) {
                        //this action is a supporter, add it to the list and go to the next one
                        supporterActionNames.add(a.name);
                        break;
                    }
                }
                */
                if(CanBeSupporter(pl, st, a, eve, db)) {
                    supporterActionNames.add(a.name);
                    break;
                }
            }
        }

        
        return supporterActionNames;
    }


    /**
     * Returns true if the TemporalEvent abs of action a can be supporter
     * of Temporal database db.
     * @param pl
     * @param st
     * @param a
     * @param abs
     * @param db
     * @return
     */
    private boolean CanBeSupporter(Planner pl, State st, AbstractAction a, AbstractTemporalEvent abs, TemporalDatabase db) {
        if(abs.isPersistenceEvent()) {
            return false;
        }

        if(!abs.varType.equals(db.stateVariable.type)) {
            return false;
        }
        Reference svRef = new Reference(abs.stateVariableReference);
        if(svRef.refs.size() != 2)
            throw new FAPEException("event on something that is not a state variable: "+svRef);
        if(!svRef.refs.get(1).equals(db.stateVariable.predicateName))
            return false;


        String predVar = svRef.GetConstantReference();
        Set<String> possiblePredVarValues;
        if(st.parameterBindings.containsKey(predVar)) {
            possiblePredVarValues = new TreeSet(st.parameterBindings.get(predVar).domain);
        } else {
            String type = null;
            for(Instance i : a.params) {
                if(predVar.equals(i.name)) {
                    type = i.type;
                }
            }
            if(type == null) {
                throw new FAPEException("Unable to find variable: "+predVar);
            }
            possiblePredVarValues = new TreeSet(pl.types.instances(type));
        }

        Set possibleRightPredVarValues = new TreeSet(st.parameterBindings.get(db.stateVariable.variable).domain);
        possiblePredVarValues.retainAll(possibleRightPredVarValues);
        if(possiblePredVarValues.isEmpty())
            return false;





        String supportVar = ((TransitionEvent) abs.event).to.var;
        Set<String> possibleLeftValues;
        if(st.parameterBindings.containsKey(supportVar)) {
            possibleLeftValues = new TreeSet(st.parameterBindings.get(supportVar).domain);
        } else {
            String type = null;
            for(Instance i : a.params) {
                if(supportVar.equals(i.name)) {
                    type = i.type;
                }
            }
            if(type == null) {
                throw new FAPEException("Unable to find variable: "+supportVar);
            }
            possibleLeftValues = new TreeSet(pl.types.instances(type));
        }

        Set possibleRightValues = new TreeSet(st.parameterBindings.get(db.GetGlobalConsumeValue()).domain);
        possibleLeftValues.retainAll(possibleRightValues);
        if(possibleLeftValues.isEmpty()) {
            return false;
        }


        return true;
    }
}
