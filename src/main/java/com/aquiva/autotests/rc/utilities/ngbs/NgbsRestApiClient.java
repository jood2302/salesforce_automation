package com.aquiva.autotests.rc.utilities.ngbs;

import com.aquiva.autotests.rc.model.ngbs.dto.account.*;
import com.aquiva.autotests.rc.model.ngbs.dto.contracts.ContractNgbsDTO;
import com.aquiva.autotests.rc.model.ngbs.dto.discounts.*;
import com.aquiva.autotests.rc.model.ngbs.dto.license.*;
import com.aquiva.autotests.rc.model.ngbs.dto.packages.PackageNgbsDTO;
import com.aquiva.autotests.rc.model.ngbs.dto.partner.PartnerNgbsDTO;
import com.aquiva.autotests.rc.model.ngbs.dto.partner.PartnerPackageDTO;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.RestApiClient;
import io.qameta.allure.Step;

import java.util.List;

import static com.aquiva.autotests.rc.utilities.RestApiAuthentication.usingBasicAuthentication;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiHelper.*;
import static com.aquiva.autotests.rc.utilities.ngbs.ServiceLocationAddressNgbsFactory.createServiceLocationWithDefaultAddress;
import static io.qameta.allure.Allure.step;
import static java.util.Arrays.asList;

/**
 * Class for handling calls to NGBS API.
 * <br/>
 * Useful for getting the data from NGBS for entities like accounts, packages, contracts, discounts, etc...
 */
public class NgbsRestApiClient {
    private static final RestApiClient CLIENT = new RestApiClient(
            usingBasicAuthentication(NGBS_API_USERNAME, NGBS_API_PASSWORD),
            "Unable to get a response from NGBS! Details: "
    );

    /**
     * Get all the discounts on the NGBS account.
     *
     * @param billingId ID for the NGBS account (e.g. "235714001")
     * @param packageId ID for the package on the account in NGBS (e.g. "235798001")
     * @return list of all the active discounts on the NGBS account
     */
    @Step("Get discounts on the NGBS account")
    public static List<DiscountNgbsDTO> getDiscountsFromNGBS(String billingId, String packageId) {
        var url = getDiscountTemplateGroupURL(billingId, packageId);

        return CLIENT.getAsList(url, DiscountNgbsDTO.class);
    }

    /**
     * Create a new discount on the NGBS account.
     *
     * @param billingId      ID for the NGBS account (e.g. "235714001")
     * @param packageId      ID for the package on the account in NGBS (e.g. "235798001")
     * @param discountObject discount object with the data for creating a new discount
     * @return response from the NGBS mapped to the NGBS Discount object
     */
    @Step("Create a discount on the NGBS account")
    public static DiscountNgbsDTO createDiscountInNGBS(String billingId, String packageId, DiscountNgbsDTO discountObject) {
        var url = getDiscountTemplateGroupURL(billingId, packageId);
        return CLIENT.post(url, discountObject, DiscountNgbsDTO.class);
    }

    /**
     * Create a promotion discount on the account in the NGBS.
     *
     * @param billingId        ID for the NGBS account (e.g. "235714001")
     * @param packageId        ID for the package on the account in NGBS (e.g. "235798001")
     * @param promoDiscountDTO data for creating a new promotion discount on the account
     */
    @Step("Create a promo discount on the NGBS account")
    public static void createPromoDiscountInNGBS(String billingId, String packageId,
                                                 PromotionDiscountNgbsDTO promoDiscountDTO) {
        var url = getDiscountTemplateGroupPromoCodeURL(billingId, packageId);
        var jsonBody = JsonUtils.writeJsonAsString(promoDiscountDTO);

        CLIENT.post(url, jsonBody);
    }

    /**
     * Delete one of the existing discounts on the NGBS account.
     *
     * @param billingId  ID for the NGBS account (e.g. "235714001")
     * @param packageId  ID for the package on the account in NGBS (e.g. "235798001")
     * @param discountId ID for the discount template in NGBS (e.g. "4730001")
     */
    @Step("Delete a discount on the NGBS account")
    public static void deleteDiscountFromNGBS(String billingId, String packageId, String discountId) {
        var url = getDiscountTemplateGroupURL(billingId, packageId) + "/" + discountId;
        CLIENT.delete(url);
    }

