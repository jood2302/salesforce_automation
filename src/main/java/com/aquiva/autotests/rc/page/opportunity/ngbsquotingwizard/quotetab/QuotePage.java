package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab;

import com.aquiva.autotests.rc.model.ngbs.testdata.AreaCode;
import com.aquiva.autotests.rc.page.components.*;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NgbsQuotingWizardFooter;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.*;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.EngageLegalRequestModal.AMENDMENT_ENGAGEMENT_TYPE;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToIntToString;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.StringHelper.USD_CURRENCY_SIGN;
import static com.codeborne.selenide.ClickOptions.usingJavaScript;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.*;
import static com.codeborne.selenide.Selenide.$;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

/**
 * Quote Details tab in {@link NGBSQuotingWizardPage}
 * that contains some useful fields and picklists,
 * {@link ShippingAddressForm}, {@link Calendar} and {@link AreaCodeSelector}
 * for Main Area Code and Fax Area Code;
 * 'Save Changes', 'Update Contract Terms' and 'Generate PDF' buttons
 */
public class QuotePage extends NGBSQuotingWizardPage {

    //  Error Messages
    public static final String FIELD_IS_REQUIRED_ERROR = "This field is required";
    public static final String START_DATE_SHOULD_BE_IN_RANGE_ERROR =
            "Start date should be in range from billing date to +30 days from billing date.";

    //  Info message
    public static final String LINK_OFFICE_ACCOUNT_TO_SET_UP_CONTRACT_TERMS_MESSAGE =
            "In order to set up Contract Terms please link with MVP Account using Manage Account Bindings";
    public static final String PROVISIONING_NOT_ALLOWED_WHEN_PROSERV_INITIATED_MESSAGE =
            "Provisioning not allowed when ProServ is initiated";

    //  'Intended Payment method' picklist values
    public static final String CREDIT_CARD_PAYMENT_METHOD = "Credit Card";
    public static final String INVOICE_PAYMENT_METHOD = "Invoice";
    public static final String DIRECT_DEBIT_PAYMENT_METHOD = "Direct Debit";
    public static final String INVOICE_WHOLESALE_PAYMENT_METHOD = "Invoice Wholesale";
    public static final String NONE_PAYMENT_METHOD = "--None--";

    //  'Stage' picklist values
    public static final String QUOTE_STAGE = "Quote";
    public static final String AGREEMENT_STAGE = "Agreement";

    //  Date patterns
    public static final DateTimeFormatter START_AND_END_DATE_INPUT_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    public static final DateTimeFormatter BILLING_DATE_MESSAGE_FORMATTER = DateTimeFormatter.ofPattern("MM/d/yyyy");

    public final SelenideElement loadingMessage = $("quote-details").$(byText("loading..."));

    public final SelenideElement quoteNameInput = $("[data-ui-auto='quote-name']");
    public final SelenideElement provisionToggle = $("[data-ui-auto='lbo-toggle'] .slds-checkbox_faux");
    public final SelenideElement provisionToggleInfoIcon = $("[data-ui-auto='lbo-toggle-disabled-descr']");
    public final SelenideElement makePrimaryButton = $("[data-ui-auto='quote-make-primary']");
    public final SelenideElement expirationDateInput = $("[data-ui-auto='expiration-date']");
    public final SelenideElement stageInfoTooltip = $("[for='quote-type'] info");
    public final SelenideElement stagePicklist = $("[data-ui-auto='quote-type']");
    public final SelenideElement agreementStatusPicklist = $("[data-ui-auto='agreement-status']");
    public final SelenideElement contractTermsInfoPlaceholder = $(".cpq-details fieldset");
    public final SelenideElement initialTermPicklist = $("[data-ui-auto='initial-term']");
    public final SelenideElement initialTermSection = $(withText("Initial Term")).parent();
    public final SelenideElement initialTermMessage = $(".initial-terms-message");
    public final SelenideElement renewalTermPicklist = $("[data-ui-auto='renewal-term']");
    public final SelenideElement autoRenewalCheckbox = $("#auto-renewal");
    public final SelenideElement dataRetentionDurationPicklist = $("[data-ui-auto='data-retention-duration']");
    public final SelenideElement startDateInput = $("[data-ui-auto='start-date']");
    public final SelenideElement startDateSection = $("[formcontrolname*='startDate'] + [class*='help']");
    public final SelenideElement endDateInput = $("[data-ui-auto='end-date']");
    public final SelenideElement discountJustificationTextArea = $("[data-ui-auto='justification']");
    public final SelenideElement freeServiceCreditNumberOfMonths = $("#free-service-credit");
    public final SelenideElement freeServiceCreditAmount = $("#free-service-credit + div");
    public final SelenideElement paymentMethodPicklist = $("#payment-method");
    public final SelenideElement preferredLanguagePicklist = $("package-preferred-language select");
    public final SelenideElement rcMainNumberInput = $("#rcAccountNumber");

