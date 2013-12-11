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

import fape.core.execution.model.Reference;
import fape.core.execution.model.TemporalInterval;
import fape.core.planning.stn.TemporalVariable;
import fape.core.planning.temporaldatabases.events.TemporalEvent;

/**
 *
 * @author FD
 */
public class AbstractTemporalEvent {
    //this event works as an abstraction for further carbon-copying

    /**
     *
     */
        public TemporalEvent event;

    /**
     *
     */
    public TemporalInterval interval;

    /**
     *
     */
    public Reference stateVariableReference;

    /**
     *
     */
    public String varType;

    /**
     *
     * @param ProduceTemporalEvent
     * @param interval_
     * @param leftRef
     * @param varType_
     */
    public AbstractTemporalEvent(TemporalEvent ProduceTemporalEvent, TemporalInterval interval_, Reference leftRef,String varType_) {
        event = ProduceTemporalEvent;
        interval = interval_;
        stateVariableReference = leftRef;
        varType = varType_;
    }

    /**
     *
     * @param var_id
     * @return
     */
    public boolean SupportsStateVariable(String var_id) {
        /*String mSuffix = stateVariableReference.toString();
        mSuffix = mSuffix.substring(mSuffix.indexOf("."));
        String oSuffix = var_id.substring(var_id.indexOf("."));
        return mSuffix.equals(oSuffix);*/
        return varType.equals(var_id);
    }
    
    
}
