package planstack.constraints;


import planstack.UniquelyIdentified;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.VarRef;
import planstack.constraints.bindings.BindingConstraintNetwork;
import planstack.constraints.stnu.Controllability;
import planstack.constraints.stnu.pseudo.MinimalSTNUManager;
import planstack.constraints.stnu.pseudo.PseudoSTNUManager;
import planstack.constraints.stnu.STNUManager;
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
                new PseudoSTNUManager<>(),
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
