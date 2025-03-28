package com.aquiva.autotests.rc.page.prm;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byTagAndText;
import static com.codeborne.selenide.Selectors.byTitle;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

/**
 * Global Navigation Bar that can be found on almost all the pages in the PRM Portal.
 * <br/>
 * Provides a menu to see and create different available records (e.g. Deal Registration, Lead, Opportunity)
 */
public class PortalGlobalNavBar {
    //  Profile menu
    public final SelenideElement profileMenu = $("[data-region-name='profileMenu']");
    public final SelenideElement logOutButton = $(byTitle("Logout"));

    public final SelenideElement homeButton = $x("//a[./*='Home']");

    //  Sales
    public final SelenideElement salesButton = $(byTagAndText("button", "Sales"));
    public final SelenideElement dealRegistrationButton = $x("//li[./*='Deal Registration']");
    public final SelenideElement leadsButton = $x("//li[./*='Leads']");
}
