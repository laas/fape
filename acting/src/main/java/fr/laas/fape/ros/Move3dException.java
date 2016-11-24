package fr.laas.fape.ros;

/**
 * Created by abitmonn on 10/26/16.
 */
public class Move3dException extends RuntimeException {

    public Move3dException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public Move3dException(String msg) {
        super(msg);
    }

    public Move3dException(Throwable cause) {
        super(cause);
    }
}
