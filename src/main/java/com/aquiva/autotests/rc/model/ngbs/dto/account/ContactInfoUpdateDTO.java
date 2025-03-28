package com.aquiva.autotests.rc.model.ngbs.dto.account;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object to update information about the Contact on the account
 * for usage with NGBS API services.
 */
@JsonInclude(value = NON_NULL)
public class ContactInfoUpdateDTO extends DataModel {
    public String firstName;
    public String lastName;
    public String phone;
    public String email;
    public String companyName;
}
