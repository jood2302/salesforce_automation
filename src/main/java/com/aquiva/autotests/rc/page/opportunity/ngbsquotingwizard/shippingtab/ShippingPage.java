package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.shippingtab;

import com.aquiva.autotests.rc.model.ngbs.testdata.ShippingGroupAddress;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Product2;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;
import static java.time.Duration.ofSeconds;

/**
 * Shipping tab in {@link NGBSQuotingWizardPage}
 * that contains a list of device(s) which was(were) added to the cart and shipping group(s).
 */
public class ShippingPage extends NGBSQuotingWizardPage {

    public final ElementsCollection listNameOfDevices = $$(".product-list .product .slds-truncate");
    public final ElementsCollection listOfDevices = $$("shipping-product-list [cdkdrag]");
    public final ElementsCollection areaCodesOnDevices = $$("shipping-product-list [cdkdrag] .area-code");
    public final ElementsCollection shippingGroupDropLists = $$(".shipping-card-block [cdkdroplist]");

    //  Header buttons
    public final SelenideElement saveButton = $("[data-ui-auto='save-shipping']");

    //  Shipping Group Details buttons
    public final SelenideElement addNewShippingGroupButton = $("[data-ui-auto='add-shipping-location']");

    //  'Shipping Group Details' modal window (for adding new/editing existing Shipping Groups)
    public final ShippingGroupDetailsModal shippingGroupDetailsModal = new ShippingGroupDetailsModal();

    /**
     * Get a row for the given device in "Devices" section.
     * It contains info like device's name, quantity.
     *
     * @param deviceProductName displayed name of the device (e.g. "Polycom VVX450 Business IP Phone - Rental")
     * @return Shipping Device object to extract other parameters from (e.g. quantity)
     */
    public ShippingDevice getShippingDevice(String deviceProductName) {
        var shippingDeviceContainer =
                $x("//*[contains(@class,'product-box')][.//div[@title='" + deviceProductName + "']]");
        return new ShippingDevice(shippingDeviceContainer);
    }

    /**
     * Get a row for the given device in "Devices" section.
     * It contains info like device's name, quantity.
     *
     * @param deviceProductName displayed name of the device (e.g. "Polycom VVX450 Business IP Phone - Rental")
     * @param areaCodeFullName  full name of the device's area code (e.g. "United States, California, Alpine (619)")
     *                          or empty string if the row SHOULD NOT contain an area code
     * @return Shipping Device object to extract other parameters from (e.g. quantity)
     */
    public ShippingDevice getShippingDevice(String deviceProductName, String areaCodeFullName) {
        var areaCodeXpathPart = areaCodeFullName != null && !areaCodeFullName.isBlank()
                ? "[.//*[@class='area-code']='" + areaCodeFullName + "']"
                : "[not(.//*[@class='area-code'])]";
        var shippingDeviceContainer =
                $x("//*[contains(@class,'product-box')][.//div[@title='" + deviceProductName + "']]"
                        + areaCodeXpathPart);
        return new ShippingDevice(shippingDeviceContainer);
    }

    /**
     * Get a block for the shipping group in "Shipping Groups" section.
     * It contains shipping address, distributed devices, cost of delivery...
     *
     * @param shippingAddress partial or full shipping address for the group.
     *                        Should be in the following format:
     *                        "BillingStreet, BillingCity, BillingStateFullName BillingZipCode, BillingCountry"
     *                        (e.g. "Apt.123 516 Walden Dr, Beverly Hills, California 90210, United States")
     * @param shipAttentionTo name of the person who receives a shipment
     *                        (e.g. "John Doe")
     * @return Shipping Group object to extract other parameters from (e.g. devices)
     */
    public ShippingGroup getShippingGroup(String shippingAddress, String shipAttentionTo) {
        var shippingGroupContainer =
                $x("//shipping-location-item" +
                        "[.//h2[contains(text(),'" + shippingAddress + "')]]" +
                        "[.//*[@class='shipping-info-cpq']//*[contains(text(), '" + shipAttentionTo + "')]]");
        return new ShippingGroup(shippingGroupContainer);
    }

