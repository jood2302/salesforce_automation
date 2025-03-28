package com.aquiva.autotests.rc.utilities.ngbs;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_ENV_NAME;
import static java.lang.String.format;

/**
 * Helper class for {@link NgbsRestApiClient} class to store and form useful data.
 */
public class NgbsRestApiHelper {

    /**
     * Data object with values for connecting to NGBS API, accessing various REST services, etc...
     */
    private static final NgbsRestApiSettings NGBS_REST_API_SETTINGS = NgbsRestApiSettings.getInstance();

    /**
     * Username to access and send requests to NGBS via REST API.
     */
    public static final String NGBS_API_USERNAME = NGBS_REST_API_SETTINGS.getUsername();

    /**
     * Password to access and send requests to NGBS via REST API.
     */
    public static final String NGBS_API_PASSWORD = NGBS_REST_API_SETTINGS.getPassword();

    //  NGBS endpoints for different environments
    public static final String NGBS_REST_ENDPOINT_GCI = "http://185.23.251.124:8080";
    public static final String NGBS_REST_ENDPOINT_BISUAT = "http://199.68.214.251:8080";
    public static final String NGBS_REST_ENDPOINT_DEV = "http://185.23.251.118:8080";
    public static final String NGBS_REST_ENDPOINT_PATCH = "http://biq01-t01-igl01.ngbs-biqasv7.svc.c01.k01.k8s.sv701.lab.nordigy.ru:8080";

    //  Payment method
    private static final String PAYMENT_METHOD = "/account/%s/paymentMethod";
    private static final String PAYMENT_METHOD_TYPE = PAYMENT_METHOD + "/type";
    private static final String FIELDS = "/account/%s/fields";

    //  Packages
    private static final String PACKAGE_FULL_INFO = "/packages/%s/%s";

    //  Contracts
    private static final String CONTRACTS = "/account/%s/package/%s/contract";

    //  Discounts
    private static final String DISCOUNT_TEMPLATE_GROUP = "/account/%s/package/%s/discountTemplateGroup";
    private static final String DISCOUNT_TEMPLATE_GROUP_PROMO_CODE = DISCOUNT_TEMPLATE_GROUP + "/promoCode";

    //  Promotions
    private static final String DISCOUNT_GROUPS = "/discountGroups";
    private static final String DISCOUNT_GROUPS_SEARCH = DISCOUNT_GROUPS + "/search";
    private static final String DISCOUNT_GROUPS_STATUS_ACTUAL = DISCOUNT_GROUPS + "/%s/status?status=Actual";

    //  Accounts
    private static final String UPDATE_FREE_SERVICE_CREDIT = "/account/%s/freeServiceCredit";
    private static final String ACCOUNT_CONTACT_INFO = "/account/%s/contactInfo";
    private static final String GET_ACCOUNT = "/account/%s";
    private static final String UPDATE_ACCOUNT_NEW_STATUS = "/account/%s/status?newStatus=%s";

    //  Search Account
    private static final String SEARCH_ACCOUNT = "/search/account";

    //  Licenses
    private static final String LICENSES_SUMMARY = "/account/%s/package/%s/billinginfo/summary/license";

    //  Purchase
    private static final String LICENSES_PURCHASE = "/account/%s/purchase/%s/order/v2";
    private static final String RECURRING_BILLING = "/account/%s/purchase/%s/recurringBilling";

    //  Partners
    private static final String PARTNERS = "/partners";
    private static final String PARTNERS_PACKAGES = "/partners/%s/packages";

    //  Account's packages
    private static final String ACCOUNT_PACKAGE = "/account/%s/package";
    private static final String UPDATE_ACCOUNT_PACKAGE_NEW_STATUS = "/account/%s/package/%s/status?newStatus=%s";

    //  Account's service location address
    private static final String BILLING_CODES_ADDRESS = "/account/%s/billingCodes/address";

    private static final String LICENSES = "/licenses";

    private static final String PROMO_SEARCH_PROMO_CODE_PAYLOAD = "{\"promoCode\": \"%s\"}";

    /**
     * Get the header value for the 'X-ExternalSystemId' header.
     *
     * @return header name for the external system ID (e.g. "armbiams")
     */
    public static Header getExternalSystemIdHeader() {
        var value = "armbiams"; //  default value

        if (BASE_ENV_NAME != null && !BASE_ENV_NAME.isBlank() && BASE_ENV_NAME.equals("BIS-UAT-SV7")) {
            value = "bisuatsv7";
        }

        return new BasicHeader("X-ExternalSystemId", value);
    }

    /**
     * Return string URL for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/{accountId}/package/{packageId}/discountTemplateGroup</i>
     *
     * @param billingId Account ID for account from Billing
     * @param packageId Package ID for account from Billing
     * @return string representation for URL to get discounts on account
     */
    public static String getDiscountTemplateGroupURL(String billingId, String packageId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(DISCOUNT_TEMPLATE_GROUP, billingId, packageId);
    }

