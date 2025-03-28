package com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.ProServQuotingWizardPage;
import com.aquiva.autotests.rc.page.salesforce.GenericSalesforceModal;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;

/**
 * Modal window in {@link ProServQuotingWizardPage} ("ProServ Quote" tab)
 * activated by clicking on "ProServ is Out for Signature" button for ProServ Quote locking
 * and marking it as 'Out for Signature'.
 */
public class MarkProServAsOutForSignatureModal extends GenericSalesforceModal {

    public final SelenideElement markAsOutOfSignatureButton =
            dialogContainer.$(byText("Mark as \"Out for Signature\" and Lock Quote"));

    /**
     * Constructor for the modal window to locate it via its default header.
     */
    public MarkProServAsOutForSignatureModal() {
        super("Mark ProServ as \"Out for Signature\"?");
    }
}
