package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.Approval__c;
import com.sforce.soap.enterprise.sobject.LocalSubscribedAddress__c;
import com.sforce.ws.ConnectionException;

import java.util.UUID;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LocalSubscribedAddressHelper.*;

/**
 * Factory class for creating quick instances of {@link LocalSubscribedAddress__c} class.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 */
public class LocalSubscribedAddressFactory extends SObjectFactory {

    /**
     * Create a new Local Subscribed Address record of 'Registered Address of Company'
     * or 'Local Subscribed Address of Company' type
     * with India address for a given KYC Approval,
     * and insert it into Salesforce via API.
     *
     * @param approval   a KYC Approval object that the record is created for
     * @param recordType Record Type's name of created LocalSubscribedAddress__c record
     * @return a new Local Subscribed Address record for a given Approval with ID from Salesforce
     * @throws ConnectionException in case of errors while accessing API
     */
    public static LocalSubscribedAddress__c createIndiaLocalSubscribedAddressRecord(
            Approval__c approval, String recordType) throws ConnectionException {
        var newLocalSubscribedAddress = new LocalSubscribedAddress__c();
        newLocalSubscribedAddress.setApproval__c(approval.getId());

        var uniqueId = UUID.randomUUID().toString();
        newLocalSubscribedAddress.setName(uniqueId);

        if (recordType.equals(REGISTERED_ADDRESS_RECORD_TYPE)) {
            setRegisteredAddressOfCompanyRecordType(newLocalSubscribedAddress);
        } else if (recordType.equals(LOCAL_SUBSCRIBED_ADDRESS_RECORD_TYPE)) {
            setLocalSubscribedAddressOfCompanyRecordType(newLocalSubscribedAddress);
        }

        setIndiaAddress(newLocalSubscribedAddress);

        CONNECTION_UTILS.insertAndGetIds(newLocalSubscribedAddress);

        return newLocalSubscribedAddress;
    }
}
