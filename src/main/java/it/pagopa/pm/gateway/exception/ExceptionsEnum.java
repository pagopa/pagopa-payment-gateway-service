package it.pagopa.pm.gateway.exception;

public enum ExceptionsEnum {

    GENERIC_ERROR			("PPA000", "Unexpected error", 0),
    BPAY_SERVICE_REQUEST_ERROR			("PPA001", "No response from Bpay service", 1),
    BPAY_SERVICE_NEGATIVE_OUTCOME_ERROR 		("PPA001", "Negative outcome form Bpay Service", 2);




    private final String code;
    private final String description;
    private final Integer restApiCode;

    ExceptionsEnum(String code, String description, Integer restApiCode) {
        this.code = code;
        this.description = description;
        this.restApiCode = restApiCode;
    }

    /**
     * @param code
     * @return
     */
    public static ExceptionsEnum fromCode(String code) {
        ExceptionsEnum[] values = ExceptionsEnum.values();
        for (ExceptionsEnum item : values) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return GENERIC_ERROR;
    }

    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the restApiCode
     */
    public Integer getRestApiCode() {
        return restApiCode;
    }

    }
