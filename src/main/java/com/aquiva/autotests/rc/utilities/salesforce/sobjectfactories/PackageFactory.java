package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.Package__c;
import com.sforce.ws.ConnectionException;

/**
 * Factory class for creating quick instances of {@link Package__c} class.
 * <br/>
 * This object is usually created automatically through NGBS integration
 * after the New Business account is signed up successfully in NGBS,
 * but it takes some time, so the object is usually not available for the automated tests
 * that work with Existing Business Accounts. So you can use this factory to create them manually.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 */
public class PackageFactory extends SObjectFactory {

    /**
     * Create new Package__c object (Billing Account Package) and insert it into Salesforce via API.
     *
     * @param accountId            ID of the Account object in SFDC (Existing Business) to be associated with Billing Account Package
     * @param ngbsPackageId        ID for the package on the account in NGBS (e.g. "235798001")
     * @param ngbsPackageCatalogId ID for the package on the account NGBS from the Catalog (e.g. "1231005" for "RingEX Core™" package)
     * @param brandName            name of the RC Brand (e.g. "RingCentral", "RingCentral EU", "Avaya Cloud Office")
     * @param serviceName          name of the service (e.g. "Office", "Engage Digital Standalone", "Engage Voice Standalone")
     * @param paymentMethod        payment method (e.g. "Credit Card", "Invoice", "InvoiceOnBehalf", "DirectDebit")
     * @param rcAccountStatus      status of the account in NGBS (e.g. "Paid", "PoC")
     * @return Package__c object for the Existing Business Account with ID from Salesforce.
     * @throws ConnectionException in case of malformed query, DB or network errors.
     */
    public static Package__c createBillingAccountPackage(String accountId, String ngbsPackageId, String ngbsPackageCatalogId,
                                                         String brandName, String serviceName,
                                                         String paymentMethod, String rcAccountStatus)
            throws ConnectionException {
        var billingAccountPackage = new Package__c();
        billingAccountPackage.setEnterprise_Account_ID__c(accountId);
        billingAccountPackage.setAccount__c(accountId);
        billingAccountPackage.setAccount_Package_Id__c(ngbsPackageId);
        billingAccountPackage.setPackage_Catalog_Id__c(ngbsPackageCatalogId);
        billingAccountPackage.setRC_Brand__c(brandName);
        billingAccountPackage.setRC_Service_name__c(serviceName);
        billingAccountPackage.setService_Type__c(serviceName);
        billingAccountPackage.setAccount_Payment_Method__c(paymentMethod);
        billingAccountPackage.setRC_Account_Status__c(rcAccountStatus);

        CONNECTION_UTILS.insertAndGetIds(billingAccountPackage);

        return billingAccountPackage;
    }
}
