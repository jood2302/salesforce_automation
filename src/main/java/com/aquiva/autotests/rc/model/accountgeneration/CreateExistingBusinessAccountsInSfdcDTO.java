package com.aquiva.autotests.rc.model.accountgeneration;

import com.aquiva.autotests.rc.model.DataModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Data object for creating an Existing Business account in SFDC via provided user's data.
 * <br/><br/>
 * Useful data structure for parsing data from input parameters in CI/CD job
 * that is used to generate EB accounts in SFDC.
 * Typically, user provides Existing Business Account's name, billing address, contact's data (first name, last name...),
 * and NGBS-related data: AGS scenario and (optionally) contract and/or discount data for the Existing Business account;
 * (optionally) Existing Business Contact Center / Engage Account data (for Multiproduct Flow): 
 * service name, and related Opportunity data (package to select and products to add).
 */
@JsonInclude(value = NON_NULL)
public class CreateExistingBusinessAccountsInSfdcDTO extends DataModel {
    public String accountName;
    public String accountURL; // URL is populated only after the account is created
    public String serviceName;
    public String chargeTerm;
    public BillingAddressDTO billingAddress;
    public ContactDTO contact;

    public CreateNgbsAccountsDTO ngbsAccountData;
    public List<CreateMultiproductDataInSfdcDTO> multiproductData;
}
