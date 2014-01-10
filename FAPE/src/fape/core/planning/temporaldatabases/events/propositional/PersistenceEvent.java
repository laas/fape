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
import fape.core.planning.model.StateVariableValue;
import fape.core.planning.temporaldatabases.events.TemporalEvent;

/**
 *
 * @author FD
 */
public class PersistenceEvent extends TemporalEvent {

    /**
     *
     */
    public StateVariableValue value;

    /**
     *
     * @return
     */
    @Override
    public TemporalEvent cc(ConstraintNetworkManager mn, boolean assignNewID) {
        PersistenceEvent ret = new PersistenceEvent();
        ret.value = value.DeepCopy(mn, assignNewID);
        return ret;
    }

    public String toString() {
        return "@[" + start + "," + end + "):=" + value;
    }

    @Override

    public TemporalEvent DeepCopy(ConstraintNetworkManager m, boolean assignNewID) {
        PersistenceEvent e = new PersistenceEvent();
        e.value = this.value.DeepCopy(m, assignNewID);
        e.start = this.start;
        e.end = this.end;
        return e;
    }

    @Override
    public String Report() {
        return "persistence "+value.Report();
    }
}
