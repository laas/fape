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
import fape.util.Pair;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class TemporalDatabaseManager {

    List<Pair<TemporalDatabase, TemporalDatabase>> unificationConstraints = new LinkedList<>();

    List<TemporalDatabase> vars = new LinkedList<>();

    public TemporalDatabase GetNewDatabase() {
        TemporalDatabase db = new TemporalDatabase();
        vars.add(db);
        return db;
    }

    /**
     * propagates the necessary unification constraints
     *
     * @param st
     */
    public void Propagate(State st) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public List<TemporalDatabase> GetSupporters(TemporalDatabase db) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
