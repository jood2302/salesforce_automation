package com.aquiva.autotests.rc.page.salesforce.setup;

import com.aquiva.autotests.rc.page.salesforce.IframePage;
import com.codeborne.selenide.SelenideElement;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_URL;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

/**
 * Page in the 'Email' section of the 'Setup' that contains email deliverability settings.
 */
public class DeliverabilityPage extends IframePage {

    public static final String ALL_EMAIL_OPTION = "All email";

    public final SelenideElement accessLevelSelect = $("[id$='sendEmailAccessControlSelect']");
    public final SelenideElement saveSuccessMessage = $("[id$='successText']");
    public final SelenideElement saveChangesButton = $("input[value='Save']");

    /**
     * Constructor that defines Deliverability page's location
     * using its iframe's title.
     */
    public DeliverabilityPage() {
        super("Deliverability ~ Salesforce");
    }

    /**
     * Open 'Setup - Email - Deliverability' page via direct link using Base URL.
     * <p> Note: contents for Base URL are usually provided via system properties. </p>
     *
     * @return opened Deliverability Page reference
     */
    public DeliverabilityPage openPage() {
        open(BASE_URL + "/lightning/setup/OrgEmailSettings/home");
        switchToIFrame();
        return this;
    }
}
