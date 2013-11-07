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
import fape.util.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class BindingManager {
    List<Pair<ObjectVariable, List<StateVariable> > > bindings = new LinkedList<>();
    public ObjectVariable getNewObjectVariable(){
        return new ObjectVariable();
    }
}