    //  Area Code section
    public final SelenideElement mainAreaCodeInput = $("[datauiautoinput='main-area-code']");
    public final SelenideElement mainAreaCodeError = mainAreaCodeInput.$(".slds-form-element__help");
    public final SelenideElement mainAreaCodeText = mainAreaCodeInput.$("div[title]");
    public final SelenideElement faxAreaCodeInput = $("[datauiautoinput='fax-area-code']");
    public final SelenideElement faxAreaCodeError = faxAreaCodeInput.$(".slds-form-element__help");
    public final SelenideElement faxAreaCodeText = faxAreaCodeInput.$("div[title]");
    private final By clearButton = byCssSelector("[iconname='close']");
    public final SelenideElement clearFaxAreaCodeButton = faxAreaCodeInput.$(clearButton);

    public final SelenideElement shippingAddressTextArea = $("[data-ui-auto='shipping-address']");
    public final SelenideElement selfProvisionedCheckbox = $("[data-ui-auto='self-provisioned']");
    public final SelenideElement provisioningDetailsTextArea = $("[data-ui-auto='provisioning-details']");
    public final SelenideElement noteToCustomerEditorArea = $("[data-ui-auto='note-to-customer'] .angular-editor-textarea");

    //  Buttons
    public final SelenideElement moreActionsButton = $("[data-ui-auto='quote-tab-more-actions']");
    public final SelenideElement generatePdfButton = $("[data-ui-auto='generate-pdf']");
    public final SelenideElement sendWithDocuSignButton = $("[data-ui-auto='send-with-docusign']");
    public final SelenideElement engageLegalButton = $("[data-ui-auto='engage-legal']");
    public final SelenideElement createPocApprovalButton = $("[data-ui-auto='create-poc-approval']");
    public final SelenideElement manageAccountBindingsButton = $("[data-ui-auto='manage-account-bindings']");
    public final SelenideElement createContractButton = $("[data-ui-auto='create-msa']");
    public final SelenideElement copyQuoteButton = $("[data-ui-auto='copy-quote']");
    public final SelenideElement saveButton = $("[data-ui-auto='quote-save']");

    //  Shipping form
    public final ShippingAddressForm shippingAddressForm = new ShippingAddressForm();

    //  Calendar
    public final Calendar calendar = new Calendar();

    //  Area Code selector
    public final AreaCodeSelector areaCodeSelector = new AreaCodeSelector();

    //  Footer
    public final NgbsQuotingWizardFooter footer = new NgbsQuotingWizardFooter();

    //  Modal windows
    public final EngageLegalRequestModal engageLegalRequestModal = new EngageLegalRequestModal();
    public final AccountManagerModal manageAccountBindings = new AccountManagerModal();
    public final CreatePocApprovalModal pocApprovalModal = new CreatePocApprovalModal();
    public final PdfGenerateModal pdfGenerateModal = new PdfGenerateModal();
    public final ConfigurePlaybooksModal configurePlaybooksModal = new ConfigurePlaybooksModal();
    public final AttachingFilesModal attachingFilesModal = new AttachingFilesModal();
    public final SendWithDocusignModal sendWithDocusignModal = new SendWithDocusignModal();

