package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContactHelper.*;

/**
 * Factory class for creating quick instances of {@link Contact} class.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 */
public class ContactFactory extends SObjectFactory {

    /**
     * Create new Contact object with 'Internal' type and insert it into Salesforce via API.
     * <br/>
     * Note: Contact has no AccountContactRole relation with the given Account!
     * If you need it, create it separately.
     *
     * @param account   account object to be associated with Contact
     * @param ownerUser Salesforce user that will be the owner of the resulting contact
     *                  (usually, a sales user used for testing)
     * @return Contact object with unique values for required fields and ID from Salesforce.
     * @throws ConnectionException in case of malformed query, DB or network errors.
     */
    public static Contact createContactForAccount(Account account, User ownerUser) throws ConnectionException {
        var contact = new Contact();
        contact.setAccountId(account.getId());
        contact.setOwnerId(ownerUser.getId());
        setInternalRecordType(contact);
        setRequiredFieldsRandomly(contact);

        CONNECTION_UTILS.insertAndGetIds(contact);

        return contact;
    }

    /**
     * Create new Contact object with 'VAR Visible' type and insert it into Salesforce via API.
     * </br>
     * Note: Contact has no AccountContactRole relation with the given Account!
     * If you need it, create it separately.
     *
     * @param partnerAccount partner account object to be associated with Contact
     * @param ownerUser      Salesforce user that will be the owner of the resulting contact
     *                       (usually, a deal desk user used for testing)
     * @return Contact object with unique values for required fields and ID from Salesforce.
     * @throws ConnectionException in case of malformed query, DB or network errors.
     */
    public static Contact createVarVisibleContactForAccount(Account partnerAccount, User ownerUser) throws ConnectionException {
        var contact = new Contact();
        contact.setAccountId(partnerAccount.getId());
        contact.setOwnerId(ownerUser.getId());
        setVarVisibleRecordType(contact);
        setRequiredFieldsRandomly(contact);

        CONNECTION_UTILS.insertAndGetIds(contact);

        return contact;
    }
}
