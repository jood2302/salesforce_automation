package com.aquiva.autotests.rc.page.salesforce.approval.modal;

import com.aquiva.autotests.rc.page.salesforce.GenericSalesforceModal;
import com.aquiva.autotests.rc.page.salesforce.approval.ApprovalPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;

/**
 * Modal window in {@link ApprovalPage}.
 * Opened when 'Submit for Approval' action button is pressed on the Approval record.
 * <br/>
 * This dialog is to submit the Approval record for approval
 * to begin using standard SFDC approval process.
 */
public class SubmitForApprovalModal extends GenericSalesforceModal {
    public final SelenideElement commentsInput = dialogContainer.$("textarea");
    public final SelenideElement submitButton = dialogContainer.$(byText("Submit"));

    /**
     * Constructor for the modal window to locate it via its default header.
     */
    public SubmitForApprovalModal() {
        super("Submit for Approval");
    }
}
