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
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Factory;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.statements.ResourceStatement;

/**
 *
 * @author FD
 */
public class ResourceSupportingAction extends Resolver {
    public AbstractAction absAction;
    public boolean before;
    public TPRef when;
    public ParameterizedStateVariable unifyingResourceVariable;

    public boolean hasActionInsertion() {
        return true;
    }

    @Override
    public boolean apply(State st, APlanner planner) {
        assert false : "Needs to be checked.";

        Action action = Factory.getStandaloneAction(st.pb, absAction);
        //add the actual action
        st.insert(action);

        //unify the state variables supporting the resource
        ResourceStatement theSupport = null;
        for (ResourceStatement s : action.resourceStatements()) {
            if (s.sv().func().name().equals(unifyingResourceVariable.func().name())) {
                assert theSupport == null : "Distinguishing resource events upon the same resource in one action " +
                        "needs to be implemented.";
                theSupport = s;
            }
        }
        assert theSupport != null : "Could not find a supporting resource statement in the action.";
        st.addUnificationConstraint(theSupport.sv(), unifyingResourceVariable);

        //add temporal constraint
        if (before) {
            //the supporting statement must occur before the given time point
            st.enforceStrictlyBefore(theSupport.end(), when);
        } else {
            //vice-versa
            st.enforceStrictlyBefore(when, theSupport.start());
        }

        return true;
    }
}
