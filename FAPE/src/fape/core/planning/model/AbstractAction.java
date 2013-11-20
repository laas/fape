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

import fape.core.execution.model.ActionRef;
import fape.core.execution.model.Instance;
import fape.core.execution.model.TemporalConstraint;
import fape.core.planning.states.State;
import fape.util.Pair;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class AbstractAction {
    public String name;
    public List<AbstractTemporalEvent> events = new LinkedList<>();
    public List<Instance> params;
    public List<Pair<List<ActionRef>, List<TemporalConstraint>>> strongDecompositions;

    public float GetDuration(State st) {
        return 1.0f;
    }
}
