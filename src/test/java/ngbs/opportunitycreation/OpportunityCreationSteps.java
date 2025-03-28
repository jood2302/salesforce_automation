package ngbs.opportunitycreation;

import com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage;
import com.aquiva.autotests.rc.page.opportunity.OpportunityCreationPage;

import static base.Pages.opportunityCreationPage;
import static base.Pages.opportunityPage;
import static com.codeborne.selenide.Condition.enabled;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

/**
 * Test methods related to creating Opportunity via {@link OpportunityCreationPage}.
 */
public class OpportunityCreationSteps {

    //  Test data
    public final String provisioningDetails;

    public OpportunityCreationSteps() {
        provisioningDetails = "Default Provisioning Details";
    }

    /**
     * Open Quick Opportunity page, set the Close Date and Provisioning Details.
     *
     * @param accountId ID of the Account for which Opportunity is created
     *                  (this Account and its Primary Signatory Contact will be preselected)
     */
    public void openQopAndPopulateRequiredFields(String accountId) {
        step("Open Quick Opportunity creation Page (QOP)", () -> {
            opportunityCreationPage.openPage(accountId);
        });

        step("Set the Close Date and Provisioning Details fields", () -> {
            opportunityCreationPage.populateCloseDate();
            opportunityCreationPage.provisioningDetailsTextArea.setValue(provisioningDetails);
        });
    }

    /**
     * Click 'Continue to Opportunity' button and wait for {@link OpportunityRecordPage} to load.
     */
    public void pressContinueToOpp() {
        //  the button is disabled until 'Select Service Plan' is loaded completely (BI, Brand, Service)
        opportunityCreationPage.continueToOppButton.shouldBe(enabled, ofSeconds(30)).click();
        opportunityCreationPage.switchFromIFrame();
        opportunityPage.waitUntilLoaded();
    }
}