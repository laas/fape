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
package fape.core.transitions;

import fape.core.execution.model.Action;
import fape.core.execution.model.ActionRef;
import fape.core.execution.model.Instance;
import fape.core.execution.model.TemporalConstraint;
import fape.core.execution.model.statements.Statement;
import fape.core.planning.constraints.ConstraintNetworkManager;
import fape.core.planning.model.AbstractAction;
import fape.core.planning.model.AbstractTemporalEvent;
import fape.core.planning.model.StateVariable;
import fape.core.planning.model.StateVariableBoolean;
import fape.core.planning.model.StateVariableEnum;
import fape.core.planning.model.StateVariableFloat;
import fape.core.planning.model.StateVariableInteger;
import fape.core.planning.model.StateVariableValue;
import fape.core.planning.model.Type;
import fape.core.planning.states.State;
import fape.core.planning.stn.TemporalVariable;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.TemporalDatabaseManager;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.core.planning.temporaldatabases.events.propositional.PersistenceEvent;
import fape.core.planning.temporaldatabases.events.propositional.TransitionEvent;
import fape.core.planning.temporaldatabases.events.resources.ConditionEvent;
import fape.core.planning.temporaldatabases.events.resources.ConsumeEvent;
import fape.core.planning.temporaldatabases.events.resources.ProduceEvent;
import fape.core.planning.temporaldatabases.events.resources.SetEvent;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * realizes the transfers between different models
 *
 * @author FD
 */
public class TransitionIO2Planning {

    /**
     * take all of the parent and replicate it into the offspring
     *
     * @param t
     * @param types
     * @return
     */
    public static fape.core.planning.model.Type transformType(fape.core.execution.model.types.Type t, HashMap<String, fape.core.planning.model.Type> types) {
        if (types.containsKey(t.name)) {
            throw new FAPEException("Error: incremental type override " + t.name);
        }
        if (!types.containsKey(t.parent)) {
            throw new FAPEException("Error: unknown parent type: " + t.parent);
        }
        fape.core.planning.model.Type ret = new Type(types.get(t.parent));
        ret.name = t.name;
        for (Instance i : t.instances) {
            ret.contents.put(i.name, i.type);
        }
        return ret;
    }

    /**
     * taking an instance of some type, decompose it into state variables
     *
     * @param qualifyingName
     * @param name
     * @param type
     * @param types
     * @param rootType
     * @return
     */
    public static List<fape.core.planning.model.StateVariable> decomposeInstance(String qualifyingName, String name, String type, HashMap<String, fape.core.planning.model.Type> types, String rootType, boolean firstLevel) {
        LinkedList<fape.core.planning.model.StateVariable> ret = new LinkedList<>();
        if (types.containsKey(type)) {
            Type tp = types.get(type);
            if (firstLevel) {
                for (String nm : tp.contents.keySet()) {
                    ret.addAll(decomposeInstance(qualifyingName + name + ".", nm, tp.contents.get(nm), types, rootType, false));
                }
            }
            // we yet need to add a type of itself, if this is not the top level
            if (!"".equals(qualifyingName)) {
                StateVariableEnum var = new StateVariableEnum();
                var.name = qualifyingName + name;
                var.type = type;
                var.typeDerivationName = rootType + qualifyingName.substring(qualifyingName.indexOf(".")) + name;
                ret.add(var);
            }

        } else {
            StateVariable var;
            switch (type) {
                case "boolean":
                    var = new StateVariableBoolean();
                    break;
                case "float":
                    var = new StateVariableFloat();
                    break;
                case "integer":
                    var = new StateVariableInteger();
                    break;
                default:
                    throw new FAPEException("Error: Unknown type: " + type);
            }
            var.name = qualifyingName + name;
            var.type = type;
            var.typeDerivationName = rootType + qualifyingName.substring(qualifyingName.indexOf(".")) + name;
            ret.add(var);
        }
        return ret;
    }

