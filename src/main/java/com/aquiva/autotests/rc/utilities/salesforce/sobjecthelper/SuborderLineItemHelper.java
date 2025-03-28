package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.SuborderLineItem__c;

/**
 * Helper class for {@link SuborderLineItem__c} objects.
 */
public class SuborderLineItemHelper extends SObjectHelper {

    //  For 'LocationDetails__c' field
    private static final String LOCATION_DETAILS_FORMAT = "[{\"quantity\":%d,\"billingLocationId\":\"%s\", \"billingCodeId\":\"\"}]";

    /**
     * Populate LocationDetails__c field with JSON value
     * using the given service location address ID.
     *
     * @param suborderLineItem SuborderLineItem__c object to update
     *                         (should contain non-null {@code Quantity__c} value)
     * @param locationId       ID of the service location address in NGBS
     */
    public static void setLocationDetails(SuborderLineItem__c suborderLineItem, String locationId) {
        var locationDetailsValue = String.format(LOCATION_DETAILS_FORMAT, suborderLineItem.getQuantity__c().intValue(), locationId);
        suborderLineItem.setLocationDetails__c(locationDetailsValue);
    }
}
