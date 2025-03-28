package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.EngageProServModal;
import com.aquiva.autotests.rc.page.salesforce.IframePage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import java.util.List;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$x;
import static java.time.Duration.ofSeconds;

/**
 * 'Main' (landing) page of the {@link NGBSQuotingWizardPage}.
 * </br>
 * Used to manage (create, copy, delete, make primary) quotes for the Opportunity.
 */
public class NgbsQuoteSelectionQuoteWizardPage extends IframePage {

    //  Notifications
    public static final String NO_SALES_QUOTES_MESSAGE = "No Sales Quotes found";
    public static final String NO_POC_QUOTES_MESSAGE = "No POC Quotes found";

    //  Quotes sections
    public final SelenideElement salesQuotesSection = $x("//*[@data-ui-auto='sales-quotes']");
    public final SelenideElement pocQuotesSection = $x("//*[@data-ui-auto='poc-quotes']");
    public final SelenideElement proServQuotesSection = $x("//*[@data-ui-auto='proserv-quotes']");
    public final SelenideElement noSalesQuotesNotification = salesQuotesSection.$("p");
    public final SelenideElement noPocQuotesNotification = pocQuotesSection.$("p");

    //  Buttons
    public final SelenideElement addNewSalesQuoteButton = salesQuotesSection.$("[data-ui-auto='add-new-quote']");
    public final SelenideElement addNewPocQuoteButton = pocQuotesSection.$("[data-ui-auto='add-new-quote']");
    public final SelenideElement initiateProServButton = proServQuotesSection.$("[data-ui-auto='initiate-proserv']");
    public final SelenideElement initiateCcProServButton = proServQuotesSection.$("[data-ui-auto='initiate-cc-proserv']");

    public final ElementsCollection salesQuotes = salesQuotesSection.$$(".quote");
    public final ElementsCollection proServQuotes = proServQuotesSection.$$(".quote");

    //  Spinner
    public final SelenideElement spinner = $x("//c-lwc-spinner//*[@role='status']");

    //  Modal
    public final EngageProServModal engageProServDialog = new EngageProServModal();

    /**
     * Default no-arg constructor.
     * Defines default Quote Wizard location without specific iframe.
     */
    public NgbsQuoteSelectionQuoteWizardPage() {
        super();
    }

    /**
     * Constructor with iframe's title.
     * Defines default Quote Wizard location on the specific iframe.
     *
     * @param iframeTitleSubstring iframe's partial title for the Quote Wizard Selection page.
     */
    public NgbsQuoteSelectionQuoteWizardPage(String iframeTitleSubstring) {
        super(iframeTitleSubstring);
    }

    /**
     * Get Primary icon for provided Quote.
     *
     * @param quoteId Id of provided Quote
     * @return web element of Primary icon for provided Quote
     */
    public SelenideElement getPrimaryQuoteIcon(String quoteId) {
        return $x("//*[@data-ui-quote-id='" + quoteId + "']").$(".primary-icon");
    }

    /**
     * Get Name of provided Quote.
     *
     * @param quoteId Id of provided Quote
     * @return web element with Name of provided Quote
     */
    public SelenideElement getQuoteName(String quoteId) {
        return $x("//*[@data-ui-quote-id='" + quoteId + "']//a");
    }

    /**
     * Wait until the component loads most of its important elements (Sales Quote section 'Add New' button).
     * User may safely interact with any of the component's elements after this method is finished.
     */
    public void waitUntilLoaded() {
        addNewSalesQuoteButton.shouldBe(visible, ofSeconds(120));
    }

    /**
     * Get the 'Make Primary' button of the provided Quote.
     *
     * @param quoteId ID of provided Quote
     * @return web element for 'Make Primary' button of the provided Quote.
     */
    public SelenideElement getMakePrimaryButton(String quoteId) {
        return $x("//*[@data-ui-quote-id='" + quoteId + "']//*[@data-ui-auto='make-primary-quote']");
    }

    /**
     * Get the 'Delete' button of the provided Quote.
     *
     * @param quoteId ID of provided Quote
     * @return web element for 'Delete' button of the provided Quote.
     */
    public SelenideElement getDeleteButton(String quoteId) {
        return $x("//*[@data-ui-quote-id='" + quoteId + "']//*[@data-ui-auto='delete-quote']");
    }

    /**
     * Get the 'Copy' button of the provided Quote.
     *
     * @param quoteId ID of provided Quote
     * @return web element for 'Copy' button of the provided Quote.
     */
    public SelenideElement getCopyButton(String quoteId) {
        return $x("//*[@data-ui-quote-id='" + quoteId + "']//*[@data-ui-auto='copy-quote']");
    }

    /**
     * Click on the 'Make Primary' button for provided Quote.
     *
     * @param quoteId Id of a Quote to Make Primary
     */
    public void clickMakePrimaryButton(String quoteId) {
        getMakePrimaryButton(quoteId).click();
    }

    /**
     * Click on the 'Copy' button for provided Quote.
     *
     * @param quoteId Id of a Quote to Copy
     */
    public void clickCopyButton(String quoteId) {
        getCopyButton(quoteId).click();
    }

    /**
     * Click on the 'Delete' button for provided Quote.
     *
     * @param quoteId Id of a Quote to Delete
     */
    public void clickDeleteButton(String quoteId) {
        getDeleteButton(quoteId).click();
    }

    /**
     * Make a provided Quote Primary.
     *
     * @param quoteId Id of a Quote to make Primary
     */
    public void makeQuotePrimary(String quoteId) {
        clickMakePrimaryButton(quoteId);
        spinner.shouldBe(visible);
        spinner.shouldBe(hidden, ofSeconds(60));
        getPrimaryQuoteIcon(quoteId).shouldBe(visible, ofSeconds(10));
    }

    /**
     * Click on the 'Initiate ProServ' button, submit a default form,
     * and wait until it is loaded.
     * <br/>
     * Note: only available  for the GSP Brand Opportunities
     * (e.g. 'Rise America', 'RingCentral with Verizon')
     */
    public void initiateProServ() {
        initiateProServButton.shouldBe(visible, ofSeconds(20)).click();
        engageProServDialog.submitButton.shouldBe(enabled, ofSeconds(20)).click();
        spinner.shouldBe(visible);
        spinner.shouldBe(hidden, ofSeconds(60));
    }

    /**
     * Click on the 'Initiate CC ProServ' button, submit a form with selected PDF templates,
     * and wait until it is loaded.
     * <br/>
     * Note: only available  for the GSP Brand Opportunities
     * (e.g. 'Rise America', 'RingCentral with Verizon')
     */
    public void initiateCcProServ(List<String> pdfTemplates) {
        initiateCcProServButton.shouldBe(visible, ofSeconds(60)).click();
        pdfTemplates.forEach(engageProServDialog::selectPdfTemplate);
        engageProServDialog.submitButton.shouldBe(enabled, ofSeconds(20)).click();

        engageProServDialog.submitButton.shouldBe(hidden, ofSeconds(60));
        spinner.shouldBe(hidden, ofSeconds(60));
    }
}
