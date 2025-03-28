package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.Opportunity;

import java.util.Calendar;
import java.util.Date;

/**
 * Helper class to facilitate operations on {@link Opportunity} objects.
 */
public class OpportunityHelper extends SObjectHelper {
    //  Billing system's UI-values depending on 'Is_Billing_Opportunity__c' field
    public static final String BILLING_SYSTEM_NGBS = "NGBS";
    public static final String BILLING_SYSTEM_LEGACY = "Legacy";

    //  For 'Type' field
    public static final String NEW_BUSINESS_TYPE = "New Business";
    public static final String EXISTING_BUSINESS_TYPE = "Existing Business";

    //  For 'StageName' field
    public static final String QUALIFY_STAGE = "1. Qualify";
    public static final String PROOF_STAGE = "4. Proof";
    public static final String AGREEMENT_STAGE = "5. Agreement";
    public static final String ORDER_STAGE = "6. Order";
    public static final String CLOSED_WON_STAGE = "7. Closed Won";
    public static final String CLOSED_WON_FOR_PROSERV_STAGE = "7.1. Closed Won for ProServ";

    //  For 'Brand_Name__c' field
    public static final String RC_US_BRAND_NAME = "RingCentral";
    public static final String RC_UK_BRAND_NAME = "RingCentral UK";
    public static final String RC_AU_BRAND_NAME = "RingCentral AU";
    public static final String RC_EU_BRAND_NAME = "RingCentral EU";
    public static final String AVAYA_US_BRAND_NAME = "Avaya Cloud Office";
    public static final String TELUS_BUSINESS_CONNECT_BRAND_NAME = "TELUS Business Connect";
    public static final String UNIFY_OFFICE_BRAND_NAME = "Unify Office";

    //  For 'BusinessIdentity__c' field
    public static final String BI_FORMAT = "{\"currency\":\"%s\",\"biId\":\"%s\"}";
    public static final String RC_US_BI_ID = "4";
    public static final String RC_CA_BI_ID = "5";
    public static final String RC_DE_BI_ID = "33";
    public static final String RC_INDIA_MUMBAI_BUSINESS_IDENTITY_ID = "52";
    public static final String RC_INDIA_BANGALORE_BUSINESS_IDENTITY_ID = "53";
    public static final String RC_INDIA_MAHARASHTRA_BUSINESS_IDENTITY_ID = "202005";
    public static final String RC_INDIA_ANDHRA_PRADESH_BUSINESS_IDENTITY_ID = "203005";
    public static final String RC_INDIA_DELHI_METRO_BUSINESS_IDENTITY_ID = "204005";
    public static final String RC_CH_BI_ID = "58";
    public static final String UNIFY_CH_BI_ID = "59";
    public static final String AMAZON_US_BI_ID = "62005";

    //  For 'Tier_Name__c' field
    public static final String OFFICE_SERVICE = "Office";
    public static final String ENGAGE_DIGITAL_STANDALONE_SERVICE = "Engage Digital Standalone";
    public static final String ENGAGE_VOICE_STANDALONE_SERVICE = "Engage Voice Standalone";
    public static final String RINGCENTRAL_CONTACT_CENTER_SERVICE = "RingCentral Contact Center";
    public static final String FAX_SERVICE = "Fax";
    public static final String MEETINGS_SERVICE = "Meetings";
    public static final String RC_EVENTS_SERVICE = "Events";

    //  For 'RecordType.Name' field
    public static final String VAR_OPPORTUNITY_RECORD_TYPE = "VAR Opportunity";
    public static final String NEW_SALES_OPPORTUNITY_RECORD_TYPE = "New Sales Opportunity";

    //  For 'Partner_Lead_Source__c' field
    public static final String PARTNER_LEAD_SOURCE = "Partner";

    //  For 'LeadSource' field
    public static final String INBOUND_CALL_LEAD_SOURCE = "Inbound Call";

    /**
     * Set default values to some fields in Opportunity.
     * <p>This method is for new Opportunities (will set 'Is_Billing_Opportunity__c' = true)</p>
     *
     * @param opportunity Opportunity object to set up
     */
    public static void setDefaultFields(Opportunity opportunity) {
        setDefaultFields(opportunity, true);
    }

    /**
     * Set default values to some fields of Opportunity object.
     *
     * @param opportunity  Opportunity object to set up
     * @param isNewBilling 'true' for NGBS Opportunities; 'false' for Legacy Opportunities
     */
    public static void setDefaultFields(Opportunity opportunity, boolean isNewBilling) {
        var closeDate = Calendar.getInstance();
        closeDate.setTime(new Date());
        opportunity.setCloseDate(closeDate);

        opportunity.setStageName(QUALIFY_STAGE);
        opportunity.setIs_Billing_Opportunity__c(isNewBilling);
    }

    /**
     * Set value for 'BusinessIdentity__c' field on the Opportunity.
     *
     * @param opportunity     Opportunity object to set up
     * @param currencyIsoCode ISO code for currency (e.g. "USD", "EUR", etc...)
     * @param biId            ID for the Opportunity's Business Identity (e.g. "4" for "RingCentral Inc.")
     */
    public static void setBusinessIdentity(Opportunity opportunity, String currencyIsoCode, String biId) {
        var businessIdentityValue = String.format(BI_FORMAT, currencyIsoCode, biId);
        opportunity.setBusinessIdentity__c(businessIdentityValue);
    }

    /**
     * Set the required fields to be able to change StageName field later on (e.g. to close the Opportunity)
     *
     * @param opportunity Opportunity object to set up
     */
    public static void setRequiredFieldsForOpportunityStageChange(Opportunity opportunity) {
        opportunity.setWhy_We_Won__c("Product Features");
        opportunity.setFinal_Competitor_we_won_against__c("Avaya");
        opportunity.setAdditional_details_on_why_we_won__c("Test details on why we won");
        opportunity.setKey_Deal_Integration__c("MS Teams");
        opportunity.setMeetings__c("Adobe Connect");
        opportunity.setPhone__c("Arkadin");
        opportunity.setMessage__c("Amazon Chime");
        opportunity.setContact_Center__c("Aspect");
        opportunity.setDigital_Channels__c("Email");
        opportunity.setVideo_Competition__c("Adobe Connect");
        opportunity.setCC_Competitor_s__c("Aspect");
        opportunity.setCompetitor_Phone__c("Arkadin");
        opportunity.setCompetitor_Message__c("Amazon Chime");
        opportunity.setCompetitor_Digital_Channels__c("Email");
        opportunity.setIdentified_Risks__c("Test Identified Risks and Mitigation Plan");
    }

    /**
     * Set the Forecast Category fields for the Opportunity
     * to be omitted from the Forecasts (useful for creating test data on the Production environment).
     *
     * @param opportunity Opportunity object to set up
     */
    public static void setForecastedCategoryForOmission(Opportunity opportunity) {
        opportunity.setForecastCategoryName("Omitted");
        opportunity.setForecast_Category_Internal__c("Omit - Administrative/Test");
    }
}
