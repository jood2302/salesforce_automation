package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab;

import com.aquiva.autotests.rc.model.ngbs.testdata.AreaCode;
import com.aquiva.autotests.rc.page.components.AreaCodeSelector;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThanOrEqual;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.*;
import static java.time.Duration.ofSeconds;

/**
 * Modal window for assigning devices to digital lines.
 * Opens up on top of {@link CartPage} of {@link NGBSQuotingWizardPage}
 * by clicking on device-assignment button for a given digital line product.
 * <p>
 * Typically consists of:
 * <p> - search bar to filter the page content by device name
 * <p> - individual blocks for all available devices (with area code inputs, quantity inputs, etc...)
 * <p> - action buttons "Apply", "Discard", "Cancel"
 */
public class DeviceAssignmentPage {
    public final SelenideElement closeButton = $("[iconname='close'] button");
    public final SelenideElement cancelButton = $("[data-ui-auto='device-assignment-cancel-button']");
    public final SelenideElement discardButton = $("[data-ui-auto='device-assignment-discard-button']");
    public final SelenideElement applyButton = $("[data-ui-auto='device-assignment-apply-button']");
    public final SelenideElement searchInput = $("[data-ui-auto='search-bar']");
    public final ElementsCollection deviceAssignmentItems = $$("[data-ui-auto='device-assignment-parent-item']");
    public final ElementsCollection deviceAssignmentAreaCodeItems = $$("device-assignment-area-code");

    /**
     * Get Product Item block by the name of the product on the device assignment modal window.
     * <p>
     * Through this element caller can get access to corresponding fields for a product block:
     * name value, total number of phones, input field for quantity, {@link DeviceAssignmentAreaCodeItem} block, etc...
     *
     * @param productName name for a product (e.g. "Polycom IP 5000 Conference Phone")
     * @return product item entity to work with individual parts of the block
     */
    public DeviceAssignmentProductItem getProductItemByName(String productName) {
        return new DeviceAssignmentProductItem(
                $x("//device-assignment-item[.//div[@data-ui-auto-device-assignment-product-name='" + productName + "']]")
        );
    }

    /**
     * Assign devices to digital lines.
     * <p>
     * Method links devices with DigitalLine (Unlimited) products with the given quantity.
     * Every device can be assigned to one or more area codes.
     *
     * @param productToAssignName device's name to assign area code to
     * @param areaCode            Area Code entity with name of the country, state, city, and the code to assign to the phone
     * @param quantity            number of devices to assign for a given area code
     * @param isChangeAreaCode    true, if there's an existing area code on the device, and it needs to be changed with a new one.
     */
    public void assignDevices(String productToAssignName, AreaCode areaCode, int quantity, boolean isChangeAreaCode) {
        var deviceAssignmentProductLineItem = getProductItemByName(productToAssignName);
        deviceAssignmentProductLineItem.getNameElement().shouldHave(exactText(productToAssignName), ofSeconds(60));

        if (deviceAssignmentProductLineItem.getChildAreaCodeItems().size() == 0) {
            deviceAssignmentProductLineItem.getAddAreaCodeButton().click();
        }

        //  Because method works with single area codes, take the first child AreaCodeItem as input for a given product
        {
            var areaCodeInputLocator = getFirstAreaCodeItem(productToAssignName).getAreaCodeSelector().getSelf();
            var areaCodeSelector = new AreaCodeSelector(areaCodeInputLocator);
            if (isChangeAreaCode) {
                areaCodeSelector.clear();
            }
        }

        //  This block of code below is a workaround for StaleElementReferenceException
        //  that occurs after clear of AreaCodeSelector (need to re-initialize web element for area code input again)
        {
            var areaCodeInputLocator = getFirstAreaCodeItem(productToAssignName).getAreaCodeSelector().getSelf();
            var areaCodeSelector = new AreaCodeSelector(areaCodeInputLocator);
            areaCodeSelector.selectOption(areaCode.country);
        }
        //  This block of code below is a workaround for StaleElementReferenceException
        //  that occurs after selecting the country (need to re-initialize web element for area code input again)
        {
            var areaCodeInputLocator = getFirstAreaCodeItem(productToAssignName).getAreaCodeSelector().getSelf();
            var areaCodeSelector = new AreaCodeSelector(areaCodeInputLocator);
            areaCodeSelector.selectOption(areaCode.state);
            areaCodeSelector.checkThatOptionSelected(areaCode.state);
            areaCodeSelector.selectOption(areaCode.code);
        }
        //  This block of code below is a workaround for StaleElementReferenceException
        //  that occurs after entering full Area Code in AreaCodeSelector (need to re-initialize web element for area code input again)
        {
            var areaCodeInputLocator = getFirstAreaCodeItem(productToAssignName).getAreaCodeSelector().getSelf();
            var areaCodeSelector = new AreaCodeSelector(areaCodeInputLocator);
            areaCodeSelector.getSelectedAreaCodeFullName().shouldHave(text(areaCode.code));
        }

        //  Press tab because 'Apply' button becomes active on de-focus from quantity input
        getFirstAreaCodeItem(productToAssignName)
                .getAssignedItemsInput()
                .setValue(String.valueOf(quantity))
                .unfocus();
        areaCode.fullName = getFirstAreaCodeItem(productToAssignName).getAreaCodeSelector().getSelectedAreaCodeFullName().getText();

        applyButton.click();
    }

    /**
     * Assign devices to digital lines without changing existing area code.
     * <p>
     * Method links devices with DigitalLine (Unlimited) products with the given quantity.
     *
     * @param productToAssignName device's name to assign to DigitalLine product
     * @param quantity            number of devices to assign to DigitalLine
     */
    public void assignDevicesWithoutSettingAreaCode(String productToAssignName, int quantity) {
        var deviceAssignmentProductLineItem = getProductItemByName(productToAssignName);
        deviceAssignmentProductLineItem.getNameElement().shouldHave(exactText(productToAssignName), ofSeconds(60));

        //  Press tab because 'Apply' button becomes active on de-focus from quantity input
        getFirstAreaCodeItem(productToAssignName)
                .getAssignedItemsInput()
                .setValue(String.valueOf(quantity))
                .unfocus();

        applyButton.click();
    }

    /**
     * Get the first area code from device to DigitalLine assignment.
     *
     * @param productName name for a device to return the area code item block
     *                    (e.g. "Polycom IP 5000 Conference Phone").
     * @return Area Code Item block for a device
     */
    public DeviceAssignmentAreaCodeItem getFirstAreaCodeItem(String productName) {
        getProductItemByName(productName)
                .getDeviceAssignmentAreaCodeItems()
                .shouldHave(sizeGreaterThanOrEqual(1));

        return getProductItemByName(productName)
                .getChildAreaCodeItems()
                .get(0);
    }
}