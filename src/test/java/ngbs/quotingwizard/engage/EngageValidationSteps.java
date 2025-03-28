package ngbs.quotingwizard.engage;

import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.sforce.soap.enterprise.sobject.Opportunity;
import ngbs.quotingwizard.QuoteWizardSteps;

import static base.Pages.quotePage;
import static io.qameta.allure.Allure.step;

/**
 * Test methods for test cases that set up preconditions and contains methods for checking proper validation
 * during Engage Opportunity's Close and Sign-Up process.
 */
public class EngageValidationSteps {
    private final QuoteWizardSteps quoteWizardSteps;

    /**
     * New instance for the class with the test methods/steps related to checking proper validation
     * during Engage Opportunity's Close and Sign-Up process.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public EngageValidationSteps(Dataset data) {
        quoteWizardSteps = new QuoteWizardSteps(data);
    }

    /**
     * Prepare Engage Account and Opportunity to Close/Sign Up (create a new Quote
     * and update it to Active Agreement).
     *
     * @param opportunity Opportunity for which the Quote Wizard is open
     */
    public void createEngageActiveAgreementViaQuoteWizard(Opportunity opportunity) {
        step("Open the Engage Opportunity, switch to the Quote Wizard, add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                quoteWizardSteps.prepareOpportunity(opportunity.getId())
        );

        step("Open the Quote Details tab, populate the Start Date, and save changes", () -> {
            quotePage.openTab();
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();
        });

        step("Update the Quote to Active Agreement via API", () ->
                quoteWizardSteps.stepUpdateQuoteToApprovedActiveAgreement(opportunity)
        );
    }
}
