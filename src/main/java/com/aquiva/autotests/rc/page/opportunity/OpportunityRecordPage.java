package com.aquiva.autotests.rc.page.opportunity;

import com.aquiva.autotests.rc.page.opportunity.modal.*;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.aquiva.autotests.rc.page.salesforce.GenericSalesforceModal;
import com.aquiva.autotests.rc.page.salesforce.RecordPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Opportunity;

import static com.codeborne.selenide.ClickOptions.usingJavaScript;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThanOrEqual;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;
import static java.time.Duration.ofSeconds;

/**
 * The Standard Salesforce page that displays Opportunity ({@link Opportunity}) record information,
 * such as Opportunity details (Opportunity Name, Opportunity's Account, etc...), action buttons,
 * related records and many more.
 * <br/>
 * This page also hosts Quote Wizard ({@link NGBSQuotingWizardPage}) among many other things.
 */
public class OpportunityRecordPage extends RecordPage {

    //  Related lists' headers
    public static final String APPROVALS_RELATED_LIST = "Approvals";

    //  Error messages
    public static final String AREA_CODES_ARE_MISSING_TO_CLOSE_OPPORTUNITY_ERROR = "You can't Close this Opportunity\n\n" +
            "Area Codes are missing. Please select Area Codes for all Items in the Cart of the Primary Quote that are eligible for Area Code assignment";
    public static final String ERRORS_ON_QUOTE_ERROR = "Primary quote has errors\n\n" +
            "Make sure that all errors on primary quote are resolved. Only after that you can close opportunity.";
    public static final String QUOTE_IS_NOT_APPROVED_ERROR = "Primary quote is not approved\n\n" +
            "The Opportunity cannot be Closed Won because the Primary Quote does not contain the required Approvals";
    public static final String QUOTE_IS_INVALID_ERROR = "You can't Close this Opportunity\n\n" +
            "Primary Quote is invalid and doesn't reflect current Account Status. You have to create new Quote to be able to close this Opportunity";
    public static final String QUOTES_DO_NOT_EXIST_ERROR = "The opportunity doesn't have any sales agreements\n\n" +
            "To close the opportunity, please, create at least one sales agreement";
    public static final String RENTAL_PHONES_REQUIRE_CONTRACT_ERROR = "Rental phones require a long-term contract\n\n" +
            "Quote with 24+ months initial term is required to sign up with rental phones";
    public static final String ACTIVE_AGREEMENT_IS_REQUIRED_TO_CLOSE_ERROR = "You can't Close this Opportunity\n\n" +
            "You need an Active Agreement to Close this Opportunity.";
    public static final String EMPTY_BILLING_ADDRESS_ERROR = "Account's Billing Address is incomplete\n\n" +
            "Corresponding fields must be specified:\n\n" +
            "Billing Street\n" +
            "Billing City\n" +
            "Billing Zip/Postal Code\n" +
            "Billing Country";
    public static final String ONLY_BILLING_COUNTRY_POPULATED_ERROR = "Account's Billing Address is incomplete\n\n" +
            "Corresponding fields must be specified:\n\n" +
            "Billing Street\n" +
            "Billing City\n" +
            "Billing State/Province\n" +
            "Billing Zip/Postal Code";
    public static final String MASTER_ACCOUNT_IS_NOT_PAID_ERROR = "You can't Close this Opportunity\n\n" +
            "Master account is not Paid or Contact Roles mismatch";
    public static final String INVOICE_PAYMENT_METHOD_IS_REQUIRED_FOR_CLOSE_ERROR = "You can't Close this Opportunity\n\n" +
            "Invoice payment method is required";
    public static final String SUBMIT_INVOICE_ON_BEHALF_REQUEST_APPROVAL_ERROR = "You can't Close this Opportunity\n\n" +
            "Please, submit Invoice-on-behalf request approval first";
    public static final String MASTER_ACCOUNT_WASNT_BOUND_ERROR = "You can't Close this Opportunity\n\n" +
            "Master Account wasn't bound. Master Account should be linked";
    public static final String APPROVED_KYC_APPROVAL_REQUIRED_ERROR = "You can't Close this Opportunity\n\n" +
            "You need an approved KYC request to Close an opportunity";
    public static final String MONTHLY_CREDIT_LIMIT_EXCEEDED_ERROR = "You can't Close this Opportunity\n\n" +
            "Monthly credit limit or signup purchase Limit exceeded. Please, request the new Direct Debit Approval";
    public static final String PLEASE_COMPLETE_PROSERV_QUOTE_TO_CLOSE_OPPORTUNITY_ERROR = "There is no Professional Service Quote\n\n" +
            "Please complete Professional Service Quote prior to closing Opportunity";
    public static final String ZERO_ITEMS_IN_CART_ERROR = "0 items in the cart\n" +
            "\n" +
            "Please complete Professional Service Quote prior to closing Opportunity";
    public static final String PROSERV_QUOTES_NOT_MARKED_AS_SOLD_ERROR = "ProServ Quotes not marked as Sold\n" +
            "\n" +
            "ProServ Quotes need to be marked as Sold, or click \"Cancel ProServ Engagement\" to remove them.";

