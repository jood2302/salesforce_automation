package com.aquiva.autotests.rc.page.opportunity.modal;

import com.aquiva.autotests.rc.page.components.lookup.StandardLwcLookupComponent;
import com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage;
import com.aquiva.autotests.rc.page.salesforce.account.AccountRecordPage;
import com.sforce.soap.enterprise.sobject.Approval__c;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.INVOICE_ON_BEHALF_REQUEST_RECORD_TYPE;

/**
 * {@link Approval__c} creation modal of 'Invoice-on-Behalf Request' approval type.
 * <br/>
 * Contains some type-related fields, like 'Invoice Terms', address fields, 'Potential Users' etc.
 * <p>
 * <b> Note: Might be opened from {@link AccountRecordPage} or {@link OpportunityRecordPage} pages. </b>
 */
public class InvoiceOnBehalfApprovalCreationModal extends BaseInvoiceApprovalCreationModal {

    //  Error messages
    public static final String INCORRECT_ACCOUNTS_PAYABLE_CONTACT_ERROR =
            "Incorrect Contact is selected. Please select Accounts Payable Contact";
    public static final String INCORRECT_PARTNER_ACCOUNTS_PAYABLE_CONTACT_ERROR =
            "Incorrect Contact is selected. Please select Partner's Accounts Payable Contact";

    //  Fields
    
    //  'Invoice Request Detail' section
    public final StandardLwcLookupComponent partnerAccountsPayableContactSearchInput =
            new StandardLwcLookupComponent(dialogContainer.$x(".//*[./label[text()=\"Partner's Accounts Payable Contact\"]]//div[@class='slds-combobox_container']"), 
                    "Advanced Search");

    /**
     * Constructor for the modal window to locate it via its default header.
     */
    public InvoiceOnBehalfApprovalCreationModal() {
        super(INVOICE_ON_BEHALF_REQUEST_RECORD_TYPE);
    }
}
