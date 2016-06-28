package planstack.constraints.bindings;

import fr.laas.fape.exceptions.InconsistencyException;
import planstack.anml.model.concrete.VarRef;

import java.util.List;

public class InconsistentBindingConstraintNetwork extends InconsistencyException {
    private final List<VarRef> varsWithEmptyDomains;
    public InconsistentBindingConstraintNetwork(List<VarRef> varsWithEmptyDomains) {
        this.varsWithEmptyDomains = varsWithEmptyDomains;
    }

    public String toString() { return "Inconsistent binding constraints: the following vars have an empty domain: "+varsWithEmptyDomains; }
}
