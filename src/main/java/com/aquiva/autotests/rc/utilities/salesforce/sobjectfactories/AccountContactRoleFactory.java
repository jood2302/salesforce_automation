package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.SIGNATORY_ROLE;

/**
 * Factory class for creating quick instances of {@link AccountContactRole} class.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 */
public class AccountContactRoleFactory extends SObjectFactory {

    /**
     * Create a new instance of Primary Signatory AccountContactRole object and insert it into Salesforce via API.
     * <br/>
     * AccountContactRole is a "junction" object
     * to link multiple accounts to multiple contacts (and vice versa).
     * <br/>
     * Note: Primary Signatory contact roles are required for Accounts to create new Opportunities.
     *
     * @param account Account object for linked Account
     * @param contact Contact object for linked Contact
     * @return AccountContactRole object with ID from Salesforce
     * @throws ConnectionException in case of errors while accessing API
     */
    public static AccountContactRole createPrimarySignatoryContactRole(Account account, Contact contact)
            throws ConnectionException {
        return createAccountContactRole(account, contact, SIGNATORY_ROLE, true);
    }

    /**
     * Create a new instance of AccountContactRole object and insert it into Salesforce via API.
     * <br/>
     * AccountContactRole is a "junction" object
     * to link multiple accounts to multiple contacts (and vice versa).
     *
     * @param account   Account object for linked Account
     * @param contact   Contact object for linked Contact
     * @param roleName  Role name for linked Contact (e.g. "Signatory", "Influencer")
     * @param isPrimary true, if setting Contact as "Primary" for Account
     * @return AccountContactRole object with ID from Salesforce
     * @throws ConnectionException in case of errors while accessing API
     */
    public static AccountContactRole createAccountContactRole(Account account, Contact contact,
                                                              String roleName, boolean isPrimary)
            throws ConnectionException {
        var newAccountContactRole = new AccountContactRole();

        newAccountContactRole.setAccountId(account.getId());
        newAccountContactRole.setContactId(contact.getId());
        newAccountContactRole.setRole(roleName);
        newAccountContactRole.setIsPrimary(isPrimary);

        CONNECTION_UTILS.insertAndGetIds(newAccountContactRole);

        return newAccountContactRole;
    }
}
