package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;

import java.util.Calendar;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContractHelper.CONTRACT_STATUS_DRAFT;

/**
 * Factory class for creating quick instances of {@link Contract} class.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 * <br/>
 * Note: not to be mistaken with custom {@link Contract__c} ("Billing Contract") object!
 */
public class ContractFactory extends SObjectFactory {

    /**
     * Create a new Contract record in 'Draft' status on an Account and insert it in Salesforce.
     *
     * @param account account for which Contract record is created
     * @return new Contract record related to given Account with ID from Salesforce
     * @throws ConnectionException in case of malformed query, DB or network errors.
     */
    public static Contract createContract(Account account) throws ConnectionException {
        var contract = new Contract();

        contract.setAccountId(account.getId());
        contract.setOriginal_Signed_Date__c(Calendar.getInstance());
        contract.setStatus(CONTRACT_STATUS_DRAFT);
        CONNECTION_UTILS.insertAndGetIds(contract);

        return contract;
    }
}
