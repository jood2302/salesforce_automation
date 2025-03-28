package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.AccountRelation__c;
import com.sforce.ws.ConnectionException;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountRelationHelper.ELA_TYPE;

/**
 * Factory class for creating quick instances of {@link AccountRelation__c}.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 */
public class AccountRelationFactory extends SObjectFactory {

    /**
     * Create ELA Account Relation record and insert it into Salesforce via API.
     *
     * @param parentAccount parent Account to set up.
     * @param childAccount  child Account to set up.
     * @return new ELA AccountRelation__c record with ID from Salesforce
     * @throws ConnectionException in case of errors while accessing API.
     */
    public static AccountRelation__c createElaAccountRelation(Account parentAccount, Account childAccount)
            throws ConnectionException {
        var newAccountRelation = new AccountRelation__c();

        newAccountRelation.setRelation_Type__c(ELA_TYPE);
        newAccountRelation.setParent_Account__c(parentAccount.getId());
        newAccountRelation.setChild_Account__c(childAccount.getId());

        CONNECTION_UTILS.insertAndGetIds(newAccountRelation);

        return newAccountRelation;
    }
}
