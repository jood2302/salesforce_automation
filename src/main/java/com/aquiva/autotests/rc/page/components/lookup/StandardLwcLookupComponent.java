package com.aquiva.autotests.rc.page.components.lookup;

import com.aquiva.autotests.rc.page.salesforce.SalesforceLookupSearchModal;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.withText;
import static java.time.Duration.ofSeconds;

/**
 * Standard LWC Lookup web component used to dynamically send text search requests as user inputs text into it.
 * <br/>
 * Can be found on many standard Lightning record pages.
 */
public class StandardLwcLookupComponent extends AbstractLookupComponent {

    private final SelenideElement spinner = searchInputComponentElement.$x(".//div[@role='status']");

    private String searchRecordModalHeader;

    /**
     * Constructor with web element as a parameter.
     *
     * @param searchInputComponentElement SelenideElement that used to locate lookup element in DOM.
     */
    public StandardLwcLookupComponent(SelenideElement searchInputComponentElement) {
        super(searchInputComponentElement);
    }

    /**
     * Constructor with web element and search record modal header as parameters.
     *
     * @param searchInputComponentElement SelenideElement that used to locate lookup element in DOM.
     * @param searchRecordModalHeader     header of the modal window for the advanced Record Search functionality.
     */
    public StandardLwcLookupComponent(SelenideElement searchInputComponentElement, String searchRecordModalHeader) {
        this(searchInputComponentElement);
        this.searchRecordModalHeader = searchRecordModalHeader;
    }

    /**
     * Enter search query and select found element from the drop-down list.
     *
     * @param searchQuery name of the searched entity
     * @see #selectItemInComboboxViaSearchModal(String)
     */
    public void selectItemInCombobox(String searchQuery) {
        clear();
        getInput().setValue(searchQuery);

        spinner.shouldBe(hidden);
        getSearchResults()
                .findBy(text(searchQuery))
                .shouldBe(visible, ofSeconds(20))
                .click();

        getInput().shouldHave(value(searchQuery));
    }

    /**
     * Enter search query, open the record search modal,
     * and select the given record from its search results.
     * <br/>
     * Note: try this method if {@link #selectItemInCombobox(String)}
     * cannot find the records consistently.
     *
     * @param searchQuery name of the searched entity
     * @see #selectItemInCombobox(String)
     */
    public void selectItemInComboboxViaSearchModal(String searchQuery) {
        clear();
        getInput().setValue(searchQuery);

        spinner.shouldBe(hidden);
        getSelf()
                .$(withText("Show more results"))
                .shouldBe(visible, ofSeconds(20))
                .click();

        var lookupSearchModal = new SalesforceLookupSearchModal(this.searchRecordModalHeader);
        lookupSearchModal.selectItemInSearchResults(searchQuery);

        getInput().shouldHave(value(searchQuery));
    }
}
