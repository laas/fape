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

package fape.model.compact.statements;

import fape.model.compact.Reference;
import fape.model.compact.TemporalInterval;
import fape.model.compact.TimePoint;

/**
 * temporarily qualified expression
 * @author FD
 */
public class Statement {
    public TemporalInterval interval;
    public Reference leftRef;
}
