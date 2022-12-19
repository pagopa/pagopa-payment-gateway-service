package it.pagopa.pm.gateway.dto.transaction;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TransactionInfo {
    private String transactionId;
    private List<PaymentInfo> payments;
    private TransactionStatusEnum status;

    public void addPaymentInfo(PaymentInfo paymentInfo) {
        if(payments == null)
            payments = new ArrayList<>();

        payments.add(paymentInfo);
    }
}
