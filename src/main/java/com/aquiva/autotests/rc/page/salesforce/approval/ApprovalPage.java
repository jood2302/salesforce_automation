package com.aquiva.autotests.rc.page.salesforce.approval;

import com.aquiva.autotests.rc.page.salesforce.RecordPage;
import com.aquiva.autotests.rc.page.salesforce.approval.modal.SubmitForApprovalModal;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Approval__c;

import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static java.time.Duration.ofSeconds;

/**
 * The Standard Salesforce page that displays Approval ({@link Approval__c}) record information,
 * such as Approval details (Approval Name, Approval's Account, etc...), action buttons,
 * related records and many more.
 */
public class ApprovalPage extends RecordPage {

    //  Notifications
    public static final String APPROVAL_SUBMITTED_FOR_APPROVAL = "Approval was submitted for approval.";

    //  Related lists headers
    public static final String APPROVAL_HISTORY_RELATED_LIST = "Approval History";

    //  Related lists
    public final ApprovalHistoryRelatedListPage approvalHistoryRelatedListPage = new ApprovalHistoryRelatedListPage();

    //  Modals
    private final SubmitForApprovalModal submitForApprovalModal = new SubmitForApprovalModal();

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitUntilLoaded() {
        entityTitle.shouldBe(visible, ofSeconds(60));
    }

    /**
     * Submit a record for approval.
     * <br/>
     * This method searches the button among Lightning Experience actions
     * in the upper right corner of the page, clicks it,
     * opens modal window and submits the record for approval.
     * <br/>
     * Note: use this method when you specifically need to submit the record for approval
     * in the <b>user</b> context!
     * For other case, consider using {@link EnterpriseConnectionUtils#submitRecordForApproval(String)}
     * to submit for approval from the main system admin user.
     */
    public void submitForApproval() {
        clickDetailPageButton("Submit For Approval");
        submitForApprovalModal.commentsInput.setValue(TEST_STRING);
        submitForApprovalModal.submitButton.click();
        
        submitForApprovalModal.submitButton.shouldBe(hidden, ofSeconds(30));
    }
}