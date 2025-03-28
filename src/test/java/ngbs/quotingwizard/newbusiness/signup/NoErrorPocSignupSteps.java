package ngbs.quotingwizard.newbusiness.signup;

import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import ngbs.quotingwizard.QuoteWizardSteps;

import static base.Pages.*;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Selenide.closeWindow;
import static io.qameta.allure.Allure.step;

/**
 * Test methods for test cases which check
 * that Sign Up for POC Quotes doesn't require Complete Envelope,
 * and doesn't cause any validation errors.
 * <p>
 * Note: only applicable to certain brands!
 */
public class NoErrorPocSignupSteps {
    private final QuoteWizardSteps quoteWizardSteps;
    private final PocSignUpSteps pocSignUpSteps;

    private String pocQuoteId;

    //  Test data
    private final Product digitalLine;

    /**
     * New instance for test methods for test cases which check
     * that Sign Up for POC Quotes doesn't require Complete Envelope,
     * and doesn't cause any validation errors.
     */
    public NoErrorPocSignupSteps(Dataset data) {
        quoteWizardSteps = new QuoteWizardSteps(data);
        pocSignUpSteps = new PocSignUpSteps();

        digitalLine = data.getProductByDataName("LC_DL_75");
    }

    /**
     * Check that no errors are shown when user attempts to sign up a POC Quote without a complete envelope.
     *
     * @param opportunityId Id of the provided Opportunity
     */
    public void noErrorsPocSignUpTestSteps(String opportunityId) {
        step("1. Open the test Opportunity, switch to the Quote Wizard, add a new POC Quote, " +
                "select a package for it, and save changes", () ->
                quoteWizardSteps.preparePocQuote(opportunityId)
        );

        //  Workaround to skip adding devices to the automatically added DLs
        step("2. Remove DigitalLine product on the Price tab, and save changes", () -> {
            cartPage.openTab();
            cartPage.getQliFromCartByDisplayName(digitalLine.name).getDeleteButton().click();
            cartPage.saveChanges();
        });

        step("3. Open the Quote Details tab, populate Main Area Code, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(quoteWizardSteps.localAreaCode);
            quotePage.saveChanges();
        });

        step("4. Create POC Approval via Quote Wizard", () -> {
            quotePage.createPocApproval(pocSignUpSteps.linkToSignedEvaluationAgreement);

            pocQuoteId = wizardPage.getSelectedQuoteId();
            closeWindow();
        });

        step("5. Change POC Approval status to 'Approved' via API", () ->
                pocSignUpSteps.stepSetPocApprovalStatusToApproved(pocQuoteId)
        );

        step("6. Click 'Process Order' button on the Opportunity record page, " +
                "and check that the 'Preparing Data' step is completed, " +
                "and that no error notifications are shown in the Process Order modal window", () -> {
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(hidden);
        });
    }
}
