package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.packagetab.PackagePage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * Modal window in {@link NGBSQuotingWizardPage} with warning message
 * when contract is unselected on some Quotes.
 * <p>
 * Can be seen when deselecting contract checkbox on the {@link PackagePage}
 * or via {@link BillingDetailsAndTermsModal},
 * and ONLY for the Existing Business Customer quotes.
 * </p>
 */
public class WarningConfirmationModal {
    public final SelenideElement dialogContainer = $("confirmation-modal .slds-modal__container");

    //  Buttons
    public final SelenideElement closeButton = dialogContainer.$("[title='Close']");
    public final SelenideElement cancelButton = dialogContainer.$(byText("Cancel"));
    public final SelenideElement confirmButton = dialogContainer.$(byText("Confirm"));
}
