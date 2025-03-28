package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.aquiva.autotests.rc.utilities.Constants.IS_SANDBOX;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomPositiveInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createPrimarySignatoryContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContactFactory.createContactForAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContactFactory.createVarVisibleContactForAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static java.time.Clock.systemUTC;
import static java.time.format.DateTimeFormatter.ofPattern;

/**
 * Factory class for creating quick instances of {@link Account} class
 * with/without dependent objects (e.g. {@link Contact}, {@link AccountContactRole} etc.).
 * <p>
 * All factory methods also insert created objects into the SF database.
 */
public class AccountFactory extends SObjectFactory {
    //  Default values to include in Account.Name field
    private static final String NEW_CUSTOMER_DEFAULT_ACCOUNT_NAME = "NGBS New Customer";
    private static final String EXISTING_CUSTOMER_DEFAULT_ACCOUNT_NAME = "NGBS Existing Customer";
    private static final String LEGACY_EXISTING_CUSTOMER_DEFAULT_ACCOUNT_NAME = "Legacy Existing Customer";
    private static final String SERVICE_ACCOUNT_DEFAULT_NAME = "Service Account";
    private static final String PARTNER_ACCOUNT_DEFAULT_NAME = "Partner Account";

    /**
     * Create a new Account object with related Contact and Primary Signatory AccountContactRole
     * and insert them into Salesforce via API.
     *
     * @param ownerUser   Salesforce User that will be the owner of the resulting account
     *                    (usually, a sales user used for testing)
     * @param accountData data object with the Account's data
     *                    (should contain Currency ISO Code, Billing Country, Brand, etc.)
     * @return Account object with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static Account createAccountInSFDC(User ownerUser, AccountData accountData)
            throws Exception {
        if (accountData.billingId == null || accountData.billingId.isBlank()) {
            return createNewCustomerAccountInSFDC(ownerUser, accountData);
        }

        return createExistingCustomerAccountInSFDC(ownerUser, accountData);
    }

    /**
     * Create a new Account object for New Business Customer with related Contact
     * and Primary Signatory AccountContactRole and insert them into Salesforce via API.
     *
     * @param ownerUser   Salesforce User that will be the owner of the resulting account
     *                    (usually, a sales user used for testing)
     * @param accountData data object with the Account's data
     *                    (should contain Currency ISO Code, Billing Country, Brand, etc.)
     * @return Account object with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static Account createNewCustomerAccountInSFDC(User ownerUser, AccountData accountData)
            throws Exception {
        var account = setUpAccountWithCommonFields(NEW_CUSTOMER_DEFAULT_ACCOUNT_NAME,
                ownerUser.getId(), accountData, false);

        CONNECTION_UTILS.insertAndGetIds(account);

        var primaryContact = createContactForAccount(account, ownerUser);

        createPrimarySignatoryContactRole(account, primaryContact);

        return account;
    }

    /**
     * Create a new Account object for Existing Business Customer with related Contact
     * and Primary Signatory AccountContactRole and insert them into Salesforce via API.
     *
     * @param ownerUser   Salesforce User that will be the owner of the resulting account
     *                    (usually, a sales user used for testing)
     * @param accountData data object with the Account's data
     *                    (should contain Currency ISO Code, Billing Country, Brand, etc.)
     * @return Account object with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static Account createExistingCustomerAccountInSFDC(User ownerUser, AccountData accountData)
            throws Exception {
        var account = setUpAccountWithCommonFields(EXISTING_CUSTOMER_DEFAULT_ACCOUNT_NAME,
                ownerUser.getId(), accountData, false);

        setUpExistingBusinessFieldsForPaidStatus(account, accountData.billingId);

        CONNECTION_UTILS.insertAndGetIds(account);

        var primaryContact = createContactForAccount(account, ownerUser);

        createPrimarySignatoryContactRole(account, primaryContact);

        return account;
    }

    /**
     * Create a new partner Account object for New Business Customer with related Contact
     * and Primary Signatory AccountContactRole and insert them into Salesforce via API.
     *
     * @param ownerUser   Salesforce User that will be the owner of the resulting account
     *                    (usually, a sales user used for testing)
     * @param accountData data object with the Account's data
     *                    (should contain Currency ISO Code, Billing Country, Brand, etc.)
     * @return Account object with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static Account createNewPartnerAccountInSFDC(User ownerUser, AccountData accountData)
            throws Exception {
        var account = setUpAccountWithCommonFields(PARTNER_ACCOUNT_DEFAULT_NAME,
                ownerUser.getId(), accountData, true);

        var randomPartnerId = getRandomPositiveInteger();
        account.setPartner_ID__c(randomPartnerId);
        account.setPermitted_Brands__c(accountData.permittedBrands);

        CONNECTION_UTILS.insertAndGetIds(account);

        var primaryContact = createVarVisibleContactForAccount(account, ownerUser);
        account.setPartner_Contact__c(primaryContact.getId());
        CONNECTION_UTILS.update(account);

        createPrimarySignatoryContactRole(account, primaryContact);

        return account;
    }

    /**
     * Create a new Account object for New Business Customer without related Contact
     * and AccountContactRole and insert it into Salesforce via API.
     *
     * @param ownerUser   Salesforce User that will be the owner of the resulting account
     *                    (usually, a sales user used for testing)
     * @param accountData data object with the Account's data
     *                    (should contain Currency ISO Code, Billing Country, Brand, etc.)
     * @return Account object with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static Account createNewCustomerAccountWithoutContactInSFDC(User ownerUser, AccountData accountData)
            throws Exception {
        var account = setUpAccountWithCommonFields(NEW_CUSTOMER_DEFAULT_ACCOUNT_NAME,
                ownerUser.getId(), accountData, false);

        CONNECTION_UTILS.insertAndGetIds(account);

        return account;
    }

    /**
     * Create a new Account object for New Business Customer with related Contact,
     * without AccountContactRole and insert them into Salesforce via API.
     *
     * @param ownerUser   Salesforce User that will be the owner of the resulting account
     *                    (usually, a sales user used for testing)
     * @param accountData data object with the Account's data
     *                    (should contain Currency ISO Code, Billing Country, Brand, etc.)
     * @return Account object with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static Account createNewCustomerAccountWithoutContactRoleInSFDC(User ownerUser, AccountData accountData)
            throws Exception {
        var account = setUpAccountWithCommonFields(NEW_CUSTOMER_DEFAULT_ACCOUNT_NAME,
                ownerUser.getId(), accountData, false);

        CONNECTION_UTILS.insertAndGetIds(account);

        createContactForAccount(account, ownerUser);

        return account;
    }

    /**
     * Create a new ELA Service Account object with related Primary Signatory AccountContactRole
     * related to Parent Account's (New Business Customer) Contact record and insert them into Salesforce via API.
     *
     * @param ownerUser            Salesforce User that will be the owner of the resulting account
     *                             (usually, a sales user used for testing)
     * @param parentAccountContact parent Account's Contact to relate to the new Account via AccountContactRole
     * @param accountData          data object with the Account's data
     *                             (should contain Currency ISO Code, Billing Country, Brand, etc.)
     * @return Account object with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static Account createElaServiceAccountInSFDC(User ownerUser, Contact parentAccountContact, AccountData accountData)
            throws Exception {
        var account = setUpAccountWithCommonFields(NEW_CUSTOMER_DEFAULT_ACCOUNT_NAME,
                ownerUser.getId(), accountData, false);

        account.setELA_Account_Type__c(ELA_SERVICE_ACCOUNT_TYPE);

        CONNECTION_UTILS.insertAndGetIds(account);

        createPrimarySignatoryContactRole(account, parentAccountContact);

        return account;
    }

    /**
     * Create a new Account object for Legacy Existing Business Customer with Entitlements, related Contact
     * and Primary Signatory AccountContactRole and insert them into Salesforce via API.
     *
     * @param ownerUser Salesforce User that will be the owner of the resulting account
     *                  (usually, a sales user used for testing)
     * @return Account object with default parameters and ID from Salesforce
     * @throws Exception in case of malformed query, DB or network errors.
     */
    public static Account createLegacyExistingCustomerAccountInSFDC(User ownerUser) throws Exception {
        var existingAccount = new Account();

        var uniqueAccountName = getUniqueAccountName(LEGACY_EXISTING_CUSTOMER_DEFAULT_ACCOUNT_NAME);
        existingAccount.setName(uniqueAccountName);

        setRandomEnterpriseAccountId(existingAccount);
        setCustomerAccountRecordType(existingAccount);
        setUsBillingAddress(existingAccount);

        CONNECTION_UTILS.insertAndGetIds(existingAccount);

        var primaryContact = createContactForAccount(existingAccount, ownerUser);

        createPrimarySignatoryContactRole(existingAccount, primaryContact);

        var serviceProduct = CONNECTION_UTILS.querySingleRecord(
                "SELECT Id, Feature__c, Family " +
                        "FROM Product2 " +
                        "WHERE ExtID__c = '5101_17_DigitalLine Unlimited_null_Annual_null_null_null_false' " +
                        "LIMIT 1",
                Product2.class);
        EntitlementFactory.createEntitlementsForExistingAccount(existingAccount, serviceProduct);

        return existingAccount;
    }

