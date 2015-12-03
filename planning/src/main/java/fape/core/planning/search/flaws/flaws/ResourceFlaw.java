/*
 * Author:  Filip Dvorak <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvorak <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */

package fape.core.planning.search.flaws.flaws;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.states.State;

import java.util.List;

/**
 *
 * @author FD
 */
public class ResourceFlaw extends Flaw {
    //this.resolvers = new LinkedList<>();

    public ResourceFlaw() {
        throw new UnsupportedOperationException("Resources are not supported yet.");
    }

    @Override
    public List<Resolver> getResolvers(State st, APlanner planner) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int compareTo(Flaw o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
