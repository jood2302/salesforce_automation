package com.aquiva.autotests.rc.page.opportunity.modal;

import com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

/**
 * Modal window on {@link OpportunityRecordPage}
 * activated by clicking on 'Close' button on Opportunity with the Quote that has Engage Legal Approval.
 */
public class RecallEngageLegalApprovalModal {

    //  String constants used on the form
    public static final String HEADER_TEXT = "Recall the Engage Legal Approval?";
    public static final String CONTENT_TEXT = "Please note that Legal Engagement approval is in progress. " +
            "You must recall the approval to close the opportunity.";

    //  Page elements
    private final SelenideElement dialogContainer = $("[aria-describedby='modal-content-id-1']");

    public final SelenideElement header = dialogContainer.$("#modal-heading-01");
    public final SelenideElement content = dialogContainer.$("#modal-content-id-1");

    //  Buttons
    public final SelenideElement closeButton = dialogContainer.$("[title='close']");
    public final SelenideElement recallButton = dialogContainer.$("[title='Recall Legal Engagement and Close']");
}
