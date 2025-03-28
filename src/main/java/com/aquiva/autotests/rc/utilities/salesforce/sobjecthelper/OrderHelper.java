package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.Order;

/**
 * Helper class to facilitate operations on {@link Order} objects.
 */
public class OrderHelper extends SObjectHelper {

    //  For 'Status' field
    public static final String NEW_STATUS = "New";
    public static final String LOCKED_STATUS = "Locked";

    //  For 'StatusCode' field
    private static final String DRAFT_STATUS_CODE = "Draft";
    
    //  For 'Provisioning__c' field
    public static final String AUTO_PROVISIONING = "auto";
    public static final String MANUAL_PROVISIONING = "manual";

    /**
     * Set Status = 'New' and StatusCode = 'Draft' on the Order.
     *
     * @param order Order record to set up
     */
    public static void deactivateOrder(Order order) {
        order.setStatus(NEW_STATUS);
        order.setStatusCode(DRAFT_STATUS_CODE);
    }
}
