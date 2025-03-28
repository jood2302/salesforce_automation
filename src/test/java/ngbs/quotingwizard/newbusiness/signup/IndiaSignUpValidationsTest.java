package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Approval__c;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApproval;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createKycApproval;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.LocalSubscribedAddressFactory.createIndiaLocalSubscribedAddressRecord;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.getPrimaryContactOnAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_APPROVED;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_PENDING_L1;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LocalSubscribedAddressHelper.REGISTERED_ADDRESS_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.RC_INDIA_MUMBAI_BUSINESS_IDENTITY_ID;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.closeWindow;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("NGBS")
@Tag("SignUp")
@Tag("IndiaMVP")
public class IndiaSignUpValidationsTest extends BaseTest {
    private final Steps steps;
    private final IndiaSignUpSteps indiaSignUpSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Approval__c defaultKycApproval;
    private Approval__c newKycApproval;
    private Approval__c invoiceApproval;

    //  Test data
    private final String indiaBusinessIdentityId;

    public IndiaSignUpValidationsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_Contract_163077013_RC_India_NB.json",
                Dataset.class);
        steps = new Steps(data);
        indiaSignUpSteps = new IndiaSignUpSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        indiaBusinessIdentityId = RC_INDIA_MUMBAI_BUSINESS_IDENTITY_ID;
    }

    @BeforeEach
    public void setUpTest() {
        indiaSignUpSteps.setUpIndiaSignUpTest(indiaBusinessIdentityId);

        step("Link Master Office and India Accounts via SF API, " +
                "and refresh the Quote Wizard", () -> {
            indiaSignUpSteps.rcIndiaAccount.setMaster_Account__c(indiaSignUpSteps.rcOfficeAccount.getId());
            enterpriseConnectionUtils.update(indiaSignUpSteps.rcIndiaAccount);

            //  refresh to update the Quote's state with the Accounts linking done above
            refresh();
            wizardPage.waitUntilLoaded();
        });
    }

    @Test
    @TmsLink("CRM-23898")
    @TmsLink("CRM-23436")
    @DisplayName("CRM-23898 - Sign Up Validation - Approved 'KYC Approval Request' required. \n" +
            "CRM-23436 - Sign Up Validation - Approved 'Invoicing Request' required")
    @Description("CRM-23898 - Verify that India MVP Opportunity can be signed up only if there is an approved KYC Approval Request. \n" +
            "CRM-23436 - Verify that if there is no Approved Invoicing Request under the Account then attempt to Sign Up will fail validation")
    public void test() {
        step("1. Add some Products on the Add Products tab and save changes on the Price tab", () -> {
            steps.quoteWizard.addProductsOnProductsTab(indiaSignUpSteps.rcIndiaPackage.products);
            cartPage.openTab();
            cartPage.saveChanges();
        });

        step("2. Open the Quote Details tab, populate Area Code, Start Date, " +
                "select Payment Method = 'Invoice' and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.indiaAreaCode);
            quotePage.selectPaymentMethod(INVOICE_PAYMENT_METHOD);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();

            closeWindow();
        });

        step("3. Update the Quote to Active Agreement via API", () ->
                steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(indiaSignUpSteps.rcIndiaOpportunity)
        );

        //  CRM-23898, CRM-23436
        step("4. Click 'Process Order' button on the Opportunity record page, " +
                "check that the error messages are shown on the 'Preparing Data - Data validation' step in the Process Order modal window, " +
                "and close the modal window", () -> {
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications.shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    format(OBTAIN_INVOICE_PAYMENT_APPROVAL_ERROR, MVP_SERVICE, indiaSignUpSteps.officeService),
                    format(ACCOUNT_SHOULD_HAVE_APPROVED_INVOICING_REQUEST_ERROR, MVP_SERVICE),
                    format(NEED_APPROVED_KYC_REQUEST_ERROR, MVP_SERVICE)), ofSeconds(1));
            opportunityPage.processOrderModal.mvpPreparingDataActiveStep.shouldHave(exactTextCaseSensitive(DATA_VALIDATION_STEP));

            opportunityPage.processOrderModal.closeWindow();
        });

        step("5. Reject default KYC Approval on RC India Account via API", () -> {
            defaultKycApproval = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Approval__c " +
                            "WHERE Account__c = '" + indiaSignUpSteps.rcIndiaAccount.getId() + "' ",
                    Approval__c.class);

            enterpriseConnectionUtils.rejectSingleRecord(defaultKycApproval.getId());
        });

        //  CRM-23898
        step("6. Click 'Process Order' button on the Opportunity record page, " +
                "check that the error messages are shown on the 'Preparing Data - Data validation' step in the Process Order modal window, " +
                "and close the modal window", () -> {
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications.shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    format(OBTAIN_INVOICE_PAYMENT_APPROVAL_ERROR, MVP_SERVICE, indiaSignUpSteps.officeService),
                    format(ACCOUNT_SHOULD_HAVE_APPROVED_INVOICING_REQUEST_ERROR, MVP_SERVICE),
                    format(NEED_APPROVED_KYC_REQUEST_ERROR, MVP_SERVICE)), ofSeconds(1));
            opportunityPage.processOrderModal.mvpPreparingDataActiveStep.shouldHave(exactTextCaseSensitive(DATA_VALIDATION_STEP));

            opportunityPage.processOrderModal.closeWindow();
        });

        //  CRM-23898
        step("7. Create a new KYC Approval for RC India Account, then open KYC Approval page, " +
                "prepare it for approval and approve it via API", () -> {
            newKycApproval = createKycApproval(indiaSignUpSteps.rcIndiaOpportunity, indiaSignUpSteps.rcIndiaAccount,
                    indiaSignUpSteps.signedOffParticipationAgreementFileName,
                    indiaSignUpSteps.dealDeskUserWithEditKycApprovalPermissionSet.getId());

            step("Open KYC Approval page, then populate required fields via API, " +
                    "and attach required files in 'KYC Details' block and save changes", () -> {
                kycApprovalPage.openPage(newKycApproval.getId());
                steps.kycApproval.populateKycApprovalFieldsRequiredForApproval(newKycApproval);
            });

            step("Create a new Local Subscribed Address record of 'Registered Address of Company' type " +
                    "for KYC Approval via API", () -> {
                createIndiaLocalSubscribedAddressRecord(newKycApproval, REGISTERED_ADDRESS_RECORD_TYPE);
            });

            enterpriseConnectionUtils.approveSingleRecord(newKycApproval.getId());
        });

        step("8. Create Invoice Approval for RC India Opportunity " +
                "with related 'Accounts Payable' AccountContactRole record via API", () -> {
            invoiceApproval = createInvoiceApproval(indiaSignUpSteps.rcIndiaOpportunity, indiaSignUpSteps.rcIndiaAccount,
                    getPrimaryContactOnAccount(indiaSignUpSteps.rcIndiaAccount),
                    indiaSignUpSteps.dealDeskUserWithEditKycApprovalPermissionSet.getId(), false);
        });

        //  CRM-23436
        step("9. Open India Opportunity page, click 'Process Order' button, " +
                "check that the error message is shown on the 'Preparing Data - Data validation' step in the Process Order modal window, " +
                "and close the modal window", () -> {
            opportunityPage.openPage(indiaSignUpSteps.rcIndiaOpportunity.getId());
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications
                    .shouldHave(exactTexts(format(OBTAIN_INVOICE_PAYMENT_APPROVAL_ERROR, MVP_SERVICE, indiaSignUpSteps.officeService)), ofSeconds(1));
            opportunityPage.processOrderModal.mvpPreparingDataActiveStep.shouldHave(exactTextCaseSensitive(DATA_VALIDATION_STEP));

            opportunityPage.processOrderModal.closeWindow();
        });

        step("10. Set Invoice Approval__c.Status__c = 'PendingL1Approval' via API", () -> {
            invoiceApproval.setStatus__c(APPROVAL_STATUS_PENDING_L1);
            enterpriseConnectionUtils.update(invoiceApproval);
        });

        //  CRM-23436
        step("11. Click 'Process Order' button on the Opportunity record page, " +
                "check that the error message is shown on the 'Preparing Data - Data validation' step in the Process Order modal window, " +
                "and close the modal window", () -> {
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications
                    .shouldHave(exactTexts(format(OBTAIN_INVOICE_PAYMENT_APPROVAL_ERROR, MVP_SERVICE, indiaSignUpSteps.officeService)), ofSeconds(1));
            opportunityPage.processOrderModal.mvpPreparingDataActiveStep.shouldHave(exactTextCaseSensitive(DATA_VALIDATION_STEP));

            opportunityPage.processOrderModal.closeWindow();
        });

        step("12. Set Invoice Approval__c.Status__c = 'Approved' via API", () -> {
            invoiceApproval.setStatus__c(APPROVAL_STATUS_APPROVED);
            enterpriseConnectionUtils.update(invoiceApproval);
        });

        //  CRM-23898, CRM-23436
        step("13. Click 'Process Order' button on the Opportunity record page, " +
                "verify that 'Preparing Data' step is completed in the Process Order modal, " +
                "and no errors are displayed", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(hidden);
            opportunityPage.processOrderModal.selectTimeZonePicklist.getInput().shouldBe(enabled, ofSeconds(60));
        });
    }
}
