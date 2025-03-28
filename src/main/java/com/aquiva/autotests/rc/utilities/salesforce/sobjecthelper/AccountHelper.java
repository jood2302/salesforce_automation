package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.Contact;
import com.sforce.ws.ConnectionException;

import java.util.Random;

import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static java.util.Objects.requireNonNullElse;

/**
 * Helper class to facilitate operations on {@link Account} objects.
 */
public class AccountHelper extends SObjectHelper {
    //  SFDC API parameters
    private static final String S_OBJECT_API_NAME = "Account";
    private static final String CUSTOMER_ACCOUNT_RECORD_TYPE = "Customer Account";
    private static final String PARTNER_ACCOUNT_RECORD_TYPE = "Partner Account";

    //  Values for US Billing Address fields
    private static final String US_BILLING_STREET = "516 Walden Dr";
    private static final String US_BILLING_CITY = "Beverly Hills";
    private static final String US_BILLING_STATE = "CA";
    private static final String US_BILLING_POSTAL_CODE = "90210";
    public static final String US_BILLING_COUNTRY = "United States";

    //  Values for Canada Billing Address fields
    private static final String CA_BILLING_STREET = "3080 Prince Edward St";
    private static final String CA_BILLING_CITY = "Vancouver";
    private static final String CA_BILLING_STATE = "BC";
    private static final String CA_BILLING_POSTAL_CODE = "V5T 3N4";
    public static final String CA_BILLING_COUNTRY = "Canada";

    //  Values for EU Billing Address fields
    private static final String FR_BILLING_STREET = "25 Rue Richard-Lenoir";
    private static final String FR_BILLING_CITY = "Paris";
    private static final String FR_BILLING_STATE = "Reunion";
    private static final String FR_BILLING_POSTAL_CODE = "35000";
    public static final String FR_BILLING_COUNTRY = "France";

    //  Values for Germany Billing Address fields
    private static final String DE_BILLING_STREET = "Alexanderpl. 7";
    private static final String DE_BILLING_CITY = "Berlin";
    private static final String DE_BILLING_STATE = "";
    private static final String DE_BILLING_POSTAL_CODE = "10178";
    public static final String DE_BILLING_COUNTRY = "Germany";

    //  Values for India Billing Address fields
    public static final String INDIA_BILLING_STREET = "85 Nawab Hyder Ali Khan Road";
    public static final String INDIA_BILLING_CITY = "Bangaluru";
    public static final String INDIA_BILLING_STATE = "Karnataka";
    public static final String INDIA_BILLING_POSTAL_CODE = "560002";
    public static final String INDIA_BILLING_COUNTRY = "India";

    //  Values for UK Billing Address fields
    private static final String UK_BILLING_STREET = "97 Great Russell St";
    private static final String UK_BILLING_CITY = "London";
    private static final String UK_BILLING_STATE = "London";
    private static final String UK_BILLING_POSTAL_CODE = "WC1B 3QJ";
    public static final String UK_BILLING_COUNTRY = "United Kingdom";

    //  Values for Australia Billing Address fields
    private static final String AU_BILLING_STREET = "488 George St";
    private static final String AU_BILLING_CITY = "Sydney";
    private static final String AU_BILLING_STATE = "New South Wales";
    private static final String AU_BILLING_POSTAL_CODE = "NSW 2000";
    public static final String AU_BILLING_COUNTRY = "Australia";

    //  Values for Taiwan Billing Address fields
    public static final String TAIWAN_BILLING_STREET = "97 Zhongshan Road";
    public static final String TAIWAN_BILLING_CITY = "Taipei";
    public static final String TAIWAN_BILLING_STATE = "";
    public static final String TAIWAN_BILLING_POSTAL_CODE = "208";
    public static final String TAIWAN_BILLING_COUNTRY = "Taiwan";

    //  Values for Singapore Billing Address fields
    private static final String SINGAPORE_BILLING_STREET = "2 Orchard Turn";
    private static final String SINGAPORE_BILLING_CITY = "Singapore";
    private static final String SINGAPORE_BILLING_STATE = "";
    private static final String SINGAPORE_BILLING_POSTAL_CODE = "238801";
    public static final String SINGAPORE_BILLING_COUNTRY = "Singapore";

