package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * This is a modal window in {@link NGBSQuotingWizardPage} ("Main Quote" tab)
 * with form for configuring and requesting Engage Legal Approval activated by clicking on "Engage Legal" button.
 */
public class EngageLegalRequestModal {

    //  String constants used on the form
    public static final String AMENDMENT_ENGAGEMENT_TYPE = "Amendment";

    //  Page elements
    private final SelenideElement dialogContainer = $("engage-legal-modal");

    public final SelenideElement legalEngagementTypeSelect = dialogContainer.$("#select-legal-engagement-type");
    public final SelenideElement legalAccountNameInput = dialogContainer.$("#input-legal-account-name");
    public final SelenideElement askByCustomerTextarea = dialogContainer.$("[formcontrolname='askByCustomer']");

    //  Buttons
    public final SelenideElement closeButton = dialogContainer.$("[title='Close']");
    public final SelenideElement cancelButton = dialogContainer.$(byText("Cancel"));
    public final SelenideElement discardButton = dialogContainer.$(byText("Discard"));
    public final SelenideElement submitButton = dialogContainer.$(byText("Submit"));
}