    /**
     * Search promotions in NGBS by promo code.
     *
     * @param promoCode promo code used to find a promotion
     *                  (e.g. "QA-AUTO-POLYCOM-PHONE-USD")
     * @return collection of all Promotions with the matching promo code
     */
    @Step("Search promotions in NGBS by promo code")
    public static PromotionNgbsDTO[] searchPromotionsByPromoCodeInNGBS(String promoCode) {
        var url = getPromotionsSearchURL();
        var jsonBody = getPromoSearchByPromoCodeJsonBody(promoCode);
        var response = CLIENT.post(url, jsonBody);
        return JsonUtils.readJson(response, PromotionNgbsDTO[].class);
    }

    /**
     * Get promotion's details from NGBS by its ID.
     *
     * @param promotionId promotion Id used to get promotion's details
     *                    (e.g. 18866001)
     * @return promotion object with all the available details on it from NGBS
     */
    @Step("Get promotion's details from NGBS by ID")
    public static PromotionNgbsDTO getPromotionDetailsFromNGBS(String promotionId) {
        var url = getPromotionsURL() + "/" + promotionId;
        return CLIENT.get(url, PromotionNgbsDTO.class);
    }

    /**
     * Create a new Promotion in NGBS.
     * <br/>
     * Note: the promotion has Status='Actual' when created via this method.
     *
     * @param promoObject promotion object with the data for creating a new promo code
     * @return details of a new promotion object
     */
    @Step("Create a promotion in NGBS")
    public static PromotionNgbsDTO createPromotionInNGBS(PromotionNgbsDTO promoObject) {
        var url = getPromotionsURL();
        var createdPromotionResponse = CLIENT.post(url, promoObject, PromotionNgbsDTO.class);

        //  promo is created with Status='Draft' and can only be changed to Status='Actual' with additional request
        var urlStatusActual = getPromotionsStatusActualURL(createdPromotionResponse.id);
        CLIENT.post(urlStatusActual);

        return createdPromotionResponse;
    }

    /**
     * Get payment method type on the NGBS account.
     *
     * @param billingId ID for the NGBS account (e.g. "235714001").
     * @return payment method type object with the current payment method type
     * on the NGBS account (e.g. "CreditCard", "Invoice") + all the former types as well
     */
    public static PaymentMethodTypeDTO getPaymentMethodTypeFromNGBS(String billingId) {
        var url = getAccountPaymentMethodTypeURL(billingId);

        return CLIENT.get(url, PaymentMethodTypeDTO.class);
    }

    /**
     * Get payment methods list on account from the NGBS.
     *
     * @param billingId ID for the NGBS account (e.g. "235714001").
     * @return list of payment methods mapped to the NGBS Payment method objects
     */
    public static List<PaymentMethodDTO> getPaymentMethodsFromNGBS(String billingId) {
        var url = getAccountPaymentMethodURL(billingId);

        return CLIENT.getAsList(url, PaymentMethodDTO.class);
    }

    /**
     * Get intended payment method on the account from the NGBS.
     * <br/>
     * Note: use this method to get a payment method when an account is in "Initial" status,
     * and there's no active payment method yet!
     *
     * @param billingId ID for the NGBS account (e.g. "235714001").
     * @return intended payment method name on the NGBS account (e.g. "CreditCard")
     */
    public static String getIntendedPaymentMethodFromNGBS(String billingId) {
        var url = getAccountDynamicFieldsURL(billingId);
        var response = CLIENT.get(url, AccountFieldsDTO.class);
        return response.intendedPaymentMethod;
    }

    /**
     * Update a Payment Method on the NGBS account.
     * This is an indirect way to update a Billing Address on the account.
     *
     * @param billingId         ID for the NGBS account (e.g. "235714001")
     * @param paymentMethodInfo Any payment info data mapped to the NGBS PaymentMethod object
     * @return the resulting updated payment method (with the new id)
     */
    @Step("Update a Payment Method's Info on the NGBS account")
    public static PaymentMethodDTO updatePaymentMethodInNGBS(String billingId, PaymentMethodDTO paymentMethodInfo) {
        var url = getAccountPaymentMethodURL(billingId) + "/" + paymentMethodInfo.id;
        //  "default" and "id" should be null for update operations
        paymentMethodInfo.id = null;
        paymentMethodInfo.defaultVal = null;

        return CLIENT.post(url, paymentMethodInfo, PaymentMethodDTO.class);
    }

