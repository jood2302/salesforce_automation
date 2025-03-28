package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard;

import com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.*;
import com.codeborne.selenide.*;
import org.openqa.selenium.By;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_URL;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byCssSelector;
import static com.codeborne.selenide.Selenide.*;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

/**
 * Quote Wizard page.
 * <p>
 * Can be found by clicking on the 'Quote' tab on the {@link OpportunityRecordPage}
 * and selecting one of the quotes on the 'Main Quote' tab in {@link NgbsQuoteSelectionQuoteWizardPage}.
 * Used for main operations with the Quote (selecting package, products, calculating taxes, assigning devices etc.)
 */
public class NGBSQuotingWizardPage {

    /**
     * Max timeout for the progress bar to disappear after saving changes on the tabs (in seconds).
     */
    public static final int PROGRESS_BAR_TIMEOUT_AFTER_SAVE = 300;

    //  Notifications text
    public static final String ASSIGN_CORRECT_NUMBER_OF_LICENSES_TO_DL =
            "%1$s doesn't have equal number of assigned child licenses. " +
                    "Please assign correct number of child licenses to %1$s";

    public static final String QUOTING_IS_UNAVAILABLE_MESSAGE =
            "Quoting is unavailable for the currently selected brand.";
    public static final String YOU_DO_NOT_HAVE_APPROVED_DIRECT_DEBIT_REQUEST_APPROVAL =
            "You don't have approved Direct Debit Request Approval under the Account. Please, request Direct Debit Approval.";
    public static final String MONTHLY_CREDIT_LIMIT_EXCEEDED = "Monthly credit limit or signup purchase Limit exceeded. " +
            "Please, request the new Direct Debit Approval";
    public static final String PACKAGE_CAN_ONLY_HAVE_DL_UNLIMITED_IN_TOTAL = 
            "%s can only have between 1 and 99999 DL Unlimited in total - %s";

    //  Spinner
    public final SelenideElement spinner = $("[data-ui-auto='spinner']");

    //  Progress bar
    public final SelenideElement progressBar = $("[role='progressbar']");

    public final SelenideElement notification = $("notifications h2");
    public final SelenideElement errorNotification = $("notifications .slds-theme_error h2");
    public final SelenideElement closeNotificationButton = $("notifications button");
    public final SelenideElement tooltip = $(".cdk-overlay-container [role='tooltip']");

    //  Opportunity link
    public SelenideElement opportunityLink = $("[data-ui-auto='opportunity-link']");

    //  Tabs
    public final By tabButtonTooltip = byCssSelector("[class='slds-popover__body']");
    public final ElementsCollection headerButtons = $$("uqt-quote-manager button");
    public final ElementsCollection tabButtons = $$(".tab-button-group > button");
    public final SelenideElement packageTabButton = $("[data-ui-auto='package-tab']");
    public final SelenideElement productsTabButton = $("[data-ui-auto='products-tab']");
    public final SelenideElement cartTabButton = $("[data-ui-auto='price-tab']");
    public final SelenideElement quoteTabButton = $("[data-ui-auto='quote-tab']");
    public final SelenideElement shippingTabButton = $("[data-ui-auto='shipping-tab']");
    public final SelenideElement dealQualificationTabButton = $("[data-ui-auto='deal-qualification-tab']");
    public final SelenideElement proServPhasesTabButton = $("[data-ui-auto='ps-phases-tab']");

    //  Header buttons
    public final SelenideElement initiateProServButton =
            $x("//button[@data-ui-auto='initiate-proserv'][.//span[text()='Initiate ProServ']]");
    public final SelenideElement proServCreatedButton =
            $x("//button[@data-ui-auto='initiate-proserv'][.//span[text()='ProServ Created']]");
    public final SelenideElement proServSoldButton =
            $x("//button[@data-ui-auto='initiate-proserv'][.//span[text()='ProServ Sold']]");
    public final SelenideElement cancelProServButton = $("[data-ui-auto='cancel-proserv']");
    public final SelenideElement initiateCcProServButton =
            $x("//button[@data-ui-auto='initiate-cc-proserv'][.//span[text()='Initiate CC ProServ']]");
    public final SelenideElement ccProServCreatedButton =
            $x("//button[@data-ui-auto='initiate-cc-proserv'][.//span[text()='CC ProServ Created']]");
    public final SelenideElement createCaseButton = $("[data-ui-auto='report-problem']");

