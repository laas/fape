package fr.laas.fape.ros.exception;

public class ActionFailure extends Exception {

    public ActionFailure() {}
    public ActionFailure(String msg) { super(msg); }
    public ActionFailure(Throwable e) { super(e); }
    public ActionFailure(String msg, Throwable e) { super(msg, e); }
}
