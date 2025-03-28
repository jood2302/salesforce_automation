package ngbs.quotingwizard.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityShareFactory;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.QuoteFactory.createActiveSalesAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.DOWNSELL_APPROVERS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("Quote")
@Tag("ApprovalProcess")
@Tag("DownsellApproval")
public class InContractDownsellApprovalProcessMrrChangeLessThanThresholdTest extends BaseTest {
    private final Steps steps;

    private String quoteId;

    //  Test data
    private final Product digitalLineUnlimited;
    private final String initialTerm;
    private final Integer decreasedDlQuantity;

    public InContractDownsellApprovalProcessMrrChangeLessThanThresholdTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_Contract_163073013.json",
                Dataset.class);
        steps = new Steps(data);

        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        initialTerm = data.packageFolders[0].packages[0].contractTerms.initialTerm[0];
        decreasedDlQuantity = digitalLineUnlimited.existingQuantity - 1;
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.stepCreateContractInNGBS();
        }

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);

        step("Create an Active Sales Agreement for the test Account's Opportunity via API", () -> {
            createActiveSalesAgreement(steps.quoteWizard.opportunity, initialTerm);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-31905")
    @DisplayName("CRM-31905 - 'In contract downsell' GOA Approval for Quote with delta MRR less than -0.01 USD")
    @Description("Verify that if Delta_MRR__c field <= -0.01 USD on a change order Quote for a contracted Account " +
            "then 'In Contract downsell' GOA approval is required and can be approved")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, and select package for it", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        step("2. Open the Price tab, decrease New Quantity of DigitalLine Unlimited by 1, save changes " +
                "and check the Approval Status and the selected Approver", () -> {
            //  (-25$) for 1 DL * 1 = -25 USD <= -0.01 USD in MRR change (should be less than InContractDownsell__c.Threshold__c)
            cartPage.openTab();
            cartPage.setNewQuantityForQLItem(digitalLineUnlimited.name, decreasedDlQuantity);
            cartPage.saveChanges();

            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(REQUIRED_APPROVAL_STATUS));
            cartPage.approverLabel.shouldHave(exactTextCaseSensitive(REQUIRED_APPROVER_APPROVER_LABEL));
            cartPage.selectedApprover.shouldHave(exactTextCaseSensitive(IN_CONTRACT_DOWNSELL_APPROVER));
        });

        step("3. Open the Quote Details tab, populate Discount Justification, and save changes", () -> {
            quotePage.openTab();
            //  Press tab because 'Discount Justification' field should become unfocused that Save Changes button becomes an Active
            quotePage.discountJustificationTextArea
                    .setValue(TEST_STRING)
                    .unfocus();
            quotePage.saveChanges();
        });

        step("4. Open the Price tab, press 'Submit for Approval' button, " +
                "and check the Approval Status and the selected Approver afterwards", () -> {
            cartPage.openTab();
            cartPage.submitForApproval();

            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(PENDING_L1_APPROVAL_STATUS));
            cartPage.approverLabel.shouldHave(exactTextCaseSensitive(NEXT_APPROVAL_APPROVER_LABEL));
            cartPage.selectedApprover.shouldHave(exactTextCaseSensitive(IN_CONTRACT_DOWNSELL_APPROVER));

            quoteId = wizardPage.getSelectedQuoteId();
        });

        step("5. Re-login as a user from the list of Downsell Approvers, " +
                "manually share the Opportunity with this user via API, re-open the Quote Wizard for the same Quote, " +
                "open the Price tab, and check the buttons there", () -> {
            var downsellApproverUser = getUser().withFullNames(DOWNSELL_APPROVERS).execute();
            steps.sfdc.reLoginAsUser(downsellApproverUser);

            OpportunityShareFactory.shareOpportunity(steps.quoteWizard.opportunity.getId(), downsellApproverUser.getId());

            wizardPage.openPage(steps.quoteWizard.opportunity.getId(), quoteId);
            cartPage.openTab();

            cartPage.approveRejectButton.shouldBe(visible, enabled);
            cartPage.recallApprovalRequestButton.shouldBe(visible, enabled);
        });

        step("6. Press the 'Approve/Reject' button, provide a comment, press the 'Approve' button in the modal, " +
                "and check the Approval Status and the selected Approver afterwards", () -> {
            cartPage.approveQuoteViaApproveRejectModal();

            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(APPROVED_APPROVAL_STATUS));
            cartPage.approverLabel.shouldHave(exactTextCaseSensitive(LAST_APPROVER_APPROVER_LABEL));
            cartPage.selectedApprover.shouldHave(exactTextCaseSensitive(IN_CONTRACT_DOWNSELL_APPROVER));
        });
    }
}
