package ngbs.quotingwizard.existingbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static com.aquiva.autotests.rc.model.ngbs.dto.license.CatalogItem.getItemFromTestData;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("NGBS")
public class DiscountsTheSameAfterCreatingNewQuoteTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final Product dlBasic;
    private final Product[] productsWithDiscounts;

    public DiscountsTheSameAfterCreatingNewQuoteTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_OneTimeDiscount_163079013.json",
                Dataset.class);

        steps = new Steps(data);

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
    @TmsLink("CRM-22333")
    @DisplayName("CRM-22333 - Discounts set the same as on account after creating new Quote")
    @Description("When creating a new Quote, discounts stay the same as on account")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select the same package from the NGBS account, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Price tab and check that discounts are the same as on the NGBS account", () -> {
            cartPage.openTab();
            steps.cartTab.checkDiscountsInCartExistingBusiness(productsWithDiscounts);
        });

        step("3. Check that EffectivePriceNew__c, Discount_number__c, " +
                "and Discount_type__c on the QuoteLineItem in DB " +
                "are the same as 'Your Price', 'Discount', and 'Discount Type' " +
                "on the corresponding cart item with a non-zero discount in Quote Wizard", () ->
                steps.cartTab.checkCartItemsAgainstQuoteLineItemsTestSteps(steps.quoteWizard.currencyPrefix, productsWithDiscounts)
        );
    }
}
