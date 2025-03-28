package com.aquiva.autotests.rc.page.salesforce.contact;

import com.aquiva.autotests.rc.page.salesforce.GenericSalesforceModal;
import com.codeborne.selenide.SelenideElement;

/**
 * Modal window in {@link ContactRecordPage}
 * activated by clicking on "Delete" button.
 * <p>
 * This dialog allows User to delete Contact.
 * <p/>
 */
public class DeleteContactModal extends GenericSalesforceModal {

    //  Buttons
    public final SelenideElement cancelButton = dialogContainer.$("[title='Cancel']");
    public final SelenideElement deleteButton = dialogContainer.$("[title='Delete']");

    /**
     * Constructor for the modal window to locate it via its default header.
     */
    public DeleteContactModal() {
        super("Delete Contact");
    }
}
