package ngbs.approvals;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.Approval__c;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.taxExemptionManagerPage;
import static com.aquiva.autotests.rc.page.salesforce.approval.TaxExemptionManagerPage.*;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createTeaApproval;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_APPROVED;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_REJECTED;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("P1")
@Tag("Approval")
@Tag("TaxExemption")
public class TaxExemptApprovalProcessCompletionTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Approval__c teaApprovalOne;
    private Approval__c teaApprovalTwo;
    private Approval__c teaApprovalThree;

    public TaxExemptApprovalProcessCompletionTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesUser = steps.salesFlow.getSalesRepUser();

        steps.salesFlow.createAccountWithContactAndContactRole(salesUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesUser);

        step("Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("Create three Tax Exempt Approvals for the test Account via API", () -> {
            teaApprovalOne = createTeaApproval(steps.salesFlow.account.getId(), steps.quoteWizard.opportunity.getId(),
                    steps.salesFlow.contact.getId(), salesUser.getId());
            teaApprovalTwo = createTeaApproval(steps.salesFlow.account.getId(), steps.quoteWizard.opportunity.getId(),
                    steps.salesFlow.contact.getId(), salesUser.getId());
            teaApprovalThree = createTeaApproval(steps.salesFlow.account.getId(), steps.quoteWizard.opportunity.getId(),
                    steps.salesFlow.contact.getId(), salesUser.getId());
        });
    }

    @Test
    @TmsLink("CRM-33483")
    @DisplayName("CRM-33483 - Tax Exempt Approval process completion after approving / rejecting Tax Exempt Approval")
    @Description("Verify that Tax Exempt Approval process is completed after approving / rejecting Tax Exempt Approval")
    public void test() {
        step("1. Open the Tax Exemption Manager page for the 1st Tax Exempt Approval, " +
                "select 'Federal tax' and 'State tax' as 'Requested' with a checkbox, save changes, " +
                "and click 'Submit for approval' button", () -> {
            submitTaxExemptionsForApproval(teaApprovalOne.getId());
        });

        step("2. Reject the 1st Tax Exempt Approval via API", () ->
                enterpriseConnectionUtils.rejectSingleRecord(teaApprovalOne.getId())
        );

        step("3. Check the Status and Tax Exemption fields for the 1st Tax Exempt Approval, " +
                "and Tax_Exempt__c and TaxExemptionApprovals__c fields on the related Account", () -> {
            checkUpdatedTaxExemptApproval(teaApprovalOne.getId(), APPROVAL_STATUS_REJECTED);
            checkUpdatedAccount(false);
        });

        step("4. Open the Tax Exemption Manager page for the 2nd Tax Exempt Approval, " +
                "select 'Federal tax' and 'State tax' as 'Requested' with a checkbox, save changes, " +
                "and click 'Submit for approval' button", () -> {
            submitTaxExemptionsForApproval(teaApprovalTwo.getId());
        });

        step("5. Approve the 2nd Tax Exempt Approval via API", () ->
                enterpriseConnectionUtils.approveSingleRecord(teaApprovalTwo.getId())
        );

        step("6. Check the Status and Tax Exemption fields for the 2nd Tax Exempt Approval, " +
                "and Tax_Exempt__c and TaxExemptionApprovals__c fields on the related Account", () -> {
            checkUpdatedTaxExemptApproval(teaApprovalTwo.getId(), APPROVAL_STATUS_APPROVED);
            checkUpdatedAccount(true);
        });

        step("7. Open the Tax Exemption Manager page for the 3rd Tax Exempt Approval, " +
                "select 'Federal tax' and 'State tax' as 'Requested' with a checkbox, save changes, " +
                "and click 'Submit for approval' button", () -> {
            submitTaxExemptionsForApproval(teaApprovalThree.getId());
        });

        step("8. Approve the 3rd Tax Exempt Approval via API", () ->
                enterpriseConnectionUtils.approveSingleRecord(teaApprovalThree.getId())
        );

        step("9. Check the Status and Tax Exemption fields for the 3rd and 2nd Tax Exempt Approvals, " +
                "and Tax_Exempt__c and TaxExemptionApprovals__c fields on the related Account", () -> {
            checkUpdatedTaxExemptApproval(teaApprovalThree.getId(), APPROVAL_STATUS_APPROVED);
            checkUpdatedTaxExemptApproval(teaApprovalTwo.getId(), APPROVAL_STATUS_REJECTED);
            checkUpdatedAccount(true);
        });
    }

    /**
     * Open the Tax Exemption Manager for the given TEA Approval record,
     * select 'Federal tax' and 'State tax' as 'Requested', save changes,
     * and submit the record for approval.
     *
     * @param approvalId ID of the Tax Exempt Approval to submit
     */
    private void submitTaxExemptionsForApproval(String approvalId) {
        taxExemptionManagerPage.openPage(approvalId);
        taxExemptionManagerPage.taxTypesHeaders
                .shouldHave(exactTexts(FEDERAL_TAX_TYPE, STATE_TAX_TYPE, COUNTY_TAX_TYPE, LOCAL_TAX_TYPE));

        taxExemptionManagerPage.setExemptionStatus(FEDERAL_TAX_TYPE, REQUESTED_EXEMPTION_STATUS);
        taxExemptionManagerPage.setExemptionStatus(STATE_TAX_TYPE, REQUESTED_EXEMPTION_STATUS);
        taxExemptionManagerPage.saveChanges();

        taxExemptionManagerPage.clickSubmitForApproval();
    }

    /**
     * Check the Status and Tax Exemption fields on the Approval record
     * after the user accepts/rejects it via standard approval process.
     *
     * @param teaApprovalId  ID of the Tax Exempt Approval to check
     * @param expectedStatus expected value for the Status and Tax Exemption fields (e.g. "Approved")
     */
    private void checkUpdatedTaxExemptApproval(String teaApprovalId, String expectedStatus) {
        assertWithTimeout(() -> {
            var teaApprovalUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Status__c, " +
                            "FederalTaxExemption__c, StateTaxExemption__c " +
                            "FROM Approval__c " +
                            "WHERE Id = '" + teaApprovalId + "'",
                    Approval__c.class);

            assertEquals(expectedStatus, teaApprovalUpdated.getStatus__c(),
                    "Tax Exempt Approval.Status__c value");
            assertEquals(expectedStatus, teaApprovalUpdated.getFederalTaxExemption__c(),
                    "Tax Exempt Approval.FederalTaxExemption__c value");
            assertEquals(expectedStatus, teaApprovalUpdated.getStateTaxExemption__c(),
                    "Tax Exempt Approval.StateTaxExemption__c value");
        }, ofSeconds(30));
    }

    /**
     * Check the Tax Exemption fields on the Account
     * after the user accepts/rejects the related Tax Exempt Approval via standard approval process.
     *
     * @param expectedTaxExemptValue expected value for Tax Exemption fields (e.g. true, false)
     * @throws ConnectionException in case of errors while accessing API
     */
    private void checkUpdatedAccount(boolean expectedTaxExemptValue) throws ConnectionException {
        var accountUpdated = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, Tax_Exempt__c, TaxExemptionApprovals__c " +
                        "FROM Account " +
                        "WHERE Id = '" + steps.salesFlow.account.getId() + "'",
                Account.class);

        assertThat(accountUpdated.getTax_Exempt__c())
                .as("Account.Tax_Exempt__c value")
                .isEqualTo(expectedTaxExemptValue);
        assertThat(accountUpdated.getTaxExemptionApprovals__c())
                .as("Account.TaxExemptionApprovals__c value")
                .isEqualTo(expectedTaxExemptValue);
    }
}
