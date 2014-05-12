package fape.core.planning.search;

import fape.core.planning.temporaldatabases.TemporalDatabase;

public class TemporalSeparation extends SupportOption {

    public final TemporalDatabase first;
    public final TemporalDatabase second;

    public TemporalSeparation(TemporalDatabase first, TemporalDatabase second) {
        this.first = first;
        this.second = second;
    }
}
