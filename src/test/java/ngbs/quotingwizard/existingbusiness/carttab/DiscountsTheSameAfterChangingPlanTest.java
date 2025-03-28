package ngbs.quotingwizard.existingbusiness.carttab;

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
import static com.aquiva.autotests.rc.model.ngbs.dto.license.CatalogItem.getItemFromTestData;
import static com.codeborne.selenide.Condition.exactValue;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("NGBS")
@Tag("PackageTab")
public class DiscountsTheSameAfterChangingPlanTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final String chargeTerm;
    private final String upgradePackageFolderName;
    private final Package upgradePackage;
    private final Package existingPackage;
    private final Product dlBasic;
    private final Product[] productsWithDiscounts;

    public DiscountsTheSameAfterChangingPlanTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_OneTimeDiscount_163079013.json",
                Dataset.class);

        steps = new Steps(data);

        chargeTerm = data.chargeTerm;
        upgradePackageFolderName = data.packageFoldersUpgrade[0].name;
        upgradePackage = data.packageFoldersUpgrade[0].packages[0];
        existingPackage = data.packageFolders[0].packages[0];

        dlBasic = data.getProductByDataName("LC_DL-BAS_178");
        productsWithDiscounts = new Product[]{
                data.getProductByDataName("LC_DL-UNL_50"),
                dlBasic,
                data.getProductByDataName("LC_HD_687"),
                data.getProductByDataName("LC_HD_959")
        };
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.purchaseAdditionalLicensesInNGBS(getItemFromTestData(dlBasic.dataName, dlBasic.quantity));
            steps.ngbs.stepCreateDiscountsInNGBS(data.billingId, data.packageId, productsWithDiscounts);
        }

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-22335")
    @DisplayName("CRM-22335 - Discounts are set the same as on account after changing plan to original")
    @Description("When changing plan to original on Upgrade Quote discounts are set same as on account and Quote becomes Upsell")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "and select any package that is different from Account's", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());

            packagePage.packageSelector.selectPackage(chargeTerm, upgradePackageFolderName, upgradePackage);
        });

        step("2. Open the Price Tab and check that all discounts are equal to '0' (zero)", () -> {
            cartPage.openTab();
            cartPage.getAllVisibleCartItems()
                    .forEach(cartItem -> cartItem.getDiscountInput().shouldHave(exactValue("0")));
        });

        step("3. Open the Select Package Tab, select back a package from account, and save changes", () -> {
            packagePage.openTab();
            packagePage.packageSelector.selectPackage(chargeTerm, upgradePackageFolderName, existingPackage);
            packagePage.saveChanges();
        });

        step("4. Open the Price Tab, " +
                "and check that EffectivePriceNew__c, Discount_number__c, and Discount_type__c on the QuoteLineItem in DB " +
                "are the same as 'Your Price', 'Discount', and 'Discount Type' " +
                "on the corresponding cart item with a non-zero discount in Quote Wizard", () -> {
            cartPage.openTab();
            steps.cartTab.checkCartItemsAgainstQuoteLineItemsTestSteps(steps.quoteWizard.currencyPrefix, productsWithDiscounts);
        });
    }
}
