package planstack.constraints.bindings;

import fr.laas.fape.exceptions.InconsistencyException;
import planstack.anml.model.concrete.VarRef;

import java.util.List;
import java.util.stream.Collectors;

public class InconsistentBindingConstraintNetwork extends InconsistencyException {

    public InconsistentBindingConstraintNetwork() {}
    public InconsistentBindingConstraintNetwork(String msg) { super(msg); }
    public InconsistentBindingConstraintNetwork(Throwable e) { super(e); }
    public InconsistentBindingConstraintNetwork(String msg, Throwable e) {super(msg, e); }
}
