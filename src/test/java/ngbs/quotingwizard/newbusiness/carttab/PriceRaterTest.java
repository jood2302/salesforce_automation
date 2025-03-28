package ngbs.quotingwizard.newbusiness.carttab;

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
@Tag("P1")
@Tag("PDV")
@Tag("NGBS")
public class PriceRaterTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product productWithRater;
    private final Product dlUnlimited;

    public PriceRaterTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);

        productWithRater = data.getProductByDataName("LC_CRF_51");
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-7839")
    @TmsLink("CRM-7850")
    @DisplayName("CRM-7839 - New Business. Price for Items with Raters is calculated according to its Rater. \n" +
            "CRM-7850 - New Business. Totals calculation")
    @Description("CRM-7839 - Check that if User increases / decreases Quantity of Items with Rater, " +
            "the price will be recalculated according to the Rater rules. \n" +
            "CRM-7850 - Verify that totals of Quote and Items in Cart are accurate.")
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
            for (var priceRater : productWithRater.priceRater) {
                step("Minimum border = " + priceRater.minimumBorder +
                        ", maximum border = " + priceRater.maximumBorder +
                        ", current rater price = " + priceRater.raterPrice, () -> {
                    //  update product's quantity with minimum border
                    cartPage.setQuantityForQLItem(dlUnlimited.name, priceRater.minimumBorder);

                    cartPage.getQliFromCartByDisplayName(productWithRater.name)
                            .getListPrice()
                            .shouldHave(exactTextCaseSensitive(steps.quoteWizard.currencyPrefix + priceRater.raterPrice));
                    cartPage.getQliFromCartByDisplayName(productWithRater.name)
                            .getTotalPrice()
                            .shouldHave(exactTextCaseSensitive(steps.quoteWizard.currencyPrefix +
                                    priceRater.raterPrice.multiply(new BigDecimal(priceRater.minimumBorder))));

                    //  update product's quantity with maximum border
                    cartPage.setQuantityForQLItem(dlUnlimited.name, priceRater.maximumBorder);

                    cartPage.getQliFromCartByDisplayName(productWithRater.name)
                            .getListPrice()
                            .shouldHave(exactTextCaseSensitive(steps.quoteWizard.currencyPrefix + priceRater.raterPrice));
                    cartPage.getQliFromCartByDisplayName(productWithRater.name)
                            .getTotalPrice()
                            .shouldHave(exactTextCaseSensitive(steps.quoteWizard.currencyPrefix +
                                    priceRater.raterPrice.multiply(new BigDecimal(priceRater.maximumBorder))));
                });
            }
        });
    }
}
