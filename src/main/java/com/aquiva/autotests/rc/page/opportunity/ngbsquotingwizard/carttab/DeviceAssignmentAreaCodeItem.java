package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab;

import com.aquiva.autotests.rc.page.components.AreaCodeSelector;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static org.openqa.selenium.By.cssSelector;

/**
 * A single item of the Area Code on the Device Assignment modal window for each compatible product
 * (e.g. a phone).
 * Represents the row with Area Code, quantity, and some action buttons (e.g. 'delete' button).
 * Used for specific cart items, like DigitalLine Unlimited, DigitalLine Basic, Common Phone Core.
 */
public class DeviceAssignmentAreaCodeItem {
    private final SelenideElement areaCodeItem;
    private final By assignedItemsInput = cssSelector("[data-ui-auto='device-assignment-area-code-quantity']");
    private final By deleteButton = cssSelector("[data-ui-auto='remove-area-code-lookup']");
    private final By availableAmountText = cssSelector("div.availability");

    /**
     * Constructor for the Area Code Item
     * that defines its position on the web page (device assignment modal).
     *
     * @param areaCodeItem main web element for the item
     */
    public DeviceAssignmentAreaCodeItem(SelenideElement areaCodeItem) {
        this.areaCodeItem = areaCodeItem;
    }

    /**
     * Get the selector for Area Codes for the current item.
     */
    public AreaCodeSelector getAreaCodeSelector() {
        return new AreaCodeSelector(areaCodeItem.$x(".//*[@data-ui-auto='lookupCombobox']"));
    }

    /**
     * Get the available phone numbers for the selected area code.
     * <br/>
     * Usually looks like a text "Available: N", where N - number of the available phone numbers.
     */
    public SelenideElement getAvailableAmountText() {
        return areaCodeItem.$(availableAmountText);
    }

    /**
     * Get the input field for the quantity of the product to assign to the selected area code.
     */
    public SelenideElement getAssignedItemsInput() {
        return areaCodeItem.$(assignedItemsInput);
    }

    /**
     * Get the 'delete' button to delete the current area code item.
     */
    public SelenideElement getDeleteButton() {
        return areaCodeItem.$(deleteButton);
    }
}
