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
package fape.core.planning.temporaldatabases.events.propositional;

import fape.core.planning.constraints.ConstraintNetworkManager;
import fape.core.planning.model.VariableRef;
import fape.core.planning.temporaldatabases.events.TemporalEvent;

/**
 *
 * @author FD
 */
public class TransitionEvent extends TemporalEvent {

    /**
     *
     */
    public VariableRef from, to;

    /**
     *
     * @param mn
     * @param assignNewID
     * @return
     */
    @Override
    public TemporalEvent cc(ConstraintNetworkManager mn, boolean assignNewID) {
        TransitionEvent ret = new TransitionEvent();
        if(assignNewID){
            ret.mID = counter++;
        }else{
            ret.mID = this.mID;
        }
        ret.from = from;
        ret.to = to;
        return ret;
    }

    @Override
    public String toString() {
        return "@[" + start + "," + end + "):" + from + "->" + to;
    }

    @Override
    public TemporalEvent DeepCopy(ConstraintNetworkManager m, boolean assignNewID) {
        TransitionEvent e = new TransitionEvent();
        if (this.from != null) {
            e.from = this.from;
        }
        e.mID = this.mID;
        e.to = this.to;
        e.start = this.start;
        e.end = this.end;
        return e;
    }

    @Override
    public String Report() {
        return "[" + start + "," + end + "] transition "+((from==null)?"null":from)+" -> "+((to==null)?"null":to);
    }
}