    /**
     * Get the list of all the contracts on the NGBS account (active, terminated, etc...).
     *
     * @param billingId ID for the NGBS account (e.g. "235714001")
     * @param packageId ID for the package on the account in NGBS (e.g. "235798001")
     * @return list of the contracts mapped to the NGBS Contract objects
     */
    @Step("Get all the contracts on the NGBS account")
    public static List<ContractNgbsDTO> getContractsInNGBS(String billingId, String packageId) {
        var url = getContractsURL(billingId, packageId);
        return CLIENT.getAsList(url, ContractNgbsDTO.class);
    }

    /**
     * Create a new contract on the NGBS account.
     *
     * @param billingId      ID for the NGBS account (e.g. "235714001")
     * @param packageId      ID for the package on the account in NGBS (e.g. "235798001")
     * @param contractObject contract data mapped to the NGBS Contract object
     * @return response from the NGBS mapped to the NGBS Contract object
     */
    public static ContractNgbsDTO createContractInNGBS(String billingId, String packageId,
                                                       ContractNgbsDTO contractObject) {
        return updateContractInNGBS(billingId, packageId, EMPTY_STRING, contractObject);
    }

    /**
     * Update an existing contract on the NGBS account.
     *
     * @param billingId      ID for the NGBS account (e.g. "235714001")
     * @param packageId      ID for the package on the account in NGBS (e.g. "235798001")
     * @param contractId     ID for the contract in NGBS (e.g. "488001")
     * @param contractObject contract data mapped to the NGBS Contract object
     * @return response from the NGBS mapped to the NGBS Contract object
     */
    public static ContractNgbsDTO updateContractInNGBS(String billingId, String packageId, String contractId,
                                                       ContractNgbsDTO contractObject) {
        var isCreate = contractId.isBlank();
        var stepAction = isCreate ? "Create" : "Update";
        var url = isCreate ?
                getContractsURL(billingId, packageId) :
                getContractsURL(billingId, packageId) + "/" + contractId;

        return step(stepAction + " a contract on the NGBS account", () ->
                CLIENT.put(url, contractObject, ContractNgbsDTO.class)
        );
    }

    /**
     * Terminate an existing contract on the NGBS account.
     * <p></p>
     * Note: the contract won't be deleted from the account.
     * It just changes its status from "ACTIVE" to "TERMINATED".
     *
     * @param billingId  ID for the NGBS account (e.g. "235714001")
     * @param packageId  ID for the package on the account in NGBS (e.g. "235798001")
     * @param contractId ID for the contract in NGBS (e.g. "488001")
     */
    @Step("Terminate a contract on the NGBS account")
    public static void terminateContractInNGBS(String billingId, String packageId, String contractId) {
        var url = getContractsURL(billingId, packageId) + "/" + contractId;
        CLIENT.delete(url);
    }

    /**
     * Get the information about the package.
     *
     * @param packageId      ID of the package in NGBS
     *                       (e.g. "18" for RingCentral MVP Standard US).
     *                       Not to be mistaken for the long package ID on the specific NGBS account!
     * @param packageVersion version of the package in NGBS
     *                       (e.g. "3" as default for RingCentral MVP Standard US package).
     * @return response from the NGBS mapped to the NGBS Package object
     */
    @Step("Get a package information from NGBS")
    public static PackageNgbsDTO getPackageFullInfo(String packageId, String packageVersion) {
        var url = getPackageFullInfoURL(packageId, packageVersion);
        return CLIENT.get(url, PackageNgbsDTO.class);
    }

    /**
     * Add a new package to the NGBS account.
     *
     * @param billingId          ID for the NGBS account (e.g. "235714001")
     * @param packageTemplateDTO package data mapped to the NGBS Package Template object
     * @return response from the NGBS mapped to the NGBS Package object
     */
    public static PackageSummary addPackageToAccount(String billingId, PackageTemplateDTO packageTemplateDTO) {
        var url = getAccountPackageURL(billingId);

        //  additional header is needed for this request
        CLIENT.addHeaders(List.of(getExternalSystemIdHeader()));

        return CLIENT.post(url, packageTemplateDTO, PackageSummary.class);
    }

    /**
     * Activate the Account's Package in the NGBS
     * (by setting its Status to 'Active').
     *
     * @param billingId ID for the NGBS account (e.g. "235714001")
     * @param packageId ID for the package on the account in NGBS (e.g. "235798001")
     */
    @Step("Activate the Account's Package in the NGBS")
    public static void activatePackageOnAccountInNGBS(String billingId, String packageId) {
        var url = getAccountPackageActivationURL(billingId, packageId);
        CLIENT.post(url);
    }

