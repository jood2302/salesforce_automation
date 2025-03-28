package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper;
import com.sforce.soap.enterprise.sobject.*;

import static com.aquiva.autotests.rc.utilities.Constants.IS_SANDBOX;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.*;

/**
 * Factory class for creating quick instances of {@link Opportunity} class.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 */
public class OpportunityFactory extends SObjectFactory {

    /**
     * Create new Opportunity object (NGBS), and insert it into Salesforce via API.
     *
     * @param account         account object to be associated with Opportunity
     * @param contact         contact object to be associated with Opportunity as a Primary Contact
     * @param isNewCustomer   true, if Opportunity is for New Business account;
     *                        false - for Existing Business accounts.
     * @param brandName       name for Opportunity's brand
     *                        (e.g. "RingCentral", "Avaya Cloud Office", etc...)
     * @param biId            ID for the Opportunity's Business Identity
     *                        (e.g. "4" for "RingCentral Inc.")
     * @param ownerUser       user to be set as Opportunity's owner
     * @param currencyIsoCode currency ISO code for the Opportunity
     *                        (e.g. "USD", "EUR", etc...)
     * @param tierName        name for Opportunity's tier
     *                        (e.g. "Office", "RingCentral Contact Center", etc...)
     *
     * @return Opportunity object with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static Opportunity createOpportunity(
            Account account, Contact contact, boolean isNewCustomer,
            String brandName, String biId, User ownerUser, String currencyIsoCode, String tierName)
            throws Exception {
        var type = isNewCustomer ? NEW_BUSINESS_TYPE : EXISTING_BUSINESS_TYPE;

        return createOpportunity(account, contact, account.getName(), type, brandName, biId, ownerUser, currencyIsoCode, tierName);
    }

    /**
     * Create new Opportunity object (NGBS), and insert it into Salesforce via API.
     *
     * @param account         account object to be associated with Opportunity
     * @param contact         contact object to be associated with Opportunity as a Primary Contact
     * @param opportunityName name for the Opportunity
     *                        (typically, related to its account's name)
     * @param type            Opportunity type depending on its account
     *                        (e.g. "New Business", "Existing Business")
     * @param brandName       name for Opportunity's brand
     *                        (e.g. "RingCentral", "Avaya Cloud Office", etc...)
     * @param biId            ID for the Opportunity's Business Identity
     *                        (e.g. "4" for "RingCentral Inc.")
     * @param ownerUser       user to be set as Opportunity's owner
     * @param currencyIsoCode currency ISO code for the Opportunity
     *                        (e.g. "USD", "EUR", etc...)
     * @param tierName        name for Opportunity's tier
     *                        (e.g. "Office", "RingCentral Contact Center", etc...)
     * @return Opportunity object with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static Opportunity createOpportunity(
            Account account, Contact contact, String opportunityName, String type,
            String brandName, String biId, User ownerUser, String currencyIsoCode, String tierName)
            throws Exception {
        var newOpportunity = new Opportunity();
        newOpportunity.setAccountId(account.getId());
        newOpportunity.setName(opportunityName);
        newOpportunity.setType(type);
        newOpportunity.setOwnerId(ownerUser.getId());
        newOpportunity.setBrand_Name__c(brandName);
        newOpportunity.setCurrencyIsoCode(currencyIsoCode);
        newOpportunity.setTier_Name__c(tierName);
        newOpportunity.setPrimary_Opportunity_Contact__c(contact.getId());
        OpportunityHelper.setDefaultFields(newOpportunity);
        OpportunityHelper.setBusinessIdentity(newOpportunity, currencyIsoCode, biId);
        if (!IS_SANDBOX) {
            OpportunityHelper.setForecastedCategoryForOmission(newOpportunity);
        }

        CONNECTION_UTILS.insertAndGetIds(newOpportunity);

        return newOpportunity;
    }

    /**
     * Create new Opportunity object (Legacy) and insert it into Salesforce via API.
     *
     * @param account       account object to be associated with Opportunity
     * @param isNewCustomer true, if Opportunity is for New Business account;
     *                      false - for Existing Business accounts
     * @return Opportunity object with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static Opportunity createLegacyOpportunity(Account account, boolean isNewCustomer) throws Exception {
        var type = isNewCustomer ? NEW_BUSINESS_TYPE : EXISTING_BUSINESS_TYPE;

        return createLegacyOpportunity(account.getId(), account.getName(), type);
    }

    /**
     * Create new Opportunity object (Legacy) and insert it into Salesforce via API.
     *
     * @param accountId       ID of the account object to be associated with Opportunity
     * @param opportunityName name for the Opportunity
     *                        (typically, related to its account's name)
     * @param type            Opportunity type depending on its account
     *                        (e.g. "New Business", "Existing Business")
     * @return Opportunity object with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static Opportunity createLegacyOpportunity(
            String accountId, String opportunityName, String type)
            throws Exception {
        var newOpportunity = new Opportunity();
        newOpportunity.setAccountId(accountId);
        newOpportunity.setName(opportunityName);
        newOpportunity.setType(type);
        newOpportunity.setBrand_Name__c(RC_US_BRAND_NAME);
        OpportunityHelper.setDefaultFields(newOpportunity, false);

        CONNECTION_UTILS.insertAndGetIds(newOpportunity);

        return newOpportunity;
    }
}
