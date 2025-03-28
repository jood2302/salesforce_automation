package com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.producttab;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selectors.byXpath;

/**
 * Represent item row in table of products at {@link LegacyProductsPage} page.
 */
public class LegacyProductItem {

    //  String constants for buttons
    public static final String ADD_TO_CART_TEXT = "Add to Cart";
    public static final String ADDED_TO_CART_TEXT = "Added to Cart";
    public static final String UNAVAILABLE_TEXT = "Unavailable";

    private final SelenideElement productItem;

    private final By productName = byXpath(".//td[1]");
    private final By category = byXpath(".//td[2]");
    private final By plan = byXpath(".//td[3]");
    private final By price = byXpath(".//td[4]");
    private final By addToCartButton = byXpath(".//td[@class='qw-button-cell']//button");

    /**
     * Create product item row by provided
     * SelenideElement.
     *
     * @param productItem SelenideElement that match product item row.
     */
    public LegacyProductItem(SelenideElement productItem) {
        this.productItem = productItem;
    }

    /**
     * Return the actual web element behind the Product Item component.
     * <p></p>
     * Useful if test needs to perform actions on the web element itself
     * via Selenide framework actions (waits, assertions, etc...)
     *
     * @return SelenideElement that represents product item in the DOM.
     */
    public SelenideElement getSelf() {
        return productItem;
    }

    /**
     * Return value in the "Name" column of current item row.
     *
     * @return Product Name of current item row.
     */
    public SelenideElement getProductName() {
        return productItem.$(productName);
    }

    /**
     * Return value in the "Category" column of current item row.
     *
     * @return Category of current item row.
     */
    public SelenideElement getCategory() {
        return productItem.$(category);
    }

    /**
     * Return value in the "Plan" column of current item row.
     *
     * @return Plan of current item row.
     */
    public SelenideElement getPlan() {
        return productItem.$(plan);
    }

    /**
     * Return value in the "Price" column of current item row.
     *
     * @return Price of current item row.
     */
    public SelenideElement getPrice() {
        return productItem.$(price);
    }

    /**
     * Return Add to Cart button of current item row.
     *
     * @return Add to Cart button of current item row.
     */
    public SelenideElement getAddToCartButton() {
        return productItem.$(addToCartButton);
    }
}

