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

import static base.Pages.opportunityPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage.INVOICE_PAYMENT_METHOD_IS_REQUIRED_FOR_CLOSE_ERROR;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApproval;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_APPROVED;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_PENDING_L1;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_STAGE;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.closeWindow;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Engage")
@Tag("OpportunityClose")
public class EngageCloseValidationsTest extends BaseTest {
    private final Steps steps;
    private final EngageValidationSteps engageValidationSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUser;
    private Approval__c approval;

    public EngageCloseValidationsTest() {
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
    @TmsLink("CRM-20510")
    @DisplayName("CRM-20510 - New Business Engage Quote's Payment Method")
    @Description("Verify that if there \n" +
            "are no Approved Invoicing Request approvals under the Engage Account\n" +
            "and Account is New business (BillingId == null)\n" +
            "and Active Agreement's 'Intended Payment Method' != 'Invoice'\n" +
            "then after a user clicks 'Close' button on the Engage Opportunity an error notification should be shown: " +
            "'Invoice payment method is required.'")
    public void test() {
        step("1. Click 'Close' button on the Opportunity record page, " +
                        "and check the error notification 'Invoice payment method is required...'",
                this::stepCheckCloseClickButton
        );

        step("2. Create Invoice Approval for the Engage Opportunity " +
                "with the related 'Accounts Payable' AccountContactRole record via API", () -> {
            approval = createInvoiceApproval(steps.quoteWizard.opportunity, steps.salesFlow.account,
                    steps.salesFlow.contact, dealDeskUser.getId(), false);
        });

        step("3. Click 'Close' button on the Opportunity record page, " +
                        "and check the error notification 'Invoice payment method is required...'",
                this::stepCheckCloseClickButton
        );

        step("4. Set Engage Approval__c.Status__c = 'PendingL1Approval' via API", () -> {
            approval.setStatus__c(APPROVAL_STATUS_PENDING_L1);
            enterpriseConnectionUtils.update(approval);
        });

        step("5. Click 'Close' button on the Opportunity record page, " +
                        "and check the error notification 'Invoice payment method is required...'",
                this::stepCheckCloseClickButton
        );

        step("6. Set Engage Approval__c.Status__c = 'Approved' via API", () -> {
            approval.setStatus__c(APPROVAL_STATUS_APPROVED);
            enterpriseConnectionUtils.update(approval);
        });

        step("7. Click 'Close' button on the Opportunity record page, " +
                        "and check the error notification 'Invoice payment method is required...'",
                this::stepCheckCloseClickButton
        );

        step("8. Switch back to the quote in the Quote Wizard, " +
                "open the Quote Details tab, set 'Payment Method' = 'Invoice', save changes, " +
                "and update the Quote to the Active Agreement via API", () -> {
            switchTo().window(1);
            quotePage.openTab();
            quotePage.selectPaymentMethod(INVOICE_PAYMENT_METHOD);
            quotePage.saveChanges();
            closeWindow();

            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);
        });

        //  'Deal Desk Lightning' user can close the Opportunity immediately 
        //  without the Close Wizard and additional validations related to Stage changing
        //  see Opportunity_Close_Setup__mdt.ProfileNames__c for the full list of profiles under validation 
        step("9. Click 'Close' button on the Opportunity record page, check that there's no error notification, " +
                "and check its 'StageName' and 'IsClosed' fields values in DB", () -> {
            opportunityPage.clickCloseButton();
            opportunityPage.spinner.shouldBe(visible, ofSeconds(10));
            opportunityPage.spinner.shouldBe(hidden, ofSeconds(30));
            opportunityPage.alertNotificationBlock.shouldNot(exist);

            var updatedOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, StageName, IsClosed " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Opportunity.class);
            assertThat(updatedOpportunity.getStageName())
                    .as("Opportunity.StageName value")
                    .isEqualTo(CLOSED_WON_STAGE);
            assertThat(updatedOpportunity.getIsClosed())
                    .as("Opportunity.IsClosed value")
                    .isTrue();
        });
    }

    /**
     * Check the error notification after clicking 'Close' button on the Opportunity record page.
     */
    private void stepCheckCloseClickButton() {
        step("Click 'Close' button, check that expected error notification is displayed " +
                "and close Opportunity Close modal window", () -> {
            opportunityPage.clickCloseButton();

            opportunityPage.alertNotificationBlock.shouldBe(visible, ofSeconds(60));
            opportunityPage.notifications
                    .shouldHave(itemWithText(INVOICE_PAYMENT_METHOD_IS_REQUIRED_FOR_CLOSE_ERROR), ofSeconds(1));

            opportunityPage.alertCloseButton.click();
            opportunityPage.closeOpportunityModal.closeWindow();
        });
    }
}
