package com.aquiva.autotests.rc.page.opportunity.modal;

import com.aquiva.autotests.rc.page.components.LightningCombobox;
import com.aquiva.autotests.rc.page.components.lookup.StandardLwcLookupComponent;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Approval__c;

/**
 * {@link Approval__c} creation modal for 'Invoice Request' and 'Invoice-on-Behalf Request' approval types.
 * <br/>
 * Contains some elements common for both record types.
 *
 * @see InvoicingRequestApprovalCreationModal
 * @see InvoiceOnBehalfApprovalCreationModal
 */
public abstract class BaseInvoiceApprovalCreationModal extends ApprovalCreationModal {

    //  Fields

    //  'Information' section
    public final StandardLwcLookupComponent opportunitySearchLookup =
            new StandardLwcLookupComponent(dialogContainer.$x(".//*[./label[text()='Opportunity']]//div[@class='slds-combobox_container']"));

    //  'Invoice Request Detail' section
    public final LightningCombobox invoiceTermsPicklist = new LightningCombobox("Invoice Terms", dialogContainer);
    public final LightningCombobox paymentTermsPicklist = new LightningCombobox("Payment Terms", dialogContainer);
    public final SelenideElement potentialUsersInput = dialogContainer
            .$x(".//label[contains(text(),'Potential Users')]/following-sibling::div/input");
    public final StandardLwcLookupComponent accountsPayableContactSearchInput =
            new StandardLwcLookupComponent(dialogContainer.$x(".//*[./label[text()='Accounts Payable Contact']]//div[@class='slds-combobox_container']"),
                    "Advanced Search");

    //  'Description' section
    public final SelenideElement reasonCustomerRequestInvoicingInput = dialogContainer
            .$x(".//label[contains(text(),'Reason Customer is Requesting Invoicing')]/following-sibling::div/textarea");
    public final SelenideElement whyRCShouldInvoiceInput = dialogContainer
            .$x(".//label[contains(text(),'Why RingCentral Should Invoice Customer')]/following-sibling::div/textarea");

    //  'Sales potential' section
    public final SelenideElement initialUsersInput = dialogContainer
            .$x(".//label[contains(text(),'Initial Number of Users')]/following-sibling::div/input");
    public final SelenideElement initialDevicesInput = dialogContainer
            .$x(".//label[contains(text(),'Initial Number of Devices')]/following-sibling::div/input");
    public final SelenideElement pricePerUserInput = dialogContainer
            .$x(".//label[contains(text(),'Price per User')]/following-sibling::div/input");

    //  'Credit Request' section
    public final SelenideElement companyNameInput = dialogContainer
            .$x(".//label[contains(text(),'Legal Company Name - Head Office')]/following-sibling::div/input");
    public final SelenideElement streetInput = dialogContainer
            .$x(".//label[contains(text(),'Legal Physical Address Street')]/following-sibling::div/input");
    public final SelenideElement zipCodeInput = dialogContainer
            .$x(".//label[contains(text(),'Legal Physical Address Zip Code')]/following-sibling::div/input");
    public final SelenideElement cityInput = dialogContainer
            .$x(".//label[contains(text(),'Legal Physical Address City')]/following-sibling::div/input");
    public final SelenideElement stateInput = dialogContainer
            .$x(".//label[contains(text(),'Legal Physical Address State/Province')]/following-sibling::div/input");
    public final SelenideElement countryInput = dialogContainer
            .$x(".//label[contains(text(),'Legal Physical Address Country')]/following-sibling::div/input");
    public final SelenideElement signUpPurchaseLimitInput = dialogContainer
            .$x(".//label[contains(text(),'Sign-Up Purchase Limit')]/following-sibling::div/input");
    public final SelenideElement monthlyCreditLimitInput = dialogContainer
            .$x(".//label[contains(text(),'Monthly Credit Limit')]/following-sibling::div/input");

    /**
     * Constructor for {@link Approval__c} record creation modal window
     * located via its header's title that includes the active record type.
     *
     * @param approvalRecordType approval's record type (e.g. "Invoice-on-behalf Request")
     */
    public BaseInvoiceApprovalCreationModal(String approvalRecordType) {
        super(approvalRecordType);
    }
}
