package ngbs.approvals;

import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;

import java.math.BigDecimal;

import static base.Pages.invoiceApprovalCreationModal;
import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContactFactory.createContactForAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.ACCOUNTS_PAYABLE_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.INVOICING_REQUEST_RECORD_TYPE;
import static com.codeborne.selenide.Condition.exactValue;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

/**
 * Test methods for test cases related to approval request 'Sign-Up Purchase Limit' calculation functionality.
 */
public class ApprovalRequestSignUpLimitCalculationSteps {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String opportunityId;
    public Quote quote;

    /**
     * New instance for the class with the test methods/steps
     * for test cases related to approval request 'Sign-Up Purchase Limit' calculation functionality.
     */
    public ApprovalRequestSignUpLimitCalculationSteps(Dataset data) {
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    /**
     * Basic precondition for the related test cases:
     * <p> - Create a new Opportunity with a new Account and Contact </p>
     * <p> - Create additional Contact with 'Accounts Payable' AccountContactRole for the Account (for the Approval). </p>
     * <p> - Create a new Sales Quote for the Opportunity with One-Time and Recurring items. </p>
     */
    public void setUpSteps() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        opportunityId = steps.quoteWizard.opportunity.getId();

        //  to be able to create Invoicing Request Approval later in the test
        step("Create a second Contact with 'Accounts Payable' AccountContactRole for the Account via API", () -> {
            var secondAccountsPayableContact = createContactForAccount(steps.salesFlow.account, salesRepUser);
            createAccountContactRole(steps.salesFlow.account, secondAccountsPayableContact, ACCOUNTS_PAYABLE_ROLE, false);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);

        step("Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, add products on the Add Products tab, and save changes on the Price tab", () ->
                steps.cartTab.prepareCartTabViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );
    }

    /**
     * <p> - Open the Opportunity, and proceed to Invoicing Request approval creation modal </p>
     * <p> - Select 'Invoicing Request' type and proceed to approval creation form </p>
     * <p> - Validate Sign-Up Purchase Limit's value on the form </p>
     */
    public void validateApprovalSignUpPurchaseLimitTestSteps() {
        step("Open New Business Opportunity and proceed to Invoicing Request approval creation modal", () -> {
            opportunityPage.openPage(opportunityId);
            opportunityPage.openCreateNewApprovalModal();
        });

        step("Select 'Invoicing Request' type and proceed to Approval creation form", () -> {
            opportunityPage.newApprovalRecordTypeSelectionModal.selectApprovalType(INVOICING_REQUEST_RECORD_TYPE);
            opportunityPage.newApprovalRecordTypeSelectionModal.nextButton.click();

            invoiceApprovalCreationModal.approvalNameInput.shouldBe(visible, ofSeconds(20));
        });

        step("Validate Sign-Up Purchase Limit's value on the modal", () -> {
            quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, TotalPrice, Total_ARR__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + opportunityId + "'",
                    Quote.class);

            var signUpPurchaseLimitCalculated = BigDecimal.valueOf(quote.getTotalPrice())
                    .multiply(BigDecimal.valueOf(1.5));
            var signUpPurchaseLimitExpectedValue = format("%,.2f", signUpPurchaseLimitCalculated);

            invoiceApprovalCreationModal.signUpPurchaseLimitInput.shouldHave(exactValue(signUpPurchaseLimitExpectedValue));
        });
    }
}
