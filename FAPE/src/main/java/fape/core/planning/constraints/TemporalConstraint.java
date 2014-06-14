/*
 * Author:  Filip Dvo��k <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvo��k <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package fape.core.planning.constraints;

import planstack.anml.model.concrete.TPRef;

/**
 * represents a single temporal constraints between two timepoints
 *
 * @author FD
 */
public class TemporalConstraint {

    public TPRef first, second;
    public int min, max;
}
