package ngbs.quotingwizard.existingbusiness.upgrade;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static base.Pages.packagePage;
import static com.codeborne.selenide.CollectionCondition.size;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("PDV")
@Tag("NGBS")
public class UpgradeChargeTermOfficeTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final String upgradeChargeTermName;
    private final Product[] billingProductsUpgradeChargeTerm;

    public UpgradeChargeTermOfficeTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_163074013.json",
                Dataset.class);

        steps = new Steps(data);

        upgradeChargeTermName = data.packageFoldersUpgradeChargeTerm[0].chargeTerm;
        billingProductsUpgradeChargeTerm = data.packageFoldersUpgradeChargeTerm[0].packages[0].productsFromBilling;
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-8196")
    @DisplayName("CRM-8196 - Existing Business. Office package. Check preselected products on upgrade")
    @Description("Verify that after upgrading charge term Preselected products on the Price tab are equal to Billing products")
    public void test() {
        step("1. Open the Quote Wizard for the Existing Business Opportunity to add a new Sales quote, " +
                "select the same package, but with a different charge term, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());

            packagePage.packageSelector.packageFilter.selectChargeTerm(upgradeChargeTermName);
            packagePage.saveChanges();
        });

        step("2. Open the Price tab, and check that displayed products are the same as on the Billing account", () -> {
            cartPage.openTab();
            cartPage.cartItemNames.shouldHave(size(billingProductsUpgradeChargeTerm.length));
            steps.cartTab.checkProductsInCartExistingBusiness(billingProductsUpgradeChargeTerm);
        });
    }
}
