package ngbs.quotingwizard.billonbehalf;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Contact;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static base.Pages.bobApprovalCreationModal;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContactFactory.createContactForAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.ACCOUNTS_PAYABLE_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_APPROVED;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_PENDING_L1;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContactHelper.getFullName;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.CHANNEL_OPERATIONS_LIGHTNING_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("Bill-on-Behalf")
public class BobCustomerAccountInvoicingRequestChangeApcFieldTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User channelOperationsUser;
    private Contact secondAccountsPayableContact;

    public BobCustomerAccountInvoicingRequestChangeApcFieldTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_BillOnBehalfPM_82749013.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        steps.ngbs.isGenerateAccountsForSingleTest = true;
    }

    @BeforeEach
    public void setUpTest() {
        //  TODO Test Issue PBC-19777 (Need to update the "scenario" in test data with the new AGS 2.0 scenario for it)
        steps.ngbs.generateBillingAccount();

        step("Find a user with 'Channel Operations – Lightning' profile", () -> {
            channelOperationsUser = getUser().withProfile(CHANNEL_OPERATIONS_LIGHTNING_PROFILE).execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(channelOperationsUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, channelOperationsUser);

        steps.billOnBehalf.setUpBillOnBehalfSteps(steps.salesFlow.account, steps.salesFlow.contact,
                steps.quoteWizard.opportunity, channelOperationsUser);

        step("Set 'Invoice-on-behalf Request' Approval's Status = 'Approved' via API", () -> {
            steps.billOnBehalf.invoiceOnBehalfApproval.setStatus__c(APPROVAL_STATUS_APPROVED);
            enterpriseConnectionUtils.update(steps.billOnBehalf.invoiceOnBehalfApproval);
        });

        step("Login as a user with 'Channel Operations – Lightning' profile", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(channelOperationsUser)
        );
    }

    @Test
    @Tag("KnownIssue")
    @Issue("BZS-11634")
    @Tag("TestIssue")
    @Issue("PBC-19777")
    @TmsLink("CRM-24580")
    @DisplayName("CRM-24580 - Invoicing request for BoB Customer Accounts (Change only APC)")
    @Description("Verify that when BoB Customer changed to a non-BoB Customer, " +
            "Invoicing Request automatically generated in 'PendingL1Approval' status")
    public void test() {
        step("1. Open Customer Account, click 'Account Viewer' button, " +
                "proceed to 'Move to Invoice', check the text in the appeared pop-up window, " +
                "in the opened 'Invoicing Request' creation modal check the Limits, Invoice, and Payment terms values", () ->
                steps.billOnBehalf.prepareDataToSwitchBoBToInvoiceTestSteps(steps.salesFlow.account, steps.quoteWizard.opportunity)
        );

        step("2. Create the second Contact with 'Accounts Payable' AccountContactRole for the Customer Account via API", () -> {
            secondAccountsPayableContact = createContactForAccount(steps.salesFlow.account, channelOperationsUser);
            createAccountContactRole(steps.salesFlow.account, secondAccountsPayableContact, ACCOUNTS_PAYABLE_ROLE, false);
        });

        step("3. Set a different contact in 'Accounts Payable Contact' using previously created second Contact", () -> {
            bobApprovalCreationModal.accountsPayableContactSearchInput.getSelf().scrollIntoView(true);
            bobApprovalCreationModal.accountsPayableContactSearchInput
                    .selectItemInCombobox(getFullName(secondAccountsPayableContact));
        });

        step("4. Click 'Save' button, " +
                "and check that created 'Invoicing Request' Approval is in 'PendingL1Approval' status", () ->
                //  TODO Known Issue BZS-11634 ('Save' button with the modal should be hidden after save, but it's visible due to error 'NO_APPLICABLE_PROCESS'/'MANAGER_NOT_DEFINED')
                steps.billOnBehalf.clickSaveButtonAndCheckInvoicingRequestTestSteps(APPROVAL_STATUS_PENDING_L1,
                        steps.salesFlow.account.getId())
        );

        step("5. Set Status__c = 'Approved' for the created Invoicing Request Approval via API", () -> {
            steps.billOnBehalf.invoiceApproval.setStatus__c(APPROVAL_STATUS_APPROVED);
            enterpriseConnectionUtils.update(steps.billOnBehalf.invoiceApproval);
        });

        step("6. Check that the Payment method on the Customer Account is changed to 'Invoice' in both NGBS and SFDC, " +
                "and check the Account's Partner-related fields in SFDC", () ->
                steps.billOnBehalf.checkUpdatedInfoOnAccountsTestSteps(steps.salesFlow.account.getId())
        );
    }
}
