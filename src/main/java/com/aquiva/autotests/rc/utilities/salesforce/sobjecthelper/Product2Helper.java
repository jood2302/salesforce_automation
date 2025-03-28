package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.Product2;

/**
 * Helper class to facilitate operations on {@link Product2} objects.
 */
public class Product2Helper extends SObjectHelper {
    //  For 'Family' field
    public static final String FAMILY_TAXES = "Taxes";

    //  For 'Billing_Type__c' field
    public static final String RECURRING_BILLING_TYPE = "Recurring";
    
    //  For 'InitialOrderType__c' field
    public static final String AUTO_WITH_CASE_INITIAL_ORDER_TYPE = "autoWithCase";
    public static final String MANUAL_INITIAL_ORDER_TYPE = "manual";
}
