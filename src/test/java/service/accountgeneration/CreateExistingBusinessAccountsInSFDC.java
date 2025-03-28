package service.accountgeneration;

import base.BaseTest;
import com.aquiva.autotests.rc.model.accountgeneration.*;
import com.aquiva.autotests.rc.model.ngbs.dto.account.ContactInfoUpdateDTO;
import com.aquiva.autotests.rc.model.ngbs.dto.packages.PackageNgbsDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Step;
import ngbs.quotingwizard.CartTabSteps;
import ngbs.quotingwizard.QuoteWizardSteps;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.*;
import java.util.Calendar;

import static base.Pages.*;
import static com.aquiva.autotests.rc.internal.reporting.ServiceTaskLogger.*;
import static com.aquiva.autotests.rc.model.accountgeneration.CreateMultiproductDataInSfdcDTO.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.Constants.BASE_URL;
import static com.aquiva.autotests.rc.utilities.Constants.USER;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomPositiveInteger;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.*;
import static com.aquiva.autotests.rc.utilities.salesforce.SalesforceRestApiClient.getCountryCodeFromSettingsService;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createExistingCustomerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory.createOpportunity;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.ACCOUNTS_PAYABLE_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.CaseHelper.INCONTACT_COMPLETED_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.CaseHelper.INCONTACT_ORDER_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_STAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.setRequiredFieldsForOpportunityStageChange;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.*;
import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.sleep;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;
import static java.util.TimeZone.getTimeZone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Special task for creating Existing Business accounts in SFDC.
 * <br/><br/>
 * This task creates:
 * <p> - corresponding account in NGBS, with tester flags removed, with discounts/contract (if the data provided) </p>
 * <p> - Existing Business Customer Account in SFDC, with default or custom values (if the data provided) </p>
 * <p> - related SFDC objects like Contact, Account Contact Role, etc... </p>
 * <p> - Multiproduct Opportunity, if "multiproductData" is provided. </p>
 * <br/>
 * Note: for 'sf.createExistingBusinessAccountsData' list of SFDC EB Accounts Data should be in a JSON format:
 * <pre><code class='json'>
 * [
 *   {
 *     "accountName": "Existing_Business_Account_1365",
 *     "chargeTerm": "Monthly",
 *     "billingAddress": {
 *       "country": "United States",
 *       "state": "NY",
 *       "city": "New York",
 *       "street": "1 Wall St",
 *       "postalCode": "10005"
 *     },
 *     "contact": {
 *       "firstName": "Contact_First_Name_1365",
 *       "lastName": "Contact_Last_Name_1365",
 *       "email": "test.mail_1365@ringcentral.com",
 *       "phone": "+37544678904321"
 *     },
 *     "ngbsAccountData": {
 *       "scenario": "ngbs(brand=1210,package=2088005v1,dlCount=30,numberType=TollFree)",
 *       "contract": {
 *         "contractExtId": "Office_5_US",
 *         "contractProduct": {
 *           "dataName": "LC_DL-UNL_50",
 *           "quantity": 30
 *         }
 *       },
 *       "discounts": [
 *         {
 *           "dataName": "LC_HD_139",
 *           "chargeTerm": "One - Time",
 *           "discount": 25,
 *           "discountType": "%"
 *         }
 *       ],
 *       "licensesToOrder": [
 *         {
 *           "catalogId": "LC_DLI_282",
 *           "comment": "Global DigitalLine parent license; set qty to 1-50000 for Global LATAM here",
 *           "billingCycleDuration": "Monthly",
 *           "qty": 25,
 *           "subItems": [
 *             {
 *               "catalogId": "LC_IBO_290",
 *               "comment": "Global LATAM license to be ordered",
 *               "billingCycleDuration": "Monthly",
 *               "qty": 1,
 *               "subItems": [
 *                 {
 *                   "catalogId": "LC_IVN_291",
 *                   "billingCycleDuration": "Monthly",
 *                   "qty": 1,
 *                   "subItems": []
 *                 }
 *               ]
 *             }
 *           ]
 *         }
 *       ]
 *     },
 *     "multiproductData": [
 *       {
 *         "opportunityData": {
 *           "testPackage": {
 *             "id": "1270005",
 *             "version": "5"
 *           },
 *           "products": [
 *             {
 *               "dataName": "SA_CRS30_24",
 *               "quantity": 3
 *             }
 *           ]
 *         }
 *       },
 *       {
 *         "opportunityData": {
 *           "testPackage": {
 *             "id": "591",
 *             "version": "3"
 *           },
 *           "products": [
 *             {
 *               "dataName": "SA_LINECRWHATSUP_11",
 *               "quantity": 1,
 *               "discount": 10,
 *               "discountType": "%"
 *             }
 *           ]
 *         }
 *       },
 *       {
 *         "opportunityData": {
 *           "testPackage": {
 *             "id": "801",
 *             "version": "3"
 *           }
 *         }
 *       }
 *     ]
 *   },
 *   {
 *     "accountName": "Test_Account_without_Engage",
 *     "ngbsAccountData": {
 *       "scenario": "ngbs(brand=6010,package=350v2)"
 *     }
 *   }
 * ]
 * </code></pre>
 *
 * @see CreateExistingBusinessAccountsInSfdcDTO
 */
