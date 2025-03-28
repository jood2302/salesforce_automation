package ngbs.quotingwizard.billonbehalf;

import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import ngbs.quotingwizard.CartTabSteps;
import ngbs.quotingwizard.QuoteWizardSteps;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.SYNCED_STEP;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_APPROVED;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Selenide.closeWindow;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

/**
 * Test methods for test cases with Engage packages, with 'Invoice-on-Behalf' payment method and without master Office Account.
 */
//  TODO Leave this test steps class until the related test case CRM-25712 should be automated again
public class EngageWithoutOfficeBillOnBehalfSteps {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final QuoteWizardSteps quoteWizardSteps;
    private final CartTabSteps cartTabSteps;

    //  Test data
    private final String serviceType;
    private final String renewalTerm;

    /**
     * New instance of test methods for test cases with Engage packages,
     * with 'Invoice-on-Behalf' payment method and without master Office Account.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public EngageWithoutOfficeBillOnBehalfSteps(Dataset data) {
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        quoteWizardSteps = new QuoteWizardSteps(data);
        cartTabSteps = new CartTabSteps(data);

        serviceType = data.packageFolders[0].name;
        renewalTerm = data.packageFolders[0].packages[0].contractTerms.renewalTerm;
    }

    /**
     * <p> - Populate Account.Service_Type__c, RC_Service_name__c fields according to test data </p>
     * <p> - Set Invoice-on-Behalf Approval__c.Status = 'Approved' </p>
     *
     * @param account                 an Engage Account to set up
     * @param invoiceOnBehalfApproval an Invoice-on-Behalf Approval to set up
     */
    public void setUpBaseEngageWithoutOfficeBillOnBehalfTest(Account account, Approval__c invoiceOnBehalfApproval) {
        step("Populate Account's Service_Type__c and RC_Service_name__c fields via API", () -> {
            account.setService_Type__c(serviceType);
            account.setRC_Service_name__c(serviceType);
            enterpriseConnectionUtils.update(account);
        });

        step("Set Invoice-on-Behalf Approval__c.Status = 'Approved' via API", () -> {
            invoiceOnBehalfApproval.setStatus__c(APPROVAL_STATUS_APPROVED);
            enterpriseConnectionUtils.update(invoiceOnBehalfApproval);
        });
    }

    /**
     * Check that Engage 'Bill on Behalf' Opportunity without a linked master Office account can be signed up.
     *
     * @param engageProduct main Engage license to be added to the cart
     * @param opportunity   an Engage 'Bill on Behalf' Opportunity to check
     */
    public void checkEngageBillOnBehalfSignUpTestSteps(Product engageProduct, Opportunity opportunity) {
        var expectedActionsForStep = engageProduct != null
                ? "save changes, add a product, set up its quantity, and save changes on the Price tab"
                : "and save changes";
        step("1. Open the Engage Opportunity, switch to the Quote Wizard, add a new Sales quote, " +
                "select a package for it, " + expectedActionsForStep, () -> {
            quoteWizardSteps.prepareOpportunity(opportunity.getId());

            if (engageProduct != null) {
                quoteWizardSteps.addProductsOnProductsTab(engageProduct);
                cartPage.openTab();
                cartTabSteps.setUpQuantities(engageProduct);
                cartPage.saveChanges();
            }
        });

        step("2. Open the Quote Details tab and select Start Date, Renewal term, and save changes", () -> {
            quotePage.openTab();
            quotePage.setDefaultStartDate();
            quotePage.renewalTermPicklist.selectOption(renewalTerm);
            quotePage.saveChanges();

            closeWindow();
        });

        step("3. Update the Quote to Active Agreement via API", () ->
                quoteWizardSteps.stepUpdateQuoteToApprovedActiveAgreement(opportunity)
        );

        step("4. Click 'Process Order' button on the Engage Opportunity's record page, " +
                "and check that 'Preparing Data' step on the Process Order modal is passed successfully, " +
                "and that no error notifications are shown", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.engagePreparingDataActiveStep
                    .shouldHave(exactTextCaseSensitive(SYNCED_STEP), ofSeconds(60));
            opportunityPage.processOrderModal.errorNotifications.shouldHave(size(0));
        });
    }
}
