package fr.laas.fape.planning.core.planning.grounding;

import fr.laas.fape.anml.model.Function;
import fr.laas.fape.anml.model.concrete.InstanceRef;
import fr.laas.fape.structures.AbsIdentifiable;
import fr.laas.fape.structures.Ident;
import fr.laas.fape.structures.ValueConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Ident(GStateVariable.class)
public class GStateVariable extends AbsIdentifiable {

    final public Function f;
    final public List<InstanceRef> params;

    @ValueConstructor @Deprecated
    public GStateVariable(Function f, List<InstanceRef> params) {
        this.f = f;
        this.params = params;
    }

    @Override
    public String toString() {
        return f.name() +"("+String.join(",",params.stream().map(InstanceRef::toString).collect(Collectors.toList()))+")";
    }
}
