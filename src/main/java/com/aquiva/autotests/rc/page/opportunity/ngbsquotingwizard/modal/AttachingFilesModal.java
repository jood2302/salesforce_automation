package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

/**
 * This is a modal window in {@link NGBSQuotingWizardPage} (Quote Details tab)
 * with names of documents attached to the Opportunity activated by clicking on "Attach all to Opportunity" button.
 */
public class AttachingFilesModal {
    private final SelenideElement dialogContainer = $("pdf-attach");

    public final ElementsCollection successAttachedIcons = $$("[iconname='success']");
    public final SelenideElement closeButton = dialogContainer.$("footer").$(byText("Close"));
}
