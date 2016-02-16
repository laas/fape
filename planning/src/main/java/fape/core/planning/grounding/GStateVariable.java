package fape.core.planning.grounding;

import fr.laas.fape.structures.AbsIdentifiable;
import fr.laas.fape.structures.Ident;
import fr.laas.fape.structures.ValueConstructor;
import planstack.anml.model.Function;
import planstack.anml.model.concrete.InstanceRef;

import java.util.List;

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
        return f.name() + params;
    }
}
