package com.aquiva.autotests.rc.page.salesforce;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

/**
 * Salesforce page that represents the common elements for a "native" Lightning Experience interface,
 * e.g. Navigation header, App Launcher and a Name, Tabs, etc.
 */
public class SalesforcePage {

    /**
     * Top header that displays...
     * <p> - just <i>"Sandbox: SANDBOX_NAME"</i>,
     * for a main user (System Administrator) </p>
     * or
     * <p> - <i>"Logged in as FirstName LastName (username) |  Sandbox: SANDBOX_NAME | Log out as FirstName LastName"</i>,
     * for a regular user (e.g. Sales Reps) </p>
     */
    public final SelenideElement systemMessageHeader = $(".system-message");
    public final SelenideElement activeAppName = $x("//*[contains(@class,'appName')]");
}
