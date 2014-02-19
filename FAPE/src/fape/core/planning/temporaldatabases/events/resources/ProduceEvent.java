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
package fape.core.planning.temporaldatabases.events.resources;

import fape.core.planning.constraints.ConstraintNetworkManager;
import fape.core.planning.model.Action;
import fape.core.planning.temporaldatabases.events.TemporalEvent;

/**
 *
 * @author FD
 */
public class ProduceEvent extends TemporalEvent {

    /**
     *
     */
    public double howMuch;

    /**
     *
     * @return
     */
    @Override
    public TemporalEvent cc(boolean assignNewID) {
        ProduceEvent ret = new ProduceEvent();
        ret.howMuch = howMuch;
        return ret;
    }

    @Override
    public TemporalEvent DeepCopy(boolean assignNewID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public TemporalEvent bindedCopy(Action a) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String Report() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
