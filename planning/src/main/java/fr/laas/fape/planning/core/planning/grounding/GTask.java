package fr.laas.fape.planning.core.planning.grounding;

import fr.laas.fape.anml.model.concrete.InstanceRef;
import fr.laas.fape.structures.AbsIdentifiable;
import fr.laas.fape.structures.Ident;
import fr.laas.fape.structures.ValueConstructor;

import java.util.Collection;

@Ident(GTask.class)
public class GTask extends AbsIdentifiable {
    public final String name;
    public final InstanceRef[] args;

    @ValueConstructor @Deprecated
    public GTask(String name, Collection<InstanceRef> args) {
        this.name = name;
        this.args = new InstanceRef[args.size()];
        int i=0;
        for(InstanceRef arg : args) {
            this.args[i] = arg;
            i++;
        }
    }

    @Override
    public String toString() {
        String ret = name+"(";
        for(InstanceRef arg : args)
            ret+= arg+",";
        return ret+")";
    }
}
