package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.Lead;
import com.sforce.ws.ConnectionException;

/**
 * Helper class to facilitate operations on {@link Lead} objects.
 */
public class LeadHelper extends SObjectHelper {
    //  SFDC API parameters
    private static final String S_OBJECT_API_NAME = "Lead";
    private static final String PARTNER_LEAD_RECORD_TYPE = "Partner Leads";

    //  For 'Partner_Type__c' field
    public static final String BILL_ON_BEHALF_PARTNER_TYPE = "Bill-on-Behalf";

    //  For 'Partner_Lead_Source__c' field
    public static final String PARTNER_LEAD_SOURCE_TYPE = "Partner";

    //  Default values for Billing Address fields
    private static final String DEFAULT_BILLING_STREET = "516 Walden Dr";
    private static final String DEFAULT_BILLING_CITY = "Beverly Hills";
    private static final String DEFAULT_BILLING_STATE = "CA";
    private static final String DEFAULT_BILLING_POSTAL_CODE = "90210";
    private static final String DEFAULT_BILLING_COUNTRY = "United States";

    //  Values for Taiwan Billing Address fields
    public static final String TAIWAN_BILLING_STREET = "97 Zhongshan Road";
    public static final String TAIWAN_BILLING_CITY = "Taipei";
    public static final String TAIWAN_BILLING_STATE = "";
    public static final String TAIWAN_BILLING_POSTAL_CODE = "208";
    public static final String TAIWAN_BILLING_COUNTRY = "Taiwan";

    //  For 'Country__c' field
    public static final String EU_BILLING_COUNTRY = "France";
    public static final String UK_BILLING_COUNTRY = "United Kingdom";

    //  For 'Owner' field
    public static final String LEANDATA_QUEUE_OWNER = "LeanData Queue";

    /**
     * Get full name of the given Lead object.
     * <p>Typically, it contains a first name and a last name.</p>
     * Example: John Smith
     *
     * @param lead Lead object to get a full name from
     * @return full name of the Lead
     */
    public static String getFullName(Lead lead) {
        return lead.getFirstName() + " " + lead.getLastName();
    }

    /**
     * Set 'Partner Leads' record type for the Lead object.
     *
     * @param lead Lead object to set up Record type on
     * @throws ConnectionException in case of errors while accessing API
     */
    public static void setPartnerLeadRecordType(Lead lead) throws ConnectionException {
        var partnerLeadRecordTypeId = CONNECTION_UTILS.getRecordTypeId(S_OBJECT_API_NAME, PARTNER_LEAD_RECORD_TYPE);
        lead.setRecordTypeId(partnerLeadRecordTypeId);
    }

    /**
     * Set up Lead's address fields with some default values.
     *
     * @param lead Lead object to set up
     */
    public static void setDefaultAddress(Lead lead) {
        lead.setStreet(DEFAULT_BILLING_STREET);
        lead.setStreet_2__c(DEFAULT_BILLING_STREET);
        lead.setCity(DEFAULT_BILLING_CITY);
        lead.setCity__c(DEFAULT_BILLING_CITY);
        lead.setState(DEFAULT_BILLING_STATE);
        lead.setState__c(DEFAULT_BILLING_STATE);
        lead.setPostalCode(DEFAULT_BILLING_POSTAL_CODE);
        lead.setZip_Code__c(DEFAULT_BILLING_POSTAL_CODE);
        lead.setCountry(DEFAULT_BILLING_COUNTRY);
        lead.setCountry__c(DEFAULT_BILLING_COUNTRY);
    }
}
