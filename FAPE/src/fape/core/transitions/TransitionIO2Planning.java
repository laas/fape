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

import fape.core.execution.model.Instance;
import fape.core.execution.model.statements.Statement;
import fape.core.planning.model.StateVariable;
import fape.core.planning.model.StateVariableBoolean;
import fape.core.planning.model.StateVariableFloat;
import fape.core.planning.model.StateVariableInteger;
import fape.core.planning.model.Type;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
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
     * @param types
     * @return
     */
    public static List<fape.core.planning.model.StateVariable> decomposeInstance(String qualifyingName, String name, String type, HashMap<String, fape.core.planning.model.Type> types) {
        LinkedList<fape.core.planning.model.StateVariable> ret = new LinkedList<>();
        if (types.containsKey(type)) {
            Type tp = types.get(type);
            for (String nm : tp.contents.keySet()) {
                ret.addAll(decomposeInstance(qualifyingName + name + ".", nm, tp.contents.get(nm), types));
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
                    throw new FAPEException("Error: Unknown type: "+type);                    
            }
            var.name = qualifyingName + name;
            ret.add(var);
        }
        return ret;
    }
    /**
     * we take the statement on input and add it into the corresponding state variable
     * @param s
     * @param v 
     * @param st 
     */
    public static void InsertStatementIntoVariable(Statement s, StateVariable v, State st){
        if(v == null){
            throw new FAPEException("Unknown state variable: "+s.GetVariableName());
        }
        //switch here based on the variable type
        
        
        
    }
}
