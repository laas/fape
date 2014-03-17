package fape.core.planning.constraints;


import fape.util.TinyLogger;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * THis contains the set of values that can take an object variable.
 */
public class VariableValues extends IUnifiable {

    /**
     * Constructor for a domain containing multiple values
     * @param dom A set of values
     */
    public VariableValues(Collection<String> dom) {
        domain.addAll(dom);
    }

    /**
     * Constructor for a domain containing only one value.
     * @param dom the unique value
     */
    public VariableValues(String dom) {
        domain.add(dom);
    }

    /**
     * A set of possible values for the variable
     */
    public final LinkedList<String> domain = new LinkedList<>();

    /**
     * reduces the domain by keeping only the elements passed as parameter.
     *
     * @param supported Set of elements to keep.
     * @return True if the elements where removed, False otherwise.
     */
    @Override
    public boolean ReduceDomain(Collection<String> supported) {
        return domain.retainAll(supported);
    }

    @Override
    public List<String> GetDomainObjectConstants() {
        return new LinkedList<>(domain);
    }

    @Override
    public boolean EmptyDomain() {
        return domain.isEmpty();
    }

    @Override
    public String Explain() {
        return " Action parameter";
    }

    @Override
    public VariableValues DeepCopy() {
        return new VariableValues(this.domain);
    }
}