    /**
     * Generate unique account's name for Account.Name field.
     *
     * @param defaultAccountName default account's name used as a part of the unique name
     *                           (e.g. "Default Account Name")
     * @return unique string with Account name
     */
    private static String getUniqueAccountName(String defaultAccountName) {
        var prefixForAccountsOnProd = IS_SANDBOX ? EMPTY_STRING : "[TEST] ";
        return prefixForAccountsOnProd +
                UUID.randomUUID().toString().substring(0, 23)
                + " " + defaultAccountName
                + " " + (LocalDateTime.now(systemUTC()).format(ofPattern("MM/dd HH:mm")));
    }

    /**
     * Populate the fields on the Account object that are common for all types of accounts.
     *
     * @param defaultAccountName default account's name used as a part of the unique name
     *                           (e.g. "NGBS New Customer")
     * @param ownerId            Salesforce standard ID of the Account's Owner
     * @param accountData        data object with the Account's data that may vary from account to account
     *                           (e.g. brand, currency, billing country, etc.)
     * @param isPartner          true, if the account is a Partner Account
     * @return Account object with common fields populated
     * @throws ConnectionException in case of malformed query, DB or network errors.
     */
    private static Account setUpAccountWithCommonFields(String defaultAccountName, String ownerId,
                                                        AccountData accountData,
                                                        boolean isPartner)
            throws ConnectionException {
        var account = new Account();

        var uniqueAccountName = getUniqueAccountName(defaultAccountName);
        account.setName(uniqueAccountName);

        account.setOwnerId(ownerId);
        account.setCurrencyIsoCode(accountData.currencyIsoCode);
        account.setPreferred_Language__c(EN_US_PREFERRED_LANGUAGE);
        account.setRC_Brand__c(accountData.rcBrand);
        setBillingAddress(account, accountData.billingCountry);

        if (isPartner) {
            setPartnerAccountRecordType(account);
        } else {
            setCustomerAccountRecordType(account);
        }

        return account;
    }

