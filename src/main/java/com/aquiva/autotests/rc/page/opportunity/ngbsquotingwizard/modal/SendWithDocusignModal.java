package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

/**
 * Modal window in {@link NGBSQuotingWizardPage} (Quote Details tab)
 * activated by clicking on "Send with Docusign" button.
 * <p>
 * This modal window contains form for sending the quote to the customer via Docusign
 * with documents and recipients contact information.
 */
public class SendWithDocusignModal {
    private final SelenideElement dialogContainer = $("docusign-modal");

    //  'Recipients' section
    public final SelenideElement recipientsListsSection = dialogContainer.$x(".//recipients-list");
    public final SelenideElement recipientContactName = recipientsListsSection.$x(".//div[@class='slds-size_8-of-12']//p[1]");
    public final SelenideElement recipientContactEmail = recipientsListsSection.$x(".//div[@class='slds-size_8-of-12']//p[2]");
}
