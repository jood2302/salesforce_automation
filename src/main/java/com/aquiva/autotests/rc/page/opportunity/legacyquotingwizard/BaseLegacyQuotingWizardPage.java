package com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard;

import com.aquiva.autotests.rc.page.components.LegacyDatePicker;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Selectors.byTagAndText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;
import static java.time.Duration.ofSeconds;

/**
 * Base class for Legacy Quoting Wizard Page classes.
 * Contains common elements which might be used in all Legacy Quoting Wizard pages.
 */
public class BaseLegacyQuotingWizardPage {

    //  For 'Quote Type' picklist
    public static final String NEW_CUSTOMER_QUOTE_TYPE = "New Customer";

    //  For 'Quotes' picklist
    public static final String NEW_QUOTE_OPTION = "--New Quote--";
    //  e.g. "02916585 | TestAccount 4b366924-885a-42ce-a288-cea01b3df10f 1 ★"
    public static final String PRIMARY_QUOTE_OPTION_FORMAT = "%s | %s ★";

    //  Quote info
    public final SelenideElement quoteType = $x("//p[@title='Quote type']/following-sibling::p");
    public final SelenideElement quoteMessage = $("p.slds-text-title_caps");

    public final SelenideElement quotesPicklist = $x("//div[label[text()='Quotes']]//select");

    public final SelenideElement billingSystem = $("[data-ui-auto='status-billing-system']");

    //  Top buttons
    public final SelenideElement newQuoteButton = $(byTagAndText("button", "New Quote"));
    public final SelenideElement engageLegalButton = $(byTagAndText("button", "Engage Legal"));
    public final SelenideElement engageProServButton = $(byTagAndText("button", "Engage ProServ"));
    public final SelenideElement engageCcProServButton = $(byTagAndText("button", "Engage CC ProServ"));
    public final SelenideElement cancelProServEngagementButton = $(byTagAndText("button", "Cancel ProServ Engagement"));
    public final SelenideElement createPocApprovalButton = $(byTagAndText("button", "Create POC Approval"));

    //  Pipeline
    public final SelenideElement servicePlansTabButton = $("[data-name='servicePlans']");
    public final SelenideElement productsTabButton = $("[data-name='products']");
    public final SelenideElement cartTabButton = $("[data-name='cart']");
    public final SelenideElement phaseTabButton = $("[data-name='phases']");
    public final SelenideElement quoteTabButton = $("[data-name='summary']");

    //  DatePicker
    public final LegacyDatePicker legacyDatePicker = new LegacyDatePicker();

    //  Misc
    private final SelenideElement spinnerLoader = $("#wizard_loader");
    public final SelenideElement spinnerSave = $(".spinner-message.slds-text-title--caps");
    public final SelenideElement errorNotification = $x("//*[@role='alert'][contains(@class,'slds-theme--error')]");
    public final SelenideElement successNotification = $x("//*[@role='alert'][contains(@class,'slds-theme--success')]");

    /**
     * Wait until the page is loaded
     * so that the user/test may safely interact with any of its elements after this method is finished.
     */
    public void waitUntilLoaded() {
        spinnerLoader.shouldBe(hidden, ofSeconds(60));
        spinnerSave.shouldBe(hidden, ofSeconds(60));
    }
}
