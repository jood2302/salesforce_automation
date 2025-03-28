package com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.ProServQuotingWizardPage;
import com.aquiva.autotests.rc.page.salesforce.GenericSalesforceModal;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;

/**
 * Modal window in {@link ProServQuotingWizardPage} ("ProServ Quote" tab)
 * activated by clicking on "ProServ is Sold" button.
 */
public class MarkProServAsSoldModal extends GenericSalesforceModal {

    public final SelenideElement markProServAsSoldButton = dialogContainer.$(byText("Mark as \"Sold\""));

    /**
     * Constructor for the modal window to locate it via its default header.
     */
    public MarkProServAsSoldModal() {
        super("Mark ProServ as \"Sold\"?");
    }
}
