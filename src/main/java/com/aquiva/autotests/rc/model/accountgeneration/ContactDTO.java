package com.aquiva.autotests.rc.model.accountgeneration;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object that represents the info about the Contact record:
 * First Name, Last Name, Email, etc.
 * <br/>
 * Used as part of the user's data input in Existing Business Account's Generation task.
 */
@JsonInclude(value = NON_NULL)
public class ContactDTO extends DataModel {
    public String firstName;
    public String lastName;
    public String email;
    public String phone;
}
