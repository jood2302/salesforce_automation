package ngbs.quotingwizard.engage;

import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.ags.AGSRestApiClient;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import ngbs.quotingwizard.newbusiness.signup.MultiProductSignUpSteps;

import java.util.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.model.ngbs.dto.license.BillingInfoLicenseDTO.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.SIGNED_UP_STATUS;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getBillingInfoSummaryLicenses;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.PAID_RC_ACCOUNT_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.ENGAGE_VOICE_STANDALONE_SERVICE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.setQuoteToApprovedActiveAgreement;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.hidden;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test methods related to the tests that check the Engage service's usage licenses in NGBS
 * after the Engage service is signed up.
 */
public class EngageUsageLicensesAfterSignupSteps {
    private final Dataset data;
    private final Steps steps;
    private final MultiProductSignUpSteps multiProductSignUpSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUser;
    private Account account;
    private Opportunity opportunity;
    public String officeAccountBillingId;

    //  Test data
    private final String agsScenario;

    private final String officeService;
    public final String engageServiceName;
    private final Package officePackage;
    private final Map<String, Package> packageFolderNameToPackageMap;

    private final Product digitalLineUnlimited;
    private final Product officePhone;
    private final Product[] productsToAdd;

    /**
     * New instance for the class with the test methods/steps related to
     * checking the Engage service's usage licenses in NGBS
     * after the Engage service is signed up.
     *
     * @param engageDataIndex index number of the dataset in the object parsed JSON file with the test data
     *                        for the Engage Package
     */
    public EngageUsageLicensesAfterSignupSteps(int engageDataIndex) {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_Annual_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        multiProductSignUpSteps = new MultiProductSignUpSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        agsScenario = "ngbs(brand=1210,duration=a,package=1231005v2,payment=Invoice)";

        officeService = data.packageFolders[0].name;
        engageServiceName = data.packageFolders[engageDataIndex].name;
        officePackage = data.packageFolders[0].packages[0];
        var engagePackage = data.packageFolders[engageDataIndex].packages[0];

        packageFolderNameToPackageMap = Map.of(
                officeService, officePackage,
                engageServiceName, engagePackage
        );

        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        officePhone = data.getProductByDataName("LC_HD_523");

        var productsToAddAsList = new ArrayList<Product>();
        productsToAddAsList.add(officePhone);
        if (engageServiceName.equals(ENGAGE_VOICE_STANDALONE_SERVICE)) {
            productsToAddAsList.add(data.getProductByDataName("SA_CRS30_24", engagePackage));
        }
        productsToAdd = productsToAddAsList.toArray(new Product[0]);
    }

