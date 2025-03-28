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
import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("NGBS")
public class PartialNameSearchBarTest extends BaseTest {
    private final Steps steps;
    private final SearchBarSteps searchBarSteps;

    private int initialProductListSize;

    //  Test data
    private final Product productToSearch;
    private final String queryFirstWord;
    private final String queryMiddleWord;
    private final String queryLastWord;

    public PartialNameSearchBarTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_NoDLs.json",
                Dataset.class);

        steps = new Steps(data);
        searchBarSteps = new SearchBarSteps(data);

        productToSearch = data.getNewProductsToAdd()[0];
        var productNameWords = productToSearch.name.split("\\s");

        queryFirstWord = productNameWords[0];
        queryMiddleWord = productNameWords[1];
        queryLastWord = productNameWords[productNameWords.length - 1];
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-5242")
    @DisplayName("CRM-5242 - Search should properly work when entering first, middle or last word of any license name")
    @Description("To check that Search should properly work when entering first, middle or last word of any license name")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select a package for it, and open the Add Products tab", () ->
                searchBarSteps.prepareSearchBar(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the products group = '" + productToSearch.group + ", " +
                "and subgroup = '" + productToSearch.subgroup + "', " +
                "and check out the list of products", () -> {
            productsPage.openGroup(productToSearch.group);
            productsPage.openSubgroup(productToSearch.subgroup);

            productsPage.products.shouldHave(sizeGreaterThan(0));
            initialProductListSize = productsPage.products.size();
        });

        step("3. Input a search query with the license's first word in the search bar and check results", () -> {
            searchAndCheckProduct(queryFirstWord);
        });

        step("4. Input a search query with the license's middle word in the search bar and check results", () -> {
            searchAndCheckProduct(queryMiddleWord);
        });

        step("5. Input a search query with the license's last word in the search bar and check results", () -> {
            searchAndCheckProduct(queryLastWord);
        });
    }

    /**
     * Reset a search results, enter a new search query,
     * and check that the given product is found.
     *
     * @param searchQuery any string to be entered in the search bar
     */
    private void searchAndCheckProduct(String searchQuery) {
        productsPage.clearSearchBar();
        productsPage.products.shouldHave(size(initialProductListSize));

        productsPage.searchBar.setValue(searchQuery);
        productsPage.products.shouldHave(sizeLessThan(initialProductListSize));

        var productToSearchItem = productsPage.getProductItem(productToSearch);
        productToSearchItem.getSelf().shouldBe(visible);
        productToSearchItem.getNameElement().shouldHave(exactTextCaseSensitive(productToSearch.name));
    }
}
