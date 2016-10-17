package fr.laas.fape.planning.exceptions;

public class NotValidGroundAction extends RuntimeException {
    private static final long serialVersionUID = 21165348216534L;
    public NotValidGroundAction(String msg) { super(msg); }
}
