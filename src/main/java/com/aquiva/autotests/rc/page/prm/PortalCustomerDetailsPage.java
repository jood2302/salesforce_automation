package com.aquiva.autotests.rc.page.prm;

import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Account;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_PORTAL_URL;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.open;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

/**
 * The page that displays Customer Details ({@link Account}) record information.
 */
public class PortalCustomerDetailsPage {
    private static final String FIELD_XPATH_FORMAT = "//*[./span=\"%s\"]/following-sibling::*[1]";

    public final SelenideElement header = $x("//h2[./*='Customer Details']");

    public final SelenideElement accountName = $x(format(FIELD_XPATH_FORMAT, "Account Name"));
    public final SelenideElement billingStreet = $x(format(FIELD_XPATH_FORMAT, "Billing Street"));
    public final SelenideElement billingCity = $x(format(FIELD_XPATH_FORMAT, "Billing City"));
    public final SelenideElement billingState = $x(format(FIELD_XPATH_FORMAT, "Billing State/Province"));
    public final SelenideElement billingCountry = $x(format(FIELD_XPATH_FORMAT, "Billing Country"));
    public final SelenideElement billingPostalCode = $x(format(FIELD_XPATH_FORMAT, "Billing Postal Code"));
    public final SelenideElement partnerContact = $x(format(FIELD_XPATH_FORMAT, "Partner Contact"));

    /**
     * Open the Customer Details Leads page via direct link.
     *
     * @param accountId ID of the Lead record
     * @return opened Portal Customer Details page reference
     */
    public PortalCustomerDetailsPage openPage(String accountId) {
        open(format("%s/RCPartnerProgram/s/partnercustomers?id=%s", BASE_PORTAL_URL, accountId));
        waitUntilLoaded();
        return this;
    }

    /**
     * Wait until the page loads most of its important elements.
     */
    public void waitUntilLoaded() {
        header.shouldBe(visible, ofSeconds(120));
    }
}