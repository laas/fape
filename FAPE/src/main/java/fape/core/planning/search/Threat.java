package fape.core.planning.search;

import fape.core.planning.temporaldatabases.TemporalDatabase;

public class Threat extends Flaw {

    public final TemporalDatabase db1;
    public final TemporalDatabase db2;

    public Threat(TemporalDatabase db1, TemporalDatabase db2) {
        this.db1 = db1;
        this.db2 = db2;
    }
}
