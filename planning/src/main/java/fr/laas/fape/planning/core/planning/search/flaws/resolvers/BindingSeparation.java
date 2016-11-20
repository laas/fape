package fr.laas.fape.planning.core.planning.search.flaws.resolvers;


import fr.laas.fape.anml.model.concrete.Chronicle;
import fr.laas.fape.anml.model.concrete.VarInequalityConstraint;
import fr.laas.fape.anml.model.concrete.VarRef;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.states.modification.ChronicleInsertion;
import fr.laas.fape.planning.core.planning.states.modification.StateModification;

/**
 * Simply adds a difference constraint between the two variables.
 */
public class BindingSeparation implements Resolver {

    public final VarRef a, b;

    public BindingSeparation(VarRef a, VarRef b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public StateModification asStateModification(State state) {
        Chronicle chronicle = new Chronicle();
        chronicle.addConstraint(new VarInequalityConstraint(a, b));
        return new ChronicleInsertion(chronicle);
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof BindingSeparation;
        BindingSeparation o = (BindingSeparation) e;
        if(a != o.a)
            return a.id() - o.a.id();
        assert b != o.b : "Comparing two identical resolvers.";
        return b.id() - o.b.id();
    }
}
