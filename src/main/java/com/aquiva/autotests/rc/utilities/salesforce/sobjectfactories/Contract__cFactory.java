package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.Contract__cHelper;
import com.sforce.soap.enterprise.sobject.Contract;
import com.sforce.soap.enterprise.sobject.Contract__c;
import com.sforce.ws.ConnectionException;

import java.util.UUID;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.Contract__cHelper.MVP_PACKAGE_CODES;

/**
 * Factory class for creating quick instances of {@link Contract__c} class ("Billing Contract").
 * <br/>
 * All factory methods also insert created objects into the SF database.
 * <br/>
 * Note: not to be mistaken with standard {@link Contract} ("Contract") object!
 */
public class Contract__cFactory extends SObjectFactory {
    //  Prefixes for 'ExtID__c'
    private static final String RC_MVP_STANDARD_EXT_ID = "MVP_Standard_Auto";

    /**
     * Create a new instance of Billing Contract object
     * for all RC MVP packages with default parameters and unique ExtID__c value.
     *
     * @return instance of Contract__c with default parameters and ID from Salesforce
     * @throws ConnectionException in case of malformed query, DB or network errors.
     */
    public static Contract__c createBillingContractMvpAll() throws ConnectionException {
        return createBillingContract(MVP_PACKAGE_CODES, RC_MVP_STANDARD_EXT_ID);
    }

    /**
     * Create a new instance of Billing Contract object
     * for chosen packages with default parameters and unique ExtID__c value.
     *
     * @param packageIds comma-separated packages' IDs list to create a contract for
     *                   (e.g. "5", "6,7,8", "1,355,66,55", etc...)
     * @param extId      unique external ID for a contract from Billing
     *                   (e.g. "MVP_Standard_Auto 1365")
     * @return instance of Contract__c with default parameters and ID from Salesforce
     * @throws ConnectionException in case of malformed query, DB or network errors.
     */
    public static Contract__c createBillingContract(String packageIds, String extId) throws ConnectionException {
        var newContract = new Contract__c();

        Contract__cHelper.setDefaultFields(newContract);

        newContract.setAvailable_in_packages__c(packageIds);
        newContract.setExtID__c(extId + "_" + UUID.randomUUID());

        CONNECTION_UTILS.insertAndGetIds(newContract);

        return newContract;
    }
}
