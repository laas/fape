package fr.laas.fape.planning.core.planning.states.modification;

import fr.laas.fape.planning.core.planning.states.State;
import lombok.Value;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class SequenceOfStateModifications implements StateModification {

    public final List<StateModification> modifications;

    @Override
    public void apply(State st, boolean isFastForwarding) {
        for(StateModification mod : modifications)
            mod.apply(st, false);
    }

    @Override
    public Collection<Object> involvedObjects() {
        return modifications.stream()
                .map(StateModification::involvedObjects)
                .collect(Collectors.toSet());
    }
}
