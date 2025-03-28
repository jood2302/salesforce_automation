package com.aquiva.autotests.rc.page.prm;

import com.codeborne.selenide.SelenideElement;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_PORTAL_URL;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byTagAndText;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static java.time.Duration.ofSeconds;

/**
 * The page to see all the existing Deal Registration records in the PRM Portal.
 * <br/>
 * User can open the existing records from the list; create new records; etc.
 */
public class DealRegistrationListPage {
    public final SelenideElement header = $(byText("Registered Deals"));
    public final SelenideElement goButton = $(byTagAndText("button", "Go"));
    public final SelenideElement newButton = $(byTagAndText("button", "New"));
    public final SelenideElement newOnBehalfOfButton = $(byTagAndText("button", "New (On behalf Of)"));

    //  Spinner
    public final SelenideElement spinner =  $x("//div[contains(@class,'slds-spinner')]");

    /**
     * Open the Ignite PRM Portal's "Registered Deals" via direct link.
     */
    public void openPage() {
        open(BASE_PORTAL_URL + "/RCPartnerProgram/s/partnerdealregistration");
        waitUntilLoaded();
    }

    /**
     * Wait until the page loads most of its important elements.
     * User may safely interact with any of the page's elements after this method is finished.
     */
    public void waitUntilLoaded() {
        header.shouldBe(visible, ofSeconds(30));
        goButton.shouldBe(visible, ofSeconds(10));
        spinner.shouldBe(hidden, ofSeconds(10));
    }
}