    //  Values for Switzerland Billing Address fields
    private static final String SWITZERLAND_BILLING_STREET = "Falkenstrasse 1";
    private static final String SWITZERLAND_BILLING_CITY = "Zurich";
    private static final String SWITZERLAND_BILLING_STATE = "";
    private static final String SWITZERLAND_BILLING_POSTAL_CODE = "8001";
    public static final String SWITZERLAND_BILLING_COUNTRY = "Switzerland";

    //  For 'BillingState' field
    public static final String CALIFORNIA_STATE = "California";

    //  For 'RC_Account_Status__c' field
    public static final String PAID_RC_ACCOUNT_STATUS = "Paid";
    public static final String PENDING_RC_ACCOUNT_STATUS = "Pending";
    public static final String POC_RC_ACCOUNT_STATUS = "PoC";

    //  For 'RC_Brand__c' field
    public static final String RINGCENTRAL_RC_BRAND = "RingCentral";
    public static final String RINGCENTRAL_CANADA_RC_BRAND = "RingCentral Canada";

    //  For 'Service_Type__c' field
    public static final String FAX_SERVICE_TYPE = "Fax";

    //  For 'Payment_Method__c' field
    public static final String INVOICE_PAYMENT_METHOD = "Invoice";
    public static final String INVOICE_ON_BEHALF_PAYMENT_METHOD = "Invoice On Behalf";
    public static final String CREDIT_CARD_PAYMENT_METHOD = "Credit Card";

    //  For 'Account_Payment_Method__c' field
    public static final String CREDITCARD_ACCOUNT_PAYMENT_METHOD = "CreditCard";

    //  For `ELA_Account_Type__c` field
    public static final String ELA_BILLING_ACCOUNT_TYPE = "ELA Billing";
    public static final String ELA_SERVICE_ACCOUNT_TYPE = "ELA Service";

    //  For 'BusinessIdentity__c' field
    public static final String RC_US_BUSINESS_IDENTITY_NAME = "RingCentral Inc.";
    public static final String RC_CA_BUSINESS_IDENTITY_NAME = "RingCentral Canada";
    public static final String AVAYA_US_BUSINESS_IDENTITY_NAME = "Avaya Cloud Office";
    public static final String AVAYA_CA_BUSINESS_IDENTITY_NAME = "Avaya Cloud Office CA";
    public static final String AMAZON_US_BUSINESS_IDENTITY_NAME = "Amazon US";

    //  For 'Partner_Type__c' field
    public static final String BILL_ON_BEHALF_PARTNER_TYPE = "Bill-on-Behalf";
    public static final String WHOLESALE_RESELLER_PARTNER_TYPE = "Wholesale-Reseller";

    //  For 'PartnerStatus__c' field
    public static final String ACTIVE_PARTNER_STATUS = "Active";

    //  For 'Preferred_Language__c' field
    public static final String EN_US_PREFERRED_LANGUAGE = "en_US";

    //  For 'Type' field
    public static final String PARTNER_ACCOUNT_TYPE = "Partner";

    /**
     * Set internal record type for the Account object with Customer Account developer/record type's name.
     *
     * @param account Account object to set up Record type on
     * @throws ConnectionException in case of errors while accessing API
     */
    public static void setCustomerAccountRecordType(Account account) throws ConnectionException {
        var customerAccountRecordTypeId = CONNECTION_UTILS.getRecordTypeId(S_OBJECT_API_NAME, CUSTOMER_ACCOUNT_RECORD_TYPE);
        account.setRecordTypeId(customerAccountRecordTypeId);
    }

    /**
     * Set internal record type for the Account object with Partner Account developer/record type's name.
     *
     * @param account Account object to set up Record type on
     * @throws ConnectionException in case of errors while accessing API
     */
    public static void setPartnerAccountRecordType(Account account) throws ConnectionException {
        var customerAccountRecordTypeId = CONNECTION_UTILS.getRecordTypeId(S_OBJECT_API_NAME, PARTNER_ACCOUNT_RECORD_TYPE);
        account.setRecordTypeId(customerAccountRecordTypeId);
    }

