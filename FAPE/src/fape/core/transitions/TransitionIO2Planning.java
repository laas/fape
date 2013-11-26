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
import fape.core.planning.bindings.ObjectVariable;
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
import fape.core.planning.temporaldatabases.events.resources.ConsumeEvent;
import fape.core.planning.temporaldatabases.events.resources.ProduceEvent;
import fape.core.planning.temporaldatabases.events.resources.SetEvent;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import java.util.HashMap;
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
     * @return
     */
    public static List<fape.core.planning.model.StateVariable> decomposeInstance(String qualifyingName, String name, String type, HashMap<String, fape.core.planning.model.Type> types, String rootType) {
        LinkedList<fape.core.planning.model.StateVariable> ret = new LinkedList<>();
        if (types.containsKey(type)) {
            Type tp = types.get(type);
            for (String nm : tp.contents.keySet()) {
                ret.addAll(decomposeInstance(qualifyingName + name + ".", nm, tp.contents.get(nm), types, rootType));
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

    public static TemporalEvent ProduceTemporalEvent(Statement s) {
        TemporalEvent ev = null;
        if (s.operator == null) {
            int xx = 0;
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
                    eve4.to = new StateVariableValue();
                    eve4.to.value = s.from.toString();
                    ev = eve4;
                }
                break;
            case "==":
                if (s.IsResourceRelated()) {
                    throw new UnsupportedOperationException();
                } else {
                    if (s.to != null) {
                        //this is a transition event
                        TransitionEvent eve5 = new TransitionEvent();
                        eve5.from = new StateVariableValue();
                        eve5.to = new StateVariableValue();
                        eve5.from.value = s.from.toString();
                        eve5.to.value = s.to.toString();
                        ev = eve5;
                    } else {
                        //this is a persistence event
                        PersistenceEvent eve6 = new PersistenceEvent();
                        eve6.value = new StateVariableValue();
                        eve6.value.value = s.rightRef.toString();
                        ev = eve6;
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown operator.");
        }
        return ev;
    }

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
     */
    public static void InsertStatementIntoState(Statement s, StateVariable v, State st) {
        if (v == null) {
            throw new FAPEException("Unknown state variable: " + s.GetVariableName());
        }
        //switch here based on the statement type

        // create a new object variable
        ObjectVariable var = st.bindings.getNewObjectVariable();
        var.domain.add(v);

        // create a temporal database for this variable
        TemporalDatabase db = st.tdb.GetNewDatabase();
        db.var = var;

        // create a new event for the termporal database that corresponds to the
        // statement
        TemporalEvent ev = ProduceTemporalEvent(s);
        AddTimePoints(ev, s, st);

        //add the event to the database
        db.events.add(ev);

    }

    public static AbstractAction TransformAction(Action a) {
        AbstractAction act = new AbstractAction();
        act.name = a.name;
        act.params = a.params;
        for (Statement s : a.statements) {
            AbstractTemporalEvent ev = new AbstractTemporalEvent(ProduceTemporalEvent(s), s.interval, s.leftRef);
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
        return act;
    }
}
