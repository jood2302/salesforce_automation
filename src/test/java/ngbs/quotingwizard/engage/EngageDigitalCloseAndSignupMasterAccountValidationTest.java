package ngbs.quotingwizard.engage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.DATA_VALIDATION_STEP;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.INVOICE_PAYMENT_METHOD_IS_REQUIRED_FOR_ENGAGE_SIGNUP_ERROR;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApproval;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.QuoteFactory.createActiveSalesAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.INFLUENCER_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_APPROVED;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_PENDING_L1;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("Engage")
@Tag("SignUp")
@Tag("OpportunityClose")
public class EngageDigitalCloseAndSignupMasterAccountValidationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final AccountBindingsSteps accountBindingsSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUser;
    private Account engageAccount;
    private Opportunity engageOpportunity;
    private Account officeAccount;
    private Contact officeContact;
    private Opportunity officeOpportunity;
    private Approval__c officeAccountApproval;

    //  Test data
    private final String officeInitialTerm;

    public EngageDigitalCloseAndSignupMasterAccountValidationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_EngageDSAndMVP_Monthly_Contract_WithProducts.json",
                Dataset.class);

        steps = new Steps(data);
        accountBindingsSteps = new AccountBindingsSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        officeInitialTerm = data.packageFolders[1].packages[0].contractTerms.initialTerm[0];
    }

    @BeforeEach
    public void setUpTest() {
        salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        engageAccount = steps.salesFlow.account;
        var engageContact = steps.salesFlow.contact;

        steps.quoteWizard.createOpportunity(engageAccount, engageContact, salesRepUser);
        engageOpportunity = steps.quoteWizard.opportunity;

        accountBindingsSteps.createOfficeAccountRecordsForBinding(salesRepUser);
        officeAccount = accountBindingsSteps.officeAccount;
        officeContact = accountBindingsSteps.officeAccountContact;
        officeOpportunity = accountBindingsSteps.officeOpportunity;

        step("Create common Contact Role for Engage and Office Accounts based on one Contact record via API", () -> {
            createAccountContactRole(officeAccount, engageContact, INFLUENCER_ROLE, false);
        });

        step("Simulate that the Office Account is signed up via API", () -> {
            officeAccount.setRC_Account_Status__c(PAID_RC_ACCOUNT_STATUS);
            setRandomEnterpriseAccountId(officeAccount);
            enterpriseConnectionUtils.update(officeAccount);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20652")
    @TmsLink("CRM-20656")
    @DisplayName("CRM-20652 - Engage Account - Sign Up Validation - Master's Payment Method - not Required. \n" +
            "CRM-20656 - New Business Master Account Payment Method - not Required")
    @Description("CRM-20652 - Verify that Approved Invoicing Request approvals under the related Office account " +
            "is not required to Sign Up Engage Account. \n" +
            "CRM-20656 - Verify that if there are: \n" +
            "- no Approved Invoicing Request approvals under the related Office account\n" +
            "- and Master Account is New business Account\n" +
            "- and related Office Account.Payment_Method__c != 'Invoice' \n\n" +
            "then on Close button click on Engage Opportunity Notification should not be shown: " +
            "'Invoice payment method is required on related Office Account for Engage Accounts.'")
    public void test() {
        step("1. Open the Engage Digital Standalone Opportunity, switch to the Quote Wizard, " +
                "add a new Sales Quote, and select a package for it", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(engageOpportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        step("2. Prepare the Engage Opportunity for Close and Sign Up", () -> {
            step("Open the Price tab, set up all the quantities on the added products, and save changes", () -> {
                cartPage.openTab();
                steps.cartTab.setUpQuantities(data.getProductsDefault());
                cartPage.saveChanges();
            });

            step("Link Master (Office) Account and Quote with Engage objects via API and reload the Quote Wizard", () -> {
                steps.engage.linkAccounts(engageAccount, officeAccount);

                step("Create a Quote as Active Sales Agreement for Office Opportunity " +
                        "and set it as Parent Quote on Engage Quote via API ", () -> {
                    var officeQuote = createActiveSalesAgreement(officeOpportunity, officeInitialTerm);

                    var engageQuote = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id " +
                                    "FROM Quote " +
                                    "WHERE OpportunityId = '" + engageOpportunity.getId() + "'",
                            Quote.class);
                    engageQuote.setParentQuote__c(officeQuote.getId());
                    enterpriseConnectionUtils.update(engageQuote);
                });

                //  Additional refresh to update data in QW after accounts linking
                wizardBodyPage.reloadIFrame();
                wizardPage.waitUntilLoaded();
            });

            step("Populate Start Date on the Quote Details tab and save changes", () -> {
                quotePage.openTab();
                quotePage.setDefaultStartDate();
                quotePage.saveChanges();
            });

            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(engageOpportunity);
        });

        //  CRM-20652
        step("3. Click 'Process Order' button on the Opportunity record page " +
                "and check that the error notification 'Invoice payment method is required...' is not displayed in the Process Order modal, " +
                "and that the active step is the 'Data validation' step, " +
                "and close the modal window", () -> {
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications
                    .findBy(exactTextCaseSensitive(INVOICE_PAYMENT_METHOD_IS_REQUIRED_FOR_ENGAGE_SIGNUP_ERROR))
                    .shouldNot(exist);
            opportunityPage.processOrderModal.engagePreparingDataActiveStep.shouldHave(exactTextCaseSensitive(DATA_VALIDATION_STEP));

            opportunityPage.processOrderModal.closeWindow();
        });

        //  CRM-20656
        step("4. Clear Office Account's RC_User_ID__c and RC_Account_Status__c fields via API", () -> {
            //  this will simulate that the Office Account is not signed up
            var officeAccountToUpdate = new Account();
            officeAccountToUpdate.setId(officeAccount.getId());
            officeAccountToUpdate.setFieldsToNull(new String[]{"RC_User_ID__c", "RC_Account_Status__c"});
            enterpriseConnectionUtils.update(officeAccountToUpdate);
        });

        step("5. Click 'Close' button on the Opportunity record page, " +
                        "and check that the error notification 'Invoice payment method is required...' is not displayed",
                this::stepCheckCloseClickButton
        );

        step("6. Set Office Account.Payment_Method__c = 'Invoice' via API", () -> {
            officeAccount.setPayment_Method__c(INVOICE_PAYMENT_METHOD);
            enterpriseConnectionUtils.update(officeAccount);
        });

        step("7. Click 'Close' button on the Opportunity record page, " +
                        "and check that the error notification 'Invoice payment method is required...' is not displayed",
                this::stepCheckCloseClickButton
        );

        step("8. Create Invoice Approval for the Office Account " +
                "with related 'Accounts Payable' AccountContactRole record via API", () -> {
            officeAccountApproval = createInvoiceApproval(officeOpportunity, officeAccount, officeContact, salesRepUser.getId(), false);
        });

        step("9. Click 'Close' button on the Opportunity record page, " +
                        "and check that the error notification 'Invoice payment method is required...' is not displayed",
                this::stepCheckCloseClickButton
        );

        step("10. Set Office Account's Approval__c.Status__c = 'PendingL1Approval' via API", () -> {
            officeAccountApproval.setStatus__c(APPROVAL_STATUS_PENDING_L1);
            enterpriseConnectionUtils.update(officeAccountApproval);
        });

        step("11. Click 'Close' button on the Opportunity record page, " +
                        "and check that the error notification Invoice payment method is required...' is not displayed",
                this::stepCheckCloseClickButton
        );

        step("12. Set Office Account's Approval__c.Status__c = 'Approved' " +
                "and Office Account.Payment_Method__c != 'Invoice' via API", () -> {
            officeAccountApproval.setStatus__c(APPROVAL_STATUS_APPROVED);
            officeAccount.setPayment_Method__c(CREDIT_CARD_PAYMENT_METHOD);
            enterpriseConnectionUtils.update(officeAccountApproval, officeAccount);
        });

        step("13. Click 'Close' button on the Opportunity record page, " +
                        "and check that the error notification 'Invoice payment method is required...' is not displayed",
                this::stepCheckCloseClickButton
        );

        step("14. Set Engage Account.Payment_Method__c = 'Invoice' via API", () -> {
            engageAccount.setPayment_Method__c(INVOICE_PAYMENT_METHOD);
            enterpriseConnectionUtils.update(engageAccount);
        });

        step("15. Click 'Close' button on the Opportunity record page, " +
                        "and check that the error notification 'Invoice payment method is required...' is not displayed",
                this::stepCheckCloseClickButton
        );
    }

    /**
     * Check absence of notification about approved Invoice Approval requirement for Master Account
     * after clicking 'Close' button on the Opportunity record page.
     */
    private void stepCheckCloseClickButton() {
        step("Click 'Close' button, check that expected error notification is not displayed " +
                "and close Opportunity Close modal window", () -> {
            opportunityPage.clickCloseButton();

            opportunityPage.alertNotificationBlock.shouldBe(visible, ofSeconds(60));
            opportunityPage.notifications
                    .findBy(exactTextCaseSensitive(INVOICE_PAYMENT_METHOD_IS_REQUIRED_FOR_ENGAGE_SIGNUP_ERROR))
                    .shouldNot(exist, ofSeconds(1));

            opportunityPage.closeErrorAlertNotifications();
            opportunityPage.closeOpportunityModal.closeWindow();
        });
    }
}