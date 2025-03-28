package com.aquiva.autotests.rc.page.components.lookup;

import com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.Condition.visible;
import static java.time.Duration.ofSeconds;

/**
 * Custom LWC Lookup web component used to dynamically send text search requests as user inputs text into it
 * (built as a custom Lightning Web Component using Lightning Design System elements).
 * <br/>
 * Can be found on {@link LeadConvertPage}.
 */
public class CustomLwcLookupComponent extends AbstractLookupComponent {

    /**
     * Constructor with web element as a parameter.
     *
     * @param searchInputComponentElement SelenideElement that used to locate search input element in DOM.
     */
    public CustomLwcLookupComponent(SelenideElement searchInputComponentElement) {
        super(searchInputComponentElement);
    }

    /**
     * Get selected entity's web element.
     * Useful to get entity's name from it (e.g. Account Name).
     *
     * @return entity that was found and selected in search input component
     */
    public SelenideElement getSelectedEntity() {
        return searchInputComponentElement.$(".slds-truncate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitUntilSearchResultsAreVisible() {
        super.waitUntilSearchResultsAreVisible();
        searchInputComponentElement
                .$$x(".//div[@role='listbox']//li//*[local-name() = 'svg'][@data-key='clock']")
                .shouldBe(empty.because("Lookup request is finished when there are no 'clock' icons on found entities"),
                        ofSeconds(10));
    }

    /**
     * Enter search query and select found element from the drop-down list.
     *
     * @param searchQuery name of the searched entity
     */
    public void selectItemInCombobox(String searchQuery) {
        selectItemInCombobox(searchQuery, searchQuery);
    }

    /**
     * Enter search query and select found element from the drop-down list element.
     * Useful when the test needs to find an Account by one of its parameters (e.g. RC_User_ID__c)
     *
     * @param searchQuery      any string that's associated with searched entity (e.g. name, ID, etc...)
     * @param searchedItemName name of the searched entity (e.g. Account Name)
     */
    public void selectItemInCombobox(String searchQuery, String searchedItemName) {
        clear();

        /*
        Letter-by-letter query input is used because of flaky behavior of search requests.
        Sometimes default "pasting" the whole text string in the input field
        brings incorrect search results or no results at all, and development team won't fix it :(
        Sending keys one by one helps to bring correct search results most of the time (90-95%)
        */
        selectItemInComboboxWithByLetterInput(searchQuery, searchedItemName);

        getSelectedEntity().shouldBe(visible);
    }
}
