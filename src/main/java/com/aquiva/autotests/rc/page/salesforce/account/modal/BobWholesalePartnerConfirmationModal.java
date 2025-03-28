package com.aquiva.autotests.rc.page.salesforce.account.modal;

import com.aquiva.autotests.rc.page.salesforce.GenericSalesforceModal;
import com.aquiva.autotests.rc.page.salesforce.account.AccountRecordPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * Modal window on {@link AccountRecordPage}
 * used for confirmation of removing Bill-on-Behalf/Wholesale partners from Account.
 */
public class BobWholesalePartnerConfirmationModal extends GenericSalesforceModal {
    //  Messages
    public static final String THIS_OPERATION_WILL_DELETE_RELATIONSHIP_WITH_PARTNER_MESSAGE = "This Operation will delete relationship with Partner.\n" +
            " • Invoicing Approval request will be automatically created and auto-approved with current Account limits.\n" +
            " • Payment method will be switched to Invoice.\n" +
            " • All existing quotes (except for Closed Won or Downgraded Opportunities) will be invalidated.";
    public static final String THIS_OPERATION_WILL_ESTABLISH_RELATIONSHIP_WITH_PARTNER_MESSAGE = "This Operation will establish relationship with Partner.\n" +
            " • Invoice-on-Behalf or Invoice Wholesale Approval (depends on selected Partner) request will be created with current Account limits and sent to Finance for Approval.\n" +
            " • Payment method will be switched to Invoice-on-Behalf or Wholesale (depends on selected Partner).\n" +
            " • All existing quotes (except for Closed Won or Downgraded Opportunities) will be invalidated.";

    //  Content
    public final SelenideElement modalContentMessage = dialogContainer.$(".slds-modal__content .slds-p-around_medium");

    //  Buttons
    public final SelenideElement confirmButton = dialogContainer.$(byText("Confirm"));
    public final SelenideElement cancelButton = dialogContainer.$(byText("Cancel"));

    /**
     * Constructor that defines default location for the modal window on the Account record page.
     */
    public BobWholesalePartnerConfirmationModal() {
        super($("partner-confirmation-modal"));
    }
}
