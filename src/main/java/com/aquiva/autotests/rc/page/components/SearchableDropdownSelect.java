package com.aquiva.autotests.rc.page.components;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$x;

/**
 * Searchable Dropdown selector component (custom LWC in RC project).
 * <br/>
 * User can enter search queries and select an option from the search results.
 */
public class SearchableDropdownSelect {
    private final SelenideElement container;

    /**
     * Constructor for Searchable Dropdown selector with the main container/component as a parameter.
     *
     * @param label label for the selector (usually used on top of it as its name)
     */
    public SearchableDropdownSelect(SelenideElement label) {
        this.container = $x("//c-searchable-dropdown[.//label='" + label + "']/*");
    }

    /**
     * Constructor for Searchable Dropdown selector with the main container/component as a parameter.
     *
     * @param label           label for the selector (usually used on top of it as its name)
     * @param parentContainer parent container element
     */
    public SearchableDropdownSelect(String label, SelenideElement parentContainer) {
        this.container = parentContainer.$x(".//c-searchable-dropdown[.//label='" + label + "']/*");
    }

    /**
     * Get input element for the selector.
     * Can be used to evaluate the selected option or the state (enabled/disabled).
     *
     * @return input element for the combobox
     */
    public SelenideElement getInput() {
        return container.$("input");
    }

    /**
     * Get available options for selection.
     * <br/>
     * Note: make sure to click on {@link #getInput()} first to make the available options visible.
     *
     * @return collection of the web elements for options to select
     */
    public ElementsCollection getOptions() {
        return container.$$("li");
    }

    /**
     * Enter the search query, and select an option from the search results.
     *
     * @param option option that is to be selected in the combobox
     */
    public void selectOption(String option) {
        getInput().click();
        getInput().setValue(option);
        getOptions().findBy(exactTextCaseSensitive(option)).click();

        getInput().shouldHave(exactValue(option));
    }

    /**
     * Enter the search query, and select an option from the search results (using its <b>partial</b> text).
     *
     * @param option option that is to be selected in the dropdown (as substring)
     */
    public void selectOptionContainingText(String option) {
        getInput().click();
        getInput().setValue(option);
        getOptions().findBy(textCaseSensitive(option)).click();

        getInput().shouldHave(partialValue(option));
    }
}
