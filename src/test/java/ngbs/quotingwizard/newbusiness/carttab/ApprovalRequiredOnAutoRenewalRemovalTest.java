package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.dealqualificationtab.DealQualificationPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.ViewDqApproversModal;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.dealqualificationtab.DealQualificationPage.PENDING_APPROVAL_STATUS;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.ViewDqApproversModal.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitive;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("QuoteTab")
@Tag("DealQualificationTab")
@Tag("Multiproduct-Lite")
public class ApprovalRequiredOnAutoRenewalRemovalTest extends BaseTest {
    private final Steps steps;

    private User salesRepUserFromSohoSegment;

    public ApprovalRequiredOnAutoRenewalRemovalTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_NoPhones.json",
                Dataset.class);
        steps = new Steps(data);
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Sales Rep - Lightning' profile and Segment = 'SOHO'", () -> {
            salesRepUserFromSohoSegment = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withSegment(SOHO_SEGMENT)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserFromSohoSegment);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserFromSohoSegment);

        step("Login as a user with 'Sales Rep - Lightning' profile and Segment = 'SOHO'", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(salesRepUserFromSohoSegment)
        );
    }

    @Test
    @TmsLink("CRM-29378")
    @TmsLink("CRM-31512")
    @DisplayName("CRM-29378 - Approval is required for Auto Renewal removal. \n" +
            "CRM-31512 - Approval details for Auto Renewal approvals")
    @Description("CRM-29378 - Verify that Quote Approval Status is required after Auto-Renewal was switched off. \n" +
            "CRM-31512 - Verify that correct Approvers and Approver details are shown when approval is required for Auto Renewal == false")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Quote Details tab, set the Main Area Code and Discount Justification fields, " +
                "uncheck the Auto-Renewal checkbox, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.setAutoRenewalSelected(false);
            quotePage.discountJustificationTextArea.setValue(TEST_STRING);
            quotePage.saveChanges();
        });

        //  CRM-29378
        step("3. Open the Price tab, and check Quote Approval Status and DQ Approver button", () -> {
            cartPage.openTab();

            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(REQUIRED_APPROVAL_STATUS));
            cartPage.dqApproverButton.shouldHave(cssClass(RED_DQ_APPROVER_BUTTON_CSS_CLASS));
            cartPage.dqApproverButton.shouldHave(exactTextCaseSensitive(FINANCE_REVENUE_DQ_APPROVER));
        });

        step("4. Click DQ Approver button, and check Level, Approval Reason, Approver Type and Approver Name columns " +
                "on the list in the View Approvers modal, and close the modal window", () -> {
            cartPage.dqApproverButton.click();

            //  CRM-31512
            cartPage.viewDqApproversModal.approverLevels.shouldHave(exactTextsCaseSensitive(FINANCE_APPROVAL_LEVEL, EMPTY_STRING));

            //  CRM-29378, CRM-31512
            cartPage.viewDqApproversModal.approvalReasons
                    .shouldHave(exactTextsCaseSensitive(ViewDqApproversModal.AUTO_RENEW_REMOVAL_REQUESTED_BY_DQ,
                            ViewDqApproversModal.AUTO_RENEW_REMOVAL_REQUESTED_BY_DQ));

            //  CRM-31512
            cartPage.viewDqApproversModal.approverTypes
                    .shouldHave(exactTextsCaseSensitive(FINANCE_REVENUE_APPROVER_TYPE, FINANCE_FPA_APPROVER_TYPE));
            cartPage.viewDqApproversModal.approverNames
                    .shouldHave(exactTextsCaseSensitive(FINANCE_REVENUE_APPROVER_NAME, FINANCE_FPA_APPROVER_NAME));

            cartPage.viewDqApproversModal.closeButton.click();
        });

        //  CRM-31512
        step("5. Click 'Submit for Approval' button, open the Deal Qualification tab " +
                "and check 'Status' and 'Approver Reason' columns in Finance subsection", () -> {
            cartPage.submitForApproval();

            dealQualificationPage.openTab();
            dealQualificationPage.financeApprovalStatuses
                    .shouldHave(exactTextsCaseSensitive(PENDING_APPROVAL_STATUS, PENDING_APPROVAL_STATUS));
            dealQualificationPage.financeApproverReasons.shouldHave(exactTextsCaseSensitive(
                    DealQualificationPage.AUTO_RENEW_REMOVAL_REQUESTED_BY_DQ,
                    DealQualificationPage.AUTO_RENEW_REMOVAL_REQUESTED_BY_DQ)
            );
        });
    }
}
