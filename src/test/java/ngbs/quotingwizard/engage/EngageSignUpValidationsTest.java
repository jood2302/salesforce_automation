package ngbs.quotingwizard.engage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Approval__c;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.opportunityPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApproval;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.CREDIT_CARD_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_APPROVED;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_PENDING_L1;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.closeWindow;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("Engage")
@Tag("SignUp")
public class EngageSignUpValidationsTest extends BaseTest {
    private final Steps steps;
    private final EngageValidationSteps engageValidationSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUser;
    private Approval__c approval;

    public EngageSignUpValidationsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_ED_Standalone_Annual_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        engageValidationSteps = new EngageValidationSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);

        engageValidationSteps.createEngageActiveAgreementViaQuoteWizard(steps.quoteWizard.opportunity);
    }

    @Test
    @TmsLink("CRM-20651")
    @DisplayName("CRM-20651 - Engage Digital Account - Sign Up Validation - Payment Method")
    @Description("Verify that if there are no Approved Invoicing Request approvals under the Engage Account " +
            "and current Account Payment Method != 'Invoice' then on Process Order button click on Engage Opportunity " +
            "notification should be shown in Process Order modal window: 'Invoice payment method is required.'")
    public void test() {
        step("1. Click 'Process Order' button on the Opportunity record page, " +
                        "and check the error notification 'Invoice payment method is required...'",
                this::stepCheckProcessOrderClickButton
        );

        step("2. Set Account.Payment_Method__c = 'Invoice' via API", () -> {
            steps.salesFlow.account.setPayment_Method__c(INVOICE_PAYMENT_METHOD);
            enterpriseConnectionUtils.update(steps.salesFlow.account);
        });

        step("3. Click 'Process Order' button on the Opportunity record page, " +
                        "and check the error notification 'Invoice payment method is required...'",
                this::stepCheckProcessOrderClickButton
        );

        step("4. Create Invoice Approval for the Engage Opportunity " +
                "with related 'Accounts Payable' AccountContactRole record via API", () -> {
            approval = createInvoiceApproval(steps.quoteWizard.opportunity, steps.salesFlow.account,
                    steps.salesFlow.contact, dealDeskUser.getId(), false);
        });

        step("5. Click 'Process Order' button on the Opportunity record page, " +
                        "and check the error notification 'Invoice payment method is required...'",
                this::stepCheckProcessOrderClickButton
        );

        step("6. Set Approval__c.Status__c = 'PendingL1Approval' via API", () -> {
            approval.setStatus__c(APPROVAL_STATUS_PENDING_L1);
            enterpriseConnectionUtils.update(approval);
        });

        step("7. Click 'Process Order' button on the Opportunity record page, " +
                        "and check the error notification 'Invoice payment method is required...'",
                this::stepCheckProcessOrderClickButton
        );

        step("8. Set Approval__c.Status__c = 'Approved' " +
                "and Account.Payment_Method__c != 'Invoice' (e.g. 'Credit Card') via API", () -> {
            approval.setStatus__c(APPROVAL_STATUS_APPROVED);
            steps.salesFlow.account.setPayment_Method__c(CREDIT_CARD_PAYMENT_METHOD);
            enterpriseConnectionUtils.update(approval, steps.salesFlow.account);
        });

        step("9. Click 'Process Order' button on the Opportunity record page, " +
                        "and check the error notification 'Invoice payment method is required...'",
                this::stepCheckProcessOrderClickButton
        );

        step("10. Set Account.Payment_Method__c = 'Invoice' via API", () -> {
            steps.salesFlow.account.setPayment_Method__c(INVOICE_PAYMENT_METHOD);
            enterpriseConnectionUtils.update(steps.salesFlow.account);
        });

        step("11. Click 'Process Order' button on the Opportunity record page, " +
                        "and check the error notification 'Invoice payment method is required...'",
                this::stepCheckProcessOrderClickButton
        );

        step("12. Switch back to the quote in the Quote Wizard, " +
                "open the Quote Details tab, set 'Payment Method' = 'Invoice', save changes, " +
                "and update the Quote to the Active Agreement via API", () -> {
            switchTo().window(1);
            quotePage.openTab();
            quotePage.selectPaymentMethod(INVOICE_PAYMENT_METHOD);
            quotePage.saveChanges();
            closeWindow();

            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);
        });

        step("13. Click 'Process Order' button on the Opportunity record page, " +
                "and check that there's no error notification, and that Preparing Data step is passed successfully", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.engagePreparingDataActiveStep
                    .shouldHave(exactText(READY_TO_REQUEST_FUNNEL_STEP), ofSeconds(120));
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(hidden);
        });
    }

    /**
     * Check error notification in the Process Order modal window
     * after clicking 'Process Order' button on the Opportunity record page.
     */
    private void stepCheckProcessOrderClickButton() {
        step("Click 'Process Order' button, " +
                "check that the expected error notification is displayed in the Process Order modal window on the 'Data validation' step, " +
                "and close the modal window", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications
                    .shouldHave(itemWithText(format(INVOICE_PAYMENT_METHOD_IS_REQUIRED_FOR_ENGAGE_SIGNUP_ERROR, ENGAGE_DIGITAL_SERVICE)), ofSeconds(1));
            opportunityPage.processOrderModal.engagePreparingDataActiveStep
                    .shouldHave(exactTextCaseSensitive(DATA_VALIDATION_STEP));
            opportunityPage.processOrderModal.closeWindow();
        });
    }
}
