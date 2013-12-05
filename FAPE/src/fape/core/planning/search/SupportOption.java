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

package fape.core.planning.search;

import fape.core.planning.model.AbstractAction;
import fape.core.planning.model.Action;
import fape.core.planning.temporaldatabases.TemporalDatabase;

/**
 *
 * @author FD
 */
public class SupportOption {
    public TemporalDatabase tdb;
    public TemporalDatabase.ChainComponent precedingComponent;
    public AbstractAction supportingAction;
    public Action actionToDecompose;
    public int decompositionID = -1;
}
