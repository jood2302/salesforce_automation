package com.aquiva.autotests.rc.model.ngbs.dto.account;

import com.aquiva.autotests.rc.model.DataModel;

/**
 * Data object to get information about Payment Method Type on account
 * for usage with NGBS API services.
 */
public class PaymentMethodTypeDTO extends DataModel {
    public static final String CREDITCARD_PAYMENT_METHOD_TYPE = "CreditCard";
    public static final String INVOICE_PAYMENT_METHOD_TYPE = "Invoice";
    public static final String INVOICE_ON_BEHALF_PAYMENT_METHOD_TYPE = "InvoiceOnBehalf";

    public String currentType;
    public String[] formerTypes;
}
