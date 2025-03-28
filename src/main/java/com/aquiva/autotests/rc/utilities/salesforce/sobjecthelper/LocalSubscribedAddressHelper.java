package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.LocalSubscribedAddress__c;
import com.sforce.ws.ConnectionException;

/**
 * Helper class to facilitate operations on {@link LocalSubscribedAddress__c} objects
 * and to provide with some constant values of object fields.
 */
public class LocalSubscribedAddressHelper extends SObjectHelper {
    //  SFDC API parameters
    public static final String LOCAL_SUBSCRIBED_ADDRESS_SOBJECT_API_NAME = "LocalSubscribedAddress__c";
    public static final String LOCAL_SUBSCRIBED_ADDRESS_RECORD_TYPE = "Local Subscribed Address of Company";
    public static final String REGISTERED_ADDRESS_RECORD_TYPE = "Registered Address of Company";

    public static final String LOCAL_SUBSCRIBED_ADDRESS_CITY = "Mumbai";
    public static final String LOCAL_SUBSCRIBED_ADDRESS_STATE = "Maharashtra";
    public static final String LOCAL_SUBSCRIBED_ADDRESS_STREET_ADDRESS = "Ajit Glass Garden Rd";
    public static final String LOCAL_SUBSCRIBED_ADDRESS_DISTRICT = "India";
    public static final String LOCAL_SUBSCRIBED_ADDRESS_PINCODE = "400104";

    /**
     * Set LocalSubscribedAddress__c instance's RecordTypeId field
     * according to 'Registered Address of Company' value.
     *
     * @param localSubscribedAddress a LocalSubscribedAddress__c instance to set up
     * @throws ConnectionException in case of errors while accessing API
     */
    public static void setRegisteredAddressOfCompanyRecordType(LocalSubscribedAddress__c localSubscribedAddress)
            throws ConnectionException {
        var registeredAddressRecordTypeId =
                CONNECTION_UTILS.getRecordTypeId(LOCAL_SUBSCRIBED_ADDRESS_SOBJECT_API_NAME,
                        REGISTERED_ADDRESS_RECORD_TYPE);

        localSubscribedAddress.setRecordTypeId(registeredAddressRecordTypeId);
    }

    /**
     * Set LocalSubscribedAddress__c instance's RecordTypeId field
     * according to 'Local Subscribed Address of Company' value.
     *
     * @param localSubscribedAddress a LocalSubscribedAddress__c instance to set up
     * @throws ConnectionException in case of errors while accessing API
     */
    public static void setLocalSubscribedAddressOfCompanyRecordType(LocalSubscribedAddress__c localSubscribedAddress)
            throws ConnectionException {
        var localSubscribedAddressRecordTypeId =
                CONNECTION_UTILS.getRecordTypeId(LOCAL_SUBSCRIBED_ADDRESS_SOBJECT_API_NAME,
                        LOCAL_SUBSCRIBED_ADDRESS_RECORD_TYPE);

        localSubscribedAddress.setRecordTypeId(localSubscribedAddressRecordTypeId);
    }

    /**
     * Set up address fields with some default India values.
     *
     * @param localSubscribedAddress a LocalSubscribedAddress__c instance to set up
     */
    public static void setIndiaAddress(LocalSubscribedAddress__c localSubscribedAddress) {
        localSubscribedAddress.setCity__c(LOCAL_SUBSCRIBED_ADDRESS_CITY);
        localSubscribedAddress.setDistrict__c(LOCAL_SUBSCRIBED_ADDRESS_DISTRICT);
        localSubscribedAddress.setPinCode__c(LOCAL_SUBSCRIBED_ADDRESS_PINCODE);
        localSubscribedAddress.setState__c(LOCAL_SUBSCRIBED_ADDRESS_STATE);
        localSubscribedAddress.setStreetAddress__c(LOCAL_SUBSCRIBED_ADDRESS_STREET_ADDRESS);
    }
}
