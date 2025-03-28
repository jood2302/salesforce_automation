package com.aquiva.autotests.rc.page.opportunity.modal;

import com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage;
import com.aquiva.autotests.rc.page.salesforce.GenericSalesforceModal;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Approval__c;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selectors.byValue;
import static java.time.Duration.ofSeconds;

/**
 * Modal window in {@link OpportunityRecordPage}
 * activated by clicking on 'New' button from Approvals section.
 *
 * <p> Used for selecting type of {@link Approval__c} to be created. </p>
 */
public class NewApprovalRecordTypeSelectionModal extends GenericSalesforceModal {

    public final SelenideElement nextButton = dialogContainer.$(byText("Next"));

    /**
     * Constructor for the modal window to locate it via its default header.
     */
    public NewApprovalRecordTypeSelectionModal() {
        super("New Approval");
    }

    /**
     * Select type of Approval to be created.
     *
     * @param approvalType name of the Approval type (e.g. "Agent Credit Transfers")
     */
    public void selectApprovalType(String approvalType) {
        var recordTypesContainer = dialogContainer.$(".recordTypesWrapper");
        recordTypesContainer.shouldBe(visible, ofSeconds(30))
                .$(byValue(approvalType))
                .click();
    }
}
