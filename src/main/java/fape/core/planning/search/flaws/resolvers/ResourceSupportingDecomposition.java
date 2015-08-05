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

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import planstack.anml.model.abs.AbstractDecomposition;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Decomposition;
import planstack.anml.model.concrete.Factory;
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

    @Override
    public boolean apply(State st, APlanner planner) {
        assert false : "TODO: check correctness.";
        // Apply the i^th decomposition of o.actionToDecompose, where i is given by
        // o.decompositionID

        // Abstract version of the decomposition
        AbstractDecomposition absDec = resourceMotivatedActionToDecompose.decompositions().get(decompositionID);

        // Decomposition (ie implementing StateModifier) containing all changes to be made to a search state.
        Decomposition dec = Factory.getDecomposition(st.pb, resourceMotivatedActionToDecompose, absDec, st.refCounter);

        st.applyDecomposition(dec);

        //TODO(fdvorak): here we should add the binding between the statevariable of supporting resource event in one
        // of the decomposed actions for now we leave it to search

        return true;
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
