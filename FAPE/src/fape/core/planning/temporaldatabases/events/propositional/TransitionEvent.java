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

import fape.core.planning.model.StateVariableValue;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.core.planning.temporaldatabases.events.resources.ConsumeEvent;

/**
 *
 * @author FD
 */
public class TransitionEvent extends TemporalEvent {
    public StateVariableValue from, to;

    @Override
    public TemporalEvent cc() {
        TransitionEvent ret = new TransitionEvent();
        ret.from = from;
        ret.to = to;
        return ret;
    }
}
