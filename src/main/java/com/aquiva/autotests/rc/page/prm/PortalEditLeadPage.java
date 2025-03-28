package com.aquiva.autotests.rc.page.prm;

import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Lead;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_PORTAL_URL;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.open;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

/**
 * The page that displays Lead ({@link Lead}) record information.
 */
public class PortalEditLeadPage {
    public static final String LIGHTNING_OUTPUT_FIELD_FORMAT = "//*[./*=\"%s\"]//lightning-output-field";
    public static final String LIGHTNING_FORMATTED_TEXT_FORMAT = "//*[./*=\"%s\"]//lightning-formatted-text";

    public final SelenideElement header = $x("//h2[./*='Edit Leads']");

    public final SelenideElement firstName = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "First Name"));
    public final SelenideElement lastName = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Last Name"));
    public final SelenideElement emailAddress = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Email Address"));
    public final SelenideElement phoneNumber = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Phone Number"));
    public final SelenideElement street = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Street"));
    public final SelenideElement city = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "City"));
    public final SelenideElement stateOrProvince = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "State/Province"));
    public final SelenideElement postalCode = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Postal Code"));
    public final SelenideElement billingCountry = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Billing Country"));
    public final SelenideElement tier = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Tier Name"));
    public final SelenideElement brandName = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Brand Name"));
    public final SelenideElement numberOfUsers = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Number Of Users"));
    public final SelenideElement numberOfEmployees = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Number Of Employees"));
    public final SelenideElement industry = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Industry"));
    public final SelenideElement estimatedCloseDate = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Estimated Close Date"));
    public final SelenideElement howDidYouAcquireThisLead = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "How did you acquire this Lead?"));
    public final SelenideElement description = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Description"));
    public final SelenideElement website = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Website"));
    public final SelenideElement whatIsPromptingChange = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "What's prompting change?"));
    public final SelenideElement competitors = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Competitors"));
    public final SelenideElement existingSolutionProvider = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Existing Solution Provider"));
    public final SelenideElement partnerAccount = $x(format(LIGHTNING_FORMATTED_TEXT_FORMAT, "Partner Account"));
    public final SelenideElement partnerContact = $x(format(LIGHTNING_FORMATTED_TEXT_FORMAT, "Partner Contact"));
    public final SelenideElement status = $x(format(LIGHTNING_OUTPUT_FIELD_FORMAT, "Status"));
    public final SelenideElement partnerProgram = $x(format(LIGHTNING_FORMATTED_TEXT_FORMAT, "Partner Program"));

    /**
     * Open the Portal Edit Leads page via direct link.
     *
     * @param leadId ID of the Lead record
     * @return opened Portal Edit Leads Page reference
     */
    public PortalEditLeadPage openPage(String leadId) {
        open(format("%s/RCPartnerProgram/s/partnerleads?id=%s", BASE_PORTAL_URL, leadId));
        waitUntilLoaded();
        return  this;
    }

    /**
     * Wait until the page is loaded.
     * User may safely interact with any of the page's elements after this method is finished.
     */
    public void waitUntilLoaded() {
        header.shouldBe(visible, ofSeconds(120));
    }
}
