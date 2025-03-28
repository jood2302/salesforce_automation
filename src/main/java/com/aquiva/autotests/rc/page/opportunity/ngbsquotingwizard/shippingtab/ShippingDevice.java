package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.shippingtab;

import com.codeborne.selenide.SelenideElement;

/**
 * A single row on the {@link ShippingPage}.
 * It contains device's name, number of unshipped devices, and button with a tooltip for supported countries.
 */
public class ShippingDevice {
    public static final String DEVICES_LEFT_REGEX = " devices? left";

    private final SelenideElement deviceContainer;

    /**
     * Constructor for the shipping device that defines its position on the web page.
     *
     * @param deviceContainer web element for the main container of the item
     */
    public ShippingDevice(SelenideElement deviceContainer) {
        this.deviceContainer = deviceContainer;
    }

    /**
     * Return actual web element behind Shipping Device component.
     * <p></p>
     * Useful if test needs to perform actions on the web element itself
     * via Selenide framework actions (waits, assertions, etc...)
     *
     * @return web element that represents Shipping Device in the DOM.
     */
    public SelenideElement getSelf() {
        return deviceContainer;
    }

    /**
     * Get device's area code full name (e.g. "United States, California, Beverly Hills (320)")
     *
     * @return SelenideElement that represents Area Code's full name value in the shipping device
     */
    public SelenideElement getAreaCode() {
        return deviceContainer.$x(".//span[@class='area-code']");
    }

    /**
     * Get the remaining unassigned quantity for the device
     * (e.g. "10 devices left").
     */
    public SelenideElement getNumberOfDevicesLeft() {
        return deviceContainer.$x(".//span[contains(text(), 'device left') or contains(text(), 'devices left')]");
    }
}
