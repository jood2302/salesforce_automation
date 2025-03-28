package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * Modal window in {@link NGBSQuotingWizardPage} ("Main Quote" tab)
 * activated by clicking on "X" button for ProServ engagement.
 * <p>
 * After cancelling the ProServ Engagement, a ProServ Quote's "ProServ Status" field is set to "Cancelled".
 * </p>
 */
public class EngageProServCancelModal {

    //  Page elements
    private final SelenideElement dialogContainer = $("cancel-proserv-modal");

    //  Buttons
    public final SelenideElement submitButton = dialogContainer.$(byText("Submit"));
}
