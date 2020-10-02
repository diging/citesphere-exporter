package edu.asu.diging.citesphere.exporter.core.exception;

public class OutOfDateException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public OutOfDateException() {
        super();
    }

    public OutOfDateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public OutOfDateException(String message, Throwable cause) {
        super(message, cause);
    }

    public OutOfDateException(String message) {
        super(message);
    }

    public OutOfDateException(Throwable cause) {
        super(cause);
    }

}
