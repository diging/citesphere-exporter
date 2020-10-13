package edu.asu.diging.citesphere.exporter.core.exception;

public class GroupDoesNotExistException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public GroupDoesNotExistException() {
        super();
    }

    public GroupDoesNotExistException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public GroupDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public GroupDoesNotExistException(String message) {
        super(message);
    }

    public GroupDoesNotExistException(Throwable cause) {
        super(cause);
    }

}
