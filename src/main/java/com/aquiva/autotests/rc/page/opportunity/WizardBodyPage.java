package com.aquiva.autotests.rc.page.opportunity;

import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.*;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NgbsQuoteSelectionQuoteWizardPage;
import com.aquiva.autotests.rc.page.salesforce.VisualforcePage;
import com.codeborne.selenide.SelenideElement;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_VF_URL;
import static com.codeborne.selenide.Selenide.*;

/**
 * Page with custom Quote Wizard functionality.
 * <br/>
 * It contains several tabs:
 * <p> - Main Quote (Quote Wizard 2.0 / NGBS) </p>
 * <p> - Contact Center (Legacy Quote Wizard) </p>
 * <p> - ProServ Quote (Legacy Quote Wizard) </p>
 * <br/>
 * Normally, Quote Wizard is located on the {@link OpportunityRecordPage}.
 * But it also can be opened via direct link (as any VF page).
 */
public class WizardBodyPage extends VisualforcePage {

    //  Spinner
    public final SelenideElement spinner = $("spinner div.slds-spinner");
    public final SelenideElement spinnerContainer = $(".slds-spinner_container");

    //  Tab buttons
    public final SelenideElement mainQuoteTab = $("#main-tab");
    public final SelenideElement contactCenterTab = $("#cc-tab");
    public final SelenideElement proServTab = $("#proserv-tab");

    //  Tabs
    public final NgbsQuoteSelectionQuoteWizardPage mainQuoteSelectionWizardPage = new NgbsQuoteSelectionQuoteWizardPage();
    public final NGBSQuotingWizardPage mainQuoteWizardPage = new NGBSQuotingWizardPage();
    public final BaseLegacyQuotingWizardPage mainQuoteLegacyWizardPage = new BaseLegacyQuotingWizardPage();
    public final ContactCenterQuotingWizardPage contactCenterWizardPage = new ContactCenterQuotingWizardPage();
    public final ProServQuotingWizardPage proServWizardPage = new ProServQuotingWizardPage();

    //  Wizard Placeholder
    public final SelenideElement wizardPlaceholder = $(".placeholder-text");

    /**
     * Default no-arg constructor.
     * Defines default Quote Wizard location on the Opportunity record page
     * (for Sales Reps, Deal Desk agents, etc...).
     */
    public WizardBodyPage() {
        super($x("//article//iframe[@title='Quote Wizard']"));
    }

    /**
     * Open Quote Wizard page via direct link.
     * QW is opened in the Visualforce container without any additional wrappers
     * (iframes, web components, etc...).
     *
     * @param opportunityId Id of the Opportunity record for which the Quote Wizard is being open
     * @return reference to the opened Quote Wizard page
     */
    public WizardBodyPage openPage(String opportunityId) {
        open(BASE_VF_URL + "/apex/QuoteWizard?id=" + opportunityId);
        return this;
    }
}
