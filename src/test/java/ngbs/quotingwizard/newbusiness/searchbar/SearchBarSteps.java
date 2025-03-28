package ngbs.quotingwizard.newbusiness.searchbar;

import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.packagetab.PackagePage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.producttab.ProductsPage;
import ngbs.quotingwizard.QuoteWizardSteps;

import static base.Pages.productsPage;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.visible;
import static java.time.Duration.ofSeconds;

/**
 * Test methods for test cases related to the Search bar on {@link ProductsPage} of {@link NGBSQuotingWizardPage}.
 */
public class SearchBarSteps {
    private final QuoteWizardSteps quoteWizardSteps;

    //  Test data
    public final String productGroup;

    /**
     * New instance for test methods for test cases related to the Search bar
     * on the Add Products tab (Quote Wizard).
     *
     * @param data object parsed from the JSON files with the test data
     */
    public SearchBarSteps(Dataset data) {
        quoteWizardSteps = new QuoteWizardSteps(data);

        productGroup = data.packageFolders[0].packages[0].products[0].group;
    }

    /**
     * <p> 1. Open the Quote Wizard for the New Business Opportunity to add a new Sales Quote via direct VF page link.
     * <p> 2. Select a Package from provided Test data on the {@link PackagePage} of Quote Wizard.
     * <p> 3. Open {@link ProductsPage} and wait for products list and the search bar to load.
     *
     * @param opportunityId ID of the related Opportunity
     */
    public void prepareSearchBar(String opportunityId) {
        quoteWizardSteps.openQuoteWizardForNewSalesQuoteDirect(opportunityId);
        quoteWizardSteps.selectDefaultPackageFromTestData();

        productsPage.openTab();

        productsPage.searchBar.shouldBe(visible, ofSeconds(30));
        productsPage.products.shouldHave(sizeGreaterThan(0), ofSeconds(30));
    }
}