    /**
     * Get the Account information from NGBS.
     *
     * @param billingId ID for the NGBS account (e.g. "235714001")
     * @return response from the NGBS mapped to the NGBS Account object
     */
    @Step("Get the Account information from NGBS")
    public static AccountNgbsDTO getAccountInNGBS(String billingId) {
        var url = getAccountURL(billingId);
        var accountData = CLIENT.get(url);
        return JsonUtils.readJson(accountData, AccountNgbsDTO.class);
    }

    /**
     * Get a Free Service Credit on the NGBS account.
     *
     * @param billingId ID for the NGBS account (e.g. "235714001")
     * @return response from the NGBS mapped to the NGBS FSC object
     */
    @Step("Get a Free Service Credit on the NGBS account")
    public static AccountNgbsDTO.FreeServiceCreditDTO getFreeServiceCredit(String billingId) {
        var url = getAccountURL(billingId);
        var responseAccount = CLIENT.get(url, AccountNgbsDTO.class);
        return responseAccount.freeServiceCredit;
    }

    /**
     * Update a Free Service Credit on the NGBS account.
     *
     * @param billingId     ID for the NGBS account (e.g. "235714001")
     * @param serviceCredit Free Service Credit data mapped to the NGBS FSC object
     */
    @Step("Update a Free Service Credit on the NGBS account")
    public static void updateFreeServiceCredit(String billingId, FreeServiceCreditUpdateDTO serviceCredit) {
        var url = getFreeServiceCreditUpdateURL(billingId);
        CLIENT.put(url, serviceCredit);
    }

    /**
     * Update a Contact Info on the NGBS account
     * (e.g. first name, last name, email, etc.).
     *
     * @param billingId   ID for the NGBS account (e.g. "235714001")
     * @param contactInfo Contact Info data mapped to the NGBS ContactInfo object
     */
    @Step("Update a Contact Info on the NGBS account")
    public static void updateContactInfo(String billingId, ContactInfoUpdateDTO contactInfo) {
        var url = getContactInfoURL(billingId);
        CLIENT.put(url, contactInfo);
    }

    /**
     * Search for Accounts in NGBS using the last name of its Contact (from SFDC).
     *
     * @param contactLastName last name for the account's contact (e.g. "Newton")
     * @return list of found account objects with useful information (first/last name, company name, status, etc...).
     */
    @Step("Search Accounts in NGBS using their Contact's Last Name")
    public static List<AccountSummary> searchAccountsByContactLastNameInNGBS(String contactLastName) {
        var searchRequest = AccountSearchRequestDTO.byLastName(contactLastName);
        return searchAccountsInNGBS(searchRequest);
    }

    /**
     * Search for Accounts in NGBS using a set of the search parameters.
     *
     * @param accountSearchRequest search request data to find accounts using the given criteria
     *                             (e.g. Billing ID; Last Name; Status...)
     * @return list of found account objects with useful information (first/last name, company name, status, etc...).
     */
    @Step("Search Accounts in NGBS using a set of search parameters")
    public static List<AccountSummary> searchAccountsInNGBS(AccountSearchRequestDTO accountSearchRequest) {
        var url = getSearchAccountURL();
        var response = CLIENT.post(url, accountSearchRequest, AccountSearchResponseDTO.class);

        return asList(response.result);
    }

    /**
     * Get information about licenses on account.
     *
     * @param billingID ID for the NGBS account (e.g. "235714001")
     * @param packageID ID for the package on the account in NGBS (e.g. "235798001")
     * @return response from the NGBS contains information about licenses on account
     */
    @Step("Get the licenses from the billing info summary on the NGBS account")
    public static BillingInfoLicenseDTO[] getBillingInfoSummaryLicenses(String billingID, String packageID) {
        var url = getBillingInfoSummaryLicensesURL(billingID, packageID);
        var licenses = CLIENT.get(url);
        return JsonUtils.readJson(licenses, BillingInfoLicenseDTO[].class);
    }

    /**
     * Purchase licenses (mid-cycle) for the NGBS account's package
     * (i.e. add more license "to the shelf").
     *
     * @param billingID       ID for the NGBS account (e.g. "235714001")
     * @param packageID       ID for the package on the account in NGBS (e.g. "235798001")
     * @param orderRequestDTO order request data that contains info about the licenses to purchase
     */
    @Step("Purchase licenses (mid-cycle) for the NGBS account")
    public static void purchaseLicensesInNGBS(String billingID, String packageID, OrderRequestDTO orderRequestDTO) {
        var url = getLicensesPurchaseURL(billingID, packageID);
        var requestBodyAsString = JsonUtils.writeJsonAsString(orderRequestDTO);
        CLIENT.post(url, requestBodyAsString);
    }

