package com.aquiva.autotests.rc.model.ngbs.dto;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data structure that stores information about Address
 * on the NGBS account (customer/partner) for usage with NGBS API services.
 */
@JsonInclude(value = NON_NULL)
public class AddressDTO extends DataModel {
    //  For 'label' field
    public static final String ACCOUNTS_PAYABLE_LABEL = "AccountsPayable";
    //  For 'country' field
    public static final String DEFAULT_COUNTRY = "USA";
    //  For 'state' field
    public static final String DEFAULT_STATE = "CA";
    //  For 'city' field
    public static final String DEFAULT_CITY = "Beverly Hills";
    //  For 'street1' field
    public static final String DEFAULT_STREET = "516 Walden Dr";
    //  For 'zip' field
    public static final String DEFAULT_ZIP = "90210";

    public String label;
    public String firstName;
    public String street1;
    public String zip;
    public String country;
    public String state;
    public String city;
    public String lastName;
    public String phone;
    public String email;
}
