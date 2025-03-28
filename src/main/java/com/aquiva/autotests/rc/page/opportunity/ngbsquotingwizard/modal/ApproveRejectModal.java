package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * Modal window on {@link CartPage} activated by clicking on "Approve/Reject" button.
 * <p>
 * There's an Quote's approval functionality for the Existing Business Quotes (non-DQ).
 * <br/>
 * Once the EB Quote is submitted for approval via 'Submit For Approval' button
 * (see {@link CartPage#submitForApprovalButton})
 * any user with the appropriate access to the approval process
 * (e.g. assigned via standard Quote Approval Process approver)
 * can press "Approve/Reject" button on the {@link CartPage} and either approve or reject the Quote.
 * </p>
 */
public class ApproveRejectModal {
    private final SelenideElement dialogContainer = $("approve-reject-modal");

    public final SelenideElement commentInput = dialogContainer.$("textarea");
    public final SelenideElement approveButton = dialogContainer.$(byText("Approve"));
}