    /**
     * Create an Account/Contact/Contact Role/Opportunity,
     * update some Account fields, and log in to Salesforce as the Deal Desk user.
     */
    public void setUpTest() {
        dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        account = steps.salesFlow.account;

        steps.quoteWizard.createOpportunity(account, steps.salesFlow.contact, dealDeskUser);
        opportunity = steps.quoteWizard.opportunity;

        step("Set Office Account's Account_Payment_Method__c = 'Invoice', " +
                "Service_Type__c and RC_Service_name__c = 'Office' via API", () -> {
            account.setAccount_Payment_Method__c(INVOICE_PAYMENT_METHOD);
            account.setService_Type__c(officeService);
            account.setRC_Service_name__c(officeService);
            enterpriseConnectionUtils.update(account);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    /**
     * Prepare the Multiproduct Quote with MVP and Engage packages for signing up of its Engage service.
     */
    public void prepareMultiproductQuoteWithEngageForSignup() {
        step("Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select MVP and " + engageServiceName + " packages for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityForMultiProduct(opportunity.getId(), packageFolderNameToPackageMap)
        );

        step("Add necessary products on the Add Products tab, " +
                "open the Price tab, and assign phone to DL", () -> {
            steps.quoteWizard.addProductsOnProductsTab(productsToAdd);

            cartPage.openTab();
            steps.cartTab.assignDevicesToDL(officePhone.name, digitalLineUnlimited.name, steps.quoteWizard.localAreaCode,
                    officePhone.quantity);
        });

        step("Open the Quote Details tab, set Start Date and Main Area Code, save changes, " +
                "and update it to Active Agreement via API", () -> {
            quotePage.openTab();
            quotePage.setDefaultStartDate();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.saveChanges();

            var masterQuoteToUpdate = new Quote();
            masterQuoteToUpdate.setId(wizardPage.getSelectedQuoteId());
            setQuoteToApprovedActiveAgreement(masterQuoteToUpdate);
            enterpriseConnectionUtils.update(masterQuoteToUpdate);
        });

        step("Create Invoice Request Approval for the test Account " +
                "with related 'Accounts Payable' AccountContactRole record, " +
                "set Approval__c.Status = 'Approved' (all via API)", () ->
                createInvoiceApprovalApproved(opportunity, account, steps.salesFlow.contact, dealDeskUser.getId(), true)
        );

        //  to bypass signing up MVP account in the 'Process Order' modal
        step("Generate Existing Business MVP Account in NGBS for scenario '" + agsScenario + "' via AGS API, " +
                "add a contract to the Account via NGBS API, " +
                "set 'Billing Id' and 'Enterprise Account Id' fields on the SFDC Account via SFDC API, " +
                "and create new Billing Account Package object (Package__c) for the SFDC Account for the Office packages via SFDC API", () -> {
            var accountDetailsAGS = AGSRestApiClient.createAccount(agsScenario);
            officeAccountBillingId = accountDetailsAGS.getAccountBillingId();
            var officeAccountPackageId = accountDetailsAGS.getAccountPackageId();

            steps.ngbs.stepCreateContractInNGBS(officeAccountBillingId, officeAccountPackageId, officePackage.contractExtId, digitalLineUnlimited);

            account.setBilling_ID__c(officeAccountBillingId);
            account.setRC_User_ID__c(accountDetailsAGS.rcUserId);
            enterpriseConnectionUtils.update(account);

            createBillingAccountPackage(account.getId(), officeAccountPackageId, officePackage.id,
                    data.brandName, officeService, INVOICE_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);
        });

        step("Open the Opportunity record page, click 'Process Order' button, " +
                "and make sure that MVP Service is signed up", () -> {
            opportunityPage.openPage(opportunity.getId());
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.mvpTierStatus
                    .shouldHave(exactTextCaseSensitive(SIGNED_UP_STATUS), ofSeconds(60));
            opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));
            opportunityPage.processOrderModal.errorNotifications.shouldHave(size(0));
        });
    }

    /**
     * Check Service plan values (Billing Cycle Duration)
     * for Recurring and Usage licenses in NGBS for the signed up Engage service.
     */
    public void checkEngageLicensesAfterSignup(String serviceName) {
        var addedPackageId = multiProductSignUpSteps.checkAddedPackageAfterSignUp(officeAccountBillingId, serviceName);

        var billingInfoLicenses = getBillingInfoSummaryLicenses(officeAccountBillingId, addedPackageId);

        var recurringLicenses = Arrays.stream(billingInfoLicenses)
                .filter(license -> license.billingType.equals(RECURRING_BILLING_TYPE))
                .toList();
        var usageLicenses = Arrays.stream(billingInfoLicenses)
                .filter(license -> license.billingType.equals(USAGE_BILLING_TYPE))
                .toList();

        recurringLicenses.forEach(license ->
                assertThat(license.billingCycleDuration)
                        .as("Billing cycle duration of %s license", license.catalogId)
                        .isEqualTo(ANNUAL_BILLING_CYCLE_DURATION)
        );

        usageLicenses.forEach(license ->
                assertThat(license.billingCycleDuration)
                        .as("Billing cycle duration of %s license", license.catalogId)
                        .isEqualTo(MONTHLY_BILLING_CYCLE_DURATION)
        );
    }
}
