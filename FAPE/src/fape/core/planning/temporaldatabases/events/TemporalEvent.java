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

import fape.core.planning.bindings.ObjectVariable;
import fape.core.planning.stn.TemporalVariable;

/**
 *
 * @author FD
 */
public abstract class TemporalEvent {
    //public ObjectVariable objectVar;
    /*public enum ETemporalEventType{
        CONSUME, PRODUCE, SET, PERSIST, TRANSITION, CONDITION
    }*/
    
    public TemporalVariable start, end;
    
    //public ObjectVariable objectVar;
    
    public abstract TemporalEvent cc();
}
