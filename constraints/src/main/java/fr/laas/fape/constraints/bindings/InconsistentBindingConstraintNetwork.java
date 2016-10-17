package fr.laas.fape.constraints.bindings;

import fr.laas.fape.exceptions.InconsistencyException;

public class InconsistentBindingConstraintNetwork extends InconsistencyException {

    public InconsistentBindingConstraintNetwork() {}
    public InconsistentBindingConstraintNetwork(String msg) { super(msg); }
    public InconsistentBindingConstraintNetwork(Throwable e) { super(e); }
    public InconsistentBindingConstraintNetwork(String msg, Throwable e) {super(msg, e); }
}
