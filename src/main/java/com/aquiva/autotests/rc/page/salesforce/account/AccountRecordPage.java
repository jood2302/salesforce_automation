package com.aquiva.autotests.rc.page.salesforce.account;

import com.aquiva.autotests.rc.page.opportunity.modal.NewApprovalRecordTypeSelectionModal;
import com.aquiva.autotests.rc.page.salesforce.RecordPage;
import com.aquiva.autotests.rc.page.salesforce.account.modal.AccountViewerModal;
import com.aquiva.autotests.rc.page.salesforce.account.modal.BobWholesalePartnerConfirmationModal;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Account;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThanOrEqual;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;
import static java.time.Duration.ofSeconds;

/**
 * The Salesforce page that displays Account({@link Account}) record information,
 * such as Account Name, linked Opportunities, linked Contacts etc.
 */
public class AccountRecordPage extends RecordPage {

    //  Error notifications
    public static final String PRIMARY_SIGNATORY_CONTACT_ROLE_ERROR = "Please add a Primary Signatory Contact Role on the Account";

    //  Related lists' headers
    public static final String OPPORTUNITIES_RELATED_LIST = "Opportunities";
    public static final String APPROVALS_RELATED_LIST = "Approvals";

    public final SelenideElement opportunitiesTab = $x("//li[@title='Opportunities']");
    public final SelenideElement approvalTab = $x("//li[@title='Approvals']");

    //  Modal windows
    public final NewApprovalRecordTypeSelectionModal newApprovalRecordTypeSelectionModal = new NewApprovalRecordTypeSelectionModal();
    public final AccountViewerModal accountViewerModal = new AccountViewerModal();
    public final BobWholesalePartnerConfirmationModal removeBobWholesalePartnerModal = new BobWholesalePartnerConfirmationModal();
    public final AccountHighlightsPage accountHighlightsPage = new AccountHighlightsPage();

    /**
     * {@inheritDoc}
     */
    public void waitUntilLoaded() {
        detailsTab.shouldBe(visible, ofSeconds(100));
        visibleLightingActionButtons.shouldHave(sizeGreaterThanOrEqual(3), ofSeconds(100));
    }

    /**
     * Click on 'Account Viewer' button.
     * <br/><br/>
     * This method searches 'Account Viewer' button among Lightning Experience actions
     * in the upper right corner of the page
     * (even if the button is hidden in the 'show more actions' list).
     */
    public void clickAccountViewerButton() {
        clickDetailPageButton("Account Viewer");
    }

    /**
     * Click on 'Create NDA' button.
     * <br/><br/>
     * This method searches 'Create NDA' button among Lightning Experience actions
     * in the upper right corner of the page
     * (even if the button is hidden in the 'show more actions' list).
     */
    public void clickCreateNdaButton() {
        clickDetailPageButton("Create NDA");
    }

    /**
     * Click on 'Account Plan' button.
     * <br/><br/>
     * This method searches 'Account Plan' button among Lightning Experience actions
     * in the upper right corner of the page
     * (even if the button is hidden in the 'show more actions' list).
     */
    public void clickAccountPlanButton() {
        clickDetailPageButton("Account Plan");
    }

    /**
     * Open Create New Approval Modal window from 'Approval' tab.
     */
    public void openCreateNewApprovalModal() {
        approvalTab.click();
        clickHiddenListButtonOnRelatedList(APPROVALS_RELATED_LIST, NEW_BUTTON_LABEL);
    }
}
