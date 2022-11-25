package it.pagopa.pm.gateway.dto.enums;

public enum CardCircuit {

    VISA("01"),
    MASTERCARD("02"),
    MAESTRO("04"),
    AMEX("06"),
    DINERS("07"),
    PAYPAL("97"),
    UNKNOWN("93");

    private final String code;

    CardCircuit(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static CardCircuit fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (CardCircuit cardCircuit : values()) {
            if (cardCircuit.code.equals(code)) {
                return cardCircuit;
            }
        }
        return null;
    }

}