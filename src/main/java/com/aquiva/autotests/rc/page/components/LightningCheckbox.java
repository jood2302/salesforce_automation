package com.aquiva.autotests.rc.page.components;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.*;

/**
 * Standard Checkbox element from Lightning Web Components.
 *
 * @see <a href='https://www.lightningdesignsystem.com/components/checkbox/'>
 * Checkbox documentation</a>
 */
public class LightningCheckbox {
    private final SelenideElement input;
    private final SelenideElement checkbox;

    /**
     * Constructor that defines the checkbox in the DOM.
     *
     * @param inputElement web element for the rendered {@code <input>} HTML element of the checkbox
     */
    public LightningCheckbox(SelenideElement inputElement) {
        this.input = inputElement;
        this.checkbox = inputElement.$x("./following-sibling::span");
    }

    /**
     * Get the {@code <input>} part of the Checkbox component.
     * <br/>
     * Use it to check if the checkbox is enabled/disabled, checked/unchecked.
     */
    public SelenideElement getInput() {
        return input;
    }

    /**
     * Get the visible part of the Checkbox component.
     * <br/>
     * Use it to check if the checkbox is visible to the user.
     */
    public SelenideElement getCheckbox() {
        return checkbox;
    }

    /**
     * Set the checkbox to be selected or not.
     * <br/>
     * This method has to be used instead of {@link SelenideElement#setSelected(boolean)}
     * because {@code <input>} element for the checkbox is rendered invisible using CSS,
     * and {@link SelenideElement#setSelected(boolean)} only works with visible elements.
     *
     * @param isToBeSelected true, if the checkbox should be selected,
     *                       otherwise, it should be deselected.
     */
    public void setSelected(boolean isToBeSelected) {
        clickInput(isToBeSelected);
        verifySelectedInput(isToBeSelected);
    }

    /**
     * Click on the checkbox to select/deselect it
     * depending on the current state of the checkbox.
     *
     * @param isToBeSelected true, if the checkbox should be selected.
     */
    public void clickInput(boolean isToBeSelected) {
        var isSelectedNow = input.should(exist).isSelected();
        if (isToBeSelected != isSelectedNow) {
            input.click();
        }
    }

    /**
     * Verify if the checkbox is selected or not
     * depending on the expected state.
     *
     * @param isToBeSelected true, if the checkbox should be selected.
     */
    public void verifySelectedInput(boolean isToBeSelected) {
        input.shouldBe(isToBeSelected ? selected : not(selected));
    }
}
