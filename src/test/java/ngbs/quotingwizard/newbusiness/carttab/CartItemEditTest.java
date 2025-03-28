package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartItem.MIN_DISCOUNT;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartItem.MIN_QUANTITY;
import static com.aquiva.autotests.rc.utilities.StringHelper.USD_CURRENCY_ISO_CODE;
import static com.codeborne.selenide.Condition.exactValue;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("NGBS")
public class CartItemEditTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product dlUnlimited;
    private final Product phoneToAdd;

    private final int negativeIntQuantity;
    private final double decimalQuantity;

    public CartItemEditTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_1PhoneAnd1DL_RegularAndPOC.json",
                Dataset.class);
        steps = new Steps(data);

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataName("LC_HDR_619");
        phoneToAdd.discountType = USD_CURRENCY_ISO_CODE;

        negativeIntQuantity = -1;
        decimalQuantity = 123.15;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-7845")
    @DisplayName("CRM-7845 - New Business. Inputs")
    @Description("Check for validations for fields on the Price tab - Quantity and Discount")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity, add a new Sales quote, " +
                "select a package for it, add some products, and open the Price tab", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
            steps.quoteWizard.addProductsOnProductsTab(phoneToAdd);
            cartPage.openTab();
        });

        step("2. Enter zero and negative number in 'Quantity' field for one of the added products " +
                "and check that the entered value is reset back to the minimum available value", () -> {
            cartPage.setQuantityForQLItem(dlUnlimited.name, negativeIntQuantity);

            cartPage.getQliFromCartByDisplayName(dlUnlimited.name)
                    .getQuantityInput()
                    .shouldHave(exactValue(MIN_QUANTITY));

            cartPage.setQuantityForQLItem(dlUnlimited.name, 0);

            cartPage.getQliFromCartByDisplayName(dlUnlimited.name)
                    .getQuantityInput()
                    .shouldHave(exactValue(MIN_QUANTITY));
        });

        step("3. Enter decimal number in 'Quantity' field for one of the added products " +
                "and check that the entered value is reset back to its rounded value", () -> {
            var expectedRoundedQuantity = String.valueOf(Math.round(decimalQuantity));

            cartPage.getQliFromCartByDisplayName(dlUnlimited.name)
                    .getQuantityInput()
                    .setValue(String.valueOf(decimalQuantity))
                    .unfocus();

            cartPage.getQliFromCartByDisplayName(dlUnlimited.name)
                    .getQuantityInput()
                    .shouldHave(exactValue(expectedRoundedQuantity));
        });

        step("4. Enter negative number in 'Discount' field for one of the products (discount type = Percent) " +
                "and check that the entered value is reset back to the minimum available value", () -> {
            cartPage.setDiscountTypeForQLItem(dlUnlimited.name, dlUnlimited.discountType);
            cartPage.setDiscountForQLItem(dlUnlimited.name, negativeIntQuantity);

            cartPage.getQliFromCartByDisplayName(dlUnlimited.name)
                    .getDiscountInput()
                    .shouldHave(exactValue(MIN_DISCOUNT));
        });

        step("5. Enter negative number in 'Discount' field for one of the products (discount type = Currency) " +
                "and check that the entered value is reset back to the minimum available value", () -> {
            cartPage.setDiscountTypeForQLItem(phoneToAdd.name, phoneToAdd.discountType);
            cartPage.setDiscountForQLItem(phoneToAdd.name, negativeIntQuantity);

            cartPage.getQliFromCartByDisplayName(phoneToAdd.name)
                    .getDiscountInput()
                    .shouldHave(exactValue(MIN_DISCOUNT));
        });
    }
}
