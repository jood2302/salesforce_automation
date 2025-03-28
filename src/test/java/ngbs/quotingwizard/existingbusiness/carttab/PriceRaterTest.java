package ngbs.quotingwizard.existingbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static base.Pages.cartPage;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
public class PriceRaterTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product productWithRater;
    private final Product dlUnlimited;

    public PriceRaterTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_163074013.json",
                Dataset.class);
        steps = new Steps(data);

        productWithRater = data.getProductByDataName("LC_CRF_51");
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
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
    @TmsLink("CRM-7932")
    @DisplayName("CRM-7932 - Existing Business. Price for Items with Raters is calculated according to its Rater")
    @Description("Verify that rater on the Cart applies correctly and is dependent on item's Quantity")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, and select package for it", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        step("2. Open the Price tab and check the initial price for a product with rater", () -> {
            cartPage.openTab();
            cartPage.getQliFromCartByDisplayName(productWithRater.name)
                    .getListPrice()
                    .shouldHave(exactTextCaseSensitive(steps.quoteWizard.currencyPrefix + productWithRater.price));
        });

        step("3. Change quantity for the DL Unlimited and check the list price for the product with rater", () -> {
            //  Actual initial price is taken into account while calculating 'Total Price' after change in 'New Quantity'
            var actualInitialPrice = new BigDecimal(productWithRater.price)
                    .multiply(new BigDecimal(productWithRater.existingQuantity));

            for (var priceRater : productWithRater.priceRater) {
                step("Minimum border = " + priceRater.minimumBorder +
                        ", maximum border = " + priceRater.maximumBorder +
                        ", current rater price = " + priceRater.raterPrice, () -> {
                    //  update product's quantity with minimum border
                    cartPage.setNewQuantityForQLItem(dlUnlimited.name, priceRater.minimumBorder);

                    cartPage.getQliFromCartByDisplayName(productWithRater.name)
                            .getListPrice()
                            .shouldHave(exactTextCaseSensitive(steps.quoteWizard.currencyPrefix + priceRater.raterPrice));
                    cartPage.getQliFromCartByDisplayName(productWithRater.name)
                            .getTotalPrice()
                            .shouldHave(exactTextCaseSensitive(steps.quoteWizard.currencyPrefix +
                                    priceRater.raterPrice
                                            .multiply(new BigDecimal(priceRater.minimumBorder))
                                            .subtract(actualInitialPrice)));

                    //  update product's quantity with maximum border
                    cartPage.setNewQuantityForQLItem(dlUnlimited.name, priceRater.maximumBorder);

                    cartPage.getQliFromCartByDisplayName(productWithRater.name)
                            .getListPrice()
                            .shouldHave(exactTextCaseSensitive(steps.quoteWizard.currencyPrefix + priceRater.raterPrice));
                    cartPage.getQliFromCartByDisplayName(productWithRater.name)
                            .getTotalPrice()
                            .shouldHave(exactTextCaseSensitive(steps.quoteWizard.currencyPrefix +
                                    priceRater.raterPrice
                                            .multiply(new BigDecimal(priceRater.maximumBorder))
                                            .subtract(actualInitialPrice)));
                });
            }
        });
    }
}
