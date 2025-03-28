package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.PromotionsManagerModal;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selectors.byCssSelector;
import static java.lang.Double.valueOf;

public class CartItem {
    private final SelenideElement cartItemElement;

    //  Discretion circle color values
    public static final String GREEN_COLOR_STYLE_VALUE = "background: green;";
    public static final String ORANGE_COLOR_STYLE_VALUE = "background: orange;";
    public static final String RED_COLOR_STYLE_VALUE = "background: red;";
    
    public static final String MIN_QUANTITY = "1";
    public static final String MIN_DISCOUNT = "0";

    private final By displayName = byCssSelector("[data-ui-auto='cart-item-product-name']");
    private final By chargeTerm = byCssSelector("[data-ui-auto='cart-item-charge-term']");
    private final By quantityInput = byCssSelector("[data-ui-auto='cart-item-quantity']");
    private final By areaCodeButton = byCssSelector("[data-ui-auto='area-code-badge']");
    private final By deviceAssignmentButton = byCssSelector("[data-ui-auto='device-assignment-badge']");
    private final By existingQuantityInput = byCssSelector("[data-ui-auto='cart-item-existing-quantity']");
    private final By newQuantityInput = byCssSelector("[data-ui-auto='cart-item-new-quantity']");
    private final By deliveredQuantityInput = byCssSelector("[data-ui-auto='cart-item-delivered-quantity']");
    private final By listPrice = byCssSelector("list-price-range [data-ui-auto='cart-item-unit-price']");
    private final By deleteButton = byCssSelector("[data-ui-auto='cart-item-delete-button']");
    private final By yourPrice = byCssSelector("[data-ui-auto='cart-item-your-price']");
    private final By discountInput = byCssSelector("[data-ui-auto='cart-item-discount-value']");
    private final By promoIcon = byCssSelector(".promo-icon icon");
    private final By discountTypeSelect = byCssSelector("[data-ui-auto='cart-item-discount-type']");
    private final By quantityErrorMessage = byCssSelector("[data-ui-auto='quantity-error']");
    private final By totalPrice = byCssSelector("[data-ui-auto='cart-item-total-price']");
    private final By numberAssignmentLineItems = byCssSelector(".slds-p-left--xx-small");
    private final By targetPrice = byCssSelector("target-price [data-ui-auto='cart-item-unit-price']");
    private final By discretionCircle = byCssSelector("discretion > .circle");

    public CartItem(SelenideElement cartItemElement) {
        this.cartItemElement = cartItemElement;
    }

    public SelenideElement getCartItemElement() {
        return cartItemElement;
    }

    /**
     * Find Cart Item product visible name.
     *
     * @return The Cart Item product visible name.
     */
    public SelenideElement getName() {
        return cartItemElement.$(displayName);
    }

    /**
     * Find Cart Item product visible name.
     *
     * @return The Cart Item product visible name.
     */
    public SelenideElement getDisplayName() {
        return cartItemElement.$(displayName);
    }

    /**
     * Find Charge Term plan of Cart Item.
     *
     * @return Charge Term plan of Cart Item.
     */
    public SelenideElement getChargeTerm() {
        return cartItemElement.$(chargeTerm);
    }

    /**
     * Find Cart Item quantity input.
     *
     * @return Cart Item quantity input.
     */
    public SelenideElement getQuantityInput() {
        return cartItemElement.$(quantityInput);
    }

    /**
     * Get text from Cart Item quantity
     * input visible error.
     *
     * @return text from Cart Item quantity input visible error.
     */
    public String getQuantityInputErrorText() {
        return cartItemElement.$(quantityErrorMessage).getAttribute("title");
    }

    /**
     * Find Cart Item button for
     * setting Area Code.
     *
     * @return Cart Item button for setting Area Code.
     */
    public SelenideElement getAreaCodeButton() {
        return cartItemElement.$(areaCodeButton);
    }

