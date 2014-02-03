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
import fape.core.planning.temporaldatabases.TemporalDatabaseManager;

/**
 *
 * @author FD
 */
public abstract class TemporalEvent {

    public static int counter = 0;
    public int mID = -1;
    
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
     * The id of the database containing this event.
     */
    public int tdbID = -1;

    //public ObjectVariable objectVar;
    /**
     *
     * @param mn
     * @return
     */
    public abstract TemporalEvent cc(ConstraintNetworkManager mn, boolean assignNewID);

    public abstract TemporalEvent DeepCopy(ConstraintNetworkManager m, boolean assignNewID);
}
