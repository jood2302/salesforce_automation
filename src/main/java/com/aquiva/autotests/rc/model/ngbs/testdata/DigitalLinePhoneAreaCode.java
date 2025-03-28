package com.aquiva.autotests.rc.model.ngbs.testdata;

import com.aquiva.autotests.rc.model.DataModel;

/**
 * Object for storing test data for the digital line's device assignment and Shipping Group's device assignment:
 * the combination of digital line, a phone, an area code,
 * a quantity of assigned phones on the area codes,
 * a quantity of the devices that should be shipped to the shipping location...
 */
public class DigitalLinePhoneAreaCode extends DataModel {
    public Product digitalLine;
    public Product phone;
    public AreaCode areaCode;

    public int quantity;
    public int shippingQuantity;

    /**
     * Constructor to create a new data object with the DL/Shipping Group assignment's devices.
     *
     * @param digitalLine any digital line that the phone is associated with
     *                    (e.g. "DigitalLine Unlimited", "DigitalLine Basic")
     * @param phone       a phone licence that should be assigned to the DL and/or shipped to the physical location
     *                    (e.g. "Cisco 8861 Gigabit Color Business Phone")
     * @param areaCode    test data for client's Area Code associated with a DL and a phone
     * @param quantity    number of devices to assign for a given area code
     */
    public DigitalLinePhoneAreaCode(Product digitalLine, Product phone, AreaCode areaCode, int quantity) {
        this.digitalLine = digitalLine;
        this.phone = phone;
        this.areaCode = areaCode;
        this.quantity = quantity;
    }
}