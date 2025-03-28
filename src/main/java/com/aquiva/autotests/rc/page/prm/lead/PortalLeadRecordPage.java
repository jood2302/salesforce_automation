package com.aquiva.autotests.rc.page.prm.lead;

import com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Lead;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_PORTAL_URL;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.withText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

/**
 * The page that displays Lead ({@link Lead}) record information in PRM portal.
 */
public class PortalLeadRecordPage {
    public final SelenideElement header = $("[class='cPartner_Leads'] header");
    public final SelenideElement convertLeadButton = $(withText("Convert Lead"));

    /**
     * Lead Convert page in the iframe that appears after clicking the "Convert Lead" button.
     */
    public final LeadConvertPage leadConvertPageFrame = new LeadConvertPage("Visualforce Page component container");

    /**
     * Open the Portal Lead record page via direct link.
     *
     * @param leadId ID of the Lead record
     * @return opened Portal Lead record Page reference
     */
    public PortalLeadRecordPage openPage(String leadId) {
        open(format("%s/RCPartnerProgram/s/partnerleads?id=%s", BASE_PORTAL_URL, leadId));
        waitUntilLoaded();
        return this;
    }

    /**
     * Wait until the page loads of its header.
     * User may safely interact with any of the page's elements after this method is finished.
     */
    public void waitUntilLoaded() {
        header.shouldBe(visible, ofSeconds(120));
    }
}