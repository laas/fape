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
import fape.core.planning.model.Action;
import fape.core.planning.model.ParameterizedStateVariable;
import fape.core.planning.model.VariableRef;
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
    public ParameterizedStateVariable stateVariable = null;

    public abstract String Report();

    public TemporalVariable start, end;

    /**
     * The id of the database containing this event.
     * TODO: only thing preventing us from immutable events
     */
    public int tdbID = -1;

    public abstract VariableRef endValue();
    public abstract VariableRef startValue();

    public abstract TemporalEvent DeepCopy(boolean assignNewID);

    /**
     * Creates a copy of the temporal event where all references to local
     * variables (from action a) are replaced with global ones.
     * @param a
     * @return
     */
    public abstract TemporalEvent bindedCopy(Action a);
}
