package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.Dsfs__DocuSign_Status__c;
import com.sforce.ws.ConnectionException;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.SObjectFactory.CONNECTION_UTILS;

/**
 * Factory class for creating quick instances of {@link Dsfs__DocuSign_Status__c} class.
 * <br/>
 * Used for integration of DocuSign and Salesforce.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 */
public class DocuSignStatusFactory {

    /**
     * Create a new {@link Dsfs__DocuSign_Status__c} object and insert it into Salesforce via API.
     *
     * @param accountId      related Account's Salesforce ID
     * @param opportunityId  related Opportunity's Salesforce ID
     * @param quoteId        related Quote's Salesforce ID
     * @param envelopeStatus status of the DocuSign Status envelope (e.g. "New")
     * @return created {@link Dsfs__DocuSign_Status__c} object with assigned Salesforce ID
     * @throws ConnectionException if an API/DB error occurs during the insertion of the object
     */
    public static Dsfs__DocuSign_Status__c createDocuSignStatus(String accountId, String opportunityId, String quoteId,
                                                                String envelopeStatus) throws ConnectionException {
        var docuSignStatus = new Dsfs__DocuSign_Status__c();
        docuSignStatus.setDsfs__Company__c(accountId);
        docuSignStatus.setDsfs__Opportunity__c(opportunityId);
        docuSignStatus.setDfsq__Quote__c(quoteId);
        docuSignStatus.setDsfs__Envelope_Status__c(envelopeStatus);

        CONNECTION_UTILS.insertAndGetIds(docuSignStatus);

        return docuSignStatus;
    }
}
