package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab;

import com.aquiva.autotests.rc.model.ngbs.testdata.AreaCode;
import com.aquiva.autotests.rc.page.components.AreaCodeSelector;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static org.openqa.selenium.By.cssSelector;

/**
 * A single item on the Area Code assignment modal window.
 * Represents the row with Area Code, quantity, and some action buttons (e.g. 'delete' button).
 * Used for specific cart items, like Main Local Number, Main Local Fax Number, Main Toll-Free Number.
 */
public class AreaCodeLineItem {
    private final SelenideElement areaCodeLineItem;
    private final By areaCodeQuantityInput = cssSelector("input[type='number']");
    private final By deleteAreaCodeButton = cssSelector("[iconname='delete'] button");
    private final By addMoreAreaCodeButton = cssSelector("[data-ui-auto='add-more-area-code-button'] button");
    private final By availableAmountText = cssSelector("div.availability");

    /**
     * Constructor for the Area Code Line Item
     * that defines its position on the web page.
     *
     * @param areaCodeLineItem web element for the core element of the Area Code Line Item
     */
    public AreaCodeLineItem(SelenideElement areaCodeLineItem) {
        this.areaCodeLineItem = areaCodeLineItem;
    }

    /**
     * Get the selector for Area Codes for the current item.
     */
    public AreaCodeSelector getAreaCodeSelector() {
        return new AreaCodeSelector(areaCodeLineItem.$x(".//*[@data-ui-auto='lookupCombobox']"));
    }
    
    /**
     * Get input element for the number of Area Codes to assign to the cart item.
     */
    public SelenideElement getQuantityInput() {
        return areaCodeLineItem.$(areaCodeQuantityInput);
    }

    /**
     * Get the available phone numbers for the selected area code.
     * <br/>
     * Usually looks like a text "Available: N", where N - number of the available phone numbers.
     */
    public SelenideElement getAvailableAmountText() {
        return areaCodeLineItem.$(availableAmountText);
    }

    /**
     * Get 'delete' button to delete the current item.
     */
    public SelenideElement getDeleteButton() {
        return areaCodeLineItem.$(deleteAreaCodeButton);
    }

    /**
     * Get the button to add more Area Codes Line Items to the current item.
     * <br/>
     * Note: only visible for the latest visible Area Code Line Item!
     */
    public SelenideElement getAddMoreButton() {
        return areaCodeLineItem.$(addMoreAreaCodeButton);
    }

    /**
     * Select the Area Code for the current Area Code Line Item.
     *
     * @param areaCode Area Code entity with name of the country, state, city, and the code to assign
     */
    public void setAreaCode(AreaCode areaCode) {
        getAreaCodeSelector().selectCode(areaCode);
    }
}
