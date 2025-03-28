package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.openqa.selenium.By.cssSelector;

/**
 * A single block for the product (usually, a phone) on the device assignment modal window.
 * <br/>
 * User can add one or more area codes, and assign different quantities of the product to each area code.
 */
public class DeviceAssignmentProductItem {
    private final SelenideElement productItem;
    private final By nameElement = cssSelector("div[data-ui-auto-device-assignment-product-name]");
    private final By totalNumber = cssSelector("[data-ui-auto='device-total-number']");
    private final By availableNumber = cssSelector("[data-ui-auto='device-available-number']");
    private final By addAreaCodeButton = cssSelector("c-button[iconname='add']");
    private final By deviceAssignmentAreaCodeItems = cssSelector("device-assignment-area-code");

    /**
     * Constructor for the Device Assignment Product Item
     * that defines its position on the page/modal window.
     *
     * @param productItem main web element that represents the product item
     */
    public DeviceAssignmentProductItem(SelenideElement productItem) {
        this.productItem = productItem;
    }

    /**
     * Get the web element for the name of the product (e.g. "Polycom VVX 101 Basic IP Phone - Refurbished")
     * in the header of the item.
     */
    public SelenideElement getNameElement() {
        return productItem.$(nameElement);
    }

    /**
     * Get the web element for the total number of the product available for assignment.
     * <br/>
     * Corresponds to the total number of devices added to the cart on the Price tab.
     */
    public SelenideElement getTotalNumber() {
        return productItem.$(totalNumber);
    }

    /**
     * Get the web element for the available number of the product for assignment.
     * <br/>
     * Corresponds to the number of devices that are still not assigned to any area code.
     */
    public SelenideElement getAvailableNumber() {
        return productItem.$(availableNumber);
    }

    /**
     * Get the button to add a new area code to the device.
     */
    public SelenideElement getAddAreaCodeButton() {
        return productItem.$(addAreaCodeButton);
    }

    /**
     * Get the collection of web elements for all added area code items of the product.
     */
    public ElementsCollection getDeviceAssignmentAreaCodeItems() {
        return productItem.$$(deviceAssignmentAreaCodeItems);
    }

    /**
     * Get the collection of the area code items for the product
     * as {@link DeviceAssignmentAreaCodeItem} objects.
     */
    public List<DeviceAssignmentAreaCodeItem> getChildAreaCodeItems() {
        return getDeviceAssignmentAreaCodeItems().asDynamicIterable().stream()
                .map(DeviceAssignmentAreaCodeItem::new)
                .collect(toList());
    }
}
