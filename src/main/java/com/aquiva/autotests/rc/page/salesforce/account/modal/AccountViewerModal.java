package com.aquiva.autotests.rc.page.salesforce.account.modal;

import com.aquiva.autotests.rc.page.AccountViewerPage;
import com.aquiva.autotests.rc.page.salesforce.GenericSalesforceModal;
import com.aquiva.autotests.rc.page.salesforce.account.AccountRecordPage;

/**
 * Modal window on {@link AccountRecordPage}
 * shown after the user clicks 'Account Viewer' action button..
 * <p>
 * This dialog manages Account Relations between Bill-on-Behalf and Partner Accounts.
 * </p>
 */
public class AccountViewerModal extends GenericSalesforceModal {
    public final AccountViewerPage accountViewer = new AccountViewerPage(dialogContainer.$x(".//iframe[@name='Account.Account_Viewer']"));
}