    //  Spinner
    public final SelenideElement spinner = $("[role='alert'][class*='spinner']");

    public final SelenideElement stagePicklist = $x("//div[./label='Stage']//select");

    //  Tabs
    public final SelenideElement quoteTab = $x("//li[@title='Quote']");
    public final SelenideElement approvalTab = $x("//li[@title='Approval']");

    //  Alerts
    public final SelenideElement alertNotificationBlock = $("[role='alert'][class*='error']");
    public final ElementsCollection notifications = $$("[role='alert'][class*='error'] > div");
    public final SelenideElement alertCloseButton = alertNotificationBlock.$("button");

    //  Quote Wizard
    public WizardBodyPage wizardBodyPage = new WizardBodyPage();

    //  Modal windows
    public final ProcessOrderModal processOrderModal = new ProcessOrderModal();
    public final GenericSalesforceModal closeOpportunityModal = new GenericSalesforceModal();
    public final NewApprovalRecordTypeSelectionModal newApprovalRecordTypeSelectionModal =
            new NewApprovalRecordTypeSelectionModal();
    public final InvoiceOnBehalfApprovalCreationModal invoiceOnBehalfApprovalCreationModal =
            new InvoiceOnBehalfApprovalCreationModal();
    public final InvoicingRequestApprovalCreationModal invoicingRequestApprovalCreationModal =
            new InvoicingRequestApprovalCreationModal();
    public final DirectDebitApprovalCreationModal directDebitApprovalCreationModal =
            new DirectDebitApprovalCreationModal();

    /**
     * {@inheritDoc}
     */
    public void waitUntilLoaded() {
        detailsTab.shouldBe(visible, ofSeconds(100));
        visibleLightingActionButtons.shouldHave(sizeGreaterThanOrEqual(3), ofSeconds(100));
    }

    /**
     * Open 'Quote' tab on the Opportunity record page and switch to NGBS Quoting Wizard iFrame.
     */
    public void switchToNGBSQW() {
        quoteTab.shouldBe(enabled, ofSeconds(80)).click(usingJavaScript());
        wizardBodyPage.switchToIFrame();
        wizardBodyPage.mainQuoteSelectionWizardPage.waitUntilLoaded();
    }

    /**
     * Open 'Quote' tab on the Opportunity record page and switch to NGBS Quoting Wizard iFrame.
     * Use it for Opportunities that do not support Quoting.
     */
    public void switchToNGBSQWIframeWithoutQuote() {
        quoteTab.shouldBe(enabled, ofSeconds(80)).click(usingJavaScript());
        wizardBodyPage.switchToIFrame();
    }

    /**
     * Switch the current context away from the Quote Wizard
     * back to the actual record page.
     * <br/>
     * It's necessary because the current implementation places
     * the Quote Wizard with Quote selection in the separate iframe inside the Opportunity record page,
     * and the Quote Wizard with working with individual quotes is in the separate tab/window.
     * Make sure that the Opportunity's record page is the 1st tab in the browser!
     * <br/>
     * Useful if the test needs to interact with record page elements
     * (e.g. lightning buttons) after performing actions in the Quote Wizard.
     */
    public void switchToRecordPageFromNGBSQW() {
        switchTo().window(0);
    }

    /**
     * Open Create New Approval Modal window from 'Approval' tab.
     */
    public void openCreateNewApprovalModal() {
        approvalTab.click(usingJavaScript());
        clickHiddenListButtonOnRelatedList(APPROVALS_RELATED_LIST, NEW_BUTTON_LABEL);
    }

    /**
     * Click on 'Process Order' button.
     * <p></p>
     * This method searches "Process Order" button among Lightning Experience actions
     * in the upper right corner of the page
     * (even if the button is hidden in the "show more actions" list).
     */
    public void clickProcessOrderButton() {
        switchToRecordPageFromNGBSQW();
        clickDetailPageButton("Process Order");
    }

    /**
     * Click on 'Close' button.
     * <p></p>
     * This method searches "Close" button among Lightning Experience actions
     * in the upper right corner of the page.
     */
    public void clickCloseButton() {
        switchToRecordPageFromNGBSQW();
        clickDetailPageButton("Close");
    }

    /**
     * Click on 'Create a Case' button.
     * <p></p>
     * This method searches "Create a Case" button among Lightning Experience actions
     * in the upper right corner of the page.
     */
    public void clickCreateCaseButton() {
        clickDetailPageButton("Create a Case");
    }

    /**
     * Close all appearing error alert notifications on {@link OpportunityRecordPage}
     * if they are not closed immediately.
     */
    public void closeErrorAlertNotifications() {
        notifications.asDynamicIterable().forEach(n -> n.parent().$("button").click());
    }
}
