package com.aquiva.autotests.rc.page.opportunity;

import com.aquiva.autotests.rc.page.salesforce.GenericSalesforceModal;
import com.aquiva.autotests.rc.page.salesforce.account.AccountRecordPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selectors.withText;
import static java.time.Duration.ofSeconds;

/**
 * The Salesforce modal window where the type of new Opportunity is selected.
 * Opens after:
 * <p> - clicking the 'New' button on Opportunities related list
 * on Account Record page({@link AccountRecordPage}) </p>
 * <p> - clicking the 'New' button on the Opportunity tab </p>
 */
public class OpportunityRecordTypeSelectionModal extends GenericSalesforceModal {

    public final SelenideElement nextButton = dialogContainer.$(byText("Next"));

    /**
     * Constructor for the modal window to locate it via its default header.
     */
    public OpportunityRecordTypeSelectionModal() {
        super("New Opportunity");
    }

    /**
     * Select a record type for an Opportunity Record Type Selector Modal Window.
     *
     * @param recordType the name of record type (e.g. 'New Sales Opportunity')
     */
    public void selectRecordType(String recordType) {
        var recordTypeSelectContainer = dialogContainer.$("div.changeRecordTypeRightColumn");
        recordTypeSelectContainer
                .shouldBe(visible, ofSeconds(20))
                .$(withText(recordType))
                .click();
    }
}
