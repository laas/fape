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

package fape.core.planning.search.flaws.resolvers;

import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.TPRef;

/**
 *
 * @author FD
 */
public class ResourceSupportingDecomposition extends Resolver {
    public Action resourceMotivatedActionToDecompose;
    public int decompositionID;
    public boolean before;
    public TPRef when;

    @Override
    public boolean hasDecomposition() { return true; }
    @Override
    public Action actionToDecompose() { return resourceMotivatedActionToDecompose; }
}
