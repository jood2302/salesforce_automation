package ngbs.quotingwizard.billonbehalf;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Opportunity;
import ngbs.quotingwizard.newbusiness.signup.SalesQuoteSignUpSteps;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage.SUBMIT_INVOICE_ON_BEHALF_REQUEST_APPROVAL_ERROR;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_APPROVED;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_STAGE;
import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("Bill-on-Behalf")
public class BillOnBehalfCloseAndSignUpValidationTest extends BaseTest {
    private final Steps steps;
    private final SalesQuoteSignUpSteps salesQuoteSignUpSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product dlUnlimited;
    private final Product phoneToAdd;
    private final String renewalTerm;

    public BillOnBehalfCloseAndSignUpValidationTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Advanced_Monthly_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        salesQuoteSignUpSteps = new SalesQuoteSignUpSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataName("LC_HD_959");
        renewalTerm = data.packageFolders[0].packages[0].contractTerms.renewalTerm;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUserWithPermissionSet = salesQuoteSignUpSteps.getSalesRepUserWithAllowedProcessOrderWithoutShipping();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithPermissionSet);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithPermissionSet);

        steps.billOnBehalf.setUpBillOnBehalfSteps(steps.salesFlow.account, steps.salesFlow.contact,
                steps.quoteWizard.opportunity, salesRepUserWithPermissionSet);
        salesQuoteSignUpSteps.loginAsSalesRepUserWithAllowedProcessOrderWithoutShipping();
    }

    @Test
    @TmsLink("CRM-30421")
    @DisplayName("CRM-30421 - Invoice-on-behalf Request validations on 'Close' and 'Process Order'")
    @Description("Verify that the Invoice-on-behalf Request should be approved for the Opportunity to be closed or signed up")
    public void test() {
        step("1. Open the test Opportunity, create a new Sales Quote from the Quote Wizard, " +
                "add phones, assign phones to DLs, and set up a quote to become an Active Agreement", () ->
                steps.billOnBehalf.prepareOpportunityToBeClosedAndSignedUp(steps.quoteWizard.opportunity, phoneToAdd,
                        dlUnlimited, renewalTerm)
        );

        step("2. Click on 'Close' button on the Opportunity record page " +
                "and check the error message about Invoice-on-Behalf request", () -> {
            opportunityPage.clickCloseButton();

            opportunityPage.alertNotificationBlock.shouldBe(visible, ofSeconds(30));
            opportunityPage.notifications
                    .shouldHave(exactTexts(SUBMIT_INVOICE_ON_BEHALF_REQUEST_APPROVAL_ERROR), ofSeconds(1));
            opportunityPage.alertCloseButton.click();
            opportunityPage.alertNotificationBlock.shouldBe(hidden);
            opportunityPage.closeOpportunityModal.closeWindow();
        });

        step("3. Click 'Process Order' button on the Opportunity record page, " +
                "check the error message on the 'Preparing Data - Data validation' step in the Process Order modal, " +
                "and close the modal window", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications.shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    format(OBTAIN_INVOICE_ON_BEHALF_PAYMENT_APPROVAL_ERROR, MVP_SERVICE)), ofSeconds(1));
            opportunityPage.processOrderModal.mvpPreparingDataActiveStep.shouldHave(exactTextCaseSensitive(DATA_VALIDATION_STEP));

            opportunityPage.processOrderModal.closeWindow();
        });

        step("4. Set status of 'Invoice-on-behalf Request' approval to 'Approved' via API", () -> {
            steps.billOnBehalf.invoiceOnBehalfApproval.setStatus__c(APPROVAL_STATUS_APPROVED);
            enterpriseConnectionUtils.update(steps.billOnBehalf.invoiceOnBehalfApproval);
        });

        //  If the Opportunity's not closed automatically, and the Close Wizard is shown, 
        //  make sure that the Account.Segment_Name__c != 'Mid Market' or 'Enterprise' or 'Majors'
        step("5. Click 'Close' button on the Opportunity's record page, " +
                "and check that Opportunity.StageName = '7. Closed Won'", () -> {
            opportunityPage.clickCloseButton();
            opportunityPage.spinner.shouldBe(visible, ofSeconds(10));
            opportunityPage.spinner.shouldBe(hidden, ofSeconds(30));
            opportunityPage.alertNotificationBlock.shouldNot(exist);

            var updatedOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, StageName " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Opportunity.class);
            assertThat(updatedOpportunity.getStageName())
                    .as("Opportunity.StageName value")
                    .isEqualTo(CLOSED_WON_STAGE);
        });

        step("6. Press 'Process Order' button on the Opportunity's record page, " +
                "verify that 'Preparing Data' step is completed, and Timezone selector is enabled for a user", () -> {
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();

            opportunityPage.processOrderModal.selectTimeZonePicklist.getInput().shouldBe(enabled, ofSeconds(30));
        });
    }
}
