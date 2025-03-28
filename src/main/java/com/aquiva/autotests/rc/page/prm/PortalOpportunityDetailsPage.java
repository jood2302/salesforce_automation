package com.aquiva.autotests.rc.page.prm;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NgbsQuoteSelectionQuoteWizardPage;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Opportunity;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_PORTAL_URL;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.open;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

/**
 * The page that displays Opportunity Details ({@link Opportunity}) record information.
 * Contains details about the Opportunities accessed by the partners.
 */
public class PortalOpportunityDetailsPage {
    private static final String FIELD_XPATH_FORMAT = "//*[./span=\"%s\"]/following-sibling::*[1]";

    public final SelenideElement header = $x("//h2[./*='Opportunity Detail']");
    public final SelenideElement createCaseButton = $x("//button[text()='Create a Case']");

    public final SelenideElement name = $x(format(FIELD_XPATH_FORMAT, "Name"));
    public final SelenideElement partnerContact = $x(format(FIELD_XPATH_FORMAT, "Partner Contact"));
    public final SelenideElement brandName = $x(format(FIELD_XPATH_FORMAT, "Brand Name"));
    public final SelenideElement numberOfEmployees = $x(format(FIELD_XPATH_FORMAT, "Number of Employees"));
    public final SelenideElement closeDate = $x(format(FIELD_XPATH_FORMAT, "Close Date"));
    public final SelenideElement existingSolutionProvider = $x(format(FIELD_XPATH_FORMAT, "Existing Solution Provider"));
    public final SelenideElement whatIsPromptingChange = $x(format(FIELD_XPATH_FORMAT, "What's prompting change?"));
    public final SelenideElement partnerId = $x(format(FIELD_XPATH_FORMAT, "Partner ID"));
    public final SelenideElement tierName = $x(format(FIELD_XPATH_FORMAT, "Tier Name"));
    public final SelenideElement forecastedUsers = $x(format(FIELD_XPATH_FORMAT, "Forecasted Users"));
    public final SelenideElement description = $x(format(FIELD_XPATH_FORMAT, "Description"));

    //  Quote section
    public final NgbsQuoteSelectionQuoteWizardPage quoteSelectionWizardPageFrame =
            new NgbsQuoteSelectionQuoteWizardPage("Visualforce Page component container");

    /**
     * Open the Portal Opportunity Details page via direct link.
     *
     * @param opportunityId ID of provided Opportunity
     * @return opened Portal Opportunity Details Page reference
     */
    public PortalOpportunityDetailsPage openPage(String opportunityId) {
        open(format("%s/RCPartnerProgram/s/partneropportunities?id=%s", BASE_PORTAL_URL, opportunityId));
        waitUntilLoaded();
        return this;
    }

    /**
     * Wait until the page loads most of its important elements.
     */
    public void waitUntilLoaded() {
        header.shouldBe(visible, ofSeconds(280));
    }
}