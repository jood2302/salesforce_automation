package com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.carttab;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selectors.byCssSelector;
import static com.codeborne.selenide.Selectors.byName;

/**
 * One of the items on the 'Cart' tab on {@link LegacyCartPage}.
 * <br/>
 * Every cart item consists of several fields (display, inputs, selectors):
 * <p> - product name </p>
 * <p> - plan (or charge term) </p>
 * <p> - quantity </p>
 * <p> - list price and your price (incl. discounts) </p>
 * <p> - discount value and type </p>
 */
public class LegacyCartItem {
    private final SelenideElement cartItemElement;

    private final By name = byCssSelector("[data-ui-auto='cart-item-product-name']");
    private final By chargeTerm = byCssSelector("[data-ui-auto='cart-item-charge-term']");
    private final By quantityInput = byName("quantityInput");
    private final By newQuantityInput = byName("newQuantityInput");
    private final By listPrice = byCssSelector("[data-ui-auto='cart-item-unit-price']");
    private final By yourPrice = byCssSelector("[data-ui-auto='cart-item-your-price']");
    private final By discountInput = byName("discountInput");
    private final By discountTypeSelect = byName("discountTypeInput");
    private final By deleteButton = byCssSelector("[data-ui-auto='cart-item-delete-button']");

    /**
     * Create new Cart Item object (in Legacy Quote Wizard).
     *
     * @param cartItemElement web element for the Cart Item row
     */
    public LegacyCartItem(SelenideElement cartItemElement) {
        this.cartItemElement = cartItemElement;
    }

    /**
     * Get Cart Item's product name display field.
     *
     * @return display field of the Cart Item's product name
     * (e.g. "Contact Center: Basic Edition Seat")
     */
    public SelenideElement getName() {
        return cartItemElement.$(name);
    }

    /**
     * Get Cart Item's charge term display field
     * (displayed as "PLAN").
     *
     * @return display field of the Cart Item's charge term
     * (e.g. "Monthly - Contract")
     */
    public SelenideElement getChargeTerm() {
        return cartItemElement.$(chargeTerm);
    }

    /**
     * Get Cart Item's quantity input field.
     *
     * @return quantity input field for the Cart Item
     */
    public SelenideElement getQuantityInput() {
        return cartItemElement.$(quantityInput);
    }

    /**
     * Get Cart Item's new quantity input field.
     *
     * @return new quantity input field for the Cart Item
     */
    public SelenideElement getNewQuantityInput() {
        return cartItemElement.$(newQuantityInput);
    }

    /**
     * Get Cart Item's list price display field.
     *
     * @return list price display field for the Cart Item
     */
    public SelenideElement getListPrice() {
        return cartItemElement.$(listPrice);
    }

    /**
     * Get Cart Item's "your" price display field
     * (discounted list price).
     *
     * @return "your" price display field for the Cart Item
     */
    public SelenideElement getYourPrice() {
        return cartItemElement.$(yourPrice);
    }

    /**
     * Get Cart Item's discount value input field.
     *
     * @return discount value input field for the Cart Item
     */
    public SelenideElement getDiscountInput() {
        return cartItemElement.$(discountInput);
    }

    /**
     * Get Cart Item's discount type input field.
     *
     * @return discount type input field for the Cart Item (e.g. "USD", "%")
     */
    public SelenideElement getDiscountTypeInput() {
        return cartItemElement.$(discountTypeSelect);
    }

    /**
     * Get Cart Item's 'Delete' button.
     *
     * @return button for deleting chosen Cart item
     */
    public SelenideElement getDeleteButton() {
        return cartItemElement.$(deleteButton);
    }
}