    /**
     * Generate and return a message about the Billing Date of an Existing Business Account with the provided date.
     *
     * @param billingDate billing date that will be used for message generation
     * @return message about billing date of Account (for example, "Billing Date (NGBS) - 12/23/2021")
     */
    public static String getBillingDateMessage(LocalDate billingDate) {
        var expectedBillingDate = BILLING_DATE_MESSAGE_FORMATTER.format(billingDate);
        return format("Billing Date (NGBS) - %s", expectedBillingDate);
    }

    /**
     * Generate and return a Free Service Credit total value for the US Quotes.
     *
     * @param expectedFscTotal sum of QLI Free Service Credit and Free Service Taxes
     * @return Free Service Credit value with the currency code (e.g. "$1245", "$1245.89")
     */
    public static String getFreeServiceCreditTotalValueForUsQuotes(Double expectedFscTotal) {
        var formattedFscValue = expectedFscTotal.intValue() == expectedFscTotal
                ? doubleToIntToString(expectedFscTotal)
                : String.valueOf(expectedFscTotal);
        return format("%s%s", USD_CURRENCY_SIGN, formattedFscValue);
    }

    /**
     * Open the Quote Details tab by clicking on the tab's button.
     */
    public QuotePage openTab() {
        quoteTabButton.click();
        waitUntilLoaded();
        return this;
    }

    /**
     * Wait until the page is fully loaded.
     * User may safely interact with any of the page's elements after this method is finished.
     */
    public void waitUntilLoaded() {
        progressBar.shouldBe(hidden, ofSeconds(PROGRESS_BAR_TIMEOUT_AFTER_SAVE));
        quoteNameInput.shouldBe(visible, ofSeconds(90));
        loadingMessage.shouldBe(hidden, ofSeconds(60));
        errorNotification.shouldBe(hidden);
    }

    /**
     * Submit a new value in the 'Quote Name' field.
     *
     * @param quoteNewName new name for the Quote
     */
    public void setQuoteName(String quoteNewName) {
        quoteNameInput.preceding(0).click();
        quoteNameInput.setValue(quoteNewName);
    }

    /**
     * Set current date value to 'Start Date' field.
     */
    public void setDefaultStartDate() {
        startDateInput.click();
        calendar.setTodayDate();
    }

    /**
     * Set 'Auto Renewal' checkbox to be selected or not.
     * <br/>
     * This method has to be used instead of {@link SelenideElement#setSelected(boolean)}
     * because {@code input} element for the 'Auto Renewal' is covered by other element,
     * and {@link SelenideElement#setSelected(boolean)} doesn't work correctly.
     *
     * @param isAutoRenewalToBeSelected true, if the checkbox should be selected,
     *                                  otherwise, it should be deselected.
     */
    public void setAutoRenewalSelected(boolean isAutoRenewalToBeSelected) {
        var isAutoRenewalSelectedNow = autoRenewalCheckbox.should(exist).isSelected();
        if (isAutoRenewalToBeSelected != isAutoRenewalSelectedNow) {
            autoRenewalCheckbox.click(usingJavaScript());
        }
        autoRenewalCheckbox.shouldBe(isAutoRenewalToBeSelected ? selected : not(selected));
    }

    /**
     * Set value for 'Main Area Code' input field with provided {@link AreaCode} object.
     * Used in New Business Opportunities.
     *
     * @param areaCode {@link AreaCode} object with values for area code:
     *                 country name, state name, city name
     */
    public void setMainAreaCode(AreaCode areaCode) {
        areaCodeSelector.selectCode(areaCode);
    }

    /**
     * Set value for 'Fax Area Code' input field with provided {@link AreaCode} object.
     * Used in New Business Opportunities.
     *
     * @param areaCode {@link AreaCode} object with values for area code:
     *                 country name, state name, city name
     */
    public void setFaxAreaCode(AreaCode areaCode) {
        var areaCodeSelector = new AreaCodeSelector(faxAreaCodeInput);
        areaCodeSelector.selectCode(areaCode);
    }

