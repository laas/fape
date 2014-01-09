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
package fape.core.planning.model;

import fape.core.planning.constraints.ConstraintNetworkManager;
import fape.core.planning.temporaldatabases.IUnifiable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class StateVariableValue extends IUnifiable {

    public boolean Unifiable(StateVariableValue val1) {
        List<String> vals = new LinkedList<>(val1.values);
        vals.retainAll(this.values);
        return vals.size() > 0;
    }

    public List<String> values = new LinkedList<>();

    /**
     * defines the parameter representing the value
     */
    public String valueDescription;
    /**
     * 
     * @return the parameter value used to describe this main object constant
     */
    public String GetObjectParameter(){
        if(valueDescription.contains(".")){
            return valueDescription.substring(0, valueDescription.indexOf("."));
        }else{
            return valueDescription;
        }        
    }

    /**
     *
     * @param assignNewUniqueID
     */
    public StateVariableValue(boolean assignNewUniqueID) {
        if (assignNewUniqueID) {
            mID = idCounter++;
        }
    }

    public StateVariableValue DeepCopy(ConstraintNetworkManager m) {
        StateVariableValue newVar = new StateVariableValue(false);
        newVar.mID = this.mID;
        newVar.valueDescription = this.valueDescription;
        newVar.values = new LinkedList<>(this.values);
        m.AddUnifiable(newVar);
        return newVar;
    }

    @Override
    public String toString() {
        return valueDescription + " " + values.toString();
    }

    @Override
    public List<String> GetDomainObjectConstants() {
        return values;
    }

    @Override
    public boolean ReduceDomain(HashSet<String> supported) {
        int orig = values.size();
        values.retainAll(supported);
        return orig != values.size();
    }

    @Override
    public int GetUniqueID() {
        return mID;
    }

    @Override
    public boolean EmptyDomain() {
        return values.isEmpty();
    }

    public String Report() {
        return this.values.toString();
    }
}
