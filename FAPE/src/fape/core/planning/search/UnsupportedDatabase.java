package fape.core.planning.search;

import fape.core.planning.temporaldatabases.TemporalDatabase;

public class UnsupportedDatabase extends Flaw {

    public final TemporalDatabase consumer;

    public UnsupportedDatabase(TemporalDatabase tdb) {
        this.consumer = tdb;
    }
}
