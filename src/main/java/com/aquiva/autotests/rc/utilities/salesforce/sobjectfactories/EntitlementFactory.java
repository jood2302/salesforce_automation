package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.EntitlementHelper;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating quick instances of {@link Entitlement__c} class ("RC Entitlement").
 * <br/>
 * All public factory methods also insert created objects into the SF database.
 * <br/>
 * Note: not to be mistaken with the standard SFDC {@link Entitlement} object ("Support Entitlement")!
 */
public class EntitlementFactory extends SObjectFactory {
    //  Default values for entitlement's parameters
    private static final Integer DEFAULT_QUANTITY = 10;

    /**
     * Create a list of default service entitlements on the existing business account,
     * and insert them into Salesforce.
     * <br/>
     * This method adds entitlements for all dependent products as well.
     * <br/>
     * E.g. for 'DigitalLine Unlimited' product, dependent products are
     * 'Cost Recovery Fee' and 'E911 Fee' => 3 entitlements will be generated here,
     * one per product.
     *
     * @param account        existing business account used to link entitlements to
     * @param serviceProduct special service product
     * @return list of service Entitlements for the existing business account
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static List<Entitlement__c> createEntitlementsForExistingAccount(Account account, Product2 serviceProduct)
            throws Exception {
        var result = new ArrayList<Entitlement__c>();

        var priceBookId = CONNECTION_UTILS.querySingleRecord(
                "SELECT Pricebook2Id " +
                        "FROM PricebookEntry " +
                        "WHERE Product2Id = '" + serviceProduct.getId() + "' " +
                        "AND Pricebook2.IsStandard = false ",
                PricebookEntry.class)
                .getPricebook2Id();

        var servicePriceBookEntryList = CONNECTION_UTILS.query(
                "SELECT Id, Name, Product2Id, UnitPrice, CurrencyISOCode, " +
                        "Product2.Id, Product2.Feature__c, Product2.Family " +
                        "FROM PricebookEntry " +
                        "WHERE (" +
                        "Product2Id = '" + serviceProduct.getId() + "' " +
                        "OR Product2.Dependent_On_Feature__c = " + serviceProduct.getFeature__c() +
                        ") " +
                        "AND Pricebook2.Id = '" + priceBookId + "'",
                PricebookEntry.class);

        for (var priceBookEntry : servicePriceBookEntryList) {
            var entitlementFromPbe = createNewEntitlement(account, priceBookEntry);
            result.add(entitlementFromPbe);
        }

        CONNECTION_UTILS.insertAndGetIds(result);

        return result;
    }

    /**
     * Create an entitlement with a provided Price Book Entry data on the existing business account,
     * and insert it into Salesforce.
     *
     * @param account        existing business account used to link entitlement to
     * @param priceBookEntry Price Book Entry that contains needed data for Entitlement__c creation
     * @return Entitlement for the existing business account
     * @throws ConnectionException in case of malformed query, DB or network errors.
     */
    public static Entitlement__c createEntitlementForExistingAccount(Account account, PricebookEntry priceBookEntry)
            throws ConnectionException {
        var entitlement = createNewEntitlement(account, priceBookEntry);

        CONNECTION_UTILS.insertAndGetIds(entitlement);

        return entitlement;
    }

    /**
     * Create new Entitlement__c object for the Existing Business Account
     * using the data from the corresponding PricebookEntry object.
     *
     * @param account        existing business account used to link the entitlement to
     * @param priceBookEntry Price Book Entry that contains test data for Entitlement__c creation
     *                       (e.g. price, quantity, currency)
     * @return Entitlement__c object (ready to be pass on to the SOAP API to insert into the SFDC DB)
     */
    private static Entitlement__c createNewEntitlement(Account account, PricebookEntry priceBookEntry) {
        var entitlement = new Entitlement__c();

        EntitlementHelper.setDefaultFields(entitlement);
        entitlement.setAccount__c(account.getId());
        entitlement.setCurrencyIsoCode(priceBookEntry.getCurrencyIsoCode());
        entitlement.setPrice__c(priceBookEntry.getUnitPrice());
        entitlement.setQuantity__c(DEFAULT_QUANTITY.doubleValue());
        entitlement.setProduct__c(priceBookEntry.getProduct2Id());

        return entitlement;
    }
}
