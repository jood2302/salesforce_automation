package com.aquiva.autotests.rc.page.components;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.ClickOptions.usingJavaScript;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$x;
import static java.time.Duration.ofSeconds;

/**
 * Standard Combobox element (dropdown list of options / "picklist") from Lightning Web Components.
 *
 * @see <a href='https://developer.salesforce.com/docs/component-library/bundle/lightning-combobox'>
 * lightning-combobox documentation</a>
 */
public class LightningCombobox {
    private final SelenideElement comboboxContainer;

    /**
     * Constructor for LWC Combobox (picklist).
     *
     * @param comboboxLabel label element for the picklist
     *                      (usually used in front of the dropdown as its name)
     */
    public LightningCombobox(String comboboxLabel) {
        this.comboboxContainer =
                $x("//lightning-combobox[.//label[contains(text(), '" + comboboxLabel + "')]]");
    }

    /**
     * Constructor for LWC Combobox (picklist) with parent container element as parameter.
     *
     * @param comboboxLabel     label element for the picklist
     *                          (usually used in front of the dropdown as its name)
     * @param comboboxContainer parent container element
     */
    public LightningCombobox(String comboboxLabel, SelenideElement comboboxContainer) {
        this.comboboxContainer =
                comboboxContainer.$x(".//lightning-combobox[.//label[contains(text(), '" + comboboxLabel + "')]]");
    }

    /**
     * Constructor for LWC Combobox (picklist) with the main container/component as a parameter.
     *
     * @param comboboxElement web element that represents main combobox container
     *                        (should have {@code "lightning-combobox"} HTML tag)
     */
    public LightningCombobox(SelenideElement comboboxElement) {
        this.comboboxContainer = comboboxElement;
    }

    /**
     * Get input element for the combobox ("button" tag in the layout).
     * Can be used to evaluate the selected option or the state (enabled/disabled).
     *
     * @return input element for the combobox
     */
    public SelenideElement getInput() {
        return comboboxContainer.$x(".//lightning-base-combobox//button");
    }

    /**
     * Get available options for selection.
     * <br/>
     * Note: make sure to click on {@link #getInput()} first to make the available options visible.
     *
     * @return collection of the web elements for options to select
     */
    public ElementsCollection getOptions() {
        return comboboxContainer.$$x(".//lightning-base-combobox-item");
    }

    /**
     * Select an option from the dropdown.
     *
     * @param option option that is to be selected in the combobox
     */
    public void selectOption(String option) {
        getInput().shouldBe(enabled).click(usingJavaScript());
        getOptions().findBy(or("'data-value' or 'title' or exact text",
                        attribute("data-value", option),
                        attribute("title", option),
                        exactTextCaseSensitive(option)))
                .shouldBe(visible, ofSeconds(20))
                .click();
    }

    /**
     * Select an option from the dropdown using its partial text.
     *
     * @param option option that is to be selected in the combobox (as substring)
     */
    public void selectOptionContainingText(String option) {
        getInput().shouldBe(enabled).click(usingJavaScript());
        getOptions().findBy(textCaseSensitive(option))
                .shouldBe(visible, ofSeconds(20))
                .click();
    }
}
