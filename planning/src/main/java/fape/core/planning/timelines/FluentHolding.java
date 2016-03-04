package fape.core.planning.timelines;

import lombok.Value;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.VarRef;

import java.util.List;

@Value
public class FluentHolding {
    final ParameterizedStateVariable sv;
    final VarRef value;
    final TPRef start;
    final List<TPRef> end;
}