    //  Modal windows
    public final EngageProServModal engageProServDialog = new EngageProServModal();
    public final EngageProServCancelModal engageProServCancelDialog = new EngageProServCancelModal();
    public final BillingDetailsAndTermsModal billingDetailsAndTermsModal = new BillingDetailsAndTermsModal();

    /**
     * Extract the ID for the currently active Quote from browser's URL.
     * Make sure to call this method when the quote is already created.
     * <br/>
     * When the quote is created in the Quote Wizard, the URL has the following query parameters:
     * <p> {@code quoteId=0Q0190000008H8bCAE&id=0061900000DYZZHAA5} </p>
     * where id = Opportunity's ID, and quoteId = Quote's ID.
     *
     * @return standard 18-character Salesforce ID of the Quote
     * @throws IllegalStateException if there's no quote ID in the URL.
     */
    public String getSelectedQuoteId() {
        var currentURL = WebDriverRunner.url();

        if (currentURL.contains("quoteId=")) {
            return currentURL.replaceAll(".*quoteId=(\\w{18}).*", "$1");
        } else {
            throw new IllegalStateException("There's no quote ID in the current URL: " + currentURL);
        }
    }

    /**
     * Open the Quote Wizard for previously created Quote via direct link.
     * <p> Note: contents for Base URL are usually provided via system properties. </p>
     *
     * @param opportunityId ID of provided Opportunity
     * @param quoteId       ID of provided Quote
     */
    public void openPage(String opportunityId, String quoteId) {
        open(format("%s/apex/QuoteWizard?quoteId=%s&id=%s", BASE_URL, quoteId, opportunityId));
        waitUntilLoaded();
    }

    /**
     * Open the Quote Wizard to create a new Sales Quote via direct link.
     * <p> Note: contents for Base URL are usually provided via system properties. </p>
     *
     * @param opportunityId ID of the Opportunity to create a new Sales Quote for
     */
    public void openPageForNewSalesQuote(String opportunityId) {
        open(format("%s/apex/QuoteWizard?newQuoteType=Sales+Quote&id=%s", BASE_URL, opportunityId));
        waitUntilLoaded();
    }

    /**
     * Open the Quote Wizard to create a new POC Quote via direct link.
     * <p> Note: contents for Base URL are usually provided via system properties. </p>
     *
     * @param opportunityId ID of the Opportunity to create a new POC Quote for
     */
    public void openPageForNewPocQuote(String opportunityId) {
        open(format("%s/apex/QuoteWizard?newQuoteType=POC+Quote&id=%s", BASE_URL, opportunityId));
        waitUntilLoaded();
    }

    /**
     * Wait until the page loads most of its important elements.
     * User may safely interact with any of the page's elements after this method is finished.
     */
    public void waitUntilLoaded() {
        opportunityLink.shouldBe(visible, ofSeconds(120));
        progressBar.shouldBe(hidden, ofSeconds(180));
    }

    /**
     * Click on 'Initiate ProServ' button and submit some default data for it (as additional details).
     */
    public void initiateProServ() {
        initiateProServButton.shouldBe(enabled, ofSeconds(20)).click();
        engageProServDialog.submitButton.shouldBe(enabled, ofSeconds(20)).click();
        engageProServDialog.submitButton.shouldBe(hidden, ofSeconds(30));
        progressBar.shouldBe(visible, ofSeconds(10));
        progressBar.shouldBe(hidden, ofSeconds(60));
    }

    /**
     * Click on 'Initiate CC ProServ' button and submit some default data for it (as additional details).
     */
    public void initiateCcProServ() {
        initiateCcProServButton.click();
        engageProServDialog.submitButton.click();
        engageProServDialog.submitButton.shouldBe(hidden, ofSeconds(30));
        progressBar.shouldBe(visible, ofSeconds(10));
        progressBar.shouldBe(hidden, ofSeconds(60));
    }

    /**
     * Click 'X' button for ProServ engagement and submit a reason of ProServ cancellation.
     */
    public void cancelProServ() {
        cancelProServButton.click();
        engageProServCancelDialog.submitButton.click();
        engageProServCancelDialog.submitButton.shouldBe(hidden, ofSeconds(30));
        progressBar.shouldBe(visible, ofSeconds(10));
        progressBar.shouldBe(hidden, ofSeconds(60));
    }

    /**
     * Press 'Apply' on the Billing Details and Terms modal
     * and wait until the changes are applied, and the current tab is fully loaded again.
     */
    public void applyChangesInBillingDetailsAndTermsModal() {
        billingDetailsAndTermsModal.applyButton.click();
        billingDetailsAndTermsModal.applyButton.shouldBe(hidden);
        waitUntilLoaded();
    }
}