    /**
     * Get the topmost Shipping Group in the list in the "Shipping Group" section.
     *
     * @return the first Shipping Group object
     */
    public ShippingGroup getFirstShippingGroup() {
        return new ShippingGroup($("shipping-location-item"));
    }

    /**
     * Open Shipping tab by clicking on the tab's button.
     *
     * @return reference to the opened Shipping tab
     */
    public ShippingPage openTab() {
        shippingTabButton.shouldBe(visible, ofSeconds(30)).click();
        shippingGroupDropLists.first().shouldBe(visible, ofSeconds(60));
        return this;
    }

    /**
     * Press 'Save Changes' button on the Shipping Tab of the Quote Wizard.
     */
    public void saveChanges() {
        saveButton.click();
        progressBar.shouldBe(visible, ofSeconds(10));
        waitUntilLoaded();
    }

    /**
     * Press 'Add New Group' button on the Shipping Tab of the Quote Wizard,
     * fill all the required fields, and press 'Apply' button.
     *
     * @param shippingGroupAddress data object with the full address for the new Shipping Group
     */
    public void addNewShippingGroup(ShippingGroupAddress shippingGroupAddress) {
        addNewShippingGroupButton.click();
        shippingGroupDetailsModal.submitChanges(shippingGroupAddress);
    }

    /**
     * Assign a product (e.g. phone) to the shipping group.
     *
     * @param deviceProductName device's product name to assign to the group
     *                          (e.g. "Cisco SPA-122 ATA Refurbished"),
     *                          see the corresponding {@link Product2#getName()}
     * @param shippingAddress   group's shipping address (as displayed on the form,
     *                          e.g. "App.129 13 Elm Street, Foster City, California 94404, United States")
     * @param shipAttentionTo   group's shipment receiver (e.g. "John Smith")
     */
    public void assignDeviceToShippingGroup(String deviceProductName, String shippingAddress, String shipAttentionTo) {
        assignDeviceToShippingGroup(getShippingDevice(deviceProductName), getShippingGroup(shippingAddress, shipAttentionTo));
    }

    /**
     * Assign a product (e.g. phone) to the shipping group.
     * <br/><br/>
     * Note: use this method if there are more than one device (unassigned) with the same name,
     * e.g. when they are assigned to different area codes on the Price tab.
     * For simpler cases, see {@link #assignDeviceToShippingGroup(String, String, String)}.
     *
     * @param shippingDevice  unassigned device on the left side of the Shipping tab
     *                        (e.g. "Cisco SPA-122 ATA Refurbished")
     * @param shippingAddress group's shipping address (as displayed on the form,
     *                        e.g. "App.129 13 Elm Street, Foster City, California 94404, United States")
     * @param shipAttentionTo group's shipment receiver (e.g. "John Smith")
     */
    public void assignDeviceToShippingGroup(ShippingDevice shippingDevice, String shippingAddress, String shipAttentionTo) {
        assignDeviceToShippingGroup(shippingDevice, getShippingGroup(shippingAddress, shipAttentionTo));
    }

    /**
     * Assign a product (e.g. phone) to the shipping group.
     *
     * @param shippingDevice unassigned device on the left side of the Shipping tab
     *                       (e.g. "Cisco SPA-122 ATA Refurbished")
     * @param shippingGroup  Shipping Group object on the right side of the Shipping tab,
     *                       usually defined by its address.
     */
    public void assignDeviceToShippingGroup(ShippingDevice shippingDevice, ShippingGroup shippingGroup) {
        //  without this click and pause there might be issues with drag-and-dropping of devices to any group
        shippingGroup.getSelf().click();
        sleep(1000);

        actions()
                .clickAndHold(shippingDevice.getSelf())
                .pause(1000)
                .moveToElement(shippingGroup.getSelf())
                .pause(1000)
                .release(shippingGroup.getSelf())
                .pause(2000)
                .build()
                .perform();
    }
}
