package ngbs.quotingwizard.existingbusiness.upgrade;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static base.Pages.packagePage;
import static com.codeborne.selenide.Condition.exactValue;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
@Tag("Upgrade")
public class DiscountRemovalTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final String upgradePackageFolderName;
    private final Package upgradePackage;

    public DiscountRemovalTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_163074013.json",
                Dataset.class);

        steps = new Steps(data);

        upgradePackageFolderName = data.packageFoldersUpgrade[0].name;
        upgradePackage = data.packageFoldersUpgrade[0].packages[0];
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
    @TmsLink("CRM-11307")
    @DisplayName("CRM-11307 - After Upgrade to the New Package all discounts are set to 0. Existing Business")
    @Description("Check that if User Upgrades to the New Package then all the Discounts fields are set to 0")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, save changes, and add additional products on the Add Products tab", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
        });

        step("2. Open the Price tab, and set up discounts for the products", () -> {
            cartPage.openTab();
            steps.cartTab.setUpDiscounts(data.getNewProductsToAdd());
            steps.cartTab.setUpDiscounts(data.getProductsFromBilling());
        });

        step("3. Open the Select Package tab, and select a new Package", () -> {
            packagePage.openTab();
            packagePage.packageSelector.selectPackage(data.chargeTerm, upgradePackageFolderName, upgradePackage);
        });

        step("4. Open the Price tab and check that all discounts are equal to '0' (zero)", () -> {
            cartPage.openTab();

            cartPage.getAllVisibleCartItems()
                    .forEach(cartItem -> cartItem.getDiscountInput().shouldHave(exactValue("0")));
        });
    }
}
