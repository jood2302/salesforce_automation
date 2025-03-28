package com.aquiva.autotests.rc.page.salesforce.approval.modal;

import com.aquiva.autotests.rc.page.salesforce.GenericSalesforceModal;
import com.aquiva.autotests.rc.page.salesforce.approval.ApprovalPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;

/**
 * Modal window in {@link ApprovalPage}.
 * Opened when 'Approve' action button is pressed in 'Approval History' related list of the Approval record.
 * <p>
 * This dialog is to approve the Approval record using standard SFDC approval process.
 * </p>
 */
public class ApproveApprovalModal extends GenericSalesforceModal {

    /**
     * Constructor for the modal window to locate it via its default header.
     */
    public ApproveApprovalModal() {
        super("Approve Approval");
    }

    public final SelenideElement commentInput = dialogContainer.$("textarea");
    public final SelenideElement approveButton = dialogContainer.$(byText("Approve"));
}
