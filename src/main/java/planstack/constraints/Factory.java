package planstack.constraints;


import planstack.constraints.bindings.ConservativeConstraintNetwork;
import planstack.constraints.stnu.Controllability;
import planstack.constraints.stnu.MinimalSTNUManager;
import planstack.constraints.stnu.PseudoSTNUManager;
import planstack.constraints.stnu.STNUManager;
import scala.collection.immutable.List;
import scala.collection.immutable.Map$;

public class Factory {

    public static <VarRef,TPRef,ID> MetaCSP<VarRef,TPRef,ID> getMetaWithGivenControllability(Controllability controllability) {
        switch (controllability) {
            case STN_CONSISTENCY: return getMetaWithoutControllability();
            case PSEUDO_CONTROLLABILITY: return getMetaWithPseudoControllability();
            case DYNAMIC_CONTROLLABILITY: return getMetaWithDynamicControllability();
            default: throw new RuntimeException("No MetaCSP for controllability: "+controllability);
        }
    }

    /** Returns a MetaCSP that will consistent only if the underlying STNU is dynamically controllable */
    public static <VarRef,TPRef,ID> MetaCSP<VarRef,TPRef,ID> getMetaWithDynamicControllability() {
        return new MetaCSP<>(
                new ConservativeConstraintNetwork<VarRef>(),
                new STNUManager<TPRef,ID>(),
                Map$.MODULE$.<VarRef, List<PendingConstraint<VarRef,TPRef,ID>>>empty());
    }

    /** Returns a MetaCSP that will consistent only if the underlying STNU is pseudo controllable */
    public static <VarRef,TPRef,ID> MetaCSP<VarRef,TPRef,ID> getMetaWithPseudoControllability() {
        return new MetaCSP<>(
                new ConservativeConstraintNetwork<VarRef>(),
                new PseudoSTNUManager<TPRef,ID>(),
                Map$.MODULE$.<VarRef, List<PendingConstraint<VarRef,TPRef,ID>>>empty());
    }

    /** Returns a MetaCSP that will consistent only if the underlying STNU is validates stn consistency.
     * (contingent constraints are recorded bu not checked. */
    public static <VarRef,TPRef,ID> MetaCSP<VarRef,TPRef,ID> getMetaWithoutControllability() {
        return new MetaCSP<>(
                new ConservativeConstraintNetwork<VarRef>(),
                new MinimalSTNUManager<TPRef,ID>(),
                Map$.MODULE$.<VarRef, List<PendingConstraint<VarRef,TPRef,ID>>>empty());
    }
}
