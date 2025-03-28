package com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.producttab;

import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.*;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.Condition.interactable;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byCssSelector;
import static com.codeborne.selenide.Selenide.*;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

/**
 * 'Products' page: one of the tabs on the Legacy Quote Wizard pipeline.
 * <br/><br/>
 * Can be accessed via Legacy Quote Wizard on the 'Main Quote', 'Contact Center' and 'ProServ Quote' tabs.
 * <br/><br/>
 * Contains list of products that sales reps add to the cart during the quoting.
 *
 * @see BaseLegacyQuotingWizardPage
 * @see ContactCenterQuotingWizardPage
 * @see ProServQuotingWizardPage
 */
public class LegacyProductsPage extends BaseLegacyQuotingWizardPage {

    private final By productItemsLocator = byCssSelector(".cQuotingToolProductListEntry");
    public final ElementsCollection productsList = $$(productItemsLocator);
    public final SelenideElement productsItemsElement = $(productItemsLocator);

    //  Filters
    public final SelenideElement productNameFilter = $("[name='nameFilter']");
    public final SelenideElement typePicklistFilter = $("[name='typeFilter']");
    public final SelenideElement categoryPicklistFilter = $("[name='categoryFilter']");
    public final SelenideElement planPicklistFilter = $("[name='planFilter']");

    /**
     * Generate and return
     * list of all visible product item rows.
     *
     * @return list of all visible product item rows.
     */
    public List<LegacyProductItem> getAllProductItems() {
        var productItems = new ArrayList<LegacyProductItem>();
        productsList.asDynamicIterable().forEach(e -> productItems.add(new LegacyProductItem(e)));
        return productItems;
    }

    /**
     * Filter list of all visible product item rows
     * by provided Category
     * and return result list.
     *
     * @param category Value of Category for filtering, like "ProServ", "Site", etc.
     * @return list of all product
     * item rows that match provided Category.
     */
    public List<LegacyProductItem> getProductItemsByCategory(String category) {
        return getAllProductItems().stream()
                .filter(e -> e.getCategory().text().equals(category))
                .collect(toList());
    }

    /**
     * Find product item row that
     * match provided Product Name.
     *
     * @param productName Value of Product Name for search.
     * @return product
     * item row that match provided Product Name.
     */
    public LegacyProductItem getProductItemByName(String productName) {
        var productItemElement =
                $x("//tr[@data-aura-class='cQuotingToolProductListEntry'][./td/text()='" + productName + "']");
        return new LegacyProductItem(productItemElement);
    }

    /**
     * Open Products tab by clicking on the tab's button.
     */
    public LegacyProductsPage openTab() {
        sleep(2_000); // helps to avoid losing the next click on the tab
        productsTabButton.shouldBe(interactable, ofSeconds(10)).click();
        productsItemsElement.shouldBe(visible, ofSeconds(30));
        return this;
    }

    /**
     * Find and add product to Cart.
     *
     * @param productName name for added product
     */
    @Step("Find and add product to Cart")
    public void addProduct(String productName) {
        getProductItemByName(productName)
                .getAddToCartButton()
                .click();
    }

    /**
     * Find and add products to Cart.
     *
     * @param products list of products to add
     */
    public void addProducts(List<Product> products) {
        products.forEach(product -> addProduct(product.name));
    }
}
