package com.aquiva.autotests.rc.page.salesforce;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static java.time.Duration.ofSeconds;
import static java.util.Objects.requireNonNull;

/**
 * Standard Salesforce modal window activated via standard lookup input fields.
 * Helps to find more records for the lookups using the search queries from user.
 * <br/>
 * Use this modal when the search results displayed in the dropdown lists of the lookup are inconsistent.
 */
public class SalesforceLookupSearchModal extends GenericSalesforceModal {

    /**
     * Constructor for the modal window to locate it via its header.
     *
     * @param modalWindowHeaderSubstring search modal window's header
     */
    public SalesforceLookupSearchModal(String modalWindowHeaderSubstring) {
        super(requireNonNull(modalWindowHeaderSubstring));
    }

    /**
     * Select the record from the search results and click "Select" button.
     * by its Name (first column of the search results table).
     *
     * @param searchedRecordName name of the record to be selected
     */
    public void selectItemInSearchResults(String searchedRecordName) {
        dialogContainer
                .$x(".//*[@data-cell-value='" + searchedRecordName + "']//ancestor::tbody/tr/td[1]")
                .shouldBe(visible, ofSeconds(30))
                .click();

        dialogContainer
                .$(byText("Select"))
                .shouldBe(visible, ofSeconds(30))
                .click();
    }
}
