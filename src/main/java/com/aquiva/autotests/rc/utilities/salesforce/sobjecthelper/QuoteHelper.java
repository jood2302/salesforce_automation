package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.ws.ConnectionException;

import java.util.*;

/**
 * Helper class to facilitate operations on {@link Quote} objects.
 */
public class QuoteHelper extends SObjectHelper {
    //  SFDC API parameters
    public static final String SALES_QUOTE_RECORD_TYPE = "Sales Quote v2";
    public static final String POC_QUOTE_RECORD_TYPE = "POC Quote";
    public static final String PROSERV_QUOTE_RECORD_TYPE = "ProServ Quote";
    public static final String CC_PROSERV_QUOTE_RECORD_TYPE = "CC ProServ Quote";
    public static final String CONTACT_CENTER_QUOTE_RECORD_TYPE = "Contact Center Quote";

    //  For 'QuoteType__c' field
    public static final String AGREEMENT_QUOTE_TYPE = "Agreement";
    public static final String QUOTE_QUOTE_TYPE = "Quote";

    //  For 'Status' field
    public static final String ACTIVE_QUOTE_STATUS = "Active";
    public static final String EXECUTED_QUOTE_STATUS = "Executed";
    public static final String DRAFT_QUOTE_STATUS = "Draft";

    //  For 'Approved_Status__c' field
    public static final String APPROVED_APPROVAL_STATUS = "Approved";

    //  Downsell Approvers in the Quote Approval Process (see the requirements in test case CRM-31903)
    public static final List<String> DOWNSELL_APPROVERS = List.of("Amber Alincastre", "Andrea Goddard",
            "Connie Rendon", "Dane Shore", "Doug Ruth", "Eric Clouse", "Lloyd Tanodra",
            "Michail Arnaudov", "Nick Rossi", "Reymark San Juan", "Veda Grace Holt",
            "Viktor Ivanov", "Vince Kahapay", "Jana Horner");

    //  For 'Upsell_Status__c' field
    public static final String NEW_QUOTE_TYPE = "New";
    public static final String UPSELL_QUOTE_TYPE = "Upsell";
    public static final String UPGRADE_QUOTE_TYPE = "Upgrade";

    // For 'PaymentMethod__c' field
    public static final String CREDITCARD_PAYMENT_METHOD = "CreditCard";
    public static final String INVOICE_PAYMENT_METHOD = "Invoice";

    //  For 'ProServ_Status__c' field
    public static final String CREATED_PROSERV_STATUS = "Created";
    public static final String IN_PROGRESS_PROSERV_STATUS = "In progress";
    public static final String SYNCED_PROSERV_STATUS = "Synced";
    public static final String OUT_FOR_SIGNATURE_PROSERV_STATUS = "Out for Signature";
    public static final String CANCELLED_PROSERV_STATUS = "Cancelled";
    public static final String SOLD_PROSERV_STATUS = "Sold";

    //  For 'TotalProjectHoursQuoted__c' field
    public static final double TOTAL_PROJECT_HOURS_QUOTED = 3.0;

    //  For 'PSProjectComplexity__c' field
    public static final String PS_PROJECT_COMPLEXITY = "UC-Basic";

    /**
     * Fill start date for Quote object with default value (current date/time)
     *
     * @param quote Quote object to set up with Start date
     */
    public static void setDefaultStartDate(Quote quote) {
        quote.setStart_Date__c(Calendar.getInstance());
    }

    /**
     * Fill end date for Quote object with default value (current date/time + 1 year)
     *
     * @param quote Quote object to set up with End date
     */
    public static void setDefaultEndDate(Quote quote) {
        var endDateDefault = Calendar.getInstance();
        endDateDefault.setTime(new Date());
        endDateDefault.add(Calendar.YEAR, 1);

        quote.setEnd_Date__c(endDateDefault);
    }

    /**
     * Set type to 'Agreement', Approved Status to 'Approved' and Status to 'Active' for current Quote.
     *
     * @param quote Quote object to set up as Approved Active Agreement
     */
    public static void setQuoteToApprovedActiveAgreement(Quote quote) {
        quote.setQuoteType__c(AGREEMENT_QUOTE_TYPE);
        quote.setApproved_Status__c(APPROVED_APPROVAL_STATUS);
        quote.setStatus(ACTIVE_QUOTE_STATUS);
    }

    /**
     * Populate required field values for ProServ Quote
     * in order to proceed with the Process Order of the related Opportunity.
     *
     * @param quote Quote object to set up with required fields
     */
    public static void setProServQuoteRequiredFields(Quote quote) {
        quote.setTotalProjectHoursQuoted__c(TOTAL_PROJECT_HOURS_QUOTED);
        quote.setPSProjectComplexity__c(PS_PROJECT_COMPLEXITY);
        quote.setProServ_Forecasted_Close_Date__c(Calendar.getInstance());
    }

    /**
     * Get the technical Quote by master quote ID and Service name.
     *
     * @param serviceName name of the service/tier (e.g. "Engage Voice Standalone")
     * @param masterQuoteId master quote ID
     * @return technical Quote sObject
     */
    public static Quote getTechQuote(String masterQuoteId, String serviceName) throws ConnectionException {
        return CONNECTION_UTILS.querySingleRecord(
                "SELECT Id, OpportunityId, ServiceName__c, Package_Info__c, isPrimary__c, " +
                        "QuoteType__c, Status " +
                        "FROM Quote " +
                        "WHERE ServiceName__c = '" + serviceName + "' " +
                        "AND MasterQuote__c = '" + masterQuoteId + "'",
                Quote.class);
    }
}