    /**
     * Select an option in the 'Payment Method' picklist.
     *
     * @param paymentMethod any valid payment method available for the selected package
     *                      (e.g. "Invoice", "Credit Card", "Invoice On Behalf", "Invoice Wholesale")
     */
    public void selectPaymentMethod(String paymentMethod) {
        paymentMethodPicklist.selectOption(paymentMethod);
        paymentMethodPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(paymentMethod));
    }

    /**
     * Select an option in the 'Preferred Language' picklist.
     *
     * @param preferredLanguage any valid preferred language available for the selected package
     *                          (e.g. "English (US)", "German", "French")
     */
    public void selectPreferredLanguage(String preferredLanguage) {
        preferredLanguagePicklist.selectOption(preferredLanguage);
        preferredLanguagePicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(preferredLanguage));
    }

    /**
     * Press 'Save Changes' button on the Quote Details Tab of the Quote Wizard.
     */
    public void saveChanges() {
        saveButton.click();
        progressBar.shouldBe(visible, ofSeconds(10));
        waitUntilLoaded();
    }

    /**
     * Click 'Create POC Approval' button, set the link to the evaluation agreement, and click 'Create'.
     *
     * @param linkToSignedEvaluationAgreement value to be set in the 'Link to the Signed Agreement' input
     *                                        (e.g. "www.example.com")
     */
    public void createPocApproval(String linkToSignedEvaluationAgreement) {
        createPocApprovalButton.click();
        pocApprovalModal.linkAgreementInput.setValue(linkToSignedEvaluationAgreement);
        pocApprovalModal.createButton.click();

        progressBar.shouldBe(visible, ofSeconds(10));
        progressBar.shouldBe(hidden, ofSeconds(60));
        pocApprovalModal.header.shouldNot(exist);
        errorNotification.shouldBe(hidden);
    }

    /**
     * Click on the 'Generate PDF' button even if it's hidden in the 'More Actions' pop-up on the Quote Details tab.
     */
    public void clickGeneratePdfButton() {
        moreActionsButton.hover().click();
        generatePdfButton.click();
    }

    /**
     * Click on the 'Engage Legal' button and submit some default data in an opened modal window (Engagement Type,
     * Legal Account Name and Ask by Customer).
     *
     * @param accountName the value that will be set in Legal Account Name input
     */
    public void engageLegal(String accountName) {
        engageLegalButton.click();

        engageLegalRequestModal.legalEngagementTypeSelect.selectOption(AMENDMENT_ENGAGEMENT_TYPE);
        engageLegalRequestModal.legalAccountNameInput.setValue(accountName);
        engageLegalRequestModal.askByCustomerTextarea.setValue(TEST_STRING);
        engageLegalRequestModal.submitButton.click();

        progressBar.shouldBe(visible, ofSeconds(10));
        waitUntilLoaded();
    }

    /**
     * Click 'Submit' button in Account Bindings modal window and wait until spinner is no longer displayed.
     */
    public void submitAccountBindingChanges() {
        manageAccountBindings.submitButton.shouldBe(enabled, ofSeconds(10)).click();
        progressBar.shouldBe(visible, ofSeconds(10));
        waitUntilLoaded();
    }

    /**
     * Open 'Billing Details and Terms' modal,
     * select new value for 'Number of months' (special terms/free service credit), and save changes.
     *
     * @param specialTermNewValue new value for special terms, could be partial
     *                            (e.g. "1 Free Month of Service", "2 Free Months")
     */
    public void applyNewSpecialTerms(String specialTermNewValue) {
        footer.billingDetailsAndTermsButton.click();
        billingDetailsAndTermsModal.specialTermsPicklist.selectOptionContainingText(specialTermNewValue);
        applyChangesInBillingDetailsAndTermsModal();
        
        saveButton.shouldBe(enabled, ofSeconds(10));
        saveChanges();
    }
}
