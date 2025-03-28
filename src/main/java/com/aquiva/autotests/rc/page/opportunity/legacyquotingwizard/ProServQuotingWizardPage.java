package com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard;

import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.modal.*;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byTagAndText;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static java.time.Duration.ofSeconds;

/**
 * ProServ Legacy Quoting Wizard page.
 * <p>
 * Can be found in Legacy Quoting Wizard by clicking on 'ProServ Quote' tab.
 * Used for creating ProServ quotes.
 * <p/>
 */
public class ProServQuotingWizardPage extends BaseLegacyQuotingWizardPage {

    //  Top buttons
    public final SelenideElement cancelCcProServEngagementButton = $(byTagAndText("button",
            "Cancel CC ProServ Engagement"));
    public final SelenideElement syncToPrimaryQuoteButton = $(byText("Sync To Primary Quote"));
    public final SelenideElement proServIsOutForSignatureButton = $(byText("ProServ is Out for Signature"));
    public final SelenideElement proServIsSoldButton = $(byText("ProServ is Sold"));

    //  Modal windows
    public final CancelCcProServModal cancelCcProServDialog = new CancelCcProServModal();
    public final MarkProServAsOutForSignatureModal markProServAsOutForSignatureModal = new MarkProServAsOutForSignatureModal();
    public final MarkProServAsSoldModal markProServAsSoldModal = new MarkProServAsSoldModal();

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitUntilLoaded() {
        productsTabButton.shouldBe(visible, ofSeconds(60));
        quotesPicklist.shouldBe(visible, ofSeconds(30));
        super.waitUntilLoaded();
    }

    /**
     * Click 'Cancel CC ProServ Engagement' button for СС ProServ engagement
     * and click 'Submit' button in modal window.
     */
    public void cancelCcProServ() {
        cancelCcProServEngagementButton.click();
        cancelCcProServDialog.submitButton.click();
        cancelCcProServDialog.submitButton.shouldBe(hidden, ofSeconds(30));
        waitUntilLoaded();
        errorNotification.shouldBe(hidden);
        successNotification.shouldBe(visible, ofSeconds(10));
    }

    /**
     * Click 'Sync To Primary Quote' button on the ProServ Quote tab.
     */
    public void syncProServQuoteWithPrimary() {
        //  might not work via single-click in some flows (due to the popup modal window on the button)
        syncToPrimaryQuoteButton.doubleClick();
        waitUntilLoaded();
        errorNotification.shouldBe(hidden);
        successNotification.shouldBe(hidden, ofSeconds(10));
    }

    /**
     * Click 'ProServ is Out for Signature' button
     * and click 'Mark as "Out for Signature" and Lock Quote' button in modal window.
     */
    public void markProServIsOutForSignature() {
        proServIsOutForSignatureButton.shouldBe(visible, ofSeconds(10)).click();
        markProServAsOutForSignatureModal.markAsOutOfSignatureButton.click();
        waitUntilLoaded();
        errorNotification.shouldBe(hidden);
        successNotification.shouldBe(hidden, ofSeconds(10));
    }

    /**
     * Click 'ProServ is Sold' button and click 'Submit' button in modal window.
     */
    public void markProServAsSold() {
        proServIsSoldButton.click();
        markProServAsSoldModal.markProServAsSoldButton.click();
        waitUntilLoaded();
        errorNotification.shouldBe(hidden);
        successNotification.shouldBe(hidden, ofSeconds(10));
    }
}
