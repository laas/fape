package fr.laas.fape.constraints.stnu;

import fr.laas.fape.exceptions.InconsistencyException;

public class InconsistentTemporalNetwork extends InconsistencyException {

    public InconsistentTemporalNetwork() {}
    public InconsistentTemporalNetwork(String msg) { super(msg); }
    public InconsistentTemporalNetwork(Throwable e) { super(e); }
    public InconsistentTemporalNetwork(String msg, Throwable e) {super(msg, e); }
}
