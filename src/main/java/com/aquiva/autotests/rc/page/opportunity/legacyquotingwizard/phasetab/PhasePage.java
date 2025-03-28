package com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.phasetab;

import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.BaseLegacyQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.ProServQuotingWizardPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.SetValueOptions.withDate;
import static java.time.LocalDate.now;

/**
 * 'Phase' page: one of the tabs on the Legacy Quote Wizard pipeline.
 * <br/><br/>
 * Can be accessed via Legacy Quote Wizard on the 'Main Quote' and 'ProServ Quote' tabs.
 * <br/><br/>
 * Contains phases that were added and products that assigned to the phases.
 *
 * @see BaseLegacyQuotingWizardPage
 * @see ProServQuotingWizardPage
 */
public class PhasePage extends ProServQuotingWizardPage {

    public final SelenideElement addPhaseButton = $(byText("Add phase"));
    public final SelenideElement estimatedCompletionDateInput = $x("//div[./p[@title='Estimated Completion Date']]//input");
    public final SelenideElement moveAllUnassignedItemsButton = $(byText("Move all unassigned items here"));
    public final SelenideElement savePhasesButton = $x("(//button[text()='Save'])[2]");

    /**
     * Create a new Phase, add all the cart items to it, and save it.
     */
    public void addAndSavePhase() {
        addPhaseButton.click();

        var tomorrowEstimatedCompletionDate = now().plusDays(1);
        estimatedCompletionDateInput.setValue(withDate(tomorrowEstimatedCompletionDate));

        //  Need to hover over any element on the 'Phase' tab to make 'Move all unassigned items here' button visible
        estimatedCompletionDateInput.hover();
        moveAllUnassignedItemsButton.click();

        savePhasesButton.click();
        waitUntilLoaded();
    }
}