    /**
     * Set up Account's billing address fields with the values
     * according to the given Billing Country.
     *
     * @param account        Account object to set up
     * @param billingCountry billing country's for the Account
     */
    public static void setBillingAddress(Account account, String billingCountry) {
        billingCountry = requireNonNullElse(billingCountry, EMPTY_STRING);

        switch (billingCountry) {
            case CA_BILLING_COUNTRY -> setCanadaBillingAddress(account);
            case FR_BILLING_COUNTRY -> setFranceBillingAddress(account);
            case DE_BILLING_COUNTRY -> setGermanyBillingAddress(account);
            case UK_BILLING_COUNTRY -> setUkBillingAddress(account);
            case AU_BILLING_COUNTRY -> setAuBillingAddress(account);
            case INDIA_BILLING_COUNTRY -> setIndiaBillingAddress(account);
            case TAIWAN_BILLING_COUNTRY -> setTaiwanBillingAddress(account);
            case SINGAPORE_BILLING_COUNTRY -> setSingaporeBillingAddress(account);
            case SWITZERLAND_BILLING_COUNTRY -> setSwitzerlandBillingAddress(account);
            default -> setUsBillingAddress(account);
        }
    }

    /**
     * Set up Account's billing address fields with some address values in the US.
     *
     * @param account Account object to set up
     */
    public static void setUsBillingAddress(Account account) {
        account.setBillingStreet(US_BILLING_STREET);
        account.setBillingCity(US_BILLING_CITY);
        account.setBillingState(US_BILLING_STATE);
        account.setBillingPostalCode(US_BILLING_POSTAL_CODE);
        account.setBillingCountry(US_BILLING_COUNTRY);
    }

    /**
     * Set up Account's billing address fields with some address values in Canada.
     *
     * @param account Account object to set up
     */
    public static void setCanadaBillingAddress(Account account) {
        account.setBillingStreet(CA_BILLING_STREET);
        account.setBillingCity(CA_BILLING_CITY);
        account.setBillingState(CA_BILLING_STATE);
        account.setBillingPostalCode(CA_BILLING_POSTAL_CODE);
        account.setBillingCountry(CA_BILLING_COUNTRY);
    }

    /**
     * Set up Account's billing address fields with some address values in France.
     *
     * @param account Account object to set up
     */
    public static void setFranceBillingAddress(Account account) {
        account.setBillingStreet(FR_BILLING_STREET);
        account.setBillingCity(FR_BILLING_CITY);
        account.setBillingState(FR_BILLING_STATE);
        account.setBillingPostalCode(FR_BILLING_POSTAL_CODE);
        account.setBillingCountry(FR_BILLING_COUNTRY);
    }

    /**
     * Set up Account's billing address fields with some address values in Germany.
     *
     * @param account Account object to set up
     */
    public static void setGermanyBillingAddress(Account account) {
        account.setBillingStreet(DE_BILLING_STREET);
        account.setBillingCity(DE_BILLING_CITY);
        account.setBillingState(DE_BILLING_STATE);
        account.setBillingPostalCode(DE_BILLING_POSTAL_CODE);
        account.setBillingCountry(DE_BILLING_COUNTRY);
    }

    /**
     * Set up Account's billing address fields with some address values in the United Kingdom.
     *
     * @param account Account object to set up
     */
    public static void setUkBillingAddress(Account account) {
        account.setBillingStreet(UK_BILLING_STREET);
        account.setBillingCity(UK_BILLING_CITY);
        account.setBillingState(UK_BILLING_STATE);
        account.setBillingPostalCode(UK_BILLING_POSTAL_CODE);
        account.setBillingCountry(UK_BILLING_COUNTRY);
    }

