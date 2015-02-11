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

import planstack.anml.model.ParameterizedStateVariable;

/**
 *
 * @author FD
 */
public class StateVariableBinding extends Resolver {

    public StateVariableBinding(ParameterizedStateVariable a, ParameterizedStateVariable b) {
        one = a;
        two = b;
    }
    public ParameterizedStateVariable one, two;
}