    /**
     * Return string URL for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/{billingId}/package/{packageId}/discountTemplateGroup/promoCode</i>
     *
     * @param billingId ID for the NGBS account (e.g. "235714001")
     * @param packageId ID for the package on the account in NGBS (e.g. "235798001")
     * @return string representation for URL to add promotion to the NGBS account
     */
    public static String getDiscountTemplateGroupPromoCodeURL(String billingId, String packageId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(DISCOUNT_TEMPLATE_GROUP_PROMO_CODE, billingId, packageId);
    }

    /**
     * Return string URL for request to
     * <i>{ngbs.api.endpoint}/restapi/catalog/v1.0/discountGroups</i>
     *
     * @return string representation for URL to get/create/edit/delete promotions
     */
    public static String getPromotionsURL() {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getCatalogPath() +
                DISCOUNT_GROUPS;
    }

    /**
     * Return string URL for request to
     * <i>{ngbs.api.endpoint}/restapi/catalog/v1.0/discountGroups/search</i>
     *
     * @return string representation for URL to search promotions (by promo code)
     */
    public static String getPromotionsSearchURL() {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getCatalogPath() +
                DISCOUNT_GROUPS_SEARCH;
    }

    /**
     * Return string URL for request to
     * <i>{ngbs.api.endpoint}/restapi/catalog/v1.0/discountGroups/{promotionId}/status?status=Actual</i>
     *
     * @param promotionId promotion Id used to get promotion's details (e.g. 18866001)
     * @return string representation for URL to change promotion's status to 'Actual'
     */
    public static String getPromotionsStatusActualURL(String promotionId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getCatalogPath() +
                format(DISCOUNT_GROUPS_STATUS_ACTUAL, promotionId);
    }

    /**
     * Return string URL for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/{accountId}/paymentMethod/type</i>
     *
     * @param billingId Account ID for account from Billing.
     * @return string representation for URL to get payment method type on account.
     */
    public static String getAccountPaymentMethodTypeURL(String billingId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(PAYMENT_METHOD_TYPE, billingId);
    }

    /**
     * Return string URL for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/{accountId}/paymentMethod</i>
     *
     * @param billingId Account ID for account from Billing.
     * @return string representation for URL to get payment method on account.
     */
    public static String getAccountPaymentMethodURL(String billingId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(PAYMENT_METHOD, billingId);
    }

    /**
     * Return string URL for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/{accountId}/fields</i>
     *
     * @param billingId Account ID for account from Billing.
     * @return string representation for URL to get dynamic field values on the account
     */
    public static String getAccountDynamicFieldsURL(String billingId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(FIELDS, billingId);
    }

    /**
     * Return string URL for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/{accountId}/package/{packageId}/contract</i>
     *
     * @param billingId Account ID for account from Billing
     * @param packageId Package ID for account from Billing
     * @return string representation for URL to get contracts on account
     */
    public static String getContractsURL(String billingId, String packageId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(CONTRACTS, billingId, packageId);
    }