public class CreateExistingBusinessAccountsInSFDC extends BaseTest {
    private final AccountGenerationSteps accountGenerationSteps;
    private final CartTabSteps cartTabSteps;
    private final QuoteWizardSteps quoteWizardSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User runningUser;

    private Account sfdcAccount;
    private Contact contact;
    private Opportunity opportunity;

    private PackageNgbsDTO mainPackageInfo;

    //  RC Contact Center test data
    private final String rcMainNumberValue;
    private final String usGeoRegionOption;
    private final String ringCentralTeam;
    private final String inContactSegmentOption;
    private final String inContactBuId;

    private final int timeoutInSecondsForEngageServices;
    private final int timeoutInSecondsForRcCcService;

    public CreateExistingBusinessAccountsInSFDC() {
        accountGenerationSteps = new AccountGenerationSteps();
        cartTabSteps = new CartTabSteps(new Dataset());
        quoteWizardSteps = new QuoteWizardSteps(new Dataset());
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        rcMainNumberValue = getRandomPositiveInteger();
        usGeoRegionOption = "US";
        ringCentralTeam = "RingCentral";
        inContactSegmentOption = "1-50 Seats";
        inContactBuId = getRandomPositiveInteger();

        timeoutInSecondsForEngageServices = 60;
        timeoutInSecondsForRcCcService = 300;
    }

