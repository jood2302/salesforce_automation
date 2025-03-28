package ngbs.quotingwizard.newbusiness.searchbar;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.productsPage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.producttab.ProductsPage.SEARCH_PRODUCT_PLACEHOLDER_TEXT;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeNotEqual;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.openqa.selenium.Keys.BACK_SPACE;

@Tag("P1")
@Tag("NGBS")
@Tag("ProductsTab")
public class LicenseNameSearchBarTest extends BaseTest {
    private final Steps steps;
    private final SearchBarSteps searchBarSteps;

    //  Test data
    private final Product[] productsToSearch;
    private final Product[] otherProducts;
    private final Product ucaasLowVolumeProduct;
    private final Product ucaasHighVolumeProduct;
    private final String productSubgroup;
    private final String otherProductsGroup;
    private final String otherProductsSubgroup;

    private final String ucaasLowSearchValue;

    public LicenseNameSearchBarTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_NoDLs.json",
                Dataset.class);

        steps = new Steps(data);
        searchBarSteps = new SearchBarSteps(data);

        productsToSearch = data.getNewProductsToAdd();
        otherProducts = data.packageFolders[0].packages[0].productsOther;
        ucaasLowVolumeProduct = otherProducts[0];
        ucaasHighVolumeProduct = otherProducts[1];
        productSubgroup = data.packageFolders[0].packages[0].products[0].subgroup;
        otherProductsGroup = ucaasLowVolumeProduct.group;
        otherProductsSubgroup = ucaasLowVolumeProduct.subgroup;

        ucaasLowSearchValue = " UCaaS L";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-5227")
    @TmsLink("CRM-32275")
    @DisplayName("CRM-5227 - Search should work for Licenses display names. \n" +
            "CRM-32275 - Search displays relevant results if some changes were made in the search field")
    @Description("CRM-5227 - To check that Search should work for Licenses display names. \n" +
            "CRM-32275 - Verify that Search displays relevant results if some changes were made in the search field")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select a package for it, and open the Add Products tab", () ->
                searchBarSteps.prepareSearchBar(steps.quoteWizard.opportunity.getId())
        );

        //  CRM-5227
        step("2. Check the placeholder on the Add Products tab, open the products group = '" + searchBarSteps.productGroup + "', " +
                "and search phones products by their Licenses display names in the search bar", () -> {
            productsPage.searchBar.shouldHave(attribute("placeholder", SEARCH_PRODUCT_PLACEHOLDER_TEXT));

            productsPage.openGroup(searchBarSteps.productGroup);
            productsPage.openSubgroup(productSubgroup);
            var initialProductsCount = productsPage.products.size();

            for (var product : productsToSearch) {
                step("Search for '" + product.name + "' using the Add Products tab's search bar", () -> {
                    productsPage.clearSearchBar();
                    productsPage.products.shouldHave(size(initialProductsCount), ofSeconds(10));

                    productsPage.searchBar.setValue(product.name);
                    productsPage.products.shouldHave(sizeNotEqual(initialProductsCount));

                    var productItem = productsPage.getProductItem(product);
                    productItem.getSelf().shouldBe(visible);
                    productItem.getNameElement().shouldHave(exactTextCaseSensitive(product.name));
                });
            }
        });

        //  CRM-32275
        step("3. Open the products group = '" + otherProductsGroup + "', " +
                "enter '" + ucaasLowSearchValue + "' value in Search field and check displayed license", () -> {
            productsPage.openGroup(otherProductsGroup);
            productsPage.openSubgroup(otherProductsSubgroup);

            productsPage.clearSearchBar();
            productsPage.searchBar.setValue(ucaasLowSearchValue);
            productsPage.products.shouldHave(size(1), ofSeconds(10));

            var productItem = productsPage.getProductItem(ucaasLowVolumeProduct);
            productItem.getSelf().shouldBe(visible);
            productItem.getNameElement().shouldHave(exactTextCaseSensitive(ucaasLowVolumeProduct.name));
        });

        //  CRM-32275
        step("4. Delete the last symbol in the Search input field and check displayed licenses", () -> {
            productsPage.searchBar.sendKeys(BACK_SPACE);
            productsPage.products.shouldHave(size(otherProducts.length), ofSeconds(10));

            for (var otherProduct : otherProducts) {
                step("Check that license '" + otherProduct.name + "' is displayed", () -> {
                    var productItem = productsPage.getProductItem(otherProduct);
                    productItem.getSelf().shouldBe(visible);
                    productItem.getNameElement().shouldHave(exactTextCaseSensitive(otherProduct.name));
                });
            }
        });

        //  CRM-32275
        step("5. Add 'h' letter in Search field and check displayed license", () -> {
            productsPage.searchBar.sendKeys("h");
            productsPage.products.shouldHave(size(1), ofSeconds(10));

            var productItem = productsPage.getProductItem(ucaasHighVolumeProduct);
            productItem.getSelf().shouldBe(visible);
            productItem.getNameElement().shouldHave(exactTextCaseSensitive(ucaasHighVolumeProduct.name));
        });
    }
}