    /**
     * Find Cart Item button for
     * device assignment.
     *
     * @return Cart Item button for device assignment.
     */
    public SelenideElement getDeviceAssignmentButton() {
        return cartItemElement.$(deviceAssignmentButton);
    }

    /**
     * Find existing quantity value of
     * Cart Item.
     *
     * @return existing quantity value of Cart Item.
     */
    public SelenideElement getExistingQuantityInput() {
        return cartItemElement.$(existingQuantityInput);
    }

    /**
     * Find input for new quantity value of
     * Cart Item.
     *
     * @return input for new quantity value of Cart Item.
     */
    public SelenideElement getNewQuantityInput() {
        return cartItemElement.$(newQuantityInput);
    }

    /**
     * Find input for the delivered quantity value of
     * Cart Item.
     *
     * @return input for delivered quantity value of Cart Item.
     */
    public SelenideElement getDeliveredQuantityInput() {
        return cartItemElement.$(deliveredQuantityInput);
    }

    /**
     * Find button for Cart Item removal.
     *
     * @return button for Cart Item removal.
     */
    public SelenideElement getDeleteButton() {
        return cartItemElement.$(deleteButton);
    }

    /**
     * Find list price value of Cart Item.
     *
     * @return list price value of Cart Item.
     */
    public SelenideElement getListPrice() {
        return cartItemElement.$(listPrice);
    }

    /**
     * Find your price value of Cart Item.
     *
     * @return your price value of Cart Item.
     */
    public SelenideElement getYourPrice() {
        return cartItemElement.$(yourPrice);
    }

    /**
     * Find web element of target price of Cart Item.
     *
     * @return web element of target price of Cart Item.
     */
    public SelenideElement getTargetPrice() {
        return cartItemElement.$(targetPrice);
    }

    /**
     * Find target price value of Cart Item.
     * <p>
     * The value is taken as a text of Target Price element
     * excluding Currency ISO Code prefix with a length of 3 letters.
     *
     * @return target price of Cart Item.
     */
    public Double getTargetPriceValue() {
        return valueOf(getTargetPrice().getText().substring(3));
    }

    /**
     * Find Cart Item discount input.
     *
     * @return Cart Item discount input.
     */
    public SelenideElement getDiscountInput() {
        return cartItemElement.$(discountInput);
    }

    /**
     * Find Cart Item's promos tooltip.
     * <p> Appears after applying promo code in {@link PromotionsManagerModal}.</p>
     * <p> When user hovers over it, it displays a tooltip with a promo's description.</p>
     *
     * @return Cart Item promos tooltip.
     */
    public SelenideElement getPromoIcon() {
        return cartItemElement.$(promoIcon);
    }

    /**
     * Find Cart Item discount type picklist.
     *
     * @return Cart Item discount type picklist.
     */
    public SelenideElement getDiscountTypeSelect() {
        return cartItemElement.$(discountTypeSelect);
    }

    /**
     * Find available options from
     * Cart Item discount type picklist.
     *
     * @return Available options from
     * Cart Item discount type picklist.
     */
    public ElementsCollection getDiscountTypeSelectOptions() {
        return cartItemElement.$(discountTypeSelect).getOptions();
    }

    /**
     * Find visible error message on
     * quantity input of Cart Item.
     *
     * @return Visible error message on quantity input of Cart Item.
     */
    public SelenideElement getQuantityErrorMessage() {
        return cartItemElement.$(quantityErrorMessage);
    }

    /**
     * Find total price value of Cart Item.
     *
     * @return total price value of Cart Item.
     */
    public SelenideElement getTotalPrice() {
        return cartItemElement.$(totalPrice);
    }

    /**
     * Find value of assigned items.
     *
     * @return Value of assigned items.
     */
    public SelenideElement getNumberAssignmentLineItems() {
        return cartItemElement.$(numberAssignmentLineItems);
    }

    /**
     * Find discretion circle of Cart Item.
     *
     * @return web element of Cart Item discretion circle.
     */
    public SelenideElement getDiscretionCircle() {
        return cartItemElement.$(discretionCircle);
    }
}
