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

import fape.core.planning.temporaldatabases.events.TemporalEvent;

/**
 *
 * @author FD
 */
public class ConsumeEvent extends TemporalEvent {
    public double howMuch;

    @Override
    public TemporalEvent cc() {
        ConsumeEvent ret = new ConsumeEvent();
        ret.howMuch = howMuch;
        return ret;
    }
}
