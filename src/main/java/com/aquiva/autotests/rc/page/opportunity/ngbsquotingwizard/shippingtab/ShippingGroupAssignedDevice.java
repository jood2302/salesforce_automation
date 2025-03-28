package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.shippingtab;

import com.codeborne.selenide.SelenideElement;

/**
 * Any assigned device (i.e. a phone) on the selected Shipping Group.
 *
 * @see ShippingGroup
 */
public class ShippingGroupAssignedDevice {
    private final SelenideElement assignedDeviceContainer;

    /**
     * Constructor for the shipping device that defines its position on the web page.
     *
     * @param assignedDeviceContainer web element for the main container of the assigned device item
     */
    public ShippingGroupAssignedDevice(SelenideElement assignedDeviceContainer) {
        this.assignedDeviceContainer = assignedDeviceContainer;
    }

    /**
     * Get the web element for the 'delete' button of the assigned device on the Shipping Group.
     */
    public SelenideElement getDeleteButton() {
        return assignedDeviceContainer.$x(".//button[@title='Delete Device']");
    }

    /**
     * Get the web element for the quantity input of the assigned device on the Shipping Group.
     */
    public SelenideElement getQuantityInput() {
        return assignedDeviceContainer.$("[name='qty']");
    }
}
