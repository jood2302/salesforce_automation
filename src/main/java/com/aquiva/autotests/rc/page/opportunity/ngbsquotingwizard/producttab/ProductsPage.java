package com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.producttab;

import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NgbsQuotingWizardFooter;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import java.util.List;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

/**
 * 'Add Products' tab in the Quoting Wizard ({@link NGBSQuotingWizardPage}).
 * Used for adding products to the Cart.
 */
public class ProductsPage extends NGBSQuotingWizardPage {

    public static final String SEARCH_PRODUCT_PLACEHOLDER_TEXT = "Search Product";

    public static final String RENTAL_PHONES_ARE_AVAILABLE_ONLY_UNDER_CONTRACT = "Rental Phones are available only under Contract. Please select Contract on the Package tab.";

    //  Search Results filter options
    public static final String ALL_OPTION = "All";

    public final SelenideElement loadingMessage = $("licenses").$(byText("loading..."));

    public final SelenideElement searchBar = $("[data-ui-auto='search-bar']");

    /**
     * Visible products that are displayed when user select group and subgroup (no search query).
     */
    public final ElementsCollection products = $$("[data-ui-auto='license-item'] > div.row-new:not(.slds-hide)");
    /**
     * Visible products that are displayed after user enters a search query.
     */
    public final ElementsCollection productsVisibleSearchResults =
            $$(".slds-is-open [data-ui-auto='license-item'] > div.row-new:not(.slds-hide)");
    public final ElementsCollection serviceNames = $$("#groups c-button");
    public final ElementsCollection groupNames = $$(".group-item + label");
    public final ElementsCollection subgroupNames = $$(".sub-group-item label");
    public final ElementsCollection addToCartButtons = $$("[data-ui-auto-license-cart-button='add-to-cart']");

    //  Search Results section
    public final SelenideElement serviceFilterSelect = $x("//licenses-filter[.//*='Service']//select");
    public final SelenideElement groupFilterSelect = $x("//licenses-filter[.//*='Group']//select");
    public final SelenideElement subGroupFilterSelect = $x("//licenses-filter[.//*='Subgroup']//select");
    public final SelenideElement clearAllFiltersButton = $(byText("Clear All Filters"));

    private final SelenideElement searchTableHeader = $(".slds-is-open .data-header");
    public final SelenideElement nameSearchColumn = searchTableHeader.$(byText("Name"));
    public final SelenideElement groupSearchColumn = searchTableHeader.$(byText("Group"));
    public final SelenideElement subgroupSearchColumn = searchTableHeader.$(byText("Subgroup"));
    public final SelenideElement planSearchColumn = searchTableHeader.$(byText("Plan"));
    public final SelenideElement listPriceSearchColumn = searchTableHeader.$(byText("List Price"));

    public final ElementsCollection groupsSearchResults = $$("h3.slds-section__title");
    public final SelenideElement noResultsMessage = $(byText("No results"));

    //  Footer
    public final NgbsQuotingWizardFooter footer = new NgbsQuotingWizardFooter();

    /**
     * Get list of all visible products on the Add Products tab.
     *
     * @return list of all Product Items that are visible to the user
     */
    public List<ProductItem> getAllVisibleProducts() {
        return products.asDynamicIterable().stream().map(ProductItem::new).collect(toList());
    }

    /**
     * Get a product item from the currently active group/subgroup on the Add Products tab.
     *
     * @param product product's data with defined name property (e.g. "Cisco SPA-122 ATA - Refurbished")
     * @return ProductItem object located in the currently active group/subgroup and with a given name
     */
    public ProductItem getProductItem(Product product) {
        var productItemElement = $("[data-ui-auto-license-name='" + product.name + "'] > div");
        return new ProductItem(productItemElement);
    }

    /**
     * Get a group of the products.
     *
     * @param groupName {@link String} name of the group of the Product
     *                  <p>(e.g. "Services", "Devices", "Main Numbers", etc.)</p>
     * @return {@link SelenideElement} that represents group of the product
     * @see #getSubgroup(String)
     */
    public SelenideElement getGroup(String groupName) {
        return $("#groups [name^='" + groupName + "']").parent();
    }

