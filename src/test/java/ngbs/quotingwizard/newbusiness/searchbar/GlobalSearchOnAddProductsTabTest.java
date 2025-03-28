package ngbs.quotingwizard.newbusiness.searchbar;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.dto.packages.PackageNgbsDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.producttab.ProductsPage;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.codeborne.selenide.*;
import com.codeborne.selenide.ex.UIAssertionError;
import com.codeborne.selenide.impl.CollectionSource;
import com.codeborne.selenide.impl.ElementCommunicator;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.*;

import static base.Pages.productsPage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.producttab.ProductsPage.ALL_OPTION;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.allLicensesFromNgbs;
import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.impl.Plugins.inject;
import static io.qameta.allure.Allure.step;
import static java.lang.System.lineSeparator;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Multiproduct-Lite")
@Tag("NGBS")
@Tag("Search")
public class GlobalSearchOnAddProductsTabTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Map<String, Package> packageFolderNameToPackageMap;
    private final String firstPartOfYealinkSearchValue;
    private final String secondPartOfYealinkSearchValue;
    private final String searchService;
    private final String searchGroup;
    private final String searchSubgroup;
    private final String officeServiceName;
    private final String devicesGroupName;
    private final String searchValueWithResultsForAllServices;
    private final String searchValueWithNoResults;
    private final String mainNumbersGroup;
    private final String allSubgroup;
    private final List<String> expectedGroupNames;
    private final List<String> expectedSubgroupNames;
    private final List<PackageNgbsDTO.License> licensesFromNGBS;
    private final Map<String, String> elementIdNameMap;

    public GlobalSearchOnAddProductsTabTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_Annual_Contract.json",
                Dataset.class);
        steps = new Steps(data);

        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[2].name, data.packageFolders[2].packages[0],
                data.packageFolders[3].name, data.packageFolders[3].packages[0]
        );

        firstPartOfYealinkSearchValue = "ye";
        secondPartOfYealinkSearchValue = "alink";
        
        searchService = "RingCentral Contact Center";
        searchGroup = "Miscellaneous Add-ons";
        searchSubgroup = "Expert";
        officeServiceName = "Office";
        devicesGroupName = "Devices";
        
        searchValueWithResultsForAllServices = "co";
        searchValueWithNoResults = "test111";

        mainNumbersGroup = "Main Numbers";
        allSubgroup = "All";
        expectedGroupNames = List.of("Main Numbers", "[100] Main Numbers");
        expectedSubgroupNames = List.of("All", "[110] All");

        licensesFromNGBS = allLicensesFromNgbs();
        elementIdNameMap = licensesFromNGBS.stream().collect(toMap(
                license -> license.elementID,
                license -> license.name));
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-32710")
    @TmsLink("CRM-32712")
    @TmsLink("CRM-32724")
    @TmsLink("CRM-32713")
    @DisplayName("CRM-32710 - The Search is applied to all selected in Quote Services. \n" +
            "CRM-32712 - Global Search UI. \n" +
            "CRM-32724 - The Search bar cleared after user navigate to any group on left side list. \n" +
            "CRM-32713 - Resetting Search Result filters after re-initiate search by Search field")
    @Description("CRM-32710 - Verify that the Search is applied and displays results for all services. \n" +
            "CRM-32712 - Check Global Search UI. \n" +
            "CRM-32724 - Verify that the Search bar and Search results are cleared after user navigates to any group on left side list. \n" +
            "CRM-32713 - Verify that Search result filters and list view of search result reset after re-initiating search by Search field")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select MVP, Contact Center, and Engage Digital packages for it", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectPackagesForMultiProductQuote(packageFolderNameToPackageMap);
        });

        //  CRM-32713
        step("2. Open the Add Products tab, enter '" + firstPartOfYealinkSearchValue + "' in the search bar, " +
                "select values in search filter fields, and check the search results", () -> {
            productsPage.openTab();
            productsPage.searchBar.setValue(firstPartOfYealinkSearchValue);
            productsPage.serviceFilterSelect.selectOption(searchService);
            productsPage.groupFilterSelect.selectOption(searchGroup);
            productsPage.subGroupFilterSelect.selectOption(searchSubgroup);

            productsPage.groupsSearchResults.shouldHave(exactTextsCaseSensitive(searchService));

            productsPage.productsVisibleSearchResults
                    .shouldHave(sizeGreaterThan(0))
                    .should(allMatchSearchCriteria(firstPartOfYealinkSearchValue));

            for (var productItem : productsPage.getAllVisibleProducts()) {
                step("Check Group and Subgroup values for " + productItem.getNameElement().text(), () -> {
                    productItem.getGroupElement().shouldHave(exactTextCaseSensitive(searchGroup));
                    productItem.getSubgroupElement().shouldHave(exactTextCaseSensitive(searchSubgroup));
                });
            }
        });

        //  CRM-32713
        step("3. Add '" + secondPartOfYealinkSearchValue + "' to the search bar, " +
                "check Service, Group, Subgroup values in the Search Results filter section, " +
                "and check that displayed licenses are matched with the search criteria", () -> {
            var searchValue = firstPartOfYealinkSearchValue + secondPartOfYealinkSearchValue;
            productsPage.searchBar.sendKeys(secondPartOfYealinkSearchValue);

            productsPage.serviceFilterSelect.getSelectedOption().shouldHave(exactTextCaseSensitive(officeServiceName), ofSeconds(30));
            productsPage.groupFilterSelect.getSelectedOption().shouldHave(exactTextCaseSensitive(devicesGroupName));
            productsPage.subGroupFilterSelect.getSelectedOption().shouldHave(textCaseSensitive(ALL_OPTION));

            productsPage.productsVisibleSearchResults
                    .shouldHave(sizeGreaterThan(0))
                    .should(allMatchSearchCriteria(searchValue));
        });

        step("4. Clear the search query, enter '" + searchValueWithResultsForAllServices + "' in the search bar, " +
                "and check search filter section, service groups, and the search results for each service", () -> {
            productsPage.clearSearchBar();
            productsPage.searchBar.setValue(searchValueWithResultsForAllServices);

            //  CRM-32712
            productsPage.serviceFilterSelect.getSelectedOption().shouldHave(text(ALL_OPTION), ofSeconds(30));
            productsPage.groupFilterSelect.shouldBe(visible);
            productsPage.subGroupFilterSelect.shouldBe(visible);
            productsPage.clearAllFiltersButton.shouldBe(visible);

            productsPage.nameSearchColumn.shouldBe(visible);
            productsPage.groupSearchColumn.shouldBe(visible);
            productsPage.subgroupSearchColumn.shouldBe(visible);
            productsPage.planSearchColumn.shouldBe(visible);
            productsPage.listPriceSearchColumn.shouldBe(visible);

            //  CRM-32710
            var expectedServicesName = new ArrayList<>(packageFolderNameToPackageMap.keySet());
            productsPage.groupsSearchResults.shouldHave(exactTextsCaseSensitiveInAnyOrder(expectedServicesName));

            productsPage.productsVisibleSearchResults.should(allMatchSearchCriteria(searchValueWithResultsForAllServices));
        });

        //  CRM-32724
        step("5. Open 'Main Numbers' group and 'All' subgroup in the Office section, " +
                "check that Search Results filter elements are hidden, " +
                "search bar input is empty, " +
                "and that only licenses of the selected subgroup are displayed", () -> {
            productsPage.openGroup(mainNumbersGroup);
            productsPage.openSubgroup(allSubgroup);

            productsPage.searchBar.shouldBe(empty);
            productsPage.serviceFilterSelect.shouldBe(hidden);
            productsPage.groupFilterSelect.shouldBe(hidden);
            productsPage.subGroupFilterSelect.shouldBe(hidden);
            productsPage.clearAllFiltersButton.shouldBe(hidden);

            for (var productItem : productsPage.getAllVisibleProducts()) {
                var extId = productItem.getExternalId();

                step("Check that the displayed product item with External ID = " + extId + " is from the correct group/subgroup", () -> {
                    var productLicenseDataFromNGBS = licensesFromNGBS.stream()
                            .filter(license -> license.elementID.equals(extId))
                            .findFirst();

                    assertThat(productLicenseDataFromNGBS)
                            .as("Product license data with External NGBS ID = " + extId)
                            .isPresent();
                    assertThat(productLicenseDataFromNGBS.get().labels.getMainQuotingGroup())
                            .as("Group from Product license data with External NGBS ID = " + extId)
                            .isIn(expectedGroupNames);
                    assertThat(productLicenseDataFromNGBS.get().labels.getMainQuotingSubGroup())
                            .as("Subgroup from Product license data with External NGBS ID = " + extId)
                            .isIn(expectedSubgroupNames);
                });
            }
        });

        //  CRM-32712
        step("6. Enter '" + searchValueWithNoResults + "' in the search bar, " +
                "check that Search Results filter elements are hidden, " +
                "and that the search gave no results", () -> {
            productsPage.clearSearchBar();
            productsPage.searchBar.setValue(searchValueWithNoResults);

            productsPage.serviceFilterSelect.shouldBe(hidden);
            productsPage.groupFilterSelect.shouldBe(hidden);
            productsPage.subGroupFilterSelect.shouldBe(hidden);
            productsPage.clearAllFiltersButton.shouldBe(hidden);

            productsPage.products.shouldHave(size(0));
            productsPage.noResultsMessage.shouldBe(visible);
        });
    }

    /**
     * Check the search result for licenses on the Add Products tab.
     * Name, Display Name, or Name and Display on the Child license should contain the searched query.
     *
     * @param searchValue current search value in the search box
     * @see ProductsPage#productsVisibleSearchResults
     */
    private AllMatchSearchCriteria allMatchSearchCriteria(String searchValue) {
        return new AllMatchSearchCriteria(searchValue);
    }

    /**
     * Custom web elements condition to check that all visible product items in the search results
     * match the required search criteria.
     */
    private class AllMatchSearchCriteria extends WebElementsCondition {
        private static final ElementCommunicator communicator = inject(ElementCommunicator.class);

        private final String searchValue;
        private final List<String> failedProductItems;
        private List<String> productItemNameActualTexts;

        private AllMatchSearchCriteria(String searchValue) {
            this.searchValue = searchValue.toLowerCase();
            this.failedProductItems = new ArrayList<>();
        }

        @Override
        public CheckResult check(@NotNull Driver driver, @NotNull List<WebElement> elements) {
            productItemNameActualTexts = communicator.texts(driver, elements)
                    .stream()
                    .map(text -> extractProductNameFromFullText(text))
                    .toList();

            for (var productName : productItemNameActualTexts) {
                var passed = false;

                try {
                    passed = checkProduct(driver, productName);

                    if (!passed) {
                        //  Additionally, check if the "child" license complies with search requirements 
                        //  (in this case, "parent" license also should be displayed in the search results) 
                        var childLicenseItem = driver.getWebDriver()
                                .findElement(By.xpath("//uqt-license[@data-ui-auto-license-name='" + productName + "']" +
                                        "//uqt-tree-node/uqt-license" +
                                        "/div[contains(@class, 'row-new') and not(contains(@class, 'slds-hide'))]"));
                        var childLicenseItemProductName = extractProductNameFromFullText(childLicenseItem.getText());
                        passed = checkProduct(driver, childLicenseItemProductName);
                    }
                } catch (Exception ex) {
                    failedProductItems.add("Failed with " + ex + "! Product name = " + productName);
                }

                if (!passed) {
                    failedProductItems.add(productName);
                }
            }

            return new CheckResult(failedProductItems.isEmpty(), productItemNameActualTexts);
        }

        /**
         * Check whether the license from the search results with given product name
         * complies with search requirements.
         *
         * @param driver      web driver to additionally interact with the web element
         *                    (get text, get HTML attribute)
         * @param productName full name of the product to check
         */
        private boolean checkProduct(Driver driver, String productName) {
            if (productName.toLowerCase().contains(searchValue)) {
                return true;
            } else {
                //  Additionally, check the name on the NGBS license using External ID on the web element (they could be different)
                var licenseItemName = driver.getWebDriver()
                        .findElement(By.cssSelector("[data-ui-auto='license-item-name'][title='" + productName + "']"));
                var extId = licenseItemName.getAttribute("id").replace("license-element-id-", "");
                return elementIdNameMap.get(extId).toLowerCase().contains(searchValue);
            }
        }

        /**
         * Extract the product name from the full text of the product item web element.
         *
         * @param productItemFullText full text from the web element of the product item
         *                            (includes product name, group, subgroup, charge term, list price;
         *                            all split by '\n')
         */
        private String extractProductNameFromFullText(String productItemFullText) {
            return productItemFullText.substring(0, productItemFullText.indexOf("\n"));
        }

        @Override
        public void fail(CollectionSource collection, @NotNull CheckResult lastCheckResult,
                         @Nullable Exception cause, long timeoutMs) {
            throw new UIAssertionError(
                    errorMessage() +
                            lineSeparator() + "The failed product items: " + failedProductItems +
                            lineSeparator() + "Collection: " + collection.description(),
                    toString(), productItemNameActualTexts
            );
        }

        @Override
        public String errorMessage() {
            return "The product items are not complying with search criteria with search query = " + searchValue;
        }

        @Override
        public String toString() {
            return "All match the search criteria using search query = " + searchValue;
        }
    }
}
