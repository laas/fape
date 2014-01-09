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
package fape.core.planning.temporaldatabases.events;

import fape.core.planning.constraints.ConstraintNetworkManager;
import fape.core.planning.stn.TemporalVariable;
import fape.core.planning.temporaldatabases.TemporalDatabase;

/**
 *
 * @author FD
 */
public abstract class TemporalEvent {

    //public ObjectVariable objectVar;
    /*public enum ETemporalEventType{
     CONSUME, PRODUCE, SET, PERSIST, TRANSITION, CONDITION
     }*/
    public abstract String Report();
    /**
     *
     */
    public TemporalVariable start,
            /**
             *
             */
            end;
    //reflection needed for stronger reasoning

    /**
     *
     */
    public TemporalDatabase mDatabase = null;

    //public ObjectVariable objectVar;
    /**
     *
     * @param mn
     * @return
     */
    public abstract TemporalEvent cc(ConstraintNetworkManager mn);

    public abstract TemporalEvent DeepCopy(ConstraintNetworkManager m);
}
