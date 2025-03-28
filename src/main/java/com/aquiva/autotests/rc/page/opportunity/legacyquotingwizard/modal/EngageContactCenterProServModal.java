package com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.ContactCenterQuotingWizardPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * Modal window in {@link ContactCenterQuotingWizardPage} (Legacy Quote Wizard, "Contact Center" tab)
 * activated by clicking on "Engage CC ProServ" button.
 * <p>
 * After engaging "Contact Center professional services", a <b>CC ProServ Quote</b> is created
 * and can be accessed via Quote Wizard ("ProServ Quote" tab)
 * by user with profile = <b>"Professional Services"</b>.
 * </p>
 */
public class EngageContactCenterProServModal {

    //  String constants used on the form
    public static final String HEADER = "Engage Contact Center Professional Services";
    public static final String LABEL = "Please provide additional details that Professional Services team should know about";

    //  Page elements
    private final SelenideElement dialogContainer = $("[data-aura-class='cQuotingToolEngageProServ'][role='dialog']");

    //  Buttons
    public final SelenideElement closeButton = dialogContainer.$("[title='Close window.']");
    public final SelenideElement submitButton = dialogContainer.$(byText("Submit"));
    public final SelenideElement cancelButton = dialogContainer.$(byText("Cancel"));
}
