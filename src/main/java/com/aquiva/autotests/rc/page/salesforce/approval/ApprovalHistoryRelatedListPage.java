package com.aquiva.autotests.rc.page.salesforce.approval;

import com.aquiva.autotests.rc.page.salesforce.GenericSalesforceModal;
import com.aquiva.autotests.rc.page.salesforce.ListViewPage;
import com.aquiva.autotests.rc.page.salesforce.approval.modal.ApproveApprovalModal;
import com.sforce.soap.enterprise.sobject.Approval__c;

import static com.aquiva.autotests.rc.utilities.Constants.BASE_URL;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

/**
 * Separate page for "Approval History" related records of {@link Approval__c} record.
 */
public class ApprovalHistoryRelatedListPage extends ListViewPage {
    public static final String APPROVE_BUTTON_LABEL = "Approve";

    //  Step's status
    public static final String SUBMITTED_STATUS = "Submitted";
    public static final String PENDING_STATUS = "Pending";
    public static final String APPROVED_STATUS = "Approved";

    //  Modal windows
    public final ApproveApprovalModal approvalModal = new ApproveApprovalModal();
    public final GenericSalesforceModal approveNotificationModal =
            new GenericSalesforceModal($(".uiModal.open div.slds-modal__container"));

    /**
     * Get the one of the rows among the Approval History steps.
     *
     * @param stepAssignedToValue value of 'Assigned To' column of the row
     * @return Approval History step's object from the table
     */
    public ApprovalHistoryStep getStep(String stepAssignedToValue) {
        //  [5] is the column index of 'Assigned To' field in the table
        var stepWebElement = container.$x(".//table/tbody/tr[./*[5]='" + stepAssignedToValue + "']");
        return new ApprovalHistoryStep(stepWebElement);
    }

    /**
     * Open the page with Approval History related list via direct URL.
     *
     * @param approvalId Salesforce ID of the related Approval__c record
     */
    public ApprovalHistoryRelatedListPage openPage(String approvalId) {
        open(format("%s/lightning/r/Approval__c/%s/related/ProcessSteps/view", BASE_URL, approvalId));
        return this;
    }

    /**
     * Approve the Approval Request via standard Approval modal.
     */
    public void approveApprovalRequest() {
        clickListViewPageButton(APPROVE_BUTTON_LABEL);
        approvalModal.commentInput
                .shouldBe(visible, ofSeconds(10))
                .setValue(TEST_STRING);
        approvalModal.approveButton.click();

        approvalModal.commentInput.shouldBe(hidden, ofSeconds(30));
    }
}
