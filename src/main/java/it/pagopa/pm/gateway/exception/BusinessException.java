package it.pagopa.pm.gateway.exception;

public class BusinessException extends Exception{

    private static final long serialVersionUID = -5465596369601918545L;

    private ExceptionsEnum exception;
    private String message;

    /**
     * @param exception
     */
    public BusinessException(ExceptionsEnum exception) {
        super(exception.getCode() + ": " + exception.getDescription());
        this.exception = exception;
        this.message = exception.getDescription();
    }

    /**
     * In caso si voglia usare un codice di errore gia presente ma inserire un messaggio di errore piu particolareggiato
     * @param exception
     * @param customMessage
     */
    public BusinessException(ExceptionsEnum exception, String customMessage) {
        super(exception.getCode() + ": " + customMessage);
        this.exception = exception;
        this.message = customMessage;
    }

    /**
     * @return the exception
     */
    public ExceptionsEnum getException() {
        return exception;
    }

    /**
     * @param exception the exception to set
     */
    public void setException(ExceptionsEnum exception) {
        this.exception = exception;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BusinessException [exception=");
        builder.append(exception);
        builder.append(", message=");
        builder.append(message);
        builder.append("]");
        return builder.toString();
    }
}
