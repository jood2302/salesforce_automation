package ngbs.quotingwizard.existingbusiness.upgrade;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.components.packageselector.PackageSelector.PACKAGE_FROM_ACCOUNT_BADGE;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.util.Collections.singletonList;

@Tag("P1")
@Tag("NGBS")
@Tag("PackageTab")
@Tag("Upgrade")
public class UpgradeFaxToOfficeTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final String faxService;
    private final Package faxPackage;
    private final String initialPackageFullName;
    private final String upgradePackageFolderName;
    private final Package upgradePackage;
    private final Product dlUnlimited;

    public UpgradeFaxToOfficeTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_Fax_MVP_Monthly_NonContract_82739013.json",
                Dataset.class);

        steps = new Steps(data);

        faxService = data.packageFolders[0].name;
        faxPackage = data.packageFolders[0].packages[0];
        initialPackageFullName = data.packageFolders[0].packages[0].getFullName();
        upgradePackageFolderName = data.packageFoldersUpgrade[0].name;
        upgradePackage = data.packageFoldersUpgrade[0].packages[0];
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50", singletonList(upgradePackage.productsDefault));
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
    @TmsLink("CRM-20572")
    @DisplayName("CRM-20572 - Upgrade from Fax to Office")
    @Description("Check that user is able to Upgrade from Fax to Office Packages via Quote Wizard")
    public void test() {
        step("1. Open the Quote Wizard for the Existing Business Opportunity to add a new Sales Quote, " +
                "and check that selected Fax Package is a Package from Account, " +
                "and Service picklist contains only 'Fax' and 'Office' options", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());

            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(initialPackageFullName));
            packagePage.packageSelector.getSelectedPackage().getBadge()
                    .shouldHave(exactTextCaseSensitive(PACKAGE_FROM_ACCOUNT_BADGE));

            packagePage.packageSelector.packageFilter.servicePicklist.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(faxService));
            packagePage.packageSelector.packageFilter.servicePicklist.getOptions()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(faxService, upgradePackageFolderName));
        });

        step("2. Select an Office Package and save changes", () -> {
            packagePage.packageSelector.packageFilter.servicePicklist.selectOption(upgradePackageFolderName);
            packagePage.packageSelector.selectPackage(data.chargeTerm, upgradePackageFolderName, upgradePackage);
            packagePage.saveChanges();
        });

        step("3. Open the Add Products tab and check that Products related to Office Package are shown", () -> {
            productsPage.openTab();
            productsPage.findProduct(dlUnlimited).getSelf().shouldBe(visible);
        });

        step("4. Open the Price tab and check that Products related to Fax and Office Packages are shown correctly", () -> {
            cartPage.openTab();

            step("Check that the Office products are shown on the Price tab without the Existing Quantity field", () -> {
                for (var product : upgradePackage.productsDefault) {
                    step("Check the Office product = " + product.name, () -> {
                        var cartItem = cartPage.getQliFromCartByDisplayName(product.name);
                        cartItem.getDisplayName().shouldBe(visible);
                        cartItem.getExistingQuantityInput().shouldBe(hidden);
                    });
                }
            });

            step("Check that the Fax products are shown on the Price tab with the New Quantity = 0", () -> {
                for (var product : faxPackage.productsFromBilling) {
                    step("Check the Fax product = " + product.name, () -> {
                        var faxCartItem = cartPage.getQliFromCartByDisplayName(product.name);
                        faxCartItem.getDisplayName().shouldBe(visible);
                        faxCartItem.getNewQuantityInput().shouldHave(exactValue("0"));
                    });
                }
            });
        });
    }
}