    /**
     * Set up Account's billing address fields with some address values in Australia.
     *
     * @param account Account object to set up
     */
    public static void setAuBillingAddress(Account account) {
        account.setBillingStreet(AU_BILLING_STREET);
        account.setBillingCity(AU_BILLING_CITY);
        account.setBillingState(AU_BILLING_STATE);
        account.setBillingPostalCode(AU_BILLING_POSTAL_CODE);
        account.setBillingCountry(AU_BILLING_COUNTRY);
    }

    /**
     * Set up Account's billing address fields with some address values in India.
     *
     * @param account Account object to set up
     */
    public static void setIndiaBillingAddress(Account account) {
        account.setBillingStreet(INDIA_BILLING_STREET);
        account.setBillingCity(INDIA_BILLING_CITY);
        account.setBillingState(INDIA_BILLING_STATE);
        account.setBillingPostalCode(INDIA_BILLING_POSTAL_CODE);
        account.setBillingCountry(INDIA_BILLING_COUNTRY);
    }

    /**
     * Set up Account's billing address fields with some address values in Taiwan.
     *
     * @param account Account object to set up
     */
    public static void setTaiwanBillingAddress(Account account) {
        account.setBillingStreet(TAIWAN_BILLING_STREET);
        account.setBillingCity(TAIWAN_BILLING_CITY);
        account.setBillingState(TAIWAN_BILLING_STATE);
        account.setBillingPostalCode(TAIWAN_BILLING_POSTAL_CODE);
        account.setBillingCountry(TAIWAN_BILLING_COUNTRY);
    }

    /**
     * Set up Account's billing address fields with some address values in Singapore.
     *
     * @param account Account object to set up
     */
    public static void setSingaporeBillingAddress(Account account) {
        account.setBillingStreet(SINGAPORE_BILLING_STREET);
        account.setBillingCity(SINGAPORE_BILLING_CITY);
        account.setBillingState(SINGAPORE_BILLING_STATE);
        account.setBillingPostalCode(SINGAPORE_BILLING_POSTAL_CODE);
        account.setBillingCountry(SINGAPORE_BILLING_COUNTRY);
    }

    /**
     * Set up Account's billing address fields with some address values in Switzerland.
     *
     * @param account Account object to set up
     */
    public static void setSwitzerlandBillingAddress(Account account) {
        account.setBillingStreet(SWITZERLAND_BILLING_STREET);
        account.setBillingCity(SWITZERLAND_BILLING_CITY);
        account.setBillingState(SWITZERLAND_BILLING_STATE);
        account.setBillingPostalCode(SWITZERLAND_BILLING_POSTAL_CODE);
        account.setBillingCountry(SWITZERLAND_BILLING_COUNTRY);
    }

    /**
     * Set up Account's fields with IDs from NGBS + 'Paid' status.
     *
     * @param account   Account object to set up
     * @param billingId billing ID of the account from NGBS
     */
    public static void setUpExistingBusinessFieldsForPaidStatus(Account account, String billingId) {
        account.setBilling_ID__c(billingId);
        setRandomEnterpriseAccountId(account);

        account.setRC_Account_Status__c(PAID_RC_ACCOUNT_STATUS);
    }

    /**
     * Set unique 'RC_User_ID__c' for provided Account object.
     *
     * @param account account to set up
     */
    public static void setRandomEnterpriseAccountId(Account account) {
        var randomValue = new Random().nextInt(99_999_999);
        var uniqueUserId = String.format("0000%08d0000", randomValue);
        account.setRC_User_ID__c(uniqueUserId);
    }

    /**
     * Extract primary Contact object from the Account object.
     *
     * @param account Account to get primary Contact from
     * @return Contact object which is primary for its Account
     */
    public static Contact getPrimaryContactOnAccount(Account account) throws Exception {
        return CONNECTION_UTILS.querySingleRecord(
                "SELECT Id, FirstName, LastName, Email, Phone, Preferred_Language__c " +
                        "FROM Contact " +
                        "WHERE Id IN (" +
                        "SELECT ContactId " +
                        "FROM AccountContactRole " +
                        "WHERE AccountId = '" + account.getId() + "' " +
                        "AND isPrimary = true" +
                        ")",
                Contact.class);
    }
}
