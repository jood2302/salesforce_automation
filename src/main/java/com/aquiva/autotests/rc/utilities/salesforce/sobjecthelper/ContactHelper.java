package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.Contact;
import com.sforce.ws.ConnectionException;

import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomEmail;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomUSPhone;
import static java.util.UUID.randomUUID;

/**
 * Helper class to facilitate operations on {@link Contact} objects.
 */
public class ContactHelper extends SObjectHelper {
    //  SFDC API parameters
    public static final String S_OBJECT_API_NAME = "Contact";
    public static final String INTERNAL_CONTACT_RECORD_TYPE = "Internal";
    public static final String VAR_VISIBLE_CONTACT_RECORD_TYPE = "VAR Visible";

    //  Default values for main contact's parameters
    private static final String DEFAULT_FIRST_NAME = "FirstName";
    private static final String DEFAULT_LAST_NAME = "LastName";
    private static final String DEFAULT_LANGUAGE = "en_US";

    //  For 'Contact_Status__c' field
    public static final String WORKING_OPP_CONTACT_STATUS = "3. Working Opp";

    /**
     * Get full name of the given Contact object.
     * <p>Typically, it contains a first name and a last name.</p>
     * Example: John Smith
     *
     * @param contact Contact object to get a full name from
     * @return full name of the Contact
     */
    public static String getFullName(Contact contact) {
        return contact.getFirstName() + " " + contact.getLastName();
    }

    /**
     * Set 'Internal' record type for the Contact object.
     *
     * @param contact Contact object to set up with RecordTypeId
     * @throws ConnectionException in case of errors while accessing API
     */
    public static void setInternalRecordType(Contact contact) throws ConnectionException {
        var internalContactRecordTypeId = CONNECTION_UTILS.getRecordTypeId(S_OBJECT_API_NAME, INTERNAL_CONTACT_RECORD_TYPE);
        contact.setRecordTypeId(internalContactRecordTypeId);
    }

    /**
     * Set 'VAR Visible' record type for the Contact object.
     *
     * @param contact Contact object to set up with RecordTypeId
     * @throws ConnectionException in case of errors while accessing API
     */
    public static void setVarVisibleRecordType(Contact contact) throws ConnectionException {
        var varVisibleContactRecordTypeId = CONNECTION_UTILS.getRecordTypeId(S_OBJECT_API_NAME,
                VAR_VISIBLE_CONTACT_RECORD_TYPE);
        contact.setRecordTypeId(varVisibleContactRecordTypeId);
    }

    /**
     * Set randomly generated values to basic Contact fields that may be useful in tests.
     *
     * @param contact Contact instance to set up with randomly generated values
     */
    public static void setRequiredFieldsRandomly(Contact contact) {
        var randomSuffix = randomUUID().toString().substring(0, 31);
        contact.setFirstName(DEFAULT_FIRST_NAME + randomSuffix);
        contact.setLastName(DEFAULT_LAST_NAME + randomSuffix);
        contact.setEmail(getRandomEmail());
        contact.setPhone(getRandomUSPhone());
        contact.setPreferred_Language__c(DEFAULT_LANGUAGE);
    }
}
