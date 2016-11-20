package fr.laas.fape.planning.core.planning.search.flaws.resolvers;


import fr.laas.fape.anml.model.concrete.Chronicle;
import fr.laas.fape.anml.model.concrete.VarEqualityConstraint;
import fr.laas.fape.anml.model.concrete.VarRef;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.states.modification.ChronicleInsertion;
import fr.laas.fape.planning.core.planning.states.modification.StateModification;

/**
 * Binds a variable to the given value.
 */
public class VarBinding implements Resolver {

    public final VarRef var;
    public final String value;

    public VarBinding(VarRef var, String value) {
        this.var = var;
        this.value = value;
    }

    @Override
    public StateModification asStateModification(State state) {
        Chronicle chronicle = new Chronicle();
        chronicle.addConstraint(new VarEqualityConstraint(var, state.pb.instance(value)));
        return new ChronicleInsertion(chronicle);
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof VarBinding;
        VarBinding o = (VarBinding) e;
        if(!value.equals(o.value))
            return value.compareTo(o.value);
        assert var != o.var;
        return var.id() - o.var.id();
    }
}