    /**
     *
     * @param s
     * @param assignUniqueIDToValues
     * @param mn
     * @param fromDomain
     * @param toDomain
     * @return
     */
    public static TemporalEvent ProduceTemporalEvent(Statement s, boolean assignUniqueIDToValues, ConstraintNetworkManager mn, List<String> fromDomain, List<String> toDomain) {
        TemporalEvent ev = null;
        if (s.operator == null) {
            throw new FAPEException(null);
        }
        switch (s.operator) {
            case ":produce":
                ProduceEvent eve = new ProduceEvent();
                eve.howMuch = s.GetResourceValue();
                ev = eve;
                break;
            case ":consume":
                ConsumeEvent eve2 = new ConsumeEvent();
                eve2.howMuch = s.GetResourceValue();
                ev = eve2;
                break;
            case ":=":
                if (s.IsResourceRelated()) {
                    SetEvent eve3 = new SetEvent();
                    eve3.howMuch = s.GetResourceValue();
                    ev = eve3;
                } else {
                    TransitionEvent eve4 = new TransitionEvent();
                    eve4.from = null; // can be any value
                    eve4.to = new StateVariableValue(assignUniqueIDToValues);
                    eve4.to.values = new LinkedList<>(fromDomain);
                    eve4.to.valueDescription = s.from.toString();
                    //if(eve4.to.valueDescription.substring(0, 1).)
                    mn.AddUnifiable(eve4.to);
                    ev = eve4;
                }
                break;
            case "==":
                if (s.IsResourceRelated()) {
                    throw new UnsupportedOperationException();
                } else {
                    if (s.from != null) {
                        //this is a transition event
                        TransitionEvent eve5 = new TransitionEvent();
                        eve5.from = new StateVariableValue(assignUniqueIDToValues);
                        eve5.to = new StateVariableValue(assignUniqueIDToValues);
                        mn.AddUnifiable(eve5.from);
                        mn.AddUnifiable(eve5.to);
                        eve5.from.values = new LinkedList<>(fromDomain);
                        eve5.to.values = new LinkedList<>(toDomain);
                        eve5.from.valueDescription = s.from.toString();
                        eve5.to.valueDescription = s.to.toString();
                        ev = eve5;
                    } else {
                        //this is a persistence event
                        PersistenceEvent eve6 = new PersistenceEvent();
                        eve6.value = new StateVariableValue(assignUniqueIDToValues);
                        eve6.value.values = new LinkedList<>(toDomain);
                        mn.AddUnifiable(eve6.value);
                        eve6.value.valueDescription = s.to.toString();
                        ev = eve6;
                    }
                }
                break;
            case ">=": {
                ConditionEvent eve6 = new ConditionEvent();
                eve6.operator = s.operator;
                eve6.value = s.value;
                ev = eve6;
                break;
            }
            case "<=": {
                ConditionEvent eve6 = new ConditionEvent();
                eve6.operator = s.operator;
                eve6.value = s.value;
                ev = eve6;
                break;
            }
            case ">": {
                ConditionEvent eve6 = new ConditionEvent();
                eve6.operator = s.operator;
                eve6.value = s.value;
                ev = eve6;
                break;
            }
            case "<": {
                ConditionEvent eve6 = new ConditionEvent();
                eve6.operator = s.operator;
                eve6.value = s.value;
                ev = eve6;
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown operator.");
        }
        return ev;
    }

    /**
     *
     * @param ev
     * @param s
     * @param state
     */
    public static void AddTimePoints(TemporalEvent ev, Statement s, State state) {
        TemporalVariable vs = state.tempoNet.getNewTemporalVariable(), ve = state.tempoNet.getNewTemporalVariable();

        //TODO: include some other constraints on those two ...
        ev.start = vs;
        ev.end = ve;

        state.tempoNet.EnforceBefore(vs, ve);
    }

    /**
     * we take the statement on input and add it into the corresponding state
     * variable
     *
     * @param s
     * @param v
     * @param st
     * @param types
     */
    public static void InsertStatementIntoState(Statement s, StateVariable v, State st) {
        if (v == null) {
            throw new FAPEException("Unknown state variable: " + s.GetVariableName());
        }
        //switch here based on the statement type

        // create a new object variable
        //ObjectVariable var = st.bindings.getNewObjectVariable();
        //var.domain.add(v);
        // create a temporal database for this variable
        TemporalDatabase db = st.tdb.GetNewDatabase(st.conNet);
        db.domain.add(v);
        //db.var = var;

        // create a new event for the termporal database that corresponds to the
        // statement
        List<String> firstDomain = null;
        List<String> secondDomain = null;
        if (s.from != null) {
            firstDomain = new LinkedList<>();
            firstDomain.add(s.from.GetConstantReference());
        }
        if (s.to != null) {
            secondDomain = new LinkedList<>();
            secondDomain.add(s.to.GetConstantReference());
        }
        TemporalEvent ev = ProduceTemporalEvent(s, true, st.conNet, firstDomain, secondDomain);
        AddTimePoints(ev, s, st);

        //add the event to the database
        db.AddEvent(ev);

        //add the event into the consumers, unless it is a statement event
        if (ev instanceof PersistenceEvent || (ev instanceof TransitionEvent && ((TransitionEvent) ev).from != null)) {
            st.consumers.add(db);
        }
    }

    /**
     *
     * @param a
     * @param vars
     * @param mn
     * @param types
     * @return
     */
    public static AbstractAction TransformAction(Action a, HashMap<String, StateVariable> vars, ConstraintNetworkManager mn, HashMap<String, fape.core.planning.model.Type> types) {
        AbstractAction act = new AbstractAction();
        act.name = a.name;
        act.params = a.params;
        act.param2Event = new ArrayList<>(act.params.size());
        for (Statement s : a.statements) {
            String varName = s.GetVariableName();
            String varType = "-1";
            varName = varName.substring(varName.indexOf("."));
            for (StateVariable sv : vars.values()) {
                String smallDeriv = sv.typeDerivationName.substring(sv.typeDerivationName.indexOf("."));
                if (smallDeriv.equals(varName)) {
                    varType = sv.type;
                }
            }
            /*String nm = s.leftRef.refs.getFirst();
             for (Instance i : act.params) {
             if (i.name.equals(nm)) {
             varType = i.type;
             }
             }*/
            HashMap<String, Type> parameterName2Type = new HashMap<>();
            for (Instance i : a.params) {
                parameterName2Type.put(i.name, types.get(i.type));
            }
            List<String> firstDomain = null;
            List<String> secondDomain = null;
            if (s.from != null) {
                firstDomain = new LinkedList<>();
                Type t = parameterName2Type.get(s.from.GetConstantReference());
                if (t != null) {
                    firstDomain.addAll(t.instances.keySet());
                } else {
                    firstDomain.add(s.from.toString());
                }
            }
            if (s.to != null) {
                secondDomain = new LinkedList<>();
                Type t = parameterName2Type.get(s.to.GetConstantReference());
                if (t != null) {
                    secondDomain.addAll(t.instances.keySet());
                } else {
                    secondDomain.add(s.to.toString());
                }
            }
            TemporalEvent eve = ProduceTemporalEvent(s, false, mn, firstDomain, secondDomain);

            //find the supported state variables
            List<StateVariable> supportedStateVariables = new LinkedList<>();
            if (eve instanceof TransitionEvent) {
                //((TransitionEvent) eve).to.
                String tp = null;
                for (Instance i : act.params) {
                    if (i.name.equals(s.leftRef.GetConstantReference())) {
                        tp = i.type;
                    }
                }
                String searchStr = tp + s.leftRef.toString().substring(s.leftRef.toString().indexOf("."));
                for (StateVariable sv : vars.values()) {
                    if (sv.typeDerivationName.equals(searchStr)) {
                        supportedStateVariables.add(sv);
                    }
                }
                if (supportedStateVariables.isEmpty()) {
                    throw new FAPEException("Empty domain.");
                }
            }

            AbstractTemporalEvent ev = new AbstractTemporalEvent(eve, s.interval, s.leftRef, varType, supportedStateVariables);
            act.events.add(ev);
            // now lets get all unmentioned parameters and add them from events to parameters
            /*String paramName = s.leftRef.refs.getFirst();
             boolean found = false;
             for(Instance i:act.params){
             if(i.name.equals(paramName)){
             found = true;
             }
             }
             if(!found){
             Instance i = new Instance();
             i.name = paramName;
             }*/
        }
        act.strongDecompositions = a.strongDecompositions;
        act.MapParametersToEvents();
        return act;
    }
}
