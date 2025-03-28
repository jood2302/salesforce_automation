package ngbs.approvals;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.approvalPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createCreditLimitIncreaseApproval;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.QuoteFactory.createActiveSalesAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.CREDITCARD_ACCOUNT_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_NEW;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Approval")
public class CreditLimitIncreaseApprovalTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUser;
    private Quote quote;
    private String creditLimitIncreaseApprovalId;

    public CreditLimitIncreaseApprovalTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_Contract_163073013.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.stepCreateContractInNGBS();
        }

        dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);

        step("Set Account.Account_Payment_Method__c = 'CreditCard' via API", () -> {
            steps.salesFlow.account.setAccount_Payment_Method__c(CREDITCARD_ACCOUNT_PAYMENT_METHOD);
            enterpriseConnectionUtils.update(steps.salesFlow.account);
        });

        step("Create an Active Sales Agreement for the test Account's Opportunity via API", () -> {
            quote = createActiveSalesAgreement(steps.quoteWizard.opportunity, data.getInitialTerm());
        });

        step("Create a new Credit Limit Increase Approval for the test Account via API", () -> {
            var creditLimitIncreaseApproval = createCreditLimitIncreaseApproval(steps.salesFlow.account,
                    steps.quoteWizard.opportunity, quote, dealDeskUser.getId());
            creditLimitIncreaseApprovalId = creditLimitIncreaseApproval.getId();
        });

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-38199")
    @DisplayName("CRM-38199 - 'Credit Limit Increase' approval can process different options of 'Credit Card' Payment Method spelling")
    @Description("Verify that if Account.Account_Payment_Method__c populated with 'CreditCard' (without spaces), " +
            "user still can Submit 'Credit Limit Increase' approval. \n" +
            "Previously, if payment method was spelled without spaces like 'CreditCard' - approval was rejected right after the submitting")
    public void test() {
        step("1. Open Credit Increase Approval record page", () -> {
            approvalPage.openPage(creditLimitIncreaseApprovalId);
        });

        step("2. Check that the Approval__c.AccountPaymentMethod__c value " +
                "is equal to Account_Payment_Method__c on the related Account record", () -> {
            var approval = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, AccountPaymentMethod__c " +
                            "FROM Approval__c " +
                            "WHERE Id = '" + creditLimitIncreaseApprovalId + "'",
                    Approval__c.class);
            assertThat(approval.getAccountPaymentMethod__c())
                    .as("Approval__c.AccountPaymentMethod__c value")
                    .isEqualTo(CREDITCARD_ACCOUNT_PAYMENT_METHOD);
        });

        step("3. Submit the record for approval, " +
                "check that its Status has changed, and the approval process is initiated", () -> {
            approvalPage.submitForApproval();

            var approvalUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Status__c " +
                            "FROM Approval__c " +
                            "WHERE Id = '" + creditLimitIncreaseApprovalId + "'",
                    Approval__c.class);
            assertThat(approvalUpdated.getStatus__c())
                    .as("Approval__c.Status__c value")
                    .isNotEqualTo(APPROVAL_STATUS_NEW);

            var approvalProcessInstanceSteps = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM ProcessInstanceStep " +
                            "WHERE ProcessInstance.TargetObjectId = '" + creditLimitIncreaseApprovalId + "'",
                    ProcessInstanceStep.class);
            assertThat(approvalProcessInstanceSteps.size())
                    .as("Approval Process steps for the Approval__c record submitted for approval")
                    .isGreaterThanOrEqualTo(1);
        });
    }
}
