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
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.producttab.ProductItem.ADD_TO_CART_TEXT;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeLessThan;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("P1")
@Tag("NGBS")
public class SearchBarValidationsTest extends BaseTest {
    private final Steps steps;
    private final SearchBarSteps searchBarSteps;

    //  Test data
    private final Product product;

    public SearchBarValidationsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_NoDLs.json",
                Dataset.class);

        steps = new Steps(data);
        searchBarSteps = new SearchBarSteps(data);

        product = data.getProductByDataName("LC_HD_936");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-5222")
    @TmsLink("CRM-5232")
    @TmsLink("CRM-5241")
    @TmsLink("CRM-5245")
    @DisplayName("CRM-5222 - New Business. Search bar is present on the Add Products tab. \n" +
            "CRM-5232 - Search should show all licenses for empty line. \n" +
            "CRM-5241 - Search should take reasonable time. \n" +
            "CRM-5245 - Add to cart button should become active when the license is removed from Cart")
    @Description("CRM-5222 - To check that Search bar is present on the Add Products tab. \n" +
            "CRM-5232 - To check that Search should show all licenses for empty line. \n" +
            "CRM-5241 - To check that Search should take reasonable time. \n" +
            "CRM-5245 - To check that Add to cart button should become active when the license is removed from Cart")
    public void test() {
        //  For CRM-5222
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                        "select a package for it, open the Add Products tab, " +
                "and check that the Search bar is visible", () ->
                //  visibility of the search bar is included into this method
                searchBarSteps.prepareSearchBar(steps.quoteWizard.opportunity.getId())
        );

        //  For CRM-5232
        step("2. Open the products group = '" + searchBarSteps.productGroup + "', search a product by name, " +
                "check all of its elements in search results, add and remove product to cart, " +
                "check 'Add' button after that, clear search bar and check search results", () -> {
            productsPage.openGroup(product.group);
            productsPage.openSubgroup(product.subgroup);

            var initialSize = productsPage.products.size();

            productsPage.searchBar.setValue(product.name);

            //  For CRM-5241
            productsPage.products.shouldHave(sizeLessThan(initialSize), ofSeconds(5));

            var productItem = productsPage.getProductItem(product);
            productItem.getPlanElement().shouldHave(exactTextCaseSensitive(product.chargeTerm));
            productItem.getListPriceElement().shouldHave(exactTextCaseSensitive(steps.quoteWizard.currencyPrefix + product.price));
            productItem.getAddButtonElement().shouldBe(visible);

            //  Wait for the button to enabled can take some more time because of the long-running request to NGBS
            productItem.getAddButtonElement().shouldBe(enabled, ofSeconds(30)).click();
            productItem.getRemoveButtonElement().shouldBe(visible);

            //  For CRM-5245
            productItem.getRemoveButtonElement().click();
            productItem.getAddButtonElement().shouldBe(visible);
            productItem.getAddButtonElement().shouldHave(exactTextCaseSensitive(ADD_TO_CART_TEXT));

            productsPage.clearSearchBar();

            //  For CRM-5232
            productsPage.products.shouldHave(size(initialSize));
        });
    }
}
