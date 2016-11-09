package fr.laas.fape.constraints;


import fr.laas.fape.anml.model.concrete.TPRef;
import fr.laas.fape.anml.model.concrete.VarRef;
import fr.laas.fape.constraints.bindings.BindingConstraintNetwork;
import fr.laas.fape.constraints.stnu.Controllability;
import fr.laas.fape.constraints.stnu.pseudo.MinimalSTNUManager;
import fr.laas.fape.constraints.stnu.pseudo.PseudoSTNUManager;
import fr.laas.fape.constraints.stnu.STNUManager;
import fr.laas.fape.constraints.stnu.structurals.StnWithStructurals;
import planstack.UniquelyIdentified;
import scala.collection.immutable.List;
import scala.collection.immutable.Map$;

public class Factory {

    public static <TPRef extends UniquelyIdentified,ID> MetaCSP<ID> getMetaWithGivenControllability(Controllability controllability) {
        switch (controllability) {
            case STN_CONSISTENCY: return getMetaWithoutControllability();
            case PSEUDO_CONTROLLABILITY: return getMetaWithPseudoControllability();
            case DYNAMIC_CONTROLLABILITY: return getMetaWithDynamicControllability();
            default: throw new RuntimeException("No MetaCSP for controllability: "+controllability);
        }
    }

    /** Returns a MetaCSP that will consistent only if the underlying STNU is dynamically controllable */
    public static <ID> MetaCSP<ID> getMetaWithDynamicControllability() {
        return new MetaCSP<ID>(
                new BindingConstraintNetwork(),
                new STNUManager<>(),
                Map$.MODULE$.<VarRef, List<PendingConstraint<VarRef,TPRef,ID>>>empty());
    }

    /** Returns a MetaCSP that will consistent only if the underlying STNU is pseudo controllable */
    public static <ID> MetaCSP<ID> getMetaWithPseudoControllability() {
        return new MetaCSP<>(
                new BindingConstraintNetwork(),
                new StnWithStructurals<>(), // new PseudoSTNUManager<>(), // other valid option
                Map$.MODULE$.<VarRef, List<PendingConstraint<VarRef,TPRef,ID>>>empty());
    }

    /** Returns a MetaCSP that will consistent only if the underlying STNU is validates stn consistency.
     * (contingent constraints are recorded bu not checked. */
    public static <ID> MetaCSP<ID> getMetaWithoutControllability() {
        return new MetaCSP<>(
                new BindingConstraintNetwork(),
                new MinimalSTNUManager<ID>(),
                Map$.MODULE$.<VarRef, List<PendingConstraint<VarRef,TPRef,ID>>>empty());
    }
}
