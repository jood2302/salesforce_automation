package ngbs.quotingwizard.existingbusiness.upgrade;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static base.Pages.packagePage;
import static com.codeborne.selenide.CollectionCondition.size;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
public class UpgradePackageOfficeTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final PackageFolder upgradePackageFolder;
    private final Package upgradePackage;
    private final Product[] billingProductsUpgradePackage;

    public UpgradePackageOfficeTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_163074013.json",
                Dataset.class);

        steps = new Steps(data);

        upgradePackageFolder = data.packageFoldersUpgrade[0];
        upgradePackage = data.packageFoldersUpgrade[0].packages[0];
        billingProductsUpgradePackage = upgradePackage.productsFromBilling;
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
    @TmsLink("CRM-8193")
    @DisplayName("CRM-8193 - User can select a package on Upgrade Office")
    @Description("Check that user is able to choose a Package during Upgrade")
    public void test() {
        step("1. Open the Quote Wizard for the Existing Business Opportunity to add a new Sales quote, " +
                "select a package different from the preselected, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            
            packagePage.packageSelector.selectPackage(upgradePackageFolder.chargeTerm, upgradePackageFolder.name, upgradePackage);
            packagePage.saveChanges();
        });

        step("2. Open the Price tab, and check that displayed products are the same as on the Billing account", () -> {
            cartPage.openTab();
            cartPage.cartItemNames.shouldHave(size(billingProductsUpgradePackage.length));
            steps.cartTab.checkProductsInCartExistingBusiness(billingProductsUpgradePackage);
        });
    }
}
