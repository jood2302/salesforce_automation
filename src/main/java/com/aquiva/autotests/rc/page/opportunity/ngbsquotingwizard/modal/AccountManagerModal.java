package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal;

import com.aquiva.autotests.rc.page.components.lookup.AngularLookupComponent;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

/**
 * Modal window in {@link NGBSQuotingWizardPage}
 * activated by clicking on "Manage Account Bindings"
 * button (only with Enable Engage Feature toggle turned on).
 * <p>
 * This dialog manages bindings between Office and Engage accounts.
 * After correct binding Master_Account__c field on Engage Account = Office Account's ID.
 * </p>
 */
public class AccountManagerModal {
    private final SelenideElement dialogContainer = $("link-account-modal");

    //  Notification messages
    public static final String ACCOUNT_DOESNT_MEET_REQUIREMENTS_ERROR = "Account doesn't meet requirements.\n" +
            "No common contact or appropriate contact Role or contact Relation is found on master Account";
    public static final String MASTER_ACCOUNT_SHOULD_BE_EXISTING_BUSINESS_ERROR = "Account doesn't meet requirements.\n" +
            "Master Account should be an Existing Business";
    public static final String MASTER_ACCOUNT_SERVICE_TYPE_SHOULD_BE_OFFICE_ERROR = "Account doesn't meet requirements.\n" +
            "Master Account's Service Type should be Office";
    public static final String MASTER_ACCOUNT_STATUS_SHOULD_BE_PAID_ERROR = "Account doesn't meet requirements.\n" +
            "Master Account should be in Paid status";
    public static final String MASTER_ACCOUNT_BRAND_SHOULD_BE_RC_ERROR = "Account doesn't meet requirements.\n" +
            "Master Account's Brand should be one of the following brands: RC US, UK, Canada, Europe";
    public static final String MASTER_ACCOUNT_SHOULD_NOT_BE_INDIAN_ERROR = "Account doesn't meet requirements.\n" +
            "It is not allowed to link Office India to Office India Account";
    public static final String MASTER_ACCOUNT_IS_NOT_CONTRACTED_ERROR = "Account doesn't meet requirements.\n" +
            "Master Account is not contracted";
    public static final String MASTER_ACCOUNT_CANT_BE_POC_ERROR = "Account doesn't meet requirements.\n" +
            "Master Account can't be POC";
    public static final String QUOTE_BINDING_IS_NOT_REQUIRED_FOR_EXISTING_BUSINESS_MESSAGE =
            "Quote binding is not required for existing business";

    //  Info section
    public final SelenideElement infoIcon = dialogContainer.$("[iconname='info_alt']");

    //  Error notifications list
    public final SelenideElement notificationBlock = dialogContainer.$("[title='Click to show/hide all notifications']");
    public final ElementsCollection notifications = dialogContainer.$$("p.notification__text");

    //  Buttons
    public final SelenideElement cancelButton = dialogContainer.$(byText("Cancel"));
    public final SelenideElement discardButton = dialogContainer.$(byText("Discard"));
    public final SelenideElement submitButton = dialogContainer.$(byText("Submit"));
    public final SelenideElement removeBindingButton = dialogContainer.$(".remove-button");

    public final AngularLookupComponent accountSearchInput = new AngularLookupComponent();
    public final AngularLookupComponent quoteSearchInput =
            new AngularLookupComponent($x("//div[@data-ui-auto='defaultQuoteLookupCombobox']"));
}