    /**
     * Get a subgroup of the products.
     * <br/>
     * Note: make sure to open the "parent" group of the given subgroup beforehand.
     *
     * @param subgroupName name of the subgroup of the Product
     *                     <p>(e.g. "Rental" for "Phones" group, "Additional DLs" for "Services" group, etc.)</p>
     * @return {@link SelenideElement} that represents subgroup of the product
     * @see #getGroup(String)
     */
    public SelenideElement getSubgroup(String subgroupName) {
        return $("#subgroups [name^='" + subgroupName + "']").parent();
    }

    /**
     * Find a product on the tab using its group/subgroup, and a name.
     * This method searches the product inside the product groups and subgroups.
     * <br/>
     * Note: webdriver will also scroll to the found product.
     *
     * @param product product's data with defined
     *                group (e.g. "Phones"),
     *                subgroup (e.g. "Refurbished"),
     *                and name (e.g. "Cisco SPA-122 ATA - Refurbished") properties
     * @return ProductItem object located in the given product group/subgroup and with a given name
     */
    public ProductItem findProduct(Product product) {
        if (product.group != null && !product.group.isBlank()) {
            if (product.serviceName != null) {
                openGroup(product.serviceName, product.group);
            } else {
                openGroup(product.group);
            }
        }

        if (product.subgroup != null && !product.subgroup.isBlank()) {
            openSubgroup(product.subgroup);
        }

        //  Locate web element for product item and scroll to it to make it visible in the browser
        var productItem = getProductItem(product);
        productItem.getSelf().scrollIntoView("{block: \"center\"}");
        return getProductItem(product);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitUntilLoaded() {
        super.waitUntilLoaded();

        loadingMessage.shouldBe(hidden, ofSeconds(60));
        products.shouldHave(sizeGreaterThan(0), ofSeconds(60));
    }

    /**
     * Open the Add Products tab by clicking on the tab's button.
     *
     * @return reference to the opened Add Products tab
     */
    public ProductsPage openTab() {
        productsTabButton.click();
        waitUntilLoaded();
        return this;
    }

    /**
     * Open a group for selecting products
     * in case of creating a quote with multiple services.
     *
     * @param serviceName name of the products service (e.g. "Engage Voice Standalone")
     * @param groupName   name of the products group (e.g. "Services")
     * @see #openGroup(String)
     * @see #openSubgroup(String)
     */
    public void openGroup(String serviceName, String groupName) {
        $("#groups [id^='group-" + serviceName + "-" + groupName + "']").parent().click();
    }

    /**
     * Open a group for selecting products.
     *
     * @param groupName name of the products group
     *                  <p>(e.g. "Phones", "Add Ons", etc.)</p>
     * @see #openGroup(String, String)
     * @see #openSubgroup(String)
     */
    public void openGroup(String groupName) {
        getGroup(groupName).shouldBe(visible, ofSeconds(20)).click();
    }

    /**
     * Open subgroup for selecting products.
     * <br/>
     * Note: make sure to open the "parent" group of the given subgroup beforehand.
     *
     * @param subgroupName name of the subgroup of the Product
     *                     <p>(e.g. "Rental" for "Phones" group, "Additional DLs" for "Services" group, etc.)</p>
     * @see #openGroup(String)
     */
    public void openSubgroup(String subgroupName) {
        getSubgroup(subgroupName).scrollIntoView(true).click();
    }

    /**
     * Find and add product to the Cart.
     *
     * @param productToAdd product's data with defined
     *                     group (e.g. "Phones"),
     *                     subgroup (e.g. "Refurbished"),
     *                     and name (e.g. "Cisco SPA-122 ATA - Refurbished") properties
     */
    public void addProduct(Product productToAdd) {
        findProduct(productToAdd)
                .getAddButtonElement()
                .shouldBe(enabled, ofSeconds(60))
                .click();
    }

    /**
     * Find and remove product from the Cart.
     *
     * @param productToRemove product's data with defined
     *                        group (e.g. "Phones"),
     *                        subgroup (e.g. "Refurbished"),
     *                        and name (e.g. "Cisco SPA-122 ATA - Refurbished") properties
     */
    public void removeProduct(Product productToRemove) {
        findProduct(productToRemove)
                .getRemoveButtonElement()
                .shouldBe(enabled, ofSeconds(20))
                .click();
    }

    /**
     * Clear the Search bar from any of the entered values.
     * <br/>
     * Note: needs its own dedicated method as the standard Selenium and the wrapped Selenide method
     * for {@code WebElement.clear()} won't trigger all the necessary events for input clearing.
     */
    public void clearSearchBar() {
        searchBar.clear();
        sleep(1_000);
        searchBar.pressEnter();
    }
}
