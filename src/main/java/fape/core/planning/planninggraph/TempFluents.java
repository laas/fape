package fape.core.planning.planninggraph;

import planstack.anml.model.concrete.TPRef;

import java.util.Collection;
import java.util.LinkedList;

public class TempFluents {

    public final Collection<Fluent> fluents;
    public final Collection<TPRef> timepoints;

    public TempFluents(Collection<Fluent> fluents, TPRef tp) {
        this.fluents = fluents;
        this.timepoints = new LinkedList<>();
        timepoints.add(tp);
    }

    public TempFluents(Collection<Fluent> fluents, Collection<TPRef> tps) {
        this.fluents = fluents;
        this.timepoints = new LinkedList<>(tps);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Fluent)
            return o.equals(fluents);
        else
            return this == o;
    }

    @Override
    public int hashCode() {
        return fluents.hashCode();
    }
}
