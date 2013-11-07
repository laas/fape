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

package fape.core.planning.temporaldatabases;

import fape.core.planning.bindings.ObjectVariable;
import fape.core.planning.temporaldatabases.events.TemporalEvent;

import java.util.List;

/**
 * records the events for one state variable
 * @author FD
 */
public class TemporalDatabase {
    ObjectVariable var;
    List<TemporalEvent> events;
}
