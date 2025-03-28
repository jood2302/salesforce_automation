package com.aquiva.autotests.rc.page.salesforce;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.SObject;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static java.time.Duration.ofSeconds;

/**
 * Base class for Salesforce record creation modal window.
 * <p>
 * Used for creating different Salesforce {@link SObject} records.
 * Contains some common fields and buttons for every type of Salesforce record modal window creation,
 * like 'Save' and 'Close' button, errors list, etc.
 * </p>
 */
public abstract class RecordCreationModal extends GenericSalesforceModal {

    public final ErrorsPopUpModal errorsPopUpModal = new ErrorsPopUpModal();

    public final SelenideElement spinner = $("[data-aura-class='forceModalSpinner']");

    /**
     * Constructor for SObject record creation modal window with initialization
     * of its dialog container's locator using its header's title.
     *
     * @param modalWindowHeaderSubstring string that header's title of the modal window contains
     */
    public RecordCreationModal(String modalWindowHeaderSubstring) {
        super(modalWindowHeaderSubstring);
    }

    /**
     * Get 'Save' button in record creation modal window.
     *
     * @return SelenideElement that represents 'Save' button
     */
    public SelenideElement getSaveButton() {
        return dialogContainer.$(byText("Save"));
    }

    /**
     * Get collection of section headers in record creation modal window.
     *
     * @return collection of web elements that represent section headers
     */
    public ElementsCollection getSectionHeaders() {
        return dialogContainer.$$("records-record-layout-section h3");
    }

    /**
     * Submit the changes on the modal and wait until it is closed.
     */
    public void saveChanges() {
        getSaveButton().click();

        getSaveButton().shouldBe(hidden, ofSeconds(30));
        spinner.shouldBe(hidden, ofSeconds(30));
    }
}
