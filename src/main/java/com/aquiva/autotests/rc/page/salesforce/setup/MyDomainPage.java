package com.aquiva.autotests.rc.page.salesforce.setup;

import com.aquiva.autotests.rc.page.salesforce.IframePage;
import com.codeborne.selenide.SelenideElement;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_URL;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;

/**
 * Page in the 'Company Settings' section of the 'Setup' that contains 'My Domain' settings.
 */
public class MyDomainPage extends IframePage {

    public final SelenideElement header = $x("//h1[text()='My Domain Settings']");
    public final SelenideElement editRoutingAndPoliciesButton = $("[id$='editRoutingAndPoliciesButton']");
    public final SelenideElement preventLoginFromTestCheckbox = $("[id$='requireLoginCheckbox']");
    public final SelenideElement savePolicyChangesButton = $("input[value='Save']");

    /**
     * Constructor that defines My Domain page's location
     * using its iframe's title.
     */
    public MyDomainPage() {
        super("My Domain Settings ~ Salesforce");
    }

    /**
     * Open 'Setup - Company Settings - My Domain' page via direct link using Base URL.
     * <p> Note: contents for Base URL are usually provided via system properties. </p>
     *
     * @return opened My Domain Page reference
     */
    public MyDomainPage openPage() {
        open(BASE_URL + "/lightning/setup/OrgDomain/home");
        switchToIFrame();
        waitUntilLoaded();
        return this;
    }

    /**
     * Wait until the component loads most of its important elements.
     * User may safely interact with any of the component's elements after this method is finished.
     */
    public void waitUntilLoaded() {
        header.shouldBe(visible);
        editRoutingAndPoliciesButton.shouldBe(visible, enabled);
    }
}
