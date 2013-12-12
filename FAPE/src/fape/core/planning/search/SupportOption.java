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

package fape.core.planning.search;

import fape.core.planning.model.AbstractAction;
import fape.core.planning.model.Action;
import fape.core.planning.temporaldatabases.TemporalDatabase;

/**
 * non-null values represent the option
 * @author FD
 */
public class SupportOption {

    /**
     *
     */
    public TemporalDatabase tdb;

    /**
     *
     */
    public TemporalDatabase.ChainComponent precedingComponent;

    /**
     *
     */
    public AbstractAction supportingAction;

    /**
     *
     */
    public Action actionToDecompose;

    /**
     *
     */
    public int decompositionID = -1;

    public String toString() {
        //return "" + tdb + " " + precedingComponent + " " + supportingAction + " " + actionToDecompose;
        if (tdb != null && precedingComponent != null) {
            // this is database merge of one persistence into another
            return "{merge of two persistences, tdb="+tdb+", preceding="+precedingComponent;
        } else if (tdb != null) {
            //this is a database concatenation
            return "{DB Concatenation w/ "+tdb+"}";
        } else if (supportingAction != null) {
            //this is a simple applciation of an action
            return "{ActionApplication "+supportingAction+"}";
        } else if (actionToDecompose != null) {
            // this is a task decomposition
            return "{ActionDecomposition "+actionToDecompose+"}";
        } else {
            return "Unknown option.";
        }
    }
}