    /**
     * Remove licenses (mid-cycle) from the NGBS account's package
     * (i.e. "downsell").
     *
     * @param billingID       ID for the NGBS account (e.g. "235714001")
     * @param packageID       ID for the package on the account in NGBS (e.g. "235798001")
     * @param orderRequestDTO order request data that contains info about the licenses to remove
     */
    @Step("Remove licenses (mid-cycle) from the NGBS account")
    public static void removeLicensesInNGBS(String billingID, String packageID, OrderRequestDTO orderRequestDTO) {
        var url = getLicensesPurchaseURL(billingID, packageID);
        var requestBodyAsString = JsonUtils.writeJsonAsString(orderRequestDTO);
        CLIENT.post(url, requestBodyAsString);
    }

    /**
     * Trigger recurring billing run for the specified package on the NGBS account.
     *
     * @param billingID ID for the NGBS account (e.g. "235714001")
     * @param packageID ID for the package on the account in NGBS (e.g. "235798001")
     */
    @Step("Run recurring billing for the specified package on the NGBS account")
    public static void runRecurringBillingInNGBS(String billingID, String packageID) {
        var url = getRecurringBillingURL(billingID, packageID);
        CLIENT.post(url);
    }

    /**
     * Create a new service location address on the NGBS Account.
     *
     * @param billingId ID for the NGBS account (e.g. "235714001")
     * @return response from the NGBS mapped to the NGBS  object
     */
    public static ServiceLocationAddressDTO addBillingAddressInNGBS(String billingId) {
        var url = getBillingCodesAddressURL(billingId);

        var serviceLocationAddress = createServiceLocationWithDefaultAddress();

        return CLIENT.post(url, serviceLocationAddress, ServiceLocationAddressDTO.class);
    }

    /**
     * Create a new partner in the NGBS catalog.
     *
     * @param partnerObject partner data mapped to the NGBS Partner object
     * @return response from the NGBS mapped to the NGBS Partner object
     */
    public static PartnerNgbsDTO createPartnerInNGBS(PartnerNgbsDTO partnerObject) {
        var url = getPartnersURL();
        return CLIENT.post(url, partnerObject, PartnerNgbsDTO.class);
    }

    /**
     * Get all partners from the NGBS catalog.
     *
     * @return response from the NGBS mapped to the list of NGBS Partner objects
     */
    public static List<PartnerNgbsDTO> getPartnersInNGBS() {
        var url = getPartnersURL();
        return CLIENT.getAsList(url, PartnerNgbsDTO.class);
    }

    /**
     * Create a new package for the given partner in the NGBS catalog.
     *
     * @param partnerId            partner's ID to create a package for
     * @param partnerPackageObject partner's package data mapped to the NGBS Partner object
     */
    public static void createPartnerPackageInNGBS(Long partnerId, PartnerPackageDTO partnerPackageObject) {
        var url = getPartnersPackagesURL(partnerId);
        var jsonBody = JsonUtils.writeJsonAsString(partnerPackageObject);
        CLIENT.post(url, jsonBody);
    }

    /**
     * Activate the Account in the NGBS
     * (by setting its main Status to 'Active').
     *
     * @param billingId ID for the NGBS account (e.g. "235714001")
     */
    @Step("Activate the Account in the NGBS")
    public static void activateAccountInNGBS(String billingId) {
        var url = getAccountActivationURL(billingId);
        CLIENT.post(url);
    }

    /**
     * Terminate the Account in the NGBS
     * (by setting its main Status to 'Deleted').
     *
     * @param billingId ID for the NGBS account (e.g. "235714001")
     */
    @Step("Terminate the Account in the NGBS")
    public static void terminateAccountInNGBS(String billingId) {
        var url = getAccountTerminationURL(billingId);
        CLIENT.post(url);
    }

    /**
     * Get all licenses from NGBS.
     *
     * @return list of all licenses mapped to the NGBS License objects
     */
    @Step("Get all licenses from NGBS")
    public static List<PackageNgbsDTO.License> allLicensesFromNgbs() {
        var url = getAllLicensesURL();
        return CLIENT.getAsList(url, PackageNgbsDTO.License.class);
    }
}