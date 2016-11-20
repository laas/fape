package fr.laas.fape.planning.core.planning.states.modification;

import fr.laas.fape.anml.model.concrete.Chronicle;
import fr.laas.fape.planning.core.planning.states.State;
import lombok.Value;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Value
public class ChronicleInsertion implements StateModification {

    final Chronicle chronicle;


    @Override
    public void apply(State st, boolean isFastForwarding) {
        st.apply(chronicle);
    }

    @Override
    public Collection<Object> involvedObjects() {
        Set<Object> involved = new HashSet<>();
        involved.addAll(chronicle.bindingConstraints());
        involved.addAll(chronicle.temporalConstraints());
        involved.addAll(chronicle.tasks());
        involved.addAll(chronicle.statements());
        involved.addAll(chronicle.jUsedVariables());
        involved.add(chronicle);
        return involved;
    }
}
