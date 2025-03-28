package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.Case;
import com.sforce.soap.enterprise.sobject.NGBSLBOBillingLimits__c;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_URL;

/**
 * Helper class to facilitate operations on {@link Case} objects.
 */
public class CaseHelper extends SObjectHelper {

    public static final String DEAL_AND_ORDER_SUPPORT_RECORD_TYPE = "Deal and Order Support";
    public static final String INTERNAL_BUSINESS_SERVICES_RECORD_TYPE = "Internal Business Services";
    public static final String SALES_ORDER_EXCEPT_DESK_RECORD_TYPE = "Sales Order Except Desk";
    public static final String INCONTACT_ORDER_RECORD_TYPE = "inContact_Order";

    //  For 'Status' field
    public static final String NEW_STATUS = "New";

    //  For 'Priority' field
    public static final String MEDIUM_PRIORITY = "Medium";

    //  For 'Case_Category__c' field
    public static final String IGNITE_PARTNER_CATEGORY = "Ignite Partner";

    //  For 'Case_Subcategory__c' field
    public static final String ELA_TERMS_SUBCATEGORY = "ELA terms";

    //  For 'Subject' field
    public static final String MANUAL_ORDER_REQUIRED_SUBJECT = "RingCentral Video: Manual Order Required for Quote";
    public static final String UC_QUOTE_REQUESTED_FOR_SUBJECT = "UC Quote requested for %s";

    //  For 'Description' field
    public static final String PHOENIX_QUOTE_CONTAINS_UPSELL_DESCRIPTION = "Manual Order is required for this Quote. " +
            "The reason, the Quote contains Upsell.\n" +
            "Please check Opportunity in SFDC and Service Web to confirm which items need to be updated or purchased.";

    //  For 'Owner' field
    public static final String BIZ_SERV_INNOVATION_TEAM_OWNER = "Biz Serv Innovation Team";

    //  For 'Origin' field
    public static final String LEAD_ORIGIN_VALUE = "Lead";
    public static final String OPPORTUNITY_ORIGIN_VALUE = "Opportunity";
    public static final String QUOTING_PAGE_ORIGIN_VALUE = "Quoting Page";
    public static final String AUTO_EMAIL_ORIGIN = "Auto-Email";

    //  For 'inContact_Status__c' field
    public static final String INCONTACT_COMPLETED_STATUS = "Completed";

    /**
     * Get a subject for the 'Sales Order Except Desk' Case.
     *
     * @param opportunityName name of the related Opportunity
     * @return subject for the 'Sales Order Except Desk' Case
     */
    public static String getProductsForManualShippingCaseSubject(String opportunityName) {
        return "Products for manual shipping for Opportunity: " + opportunityName;
    }

    /**
     * Get formatted string which contains link to account in SF.
     * <br/>
     * Used in certain Deal Desk case's description.
     *
     * @param accountId ID of Account for which Case is created
     * @return String with link to account in SF
     * (e.g <i>"Account: https://rc--gci.sandbox.my.salesforce.com/00119000015D151AAC"</i>)
     */
    public static String formatAccountLinkInCaseDescription(String accountId) {
        return String.format("Account: " + BASE_URL + "/%s", accountId);
    }

    /**
     * Get formatted string which contains link to opportunity in SF.
     * <br/>
     * Used in certain Deal Desk case's description.
     *
     * @param opportunityId ID of Opportunity for which Case is created
     * @return String with link to opportunity in SF
     * (e.g. <i>"Opportunity: https://rc--gci.sandbox.my.salesforce.com/0061900000BmGCDAA3"</i>)
     */
    public static String formatOpportunityLinkInCaseDescription(String opportunityId) {
        return String.format("Opportunity: " + BASE_URL + "/%s", opportunityId);
    }

    /**
     * Get formatted string which contains Account ID, Opportunity ID, shipping address, product name, quantity
     * for license that should be shipped manually.
     * <br/>
     * This is used in Deal Desk case, which is created when Exception License is bought.
     * e.g. Global MVP DigitalLine.
     * <br/>
     * Note: full list of Exception Licenses can be found in Custom Setting: {@link NGBSLBOBillingLimits__c}.
     *
     * @param accountId       ID of Account for which the Case is created
     * @param opportunityId   ID of Opportunity for which the Case is created
     * @param shippingAddress shipping address from shipping group on the Shipping tab in UQT
     * @param productName     the name of the product on the Price tab in UQT
     * @param quantity        the quantity of the product on the Price tab in UQT
     * @return String with links to Account and Opportunity, shipping address, product name, and its quantity
     * (e.g "Account: https://rc--gci.sandbox.my.salesforce.com/001D500001IQoXyIAL
     * <p>
     * Opportunity: https://rc--gci.sandbox.my.salesforce.com/006D500000Kgm2EIAR
     * <p>
     * Shipping for the following products should be arranged manually:
     * <p>
     * Shipping Group 1: 20 Davis Drive, Belmont, California, 94002, United States
     * <p>
     * 1. Product Name: Poly Headset - Blackwire 3320 USB-A; Quantity: 1.00;")
     */
    public static String formatProductsForManualShippingInCaseDescription(String accountId, String opportunityId,
                                                                          String shippingAddress,
                                                                          String productName, int quantity) {
        return String.format("Account: %s/%s\n\n" +
                        "Opportunity: %s/%s\n\n" +
                        "Shipping for the following products should be arranged manually:\n\n" +
                        "Shipping Group 1: %s\n\n" +
                        "1. Product Name: %s; Quantity: %s;",
                BASE_URL, accountId,
                BASE_URL, opportunityId,
                shippingAddress,
                productName, quantity);
    }
}