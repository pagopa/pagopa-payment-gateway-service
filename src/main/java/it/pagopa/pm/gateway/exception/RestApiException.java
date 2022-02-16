package it.pagopa.pm.gateway.exception;

public class RestApiException extends Exception{

    private static final long serialVersionUID = -858656501262531102L;
    private Integer errorCode;
    private String message;

    public RestApiException(Integer errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
    }

    /**
     * @return the errorCode
     */
    public Integer getErrorCode() {
        return errorCode;
    }

    /**
     * @param errorCode the errorCode to set
     */
    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

  //  public BaseError convertToBaseError() {
    //    return new BaseError(errorCode.toString(), message);
   // }

}
