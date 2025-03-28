package com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.ProServQuotingWizardPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * Modal window in {@link ProServQuotingWizardPage} ("ProServ Quote" tab)
 * activated by clicking on "Cancel CC ProServ Engagement" button for CC ProServ cancellation.
 * <p>
 * After cancelling the CC ProServ Engagement, a CC ProServ Quote's "ProServ Status" field is set to "Cancelled".
 * </p>
 */
public class CancelCcProServModal {

    //  Page elements
    private final SelenideElement dialogContainer = $("[data-aura-class='cQuotingToolEngageProServ'][role='dialog']");

    //  Buttons
    public final SelenideElement submitButton = dialogContainer.$(byText("Submit"));
}
