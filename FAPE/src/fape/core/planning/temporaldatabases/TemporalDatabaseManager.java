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
package fape.core.planning.temporaldatabases;

import fape.core.planning.states.State;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class TemporalDatabaseManager {

    List<TemporalDatabase> vars = new LinkedList<>();

    public TemporalDatabase GetNewDatabase() {
        TemporalDatabase db = new TemporalDatabase();
        vars.add(db);
        return db;
    }

    /**
     * merges the temporal databases as needed
     *
     * @param st
     */
    public void PropagateNecessary(State st) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
