package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.shippingtab;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;

/**
 * A single block on the {@link ShippingPage} from the "Shipping Groups" section.
 * It contains distributed devices on the group, shipping address, etc.
 */
public class ShippingGroup {
    private final SelenideElement groupContainer;

    /**
     * Constructor for the shipping group that defines its position on the web page.
     *
     * @param groupContainer web element for the main container of the item
     */
    public ShippingGroup(SelenideElement groupContainer) {
        this.groupContainer = groupContainer;
    }

    /**
     * Return actual web element behind Shipping Group component.
     * <p></p>
     * Useful if test needs to perform actions on the web element itself
     * via Selenide framework actions (waits, assertions, etc...)
     *
     * @return web element that represents Shipping Group in the DOM.
     */
    public SelenideElement getSelf() {
        return groupContainer;
    }

    /**
     * Return web element for the button to edit the Shipping Group's address.
     *
     * @return web element for the button to edit the Shipping Group's address
     */
    public SelenideElement getEditButton() {
        return groupContainer.$x(".//button[@title='Edit Location']");
    }

    /**
     * Get a row for the given device in "Shipping Groups" section.
     * It contains info like device's name, quantity.
     *
     * @param deviceName displayed name of the device (e.g. "Polycom IP 5000 Conference Phone")
     * @return Shipping Device object to extract other parameters from (e.g. quantity)
     */
    public ShippingGroupAssignedDevice getAssignedDevice(String deviceName) {
        var assignedDeviceContainer = groupContainer
                .$x(".//shipping-location-prod-item[.//div[contains(text(), '" + deviceName + "')]]");
        return new ShippingGroupAssignedDevice(assignedDeviceContainer);
    }

    /**
     * Get the names for all the shipping devices that are assigned to this group.
     *
     * @return collection of the names for all the shipping devices in the current shipping group
     */
    public ElementsCollection getAllShippingDevicesNames() {
        return groupContainer.$$("shipping-location-prod-item .product-name");
    }

    /**
     * Get a shipping cost for the current shipping group.
     *
     * @return web element that contains shipping cost for the current shipping group
     */
    public SelenideElement getShippingCost() {
        return groupContainer.$(".shipping-cost");
    }

    /**
     * Set a new quantity for the assigned device on the group.
     *
     * @param deviceName  displayed name of the device (e.g. "Polycom IP 5000 Conference Phone")
     * @param newQuantity quantity of assigned devices (e.g. "99")
     */
    public void setQuantityForAssignedDevice(String deviceName, int newQuantity) {
        getAssignedDevice(deviceName)
                .getQuantityInput()
                .shouldBe(visible, enabled)
                .setValue(String.valueOf(newQuantity))
                .unfocus();
    }
}
