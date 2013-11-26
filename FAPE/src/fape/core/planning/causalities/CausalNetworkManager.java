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
package fape.core.planning.causalities;

import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.util.Pair;

/**
 *
 * @author FD
 */
public class CausalNetworkManager {

    CausalNetwork net = new CausalNetwork();

    public void AddEdge(TemporalEvent supporter, TemporalEvent supportee) {
        net.edges.add(new Pair(supporter, supportee));
    }
}
