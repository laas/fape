package fr.laas.fape.exceptions;

public class InconsistencyException extends RuntimeException {
    public InconsistencyException() {}
    public InconsistencyException(String msg) { super(msg); }
    public InconsistencyException(Throwable e) { super(e); }
    public InconsistencyException(String msg, Throwable e) {super(msg, e); }
}
