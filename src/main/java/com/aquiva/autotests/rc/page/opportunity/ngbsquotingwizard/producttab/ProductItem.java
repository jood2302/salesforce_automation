package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.producttab;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selectors.byCssSelector;
import static java.util.Objects.requireNonNull;

/**
 * Any single product item on the {@link ProductsPage}.
 * <br/>
 * It's represented as a single row with product's name, list price, plan (charge term),
 * and action button ("Add", "Remove"...).
 */
public class ProductItem {
    private final SelenideElement productItem;

    //  String constants for buttons
    public static final String ADD_TO_CART_TEXT = "Add";
    public static final String REMOVE_FROM_CART_TEXT = "Remove";

    private final By addToCartButton = byCssSelector("[data-ui-auto-license-cart-button='add-to-cart']");
    private final By removeFromCartButton = byCssSelector("[data-ui-auto-license-cart-button='remove-from-cart']");
    private final By productName = byCssSelector("[data-ui-auto='license-item-name']");
    private final By group = byCssSelector("[data-ui-auto='license-item-group']");
    private final By subgroup = byCssSelector("[data-ui-auto='license-item-subgroup']");
    private final By chargeTerm = byCssSelector("[data-ui-auto='license-item-charge-term']");
    private final By price = byCssSelector("[data-ui-auto='license-item-price']");
    private final By priceToolTip = byCssSelector("[data-ui-auto='license-item-price-tooltip']");

    /**
     * Constructor for the product item that defines its position on the web page.
     *
     * @param productItem web element for the main container of the item
     */
    public ProductItem(SelenideElement productItem) {
        this.productItem = productItem;
    }

    /**
     * Get the main container of the item.
     *
     * @return web element for the main container
     */
    public SelenideElement getSelf() {
        return productItem;
    }

    /**
     * Get the name of the item.
     * Can be the element for the name of the product.
     *
     * @return web element for the name of the item
     */
    public SelenideElement getNameElement() {
        return productItem.$(productName);
    }

    /**
     * Get the group of the product item.
     *
     * @return web element for the group of the product
     */
    public SelenideElement getGroupElement() {
        return productItem.$(group);
    }

    /**
     * Get the subgroup of the product item.
     *
     * @return web element for the subgroup of the product
     */
    public SelenideElement getSubgroupElement() {
        return productItem.$(subgroup);
    }

    /**
     * Get the value in the 'Plan' column for the product item
     * (e.g. "Annual", "Monthly", "One - Time").
     *
     * @return web element for the value in the 'Plan' column of the product
     */
    public SelenideElement getPlanElement() {
        return productItem.$(chargeTerm);
    }

    /**
     * Get the value in the 'List Price' column for the product item.
     * Usually, it's currency ISO Code + X.XX (price).
     * E.g. "USD 468.00", "EUR 8.00".
     *
     * @return web element for the value in the 'Price' column of the product
     */
    public SelenideElement getListPriceElement() {
        return productItem.$(price);
    }

    /**
     * Get the 'Add' button for the product item.
     *
     * @return web element for the 'Add to Cart' button of the product
     */
    public SelenideElement getAddButtonElement() {
        return productItem.$(addToCartButton);
    }

    /**
     * Get the 'Remove' button for the product item.
     *
     * @return web element for the 'Remove from Cart' button of the product
     */
    public SelenideElement getRemoveButtonElement() {
        return productItem.$(removeFromCartButton);
    }

    /**
     * Get the tooltip icon on the price of the product.
     * When user hovers over this icon the page displays a tooltip
     * with the price rater for the product
     * (i.e. different prices of the product depending on its quantity).
     *
     * @return web element for the tooltip icon on the price of the product
     */
    public SelenideElement getPriceToolTipElement() {
        return productItem.$(priceToolTip);
    }

    /**
     * Get an external ID of the given product item
     * from its internal HTML attribute.
     * (e.g. "LC_DL-UNL_50" for "DigitalLine Unlimited" license)
     **/
    public String getExternalId() {
        var id = getNameElement().getAttribute("id");
        return requireNonNull(id).replace("license-element-id-", "");
    }
}
