package com.aquiva.autotests.rc.model.ngbs.dto.account;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object with brief Account's information for usage with NGBS API services.
 */
@JsonInclude(value = NON_NULL)
public class AccountSummary extends DataModel {
    public String id;
    public String status;
    public String firstName;
    public String lastName;
    public String companyName;
    public String email;
    public String phone;
    public PackageSummary[] packages;
}