    @BeforeEach
    public void setUpTest() {
        step("Obtain a current User's Id for the Account Generation flow via API", () -> {
            runningUser = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM User " +
                            "WHERE Username = '" + USER + "'",
                    User.class);
        });
    }

    @Test
    @DisplayName("Create Existing Business account(s) in SFDC")
    public void test() throws IOException {
        var resultsFile = initializeAndGetResultsFile("new_sfdc_eb_accounts");
        var existingBusinessAccountsData = getExistingBusinessAccountsData();

        var processedData = new ArrayList<CreateExistingBusinessAccountsInSfdcDTO>();
        for (var data : existingBusinessAccountsData) {
            step("Create an Existing Business Account in SFDC with Name = '" + data.accountName + "'", () -> {
                if (data.ngbsAccountData == null) {
                    throw new IllegalArgumentException(
                            "'ngbsAccountData' parameter is not provided for the current Account data object! \n" +
                                    "Account data: " + data);
                }

                accountGenerationSteps.createAccountInNGBS(data.ngbsAccountData);
                createAccountWithRelatedObjectsInSFDC(data);
                accountGenerationSteps.removeTesterFlagsOnAccountViaSCP(data.ngbsAccountData);

                processedData.add(data);
                updateResultsFile(resultsFile, processedData);
            });
        }

        logResults(resultsFile);
    }

    /**
     * Create Account object for Existing Business Customer with default and custom user-defined parameters,
     * related records, like Contact, AccountContactRole, and insert them into Salesforce.
     *
     * @param accountData data object parsed from user's input parameter for creating Existing Business Account in SFDC
     *                    (with custom data values, like Account's Billing Address or Account's Contact First Name...)
     * @throws Exception in case of malformed query, DB or network errors.
     */
    private void createAccountWithRelatedObjectsInSFDC(CreateExistingBusinessAccountsInSfdcDTO accountData) throws Exception {
        //  get the main account's package info from NGBS to retrieve Product name, Currency and Brand
        var accountDTO = getAccountInNGBS(accountData.ngbsAccountData.billingId);
        var catalogID = accountDTO.packages[0].catalogId;
        var versionID = accountDTO.packages[0].version;
        mainPackageInfo = getPackageFullInfo(catalogID, versionID);
        accountData.serviceName = mainPackageInfo.productName;
        accountData.chargeTerm = accountData.chargeTerm != null && !accountData.chargeTerm.isBlank()
                ? accountData.chargeTerm
                : accountDTO.getMainPackage().masterDuration;

        sfdcAccount = createExistingBusinessAccount(accountData);
        contact = getPrimaryContactOnAccount(sfdcAccount);

        step("Create a new Billing Account Package object (Package__c) for the Account", () -> {
            createBillingAccountPackage(sfdcAccount.getId(), accountData.ngbsAccountData.packageId, catalogID,
                    mainPackageInfo.getLabelsBrandName(), accountData.serviceName,
                    AccountHelper.INVOICE_PAYMENT_METHOD, AccountHelper.PAID_RC_ACCOUNT_STATUS);
        });

        //  for cases with "multiproductData" Accounts Payable Contact Role is created later
        //  as a part of the Invoicing Request Approval creation
        if (accountData.multiproductData == null || accountData.multiproductData.isEmpty()) {
            step("Create non-primary 'Accounts Payable' AccountContactRole " +
                    "for the Existing Business Account and its related Contact", () -> {
                createAccountContactRole(sfdcAccount, contact, ACCOUNTS_PAYABLE_ROLE, false);
            });
        }

        if (accountData.contact != null) {
            step("Update Existing Business Account's Contact fields using provided user's input data", () ->
                    updateContactFieldsInSFDC(contact, accountData.contact)
            );
        }

        if (accountData.billingAddress != null) {
            step("Update Existing Business Account's Billing Address fields using provided user's input data", () ->
                    updateAccountBillingAddressInSFDC(sfdcAccount, accountData.billingAddress)
            );
        }

        updateAccountInfoInNGBS(accountData);

        if (accountData.multiproductData != null && !accountData.multiproductData.isEmpty()) {
            signUpMultiproductOpportunity(accountData);
        }

        accountData.accountURL = BASE_URL + "/" + sfdcAccount.getId();
    }

    /**
     * Create Account object for Existing Business Customer with related Contact and AccountContactRole records
     * using Billing ID and Enterprise ID from NGBS, custom account's data from user's input, and insert them into Salesforce via API.
     *
     * @param accountData data object parsed from user's input parameter for creating Existing Business Account in SFDC
     * @return Account object for Existing Business Customer
     * @throws Exception in case of malformed query, DB or network errors.
     */
    @Step("Create Existing Business Account with related Contact and Primary 'Signatory' AccountContactRole in SFDC")
    private Account createExistingBusinessAccount(CreateExistingBusinessAccountsInSfdcDTO accountData) throws Exception {
        var existingBusinessAccount = createExistingCustomerAccountInSFDC(runningUser,
                new AccountData()
                        .withBillingId(accountData.ngbsAccountData.billingId)
                        .withCurrencyIsoCode(mainPackageInfo.currency)
                        .withRcBrand(RINGCENTRAL_RC_BRAND)
                        .withBillingCountry(US_BILLING_COUNTRY)
        );

        existingBusinessAccount.setName(accountData.accountName);
        existingBusinessAccount.setRC_User_ID__c(accountData.ngbsAccountData.rcUserId);
        existingBusinessAccount.setRC_Brand__c(mainPackageInfo.getLabelsBrandName());
        existingBusinessAccount.setService_Type__c(accountData.serviceName);
        existingBusinessAccount.setRC_Service_name__c(accountData.serviceName);
        if (accountData.ngbsAccountData.scenario.contains(".poc(")) {
            existingBusinessAccount.setRC_Account_Status__c(POC_RC_ACCOUNT_STATUS);
        }
        existingBusinessAccount.setRC_Signup_Date__c(Calendar.getInstance(getTimeZone("UTC")));
        enterpriseConnectionUtils.update(existingBusinessAccount);

        return existingBusinessAccount;
    }

    /**
     * Update NGBS Account with the user's input values
     * (e.g. account name, contact info, billing address).
     *
     * @param accountData data object parsed from user's input parameter
     *                    for creating Existing Business Account in SFDC
     *                    that should contain NGBS Account's data
     *                    (name, related contact info, billing address)
     */
    @Step("Update data on the Account in NGBS")
    private void updateAccountInfoInNGBS(CreateExistingBusinessAccountsInSfdcDTO accountData) {
        var contactInfoUpdateDTO = new ContactInfoUpdateDTO();

        contactInfoUpdateDTO.companyName = accountData.accountName;
        if (accountData.contact != null) {
            contactInfoUpdateDTO.firstName = accountData.contact.firstName;
            contactInfoUpdateDTO.lastName = accountData.contact.lastName;
            contactInfoUpdateDTO.email = accountData.contact.email;
            contactInfoUpdateDTO.phone = accountData.contact.phone;
        }
        updateContactInfo(accountData.ngbsAccountData.billingId, contactInfoUpdateDTO);

        if (accountData.billingAddress != null) {
            //  Billing Address can only be updated via the current Payment Method on the NGBS account
            var paymentMethodsFromNGBS = getPaymentMethodsFromNGBS(accountData.ngbsAccountData.billingId);
            //  AGS scripts create NGBS accounts with Credit Card payment method by default (change here if it's no longer like that)
            var creditCardPaymentMethod = paymentMethodsFromNGBS.stream()
                    .filter(paymentMethodDTO -> paymentMethodDTO.creditCardInfo != null)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No payment method with Credit Card info found on the NGBS account!"));

            var countryCode = getCountryCodeFromSettingsService(accountData.billingAddress.country);
            creditCardPaymentMethod.creditCardInfo.address.country = countryCode;
            creditCardPaymentMethod.creditCardInfo.address.state = accountData.billingAddress.state;
            creditCardPaymentMethod.creditCardInfo.address.city = accountData.billingAddress.city;
            creditCardPaymentMethod.creditCardInfo.address.street1 = accountData.billingAddress.street;
            creditCardPaymentMethod.creditCardInfo.address.zip = accountData.billingAddress.postalCode;

            updatePaymentMethodInNGBS(accountData.ngbsAccountData.billingId, creditCardPaymentMethod);
        }
    }

    /**
     * Update SFDC Account's Billing Address fields with user's input values.
     *
     * @param accountToUpdate    Billing Address fields are updated on this SFDC Account
     * @param billingAddressData user-defined Billing Address information on Account record (Country, City, Street, etc.)
     * @throws ConnectionException in case of errors while accessing API
     */
    private void updateAccountBillingAddressInSFDC(Account accountToUpdate, BillingAddressDTO billingAddressData)
            throws ConnectionException {
        accountToUpdate.setBillingCountry(billingAddressData.country);
        accountToUpdate.setBillingCity(billingAddressData.city);
        accountToUpdate.setBillingState(billingAddressData.state);
        accountToUpdate.setBillingStreet(billingAddressData.street);
        accountToUpdate.setBillingPostalCode(billingAddressData.postalCode);

        enterpriseConnectionUtils.update(accountToUpdate);
    }

    /**
     * Update SFDC Contact's personal data fields with user's input values
     * (First name, last names, email, phone).
     *
     * @param contactToUpdate Billing Address fields are updated on this SFDC Account
     * @param contactData     user-defined personal information on Contact record (First name, last names, email, phone)
     * @throws ConnectionException in case of errors while accessing API
     */
    private void updateContactFieldsInSFDC(Contact contactToUpdate, ContactDTO contactData)
            throws ConnectionException {
        contactToUpdate.setFirstName(contactData.firstName);
        contactToUpdate.setLastName(contactData.lastName);
        contactToUpdate.setEmail(contactData.email);
        contactToUpdate.setPhone(contactData.phone);

        enterpriseConnectionUtils.update(contactToUpdate);
    }

    /**
     * Create a MultiProduct Opportunity for 1 or more additional services (Engage, Contact Center),
     * and sign it up via CRM (UI-Less Process Order) to make the related "tech" account(s) into NGBS Existing Business Account(s).
     * <br/>
     * This method also creates other related records in SFDC:
     * e.g. approved Invoicing Request Approval for the Office Account.
     *
     * @param accountData data object parsed from user's input parameter for creating Existing Business Account in SFDC
     *                    that should contain Multiproduct data (package to select, products to add...)
     */
    @Step("Create and sign up the Multiproduct Opportunity")
    private void signUpMultiproductOpportunity(CreateExistingBusinessAccountsInSfdcDTO accountData) {
        var multiproductData = accountData.multiproductData;

        //  Pre-process MultiProduct test data
        var isEngageDigital = false;
        var isEngageVoice = false;
        var isContactCenter = false;
        for (var mpData : multiproductData) {
            var testPackage = mpData.opportunityData.testPackage;
            var packageInfo = getPackageFullInfo(testPackage.id, testPackage.version);
            mpData.serviceName = packageInfo.productName;

            mpData.opportunityData.products = mpData.opportunityData.products == null
                    ? new Product[]{}
                    : mpData.opportunityData.products;
            for (var product : mpData.opportunityData.products) {
                var licenseDataOptional = packageInfo.getLicenseDataByID(product.dataName);
                assertThat(licenseDataOptional)
                        .as("License data for the product with dataName = " + product.dataName)
                        .isPresent();
                var licenseData = licenseDataOptional.get();

                product.name = licenseData.displayName;
                product.serviceName = packageInfo.productName;
                product.group = licenseData.labels.getMainQuotingGroup();
                product.subgroup = licenseData.labels.getMainQuotingSubGroup();
                product.discount = product.discount != null ? product.discount : 0;
            }

            switch (mpData.serviceName) {
                case ENGAGE_DIGITAL_STANDALONE_SERVICE -> isEngageDigital = true;
                case ENGAGE_VOICE_STANDALONE_SERVICE -> isEngageVoice = true;
                case RC_CONTACT_CENTER_SERVICE -> isContactCenter = true;
            }
        }

        step("Create an Existing Business Office Opportunity via API", () -> {
            var billingId = accountData.ngbsAccountData.billingId;
            var accountDTO = getAccountInNGBS(billingId);

            opportunity = createOpportunity(sfdcAccount, contact,
                    false, mainPackageInfo.getLabelsBrandName(), valueOf(accountDTO.businessIdentityId),
                    runningUser, mainPackageInfo.currency, mainPackageInfo.productName);
        });

        step("Set Office Account.Payment_Method__c = 'Invoice' via API", () -> {
            sfdcAccount.setPayment_Method__c(AccountHelper.INVOICE_PAYMENT_METHOD);
            enterpriseConnectionUtils.update(sfdcAccount);
        });

        step("Open test sandbox login page, and log in to the SF as a test user", () -> {
            loginPage.openPage().login();
        });

        step("Open the Quote Wizard to add new Sales Quote", () -> {
            wizardPage.openPageForNewSalesQuote(opportunity.getId());
            packagePage.packageSelector.waitUntilLoaded();
        });

        step("Select all the packages from the test data", () -> {
            packagePage.packageSelector.packageFilter.selectChargeTerm(accountData.chargeTerm);
            packagePage.packageSelector.setContractSelected(true);

            // select Office/MVP package
            packagePage.packageSelector
                    .getPackageFolderByName(accountData.serviceName)
                    .expandFolder()
                    .getPackageByDataAttribute(mainPackageInfo.id, mainPackageInfo.version, "Regular")
                    .selectPackage();

            //  select additional packages (Contact Center / Engage)
            for (var mpData : multiproductData) {
                var testPackage = mpData.opportunityData.testPackage;
                packagePage.packageSelector
                        .getPackageFolderByName(mpData.serviceName)
                        .expandFolder()
                        .getPackageByDataAttribute(testPackage.id, testPackage.version, "Regular")
                        .selectPackage();
            }
        });

        multiproductData.forEach(mpData -> {
            var products = mpData.opportunityData.products;
            if (products != null && products.length != 0) {
                quoteWizardSteps.addProductsOnProductsTab(mpData.opportunityData.products);
            }
        });

        step("Open the Price tab, set up quantities and discounts (if necessary), and save changes", () -> {
            cartPage.openTab();

            for (var mpData : multiproductData) {
                for (var currentProduct : mpData.opportunityData.products) {
                    //  the script should only set quantities for enabled 'New Quantity' inputs
                    //  the logic behind input's state is to be checked in the tests
                    var isQuantityEnabled = cartPage.getQliFromCartByDisplayName(currentProduct.name)
                            .getNewQuantityInput()
                            .isEnabled();
                    if (isQuantityEnabled) {
                        step("Setting up quantity for '" + currentProduct.name + "'", () -> {
                            cartPage.setNewQuantityForQLItem(currentProduct.name, currentProduct.quantity);
                        });
                    }

                    cartTabSteps.setUpDiscounts(currentProduct);
                }

                //  TODO Remove it when the Known Issue PBC-20875 is fixed
                //  TODO Some EV packages have a Primary license that should have min quantity = 3 which is not set by default
                if (List.of("1270005", "1271005", "1272005").contains(mpData.opportunityData.testPackage.id)) {
                    cartPage.getQliFromCartByDataId("SA_SEAT_4")
                            .getNewQuantityInput()
                            .setValue("3")
                            .unfocus();
                }
            }

            cartPage.saveChanges();
        });

        if (isContactCenter) {
            step("Click 'Initiate CC ProServ' button, click 'Submit' button in popup window " +
                    "and check that 'Initiate CC ProServ' button is hidden and 'CC ProServ Created' button is visible and disabled", () -> {
                cartPage.initiateCcProServ();

                cartPage.initiateCcProServButton.shouldBe(hidden);
                cartPage.ccProServCreatedButton.shouldBe(disabled);
            });

            step("Set Quote.ProServ_Status__c = 'Sold' for CC ProServ Quote via API", () -> {
                var ccProServQuote = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id " +
                                "FROM Quote " +
                                "WHERE OpportunityId = '" + opportunity.getId() + "' " +
                                "AND RecordType.Name = '" + CC_PROSERV_QUOTE_RECORD_TYPE + "'",
                        Quote.class);
                ccProServQuote.setProServ_Status__c(SOLD_PROSERV_STATUS);
                enterpriseConnectionUtils.update(ccProServQuote);
            });
        }

        step("Open the Quote Details tab, set Quote Stage = 'Agreement', Start Date, save changes, " +
                "and update it to Active Agreement via API", () -> {
            quotePage.openTab();
            quotePage.stagePicklist.selectOption(AGREEMENT_QUOTE_TYPE);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();

            var masterQuoteToUpdate = new Quote();
            masterQuoteToUpdate.setId(wizardPage.getSelectedQuoteId());
            setQuoteToApprovedActiveAgreement(masterQuoteToUpdate);
            enterpriseConnectionUtils.update(masterQuoteToUpdate);
        });

        step("Create Invoicing Request Approval for the test Account " +
                "with related 'Accounts Payable' AccountContactRole record, " +
                "set Approval__c.Status = 'Approved' (all via API)", () ->
                createInvoiceApprovalApproved(opportunity, sfdcAccount, contact, runningUser.getId(), true)
        );

        step("Set the required fields for Stage changing on the Opportunity, " +
                "close it, and set ServiceInfo__c.UpgradeStepStatus__c = true (for the Master Quote) via API", () -> {
            setRequiredFieldsForOpportunityStageChange(opportunity);
            opportunity.setStageName(CLOSED_WON_STAGE);

            var serviceInfo = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM ServiceInfo__c " +
                            "WHERE Quote__c = '" + wizardPage.getSelectedQuoteId() + "'",
                    ServiceInfo__c.class);
            serviceInfo.setUpgradeStepStatus__c(true);

            enterpriseConnectionUtils.update(opportunity, serviceInfo);
        });

        step("Open the Opportunity record page, click 'Process Order' button, " +
                "and make sure that MVP Service is ready to be synced", () -> {
            opportunityPage.openPage(opportunity.getId());
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.mvpTierStatus
                    .shouldHave(exactTextCaseSensitive(NOT_SYNCED_STATUS), ofSeconds(60));
            opportunityPage.processOrderModal.spinner.shouldBe(hidden, ofSeconds(60));
            opportunityPage.processOrderModal.errorNotifications.shouldHave(size(0));
        });

        step("Follow the Sync with NGBS process of MVP/Office service to the end", () -> {
            processOrderModal.mvpAllSyncStepNames.shouldHave(sizeGreaterThanOrEqual(1), ofSeconds(60));
            //  additional wait for the 'Next' button to become clickable
            sleep(2_000);

            while (processOrderModal.nextButton.isDisplayed()) {
                processOrderModal.nextButton.click();

                processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60));
                processOrderModal.spinner.shouldBe(hidden, ofSeconds(60));
                processOrderModal.errorNotifications.shouldHave(size(0));
            }

            opportunityPage.processOrderModal.mvpTierStatus
                    .shouldHave(exactTextCaseSensitive(SYNCED_WITH_NGBS_STATUS), ofSeconds(60));
        });

        if (isEngageVoice) {
            step("Sign Up 'Engage Voice' service via Process Order modal", () -> {
                step("Expand 'Engage Voice' service and click 'Process Engage Voice' button", () -> {
                    opportunityPage.processOrderModal.engageVoiceExpandButton.click();

                    opportunityPage.processOrderModal.processEngageVoiceButton.click();
                    opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));
                    opportunityPage.processOrderModal.engageVoiceValidateAddressButton.shouldBe(disabled);
                });

                step("Select Engage Voice Platform Location, RC Engage Voice Platform and Timezone", () -> {
                    opportunityPage.processOrderModal.engageVoicePlatformLocationSelect.selectOption(US1_PLATFORM_LOCATION);
                    opportunityPage.processOrderModal.selectRcEngagePlatformFirstOption(ENGAGE_VOICE_SERVICE);
                    opportunityPage.processOrderModal.engageVoiceTimezoneSelect.selectOption(ANCHORAGE_TIME_ZONE);
                });

                step("Click 'Validate' button and make sure that the address is validated", () -> {
                    opportunityPage.processOrderModal.engageVoiceValidateAddressButton.click();
                    opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(30));

                    //  If the address is not verified automatically, user needs to handle a pop-up modal
                    if (opportunityPage.processOrderModal.addressVerificationContinueButton.isDisplayed()) {
                        opportunityPage.processOrderModal.addressVerificationContinueButton.click();
                        opportunityPage.processOrderModal.addressVerificationContinueButton.shouldBe(hidden);
                    }

                    opportunityPage.processOrderModal.engageVoiceValidateAddressButton.shouldBe(disabled);
                });

                step("Click 'Sign Up Engage Voice' button and check notification that Engage Voice submitted successfully", () -> {
                    opportunityPage.processOrderModal.signUpButton.click();
                    opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));
                    opportunityPage.processOrderModal.successNotifications
                            .shouldHave(exactTexts(format(SERVICE_SUBMITTED_SUCCESSFULLY, ENGAGE_VOICE_SERVICE)), ofSeconds(60));
                });

                step("Check that the Engage Voice Account received Billing_ID__c and RC_User_ID__c from the NGBS", () ->
                        checkTechAccountAfterSignUp(ENGAGE_VOICE_STANDALONE_SERVICE, multiproductData, timeoutInSecondsForEngageServices)
                );
            });
        }

        if (isEngageDigital) {
            step("Sign Up 'Engage Digital' service via Process Order modal", () -> {
                step("Expand 'Engage Digital' service and click 'Process Engage Digital' button", () -> {
                    opportunityPage.processOrderModal.engageDigitalExpandButton.click();

                    opportunityPage.processOrderModal.processEngageDigitalButton.click();
                    opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));
                    opportunityPage.processOrderModal.engageDigitalValidateAddressButton.shouldBe(disabled);
                });

                step("Select Engage Digital Platform Location, RC Engage Digital Platform, Language and Timezone " +
                        "and populate RC Engage Digital Domain field", () -> {
                    opportunityPage.processOrderModal.engageDigitalPlatformLocationSelect.selectOption(US1_PLATFORM_LOCATION);
                    opportunityPage.processOrderModal.selectRcEngagePlatformFirstOption(ENGAGE_DIGITAL_SERVICE);
                    opportunityPage.processOrderModal.engageDigitalLanguageSelect.selectOption(EN_US_LANGUAGE);
                    opportunityPage.processOrderModal.engageDigitalTimezoneSelect.selectOption(ANCHORAGE_TIME_ZONE);

                    //  Engage Digital Domain should contain up to 32 symbols
                    var randomEngageDomainValue = UUID.randomUUID().toString().substring(0, 31);
                    opportunityPage.processOrderModal.engageDigitalDomainInput.setValue(randomEngageDomainValue);
                });

                step("Click 'Validate' button and make sure that the address is validated", () -> {
                    opportunityPage.processOrderModal.engageDigitalValidateAddressButton.click();
                    opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(30));

                    //  If the address is not verified automatically, user need to handle a pop-up modal
                    if (opportunityPage.processOrderModal.addressVerificationContinueButton.isDisplayed()) {
                        opportunityPage.processOrderModal.addressVerificationContinueButton.click();
                        opportunityPage.processOrderModal.addressVerificationContinueButton.shouldBe(hidden);
                    }

                    opportunityPage.processOrderModal.engageDigitalValidateAddressButton.shouldBe(disabled);
                });

                step("Click 'Sign Up Engage Digital' button and check notification that Engage Digital submitted successfully", () -> {
                    opportunityPage.processOrderModal.signUpButton.click();
                    opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(150));
                    opportunityPage.processOrderModal.successNotifications
                            .shouldHave(exactTexts(format(SERVICE_SUBMITTED_SUCCESSFULLY, ENGAGE_DIGITAL_SERVICE)), ofSeconds(60));
                });

                step("Check that the Engage Digital Account received Billing_ID__c and RC_User_ID__c from the NGBS", () ->
                        checkTechAccountAfterSignUp(ENGAGE_DIGITAL_STANDALONE_SERVICE, multiproductData, timeoutInSecondsForEngageServices)
                );
            });
        }

        if (isContactCenter) {
            step("Sign Up 'Contact Center' service via Process Order modal", () -> {
                step("Expand RingCentral Contact Center section, populate necessary fields in 'Add General Information' section", () -> {
                    opportunityPage.processOrderModal.expandContactCenterSection();
                    opportunityPage.processOrderModal.clickProcessContactCenter();

                    opportunityPage.processOrderModal.rcCcTimezoneSelect.selectOption(ALASKA_TIME_ZONE);
                    opportunityPage.processOrderModal.selectGeoRegionPicklist.selectOption(usGeoRegionOption);
                    opportunityPage.processOrderModal.selectImplementationTeamPicklist.selectOption(ringCentralTeam);
                    opportunityPage.processOrderModal.selectInContactSegmentPicklist.selectOption(inContactSegmentOption);
                    opportunityPage.processOrderModal.ccNumberInput.setValue(rcMainNumberValue);
                });

                step("Click 'Validate' button in Add Address Validation section, " +
                        "check that 'Sign Up Contact Center' button is visible and enabled, click 'Sign Up Contact Center' button " +
                        "and check that success notification is shown in the Process Order modal window", () -> {
                    opportunityPage.processOrderModal.contactCenterValidateButton.click();

                    //  If the address is not verified automatically, user need to handle a pop-up modal
                    if (opportunityPage.processOrderModal.addressVerificationContinueButton.isDisplayed()) {
                        opportunityPage.processOrderModal.addressVerificationContinueButton.click();
                        opportunityPage.processOrderModal.addressVerificationContinueButton.shouldBe(hidden);
                    }

                    opportunityPage.processOrderModal.contactCenterValidateButton.shouldBe(disabled);

                    opportunityPage.processOrderModal.signUpButton.shouldBe(enabled, ofSeconds(10)).click();
                    opportunityPage.processOrderModal.successNotifications
                            .shouldHave(exactTexts(format(SERVICE_SUBMITTED_SUCCESSFULLY, CONTACT_CENTER_SERVICE)), ofSeconds(60));
                });

                step("Populate 'inContact_BU_ID__c' and 'inContact_Status__c' fields " +
                        "of the related Case with Record Type = 'inContact_Order' via API", () -> {
                    var inContactOrderCase = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id " +
                                    "FROM Case " +
                                    "WHERE Opportunity_Reference__c = '" + opportunity.getId() + "' " +
                                    "AND RecordTypeName__c = '" + INCONTACT_ORDER_RECORD_TYPE + "'",
                            Case.class);
                    inContactOrderCase.setInContact_BU_ID__c(Double.valueOf(inContactBuId));
                    inContactOrderCase.setInContact_Status__c(INCONTACT_COMPLETED_STATUS);
                    enterpriseConnectionUtils.update(inContactOrderCase);
                });

                step("Check that the RC Contact Center Account received Billing_ID__c and RC_User_ID__c from the NGBS", () ->
                        checkTechAccountAfterSignUp(RC_CONTACT_CENTER_SERVICE, multiproductData, timeoutInSecondsForRcCcService)
                );
            });
        }
    }

    /**
     * Check fields on the signed up Technical Accounts (e.g. Billing ID, Enterprise Account ID).
     *
     * @param serviceName      name of the service to check the fields on the Technical Account
     *                         (e.g. "Engage Digital Standalone", "Engage Voice Standalone", "RingCentral Contact Center")
     * @param multiproductData user's input data for creating Multiproduct Opportunity
     * @param timeoutInSeconds timeout in seconds to wait for the fields to receive values from the external system (e.g. 60)
     */
    private void checkTechAccountAfterSignUp(String serviceName, List<CreateMultiproductDataInSfdcDTO> multiproductData, int timeoutInSeconds) {
        //  Billing_ID__c and RC_User_ID__c on Tech Accounts might be received from the external system with a delay
        var signedUpTechAccount = step("Wait until the Tech Account's Billing_ID__c and RC_User_ID__c receive values from the external system", () ->
                assertWithTimeout(() -> {
                    var techAccounts = enterpriseConnectionUtils.query(
                            "SELECT Id, Billing_ID__c, RC_User_ID__c " +
                                    "FROM Account " +
                                    "WHERE Master_Account__c = '" + sfdcAccount.getId() + "' " +
                                    "AND Service_Type__c = '" + serviceName + "'",
                            Account.class);
                    assertEquals(1, techAccounts.size(), "Number of Tech Accounts for service = " + serviceName);

                    var techAccount = techAccounts.get(0);
                    assertNotNull(techAccount.getBilling_ID__c(), "Technical Account.Billing_ID__c value");
                    assertNotNull(techAccount.getRC_User_ID__c(), "Technical Account.RC_User_ID__c value");

                    return techAccount;
                }, ofSeconds(timeoutInSeconds), ofSeconds(5))
        );

        multiproductData.stream()
                .filter(mpData -> mpData.serviceName.equals(serviceName))
                .findFirst()
                .ifPresent(mpData -> {
                    mpData.billingId = signedUpTechAccount.getBilling_ID__c();
                    mpData.rcUserId = signedUpTechAccount.getRC_User_ID__c();
                    mpData.accountURL = BASE_URL + "/" + signedUpTechAccount.getId();
                });
    }

    /**
     * Get a collection of input data objects for creating Existing Business Accounts in SFDC
     * from the system property variable.
     *
     * @return list of input data objects to create Existing Business Account(s) with
     */
    private List<CreateExistingBusinessAccountsInSfdcDTO> getExistingBusinessAccountsData() {
        var existingBusinessAccountsDataInputString = System.getProperty("sf.createExistingBusinessAccountsData");
        if (existingBusinessAccountsDataInputString == null || existingBusinessAccountsDataInputString.isBlank()) {
            throw new IllegalArgumentException("No Account Input Data have been provided! " +
                    "Make sure to add SFDC account and contact data, as well as data for creating account in NGBS " +
                    "for this task via 'sf.createExistingBusinessAccountsData' parameter!");
        }

        var accountsDataParsed = JsonUtils.readJson(
                existingBusinessAccountsDataInputString, CreateExistingBusinessAccountsInSfdcDTO[].class);
        return List.of(accountsDataParsed);
    }
}
