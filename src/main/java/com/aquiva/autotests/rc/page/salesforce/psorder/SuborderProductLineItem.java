package com.aquiva.autotests.rc.page.salesforce.psorder;

import com.codeborne.selenide.SelenideElement;

/**
 * Page Object for the Suborder Product Line Item in Salesforce.
 * <p>
 * Contains name, plan, price, quantity of the product, and a button to remove the Product.
 */
public class SuborderProductLineItem {
    private final SelenideElement suborderLineItemElement;

    /**
     * Constructor for the Suborder Line Item that defines its position on the web page.
     *
     * @param suborderLineItemElement web element for the main container of the item
     */
    public SuborderProductLineItem(SelenideElement suborderLineItemElement) {
        this.suborderLineItemElement = suborderLineItemElement;
    }

    /**
     * Get a web element for a 'Phase/Total Qty' field of the Suborder Product Line Item.
     */
    public SelenideElement getPhaseTotalQuantityInput() {
        return suborderLineItemElement.$("#quantity");
    }

    /**
     * Get a web element for a button that removes Suborder Product Line Item.
     */
    public SelenideElement getRemoveProductButton() {
        return suborderLineItemElement.$("#remove-product-button");
    }

    /**
     * Get the Product Name of the Suborder Product Line Item
     * (e.g. 'Additional Data Collection Session').
     */
    public SelenideElement getProductName() {
        return suborderLineItemElement.$("#product-name");
    }

    /**
     * Get a web element for a 'Add Location' button of the Suborder Product Line Item.
     */
    public SelenideElement getAddLocationButton() {
        return suborderLineItemElement.$x("./following-sibling::tr//button[@id='add-location-button']");
    }
}
