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

package fape.core.planning.search;

import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.TPRef;

/**
 *
 * @author FD
 */
public class ResourceSupportingAction extends SupportOption {
    public AbstractAction action;
    public boolean before;
    public TPRef when;
    public ParameterizedStateVariable unifyingResourceVariable;
}
