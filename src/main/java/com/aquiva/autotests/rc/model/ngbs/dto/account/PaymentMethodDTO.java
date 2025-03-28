package com.aquiva.autotests.rc.model.ngbs.dto.account;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.model.ngbs.dto.AddressDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with payment method information for usage with NGBS API services.
 */
@JsonInclude(value = NON_NULL)
public class PaymentMethodDTO extends DataModel {

    public Long id;
    public InvoiceOnBehalfInfoDTO invoiceOnBehalfInfo;
    public DirectDebitInfoDTO directDebitInfo;
    public CreditCardInfoDTO creditCardInfo;
    @JsonProperty("default")
    public Boolean defaultVal;

    /**
     * Inner data structure that stores information about Invoice On Behalf payment method
     * on account for usage with NGBS API services.
     */
    public static class InvoiceOnBehalfInfoDTO extends DataModel {
        public Long partnerId;
    }

    /**
     * Inner data structure that stores information about Direct Debit payment method
     * on account for usage with NGBS API services.
     */
    public static class DirectDebitInfoDTO extends DataModel {
    }

    /**
     * Inner data structure that stores information about Credit Card payment method
     * on account for usage with NGBS API services.
     */
    public static class CreditCardInfoDTO extends DataModel {
        public Integer tokenSource;
        public String token;
        public String cardType;
        public AddressDTO address;
    }
}
