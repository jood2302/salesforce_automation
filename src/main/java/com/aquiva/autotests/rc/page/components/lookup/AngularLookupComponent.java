package com.aquiva.autotests.rc.page.components.lookup;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.AccountManagerModal;
import com.aquiva.autotests.rc.page.opportunity.OpportunityCreationPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$x;
import static java.time.Duration.ofSeconds;

/**
 * Angular version of Lookup web component used to dynamically send text search requests as user inputs text into it
 * (built using Angular JS framework).
 * <br/>
 * Can be found on {@link OpportunityCreationPage}, {@link AccountManagerModal}, among others.
 */
public class AngularLookupComponent extends AbstractLookupComponent {

    /**
     * Constant for search without results.
     */
    public static final String NO_RESULTS = "No results to display";

    /**
     * Constructor for lookup component with default locator for parent web element.
     */
    public AngularLookupComponent() {
        super($x("//div[@data-ui-auto='defaultAccountLookupCombobox']"));
    }

    /**
     * Constructor for lookup component with web element as a parameter for its parent.
     *
     * @param searchInputComponentElement SelenideElement that used to locate lookup element in DOM.
     */
    public AngularLookupComponent(SelenideElement searchInputComponentElement) {
        super(searchInputComponentElement);
    }

    /**
     * Get selected entity's web element.
     * Useful to get entity's name from it (e.g. Account Name).
     *
     * @return entity that was found and selected in lookup component
     */
    public SelenideElement getSelectedEntity() {
        return searchInputComponentElement.$("div.selected-value");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitUntilSearchResultsAreVisible() {
        super.waitUntilSearchResultsAreVisible();
        searchInputComponentElement.$(".animated-background").shouldBe(hidden, ofSeconds(10));
    }

    /**
     * Enter search query and select found element from the drop-down list element.
     *
     * @param searchQuery name of the searched entity
     */
    public void selectItemInCombobox(String searchQuery) {
        clear();

        //  TODO Replace with the previous simple version when the Known Issue BZS-15986 is resolved
        selectItemInComboboxWithByLetterInput(searchQuery, searchQuery);

        getSelectedEntity().shouldBe(visible);
    }
}
