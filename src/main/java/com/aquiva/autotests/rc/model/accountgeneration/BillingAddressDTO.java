package com.aquiva.autotests.rc.model.accountgeneration;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object that represents the info about the Billing Address information
 * on Account record: Country, City, Street, etc.
 * <br/>
 * Used as part of the user's data input in Existing Business Account's Generation task.
 */
@JsonInclude(value = NON_NULL)
public class BillingAddressDTO extends DataModel {
    public String country;
    public String state;
    public String city;
    public String street;
    public String postalCode;
}
