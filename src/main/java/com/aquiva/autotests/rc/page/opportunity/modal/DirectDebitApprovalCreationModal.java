package com.aquiva.autotests.rc.page.opportunity.modal;

import com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage;
import com.aquiva.autotests.rc.page.salesforce.account.AccountRecordPage;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Approval__c;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.DIRECT_DEBIT_APPROVAL_RECORD_TYPE;

/**
 * {@link Approval__c} creation modal of 'Direct Debit Request' approval type.
 * <br/>
 * Contains some type-related fields, like 'Accounts Payable Email Address'.
 * <p>
 * <b> Note: Might be opened from {@link AccountRecordPage} or {@link OpportunityRecordPage} pages. </b>
 */
public class DirectDebitApprovalCreationModal extends BaseInvoiceApprovalCreationModal {

    //  Fields
    public final SelenideElement accountsPayableEmailAddressInput = dialogContainer
            .$x(".//label[contains(text(),'Accounts Payable Email Address')]/following-sibling::div/input");

    /**
     * Constructor for the modal window to locate it via its default header.
     */
    public DirectDebitApprovalCreationModal() {
        super(DIRECT_DEBIT_APPROVAL_RECORD_TYPE);
    }
}
