package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.Entitlement__c;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.DISCOUNT_TYPE_CURRENCY;

/**
 * Helper class to facilitate operations on {@link Entitlement__c} objects.
 */
public class EntitlementHelper extends SObjectHelper {
    /**
     * Set default values to some fields of Entitlement__c object.
     *
     * @param entitlement Entitlement__c object to set up.
     */
    public static void setDefaultFields(Entitlement__c entitlement) {
        entitlement.setActive__c(true);
        entitlement.setDiscount_Type__c(DISCOUNT_TYPE_CURRENCY);
        entitlement.setDiscount__c(0D);
    }
}
