package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
public class AddCartButtonTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    public AddCartButtonTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_2TypesOfDLs_RegularAndPOC.json",
                Dataset.class);

        steps = new Steps(data);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-11311")
    @DisplayName("CRM-11311 - User can add a new Item to Cart. New Business")
    @Description("To check that user can add the new product from the Add Products tab to the Price tab by pressing 'Add' Button")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, add some products on the Add Products tab", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
        });

        step("2. Open the Price tab, and check added and default products", () -> {
            cartPage.openTab();
            steps.cartTab.checkProductsInCartNewBusiness(data.getNewProductsToAdd());
            steps.cartTab.checkProductsInCartNewBusiness(data.getProductsDefault());
        });
    }
}
