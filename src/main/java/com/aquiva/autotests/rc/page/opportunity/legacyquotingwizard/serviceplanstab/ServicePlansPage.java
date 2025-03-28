package com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.serviceplanstab;

import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.BaseLegacyQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.ContactCenterQuotingWizardPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

/**
 * 'Service plans' page: one of the tabs on the Legacy Quote Wizard pipeline.
 * <br/><br/>
 * Can be accessed via Legacy Quote Wizard on the 'Main Quote' and 'Contact Center' tabs.
 * <br/><br/>
 * Contains items similar to packages in Quote Wizard 2.0.
 *
 * @see BaseLegacyQuotingWizardPage
 * @see ContactCenterQuotingWizardPage
 */
public class ServicePlansPage extends BaseLegacyQuotingWizardPage {
    public final ElementsCollection servicePlansListEntries = $$("tr.cQuotingToolTierListEntry");

    public final SelenideElement saveAndNextButton = $(byText("Save & Next"));
}
