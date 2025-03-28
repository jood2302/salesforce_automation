package com.aquiva.autotests.rc.page.components.lookup;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.*;
import static java.time.Duration.ofSeconds;

/**
 * Base class for various implementations of Lookup web component:
 * a search box used to send text request queries and select found entities from the drop-down list.
 * After entering some symbols the component may return relevant "lookup" results, if there's any, for user to select.
 * <br/><br/>
 * <p> - This component typically used in pages as search lookup field for Accounts, Contacts, Quotes, etc... </p>
 * <p> - Search requests are sent dynamically, with every change in the input field. </p>
 */
public abstract class AbstractLookupComponent {

    /**
     * Parent web element (container).
     */
    protected final SelenideElement searchInputComponentElement;

    /**
     * Constructor for lookup component with web element as a parameter for its parent.
     *
     * @param searchInputComponentElement SelenideElement that used to locate lookup element in DOM.
     */
    public AbstractLookupComponent(SelenideElement searchInputComponentElement) {
        this.searchInputComponentElement = searchInputComponentElement;
    }

    /**
     * Return actual web element behind lookup component.
     * <br/>
     * Useful if test needs to perform actions on the web element itself
     * via Selenide framework actions (waits, assertions, etc...)
     *
     * @return SelenideElement that represents lookup in the DOM.
     */
    public SelenideElement getSelf() {
        return searchInputComponentElement;
    }

    /**
     * Get input web element for entering a search query.
     *
     * @return SelenideElement that represents input part of the component
     */
    public SelenideElement getInput() {
        return searchInputComponentElement.$x(".//input");
    }

    /**
     * Get list of search results in the form of a drop-down list
     * after user enters some query in the input field.
     *
     * @return collection of search results after entering a search query
     */
    public ElementsCollection getSearchResults() {
        return searchInputComponentElement.$$x(".//div[@role='listbox']//li");
    }

    /**
     * Wait until search results are available.
     * Note: override this method and add a custom condition if needed in the child class.
     */
    public void waitUntilSearchResultsAreVisible() {
        getSearchResults().shouldHave(sizeGreaterThan(0));
    }

    /**
     * Clear the selected value in the lookup component
     * OR
     * clear anything entered in the input field.
     * If the input is empty, then do nothing.
     */
    public void clear() {
        var clearButton = searchInputComponentElement
                .shouldBe(visible, ofSeconds(30))
                .$x(".//button");
        if (clearButton.isDisplayed()) {
            clearButton.hover().click();
        } else if (getInput().val() != null && !getInput().val().isBlank()) {
            getInput().clear();
            getInput().unfocus();
        }

        getInput().shouldBe(visible, enabled);
    }

    /**
     * Enter search query letter-by-letter and select found element from the drop-down list element.
     *
     * @param searchQuery      any string that's associated with searched entity (e.g. name, ID, etc...)
     * @param searchedItemName name of the searched entity (e.g. Account Name)
     */
    public void selectItemInComboboxWithByLetterInput(String searchQuery, String searchedItemName) {
        for (int i = 0; i < searchQuery.length(); i++) {
            getInput().setValue(searchQuery.substring(0, i + 1));

            waitUntilSearchResultsAreVisible();

            var foundElement = getSearchResults().findBy(text(searchedItemName));
            if (foundElement.isDisplayed()) {
                foundElement.hover().click();
                break;
            }
        }
    }
}
