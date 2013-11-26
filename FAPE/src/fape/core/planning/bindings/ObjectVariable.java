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

package fape.core.planning.bindings;

import fape.core.planning.model.StateVariable;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class ObjectVariable {
    
    public List<StateVariable> domain = new LinkedList<>();
    
    /*public String typeDerivation;
    
    public ObjectVariable(String typeDerivation_){
        typeDerivation = typeDerivation_;
    }*/
    
    private static int idCounter = 0;
    private final int mID = idCounter++;

    @Override
    public boolean equals(Object obj) {
        return mID == ((ObjectVariable)(obj)).mID;
    }
    
    public boolean equals(ObjectVariable obj) {
        return mID == obj.mID;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + this.mID;
        return hash;
    }
}