    /**
     * Return string URL for request to
     * <i>{ngbs.api.endpoint}/restapi/catalog/v1.0/packages/%s/1</i>
     * <p>
     * <b>Not to be confused with Package ID for account from Billing!</b>
     * </p>
     *
     * @param packageId      Package ID from billing catalog (e.g.
     *                       <p><b>packageId="1231005"</b> for "RingEX Core™" for "RingCentral" brand (US),</p>
     *                       <p><b>packageId="6"</b> for "RingCentral Meetings Free" for "RingCentral" brand (US),</p>
     *                       <p><b>packageId="84"</b> for "RingCentral MVP Standard" for "RingCentral UK" brand (UK))</p>
     * @param packageVersion Package Version from billing catalog (e.g. "1", "2", "3")
     * @return string representation for URL to get package summary
     */
    public static String getPackageFullInfoURL(String packageId, String packageVersion) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getCatalogPath() +
                format(PACKAGE_FULL_INFO, packageId, packageVersion);
    }

    /**
     * Return string URL for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/%s/package</i>
     *
     * @param billingId Account ID for account from Billing
     * @return string representation for URL to create a new Account's package
     */
    public static String getAccountPackageURL(String billingId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(ACCOUNT_PACKAGE, billingId);
    }

    /**
     * Return string URL for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/%s/package/%s/status?newStatus=Active</i>
     *
     * @param billingId Account ID for account from Billing
     * @param packageId Package ID for account from Billing
     * @return string representation for URL to activate Account's package
     */
    public static String getAccountPackageActivationURL(String billingId, String packageId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(UPDATE_ACCOUNT_PACKAGE_NEW_STATUS, billingId, packageId, "Active");
    }

    /**
     * Return string url for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/%s/freeServiceCredit</i>
     *
     * @param billingId Account ID for account from Billing
     * @return string representation for URL to update Free Service Credit
     */
    public static String getFreeServiceCreditUpdateURL(String billingId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(UPDATE_FREE_SERVICE_CREDIT, billingId);
    }

    /**
     * Return string url for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/%s/contactInfo</i>
     *
     * @param billingId Account ID for account from Billing
     * @return string representation for URL to get/update Contact Info
     */
    public static String getContactInfoURL(String billingId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(ACCOUNT_CONTACT_INFO, billingId);
    }

    /**
     * Return string url for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/%s</i>
     *
     * @param billingId Account ID for account from Billing
     * @return string representation for URL to get Account Info
     */
    public static String getAccountURL(String billingId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(GET_ACCOUNT, billingId);
    }

    /**
     * Return string url for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/%s/status?newStatus=Active</i>
     *
     * @param billingId Account ID for account from Billing
     * @return string representation for URL to terminate Account in NGBS (set Account status 'Active')
     */
    public static String getAccountActivationURL(String billingId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(UPDATE_ACCOUNT_NEW_STATUS, billingId, "Active");
    }

    /**
     * Return string url for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/%s/status?newStatus=Deleted</i>
     *
     * @param billingId Account ID for account from Billing
     * @return string representation for URL to terminate Account in NGBS (set Account status 'Deleted')
     */
    public static String getAccountTerminationURL(String billingId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(UPDATE_ACCOUNT_NEW_STATUS, billingId, "Deleted");
    }

    /**
     * Return string url for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/search/account</i>
     *
     * @return string representation for URL to search account
     */
    public static String getSearchAccountURL() {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() + SEARCH_ACCOUNT;
    }

    /**
     * Return string url for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/%s/package/%s/billinginfo/summary/license</i>
     *
     * @param billingId Account ID for account from Billing
     * @param packageId Package ID for account from Billing
     * @return string representation for URL to get summary about license(s)
     */
    public static String getBillingInfoSummaryLicensesURL(String billingId, String packageId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(LICENSES_SUMMARY, billingId, packageId);
    }

    /**
     * Return string url for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/%s/purchase/%s/order/v2</i>
     *
     * @param billingId Account ID for account from Billing
     * @param packageId Package ID for account from Billing
     * @return string representation for URL to purchase licenses for the account (mid-cycle)
     */
    public static String getLicensesPurchaseURL(String billingId, String packageId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(LICENSES_PURCHASE, billingId, packageId);
    }

    /**
     * Return string url for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/%s/purchase/%s/recurringBilling
     *
     * @param billingId Account ID for account from Billing
     * @param packageId Package ID for account from Billing
     * @return string representation for URL to run recurring billing request
     */
    public static String getRecurringBillingURL(String billingId, String packageId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(RECURRING_BILLING, billingId, packageId);
    }

    /**
     * Return string URL for request to
     * <i>{ngbs.api.endpoint}/restapi/account-manager/v1.0/account/{billingId}/billingCodes/address</i>
     *
     * @param billingId Account ID for account from Billing
     * @return string representation for URL to get/post service location address
     */
    public static String getBillingCodesAddressURL(String billingId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getAccountManagerPath() +
                format(BILLING_CODES_ADDRESS, billingId);
    }

    /**
     * Return string url for request to
     * <i>{ngbs.api.endpoint}/restapi/catalog/v1.0/partners</i>
     */
    public static String getPartnersURL() {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getCatalogPath() + PARTNERS;
    }

    /**
     * Return string url for request to
     * <i>{ngbs.api.endpoint}/restapi/catalog/v1.0/partners/{partnerId}/packages</i>
     *
     * @param partnerId unique partner's ID (e.g. 9070005)
     */
    public static String getPartnersPackagesURL(Long partnerId) {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getCatalogPath() +
                format(PARTNERS_PACKAGES, partnerId);
    }

    /**
     * Get body/payload for the "Search promotions in NGBS by Promo Code" request.
     *
     * @param promoCode last name of the Account's primary contact
     * @return string representation for JSON body for 'search' request to NGBS API
     */
    public static String getPromoSearchByPromoCodeJsonBody(String promoCode) {
        return format(PROMO_SEARCH_PROMO_CODE_PAYLOAD, promoCode);
    }

    /**
     * Return string URL for request to
     * <i>{ngbs.api.endpoint}/restapi/catalog/v1.0/licenses</i>
     *
     * @return string representation for URL to get all licenses
     */
    public static String getAllLicensesURL() {
        return NGBS_REST_API_SETTINGS.getEndpoint() + NGBS_REST_API_SETTINGS.getCatalogPath()
                + LICENSES;
    }
}
