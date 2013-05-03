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

package fape.core.execution.model.statements;

import fape.core.execution.model.Reference;
import fape.core.execution.model.TemporalInterval;
import fape.core.execution.model.TimePoint;

/**
 * temporarily qualified expression
 * @author FD
 */
public class Statement {
    public TemporalInterval interval;
    public Reference leftRef;
}
