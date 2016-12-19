package fr.laas.fape.constraints;


import fr.laas.fape.anml.model.concrete.TPRef;
import fr.laas.fape.anml.model.concrete.VarRef;
import fr.laas.fape.constraints.bindings.BindingConstraintNetwork;
import fr.laas.fape.constraints.stnu.Controllability;
import fr.laas.fape.constraints.stnu.structurals.StnWithStructurals;
import scala.collection.immutable.List;
import scala.collection.immutable.Map$;

public class Factory {

    public static MetaCSP getMetaWithGivenControllability(Controllability controllability) {
        switch (controllability) {
            case STN_CONSISTENCY: return getMetaWithoutControllability();
            case PSEUDO_CONTROLLABILITY: return getMetaWithPseudoControllability();
            default: throw new UnsupportedOperationException("No MetaCSP for controllability: "+controllability);
        }
    }

    /** Returns a MetaCSP that will consistent only if the underlying STNU is pseudo controllable */
    public static MetaCSP getMetaWithPseudoControllability() {
        return new MetaCSP(
                new BindingConstraintNetwork(),
                new StnWithStructurals(),
                Map$.MODULE$.<VarRef, List<PendingConstraint<VarRef,TPRef>>>empty());
    }

    /** Returns a MetaCSP that will consistent only if the underlying STNU is validates stn consistency.
     * (contingent constraints are recorded bu not checked. */
    public static MetaCSP getMetaWithoutControllability() {
        return new MetaCSP(
                new BindingConstraintNetwork(),
                new StnWithStructurals(),
                Map$.MODULE$.<VarRef, List<PendingConstraint<VarRef,TPRef>>>empty());
    }
}
