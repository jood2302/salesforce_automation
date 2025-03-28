package com.aquiva.autotests.rc.page.opportunity.modal;

import com.aquiva.autotests.rc.page.components.LightningCombobox;
import com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage;
import com.aquiva.autotests.rc.page.salesforce.account.AccountRecordPage;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Approval__c;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.INVOICING_REQUEST_RECORD_TYPE;

/**
 * {@link Approval__c} creation modal of 'Invoicing Request' approval type.
 * <br/>
 * Contains some type-related fields, like 'RC Point of Presence', purposes for procuring the Service,
 * 'Type of Customer' etc.
 * <p>
 * <b> Note: Might be opened from {@link AccountRecordPage} or {@link OpportunityRecordPage} pages. </b>
 */
public class InvoicingRequestApprovalCreationModal extends BaseInvoiceApprovalCreationModal {

    //  Fields

    //  'Information' section
    public final LightningCombobox industryPicklist = new LightningCombobox("Industry", dialogContainer);

    //  'MultiProduct Limits' section
    public final SelenideElement signUpPurchaseLimitOfficeInput = dialogContainer
            .$x(".//label[contains(text(),'Sign-Up Purchase Limit - Office')]/following-sibling::div/input");
    public final SelenideElement monthlyCreditLimitOfficeInput = dialogContainer
            .$x(".//label[contains(text(),'Monthly Credit Limit - Office')]/following-sibling::div/input");
    public final SelenideElement signUpPurchaseLimitEngageDigitalInput = dialogContainer
            .$x(".//label[contains(text(),'Sign-Up Purchase Limit - Engage Digital')]/following-sibling::div/input");
    public final SelenideElement monthlyCreditLimitEngageDigitalInput = dialogContainer
            .$x(".//label[contains(text(),'Monthly Credit Limit - Engage Digital')]/following-sibling::div/input");
    public final SelenideElement signUpPurchaseLimitRcCcInput = dialogContainer
            .$x(".//label[contains(text(),'Sign-Up Purchase Limit - Contact Center')]/following-sibling::div/input");
    public final SelenideElement monthlyCreditLimitRcCcInput = dialogContainer
            .$x(".//label[contains(text(),'Monthly Credit Limit - Contact Center')]/following-sibling::div/input");

    /**
     * Constructor for the modal window to locate it via its default header.
     */
    public InvoicingRequestApprovalCreationModal() {
        super(INVOICING_REQUEST_RECORD_TYPE);
    }
}
