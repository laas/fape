package fr.laas.fape.planning.exceptions;

public class NoSolutionException extends Exception {
    private static final long serialVersionUID = 5476543166535187454L;

    final String message;
    public NoSolutionException(String msg) { this.message = msg; }
    public NoSolutionException() { this.message = "no message =)"; }

    @Override public String toString() {
        return "NoSolution: "+message;
    }
}
