package fape.exceptions;

public class NoSolutionException extends Exception {

    String message = "";
//    public NoSolutionException() {}
    public NoSolutionException(String msg) { this.message = msg; }

    @Override public String toString() {
        return "NoSolution: "+message;
    }
}
