package fr.laas.fape.constraints.bindings;

import fr.laas.fape.anml.model.concrete.VarRef;

import java.util.List;
import java.util.stream.Collectors;

public class VarWithEmptyDomain extends InconsistentBindingConstraintNetwork {

    private final List<VarRef> varsWithEmptyDomains;
    public VarWithEmptyDomain(List<VarRef> varsWithEmptyDomains) {
        this.varsWithEmptyDomains = varsWithEmptyDomains;
    }

    public String toString() { return "Inconsistent binding constraints: the following vars have an empty domain: "+
            varsWithEmptyDomains.stream().map(x -> x.label()).collect(Collectors.toList()); }
}
