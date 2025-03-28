package com.aquiva.autotests.rc.page.salesforce.account.modal;

import com.aquiva.autotests.rc.page.components.LightningCombobox;
import com.aquiva.autotests.rc.page.components.lookup.CustomLwcLookupComponent;
import com.aquiva.autotests.rc.page.opportunity.modal.InvoiceOnBehalfApprovalCreationModal;
import com.aquiva.autotests.rc.page.opportunity.modal.InvoicingRequestApprovalCreationModal;
import com.aquiva.autotests.rc.page.salesforce.GenericSalesforceModal;
import com.aquiva.autotests.rc.page.salesforce.account.AccountRecordPage;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Approval__c;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * {@link Approval__c} creation modal layout of 'Invoicing Request', 'Invoice-on-Behalf Request' approval types
 * for users with 'Channel Operations - Lightning' profile.
 * <br/>
 * Contains some type-related fields, like 'Invoice Terms', address fields, 'Potential Users' etc.
 * <br/>
 * <p><b> Note: Might be opened from {@link AccountRecordPage} page. </b></p>
 * <p><b> Note: not to be confused with {@link InvoicingRequestApprovalCreationModal}
 * or {@link InvoiceOnBehalfApprovalCreationModal}!</b></p>
 */
public class ApprovalCreationForChannelOperationsModal extends GenericSalesforceModal {

    //  Fields
    public final LightningCombobox invoiceTermsPicklist = new LightningCombobox("Invoice Terms", dialogContainer);
    public final LightningCombobox paymentTermsPicklist = new LightningCombobox("Payment Terms", dialogContainer);
    public final SelenideElement signUpPurchaseLimitInput =
            dialogContainer.$x(".//label[contains(text(),'Sign-Up Purchase Limit')]/following-sibling::div/input");
    public final SelenideElement monthlyCreditLimitInput =
            dialogContainer.$x(".//label[contains(text(),'Monthly Credit Limit')]/following-sibling::div/input");
    public final SelenideElement initialNumberOfDevicesInput =
            dialogContainer.$x(".//label[contains(text(),'Initial Number of Devices')]/following-sibling::div/input");
    public final CustomLwcLookupComponent opportunitySearchInput =
            new CustomLwcLookupComponent(dialogContainer.$x(".//*[./label[text()='Opportunity']]/div"));
    public final CustomLwcLookupComponent accountSearchInput =
            new CustomLwcLookupComponent(dialogContainer.$x(".//*[./label[text()='Account']]/div"));
    public final CustomLwcLookupComponent accountsPayableContactSearchInput =
            new CustomLwcLookupComponent(dialogContainer.$x(".//*[./label[text()='Accounts Payable Contact']]/div"));
    public final CustomLwcLookupComponent partnerAccountsPayableContactSearchInput =
            new CustomLwcLookupComponent(dialogContainer.$x(".//*[./label[text()=\"Partner's Accounts Payable Contact\"]]/div"));

    //  Buttons
    public final SelenideElement saveButton = dialogContainer.$(byText("Save"));

    public final SelenideElement spinner = dialogContainer.$x(".//div[@c-lwcspinner_lwcspinner]");

    /**
     * Constructor that defines default location for the modal window on the Account record page.
     */
    public ApprovalCreationForChannelOperationsModal() {
        super($("#BobEngageApprovalLayout"));
    }
}