    /**
     * Data object for some of the Account's data that is used to create Accounts in tests.
     * Useful for creating test data for Account objects.
     */
    public static class AccountData {
        public String billingId;
        public String currencyIsoCode;
        public String rcBrand;
        public String permittedBrands;
        public String billingCountry;

        /**
         * Default no-arg constructor for Account's test data.
         * Use {@code withXXX()} methods to dynamically set the fields for such object.
         */
        public AccountData() {
        }

        /**
         * Constructor for Account's test data.
         *
         * @param data test data object that contains the necessary fields for Account's data
         *             (e.g. billing ID, currency, brand, billing country, etc.)
         */
        public AccountData(Dataset data) {
            this.billingId = data.getBillingId();
            this.currencyIsoCode = data.getCurrencyIsoCode();
            this.rcBrand = data.getBrandName();
            this.permittedBrands = data.getBrandName();
            this.billingCountry = data.getBillingCountry();
        }

        /**
         * Set the billing ID for the Account's data.
         *
         * @param billingId billing ID for the Account (e.g. "3551258002")
         * @return AccountData object with the billing ID set
         */
        public AccountData withBillingId(String billingId) {
            this.billingId = billingId;
            return this;
        }

        /**
         * Set the Currency ISO code for the Account's data.
         *
         * @param currencyIsoCode Currency ISO code for the Account (e.g. "USD")
         * @return AccountData object with the Currency ISO code set
         */
        public AccountData withCurrencyIsoCode(String currencyIsoCode) {
            this.currencyIsoCode = currencyIsoCode;
            return this;
        }

        /**
         * Set the RC Brand for the Account's data.
         *
         * @param rcBrand RC Brand for the Account (e.g. "RingCentral")
         * @return AccountData object with the RC Brand set
         */
        public AccountData withRcBrand(String rcBrand) {
            this.rcBrand = rcBrand;
            return this;
        }

        /**
         * Set the Permitted Brands on the Account's data.
         *
         * @param permittedBrands brands separated by ";" (e.g. "RingCentral;RingCentral EU")
         * @return AccountData object with the Permitted Brands set
         */
        public AccountData withPermittedBrands(String permittedBrands) {
            this.permittedBrands = permittedBrands;
            return this;
        }

        /**
         * Set the Billing Country for the Account's data.
         *
         * @param billingCountry Billing Country for the Account (e.g. "United States")
         * @return AccountData object with the Billing Country set
         */
        public AccountData withBillingCountry(String billingCountry) {
            this.billingCountry = billingCountry;
            return this;
        }
    }
}
