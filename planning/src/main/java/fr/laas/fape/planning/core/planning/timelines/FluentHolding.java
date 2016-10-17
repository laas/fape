package fr.laas.fape.planning.core.planning.timelines;

import fr.laas.fape.anml.model.concrete.TPRef;
import fr.laas.fape.anml.model.concrete.VarRef;
import lombok.Value;
import fr.laas.fape.anml.model.ParameterizedStateVariable;

import java.util.List;

@Value
public class FluentHolding {
    final ParameterizedStateVariable sv;
    final VarRef value;
    final TPRef start;
    final List<TPRef> end;
}
