package com.aquiva.autotests.rc.model.ngbs.dto.account;

import com.aquiva.autotests.rc.model.DataModel;

/**
 * Data object to get information about dynamic fields on the account
 * for usage with NGBS API services.
 */
public class AccountFieldsDTO extends DataModel {
    public static final String CREDITCARD_INTENDED_PAYMENT_METHOD = "CreditCard";

    public String intendedPaymentMethod;
}
