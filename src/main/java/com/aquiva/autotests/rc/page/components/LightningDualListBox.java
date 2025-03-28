package com.aquiva.autotests.rc.page.components;

import com.codeborne.selenide.SelenideElement;

import java.util.List;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

/**
 * Standard Dual Listbox element (double list of 'Available' and 'Chosen' for multiple options selections)
 * from Lightning Web Components.
 *
 * @see <a href='https://developer.salesforce.com/docs/component-library/bundle/lightning-dual-listbox'>
 * lightning-combobox documentation</a>
 */
public class LightningDualListBox {
    private final SelenideElement dualListBoxContainer;

    /**
     * Constructor for LWC Dual Listbox (multiple options' selector)
     * with the main container/component as a parameter.
     *
     * @param dualListBoxElement web element that represents main dual listbox container
     *                           (should have {@code "lightning-dual-listbox"} HTML tag)
     */
    public LightningDualListBox(SelenideElement dualListBoxElement) {
        dualListBoxContainer = dualListBoxElement;
    }

    /**
     * Select options from the list of the available ones.
     * Effectively, moves the options from the 'Available' (left) list
     * to the 'Chosen' (right) list.
     *
     * @param options list of options that should be selected in the listbox
     */
    public void selectOptions(List<String> options) {
        options.forEach(option -> {
            dualListBoxContainer.$$("ul[id*='source-list'] > li").findBy(text(option))
                    .scrollIntoView("{block: \"center\"}")
                    .click();
            dualListBoxContainer.$x(".//button[contains(@title, 'to Chosen')]").doubleClick(); //  single-click might randomly fail

            dualListBoxContainer.$$("ul[id*='selected-list'] > li").findBy(text(option)).shouldBe(visible);
        });
    }
}
