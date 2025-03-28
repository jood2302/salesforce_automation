package com.aquiva.autotests.rc.page;

import com.aquiva.autotests.rc.page.components.lookup.AngularLookupComponent;
import com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage;
import com.aquiva.autotests.rc.page.salesforce.IframePage;
import com.aquiva.autotests.rc.page.salesforce.account.AccountRecordPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * Account Viewer page that shows Account Relations on {@link AccountRecordPage}
 * or on {@link OpportunityRecordPage} after Sign Up attempt, in case of ELA Service Account has errors.
 */
public class AccountViewerPage extends IframePage {

    //  Error messages
    public static final String PLEASE_SELECT_NEW_PARTNER_ERROR = "Please select new partner first.";

    public final SelenideElement hierarchyContainer = $("#account-viewer-hierarchy-container");

    //  Tabs
    public final SelenideElement partnerOperationsTab = $(byText("Partner Operations"));

    //  Buttons
    public final SelenideElement moveToInvoiceButton = $(byText("Move to Invoice"));
    public final SelenideElement switchToPartnerButton = $(byText("Switch to Partner"));
    public final SelenideElement partnerAccountSearchInputError = $(".slds-form-element__help");
    public final AngularLookupComponent partnerAccountSearchInput = new AngularLookupComponent();

    /**
     * Constructor for Account Viewer page with iframe's web element as parameter.
     * Defines Account Viewer location.
     */
    public AccountViewerPage(SelenideElement iframeElement) {
        super(iframeElement);
    }
}
