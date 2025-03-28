package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartItem;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.List;

import static base.Pages.cartPage;
import static base.Pages.wizardPage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.ZERO_DISCRETION_VALUE;
import static com.aquiva.autotests.rc.utilities.NumberHelper.getNumberValueWithoutTrailingZeros;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.Double.parseDouble;
import static java.math.RoundingMode.HALF_UP;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("TargetPrice")
public class TargetPriceCalculationsTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Double yourPriceTotal;
    private CartItem dlUnlimitedCartItem;

    //  Test data
    private final Product dlUnlimited;
    private final Product creditBundle;
    private final Product yealinkPhone;
    private final Product polycomPhone;
    private final List<Product> nonFeeRecurringProducts;

    public TargetPriceCalculationsTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Advanced_Monthly_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        creditBundle = data.getProductByDataName("LC_IB_351");
        yealinkPhone = data.getProductByDataName("LC_HD_959");
        polycomPhone = data.getProductByDataName("LC_HD_936");

        nonFeeRecurringProducts = List.of(
                data.getProductByDataName("LC_SC_29"), data.getProductByDataName("LC_MLN_31"),
                data.getProductByDataName("LC_MLFN_45"), data.getProductByDataName("LC_DL_75"),
                dlUnlimited, creditBundle
        );

        dlUnlimited.quantity = 10;
        yealinkPhone.quantity = 3;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-30052")
    @TmsLink("CRM-29527")
    @DisplayName("CRM-30052 - Single Service primary quote (Monthly). \n" +
            "CRM-29527 - Accept Recommendations")
    @Description("CRM-30052 - Verify that overall discretion is calculated correctly for single service monthly primary quotes. \n" +
            "CRM-29527 - Verify that recommended discount can be applied via Target Price Details modal window")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                        "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Add several One-Time and Recurring products to the Cart", () ->
                steps.quoteWizard.addProductsOnProductsTab(yealinkPhone, polycomPhone, creditBundle)
        );

        //  CRM-30052
        step("3. Open the Price tab, check that there are no discounts in the Cart, " +
                "and that quantities of all recurring items are equal to 1, save changes, " +
                "and check that discretion value is equal to 0%", () -> {
            cartPage.openTab();
            cartPage.getAllVisibleCartItems()
                    .forEach(cartItem -> cartItem.getDiscountInput().shouldHave(exactValue("0")));
            cartPage.getAllVisibleCartItems()
                    .stream()
                    .filter(cartItem -> cartItem.getChargeTerm().getText().equals(data.chargeTerm))
                    .forEach(cartItem -> cartItem.getQuantityInput().shouldHave(exactValue("1")));

            cartPage.saveChanges();

            cartPage.discretionValue.shouldHave(exactTextCaseSensitive(ZERO_DISCRETION_VALUE));
        });

        //  CRM-30052
        step("4. Check that Quote.OverallDiscretion__c and Quote.AverageDiscount__c field values are equal to 0", () -> {
            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT OverallDiscretion__c, AverageDiscount__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);
            assertThat(quote.getOverallDiscretion__c())
                    .as("Quote.OverallDiscretion__c value")
                    .isEqualTo(0);
            assertThat(quote.getAverageDiscount__c())
                    .as("Quote.AverageDiscount__c value")
                    .isEqualTo(0);
        });

        step("5. Set discounts and quantities for some One-Time and Recurring products, and save changes", () -> {
            steps.cartTab.setUpQuantities(dlUnlimited, yealinkPhone);
            steps.cartTab.setUpDiscounts(creditBundle, polycomPhone);

            cartPage.saveChanges();
        });

        //  CRM-30052
        step("6. Check 'Discretion' value in the Quote Wizard", () -> {
            var targetPriceTotal = nonFeeRecurringProducts
                    .stream()
                    .mapToDouble(nonFeeRecurringProduct -> {
                        var cartItem = cartPage.getQliFromCartByDisplayName(nonFeeRecurringProduct.name);
                        cartItem.getQuantityInput().shouldNotBe(empty); //  to avoid NPE in the formula below 
                        return cartItem.getTargetPriceValue() * parseDouble(cartItem.getQuantityInput().getValue());
                    })
                    .sum();
            yourPriceTotal = nonFeeRecurringProducts.stream()
                    .mapToDouble(product -> parseDouble(product.yourPrice) * product.quantity)
                    .sum();

            var expectedDiscretionValue = BigDecimal.valueOf((targetPriceTotal - yourPriceTotal) / targetPriceTotal * 100)
                    .setScale(2, HALF_UP).stripTrailingZeros() + "%";
            cartPage.discretionValue.shouldHave(exactText(expectedDiscretionValue));
        });

        //  CRM-30052
        step("7. Check the current Quote.OverallDiscretion__c and Quote.AverageDiscount__c fields", () -> {
            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, OverallDiscretion__c, AverageDiscount__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + wizardPage.getSelectedQuoteId() + "'",
                    Quote.class);

            cartPage.discretionValue.shouldHave(exactText(quote.getOverallDiscretion__c() + "%"));

            var listPriceTotal = nonFeeRecurringProducts.stream()
                    .mapToDouble(product -> parseDouble(product.price) * product.quantity)
                    .sum();

            var expectedAverageDiscountValue = BigDecimal.valueOf((listPriceTotal - yourPriceTotal) / listPriceTotal * 100)
                    .setScale(2, HALF_UP).doubleValue();
            assertThat(quote.getAverageDiscount__c())
                    .as("Quote.AverageDiscount__c value")
                    .isEqualTo(expectedAverageDiscountValue);
        });

        //  CRM-29527
        step("8. Check that 'Target Price' for the DL Unlimited is not equal to its 'Your Price'", () -> {
            dlUnlimitedCartItem = cartPage.getQliFromCartByDisplayName(dlUnlimited.name);

            dlUnlimitedCartItem.getTargetPrice().shouldNotHave(exactTextCaseSensitive(dlUnlimited.yourPrice));
        });

        //  CRM-29527
        step("9. Click on the Target Price for the DL Unlimited, " +
                "click 'Accept Recommendations' button in Target Price Details modal window, " +
                "and check that Recommended Discount is applied to Your Discount for DL Unlimited " +
                "and its Target Price is now equal to Your Price", () -> {
            dlUnlimitedCartItem.getTargetPrice().click();

            var recommendedDiscountValue = cartPage.targetPriceModal.getRecommendedDiscount(dlUnlimited.name)
                    .shouldHave(attribute("title"))
                    .getAttribute("title");
            cartPage.targetPriceModal.acceptRecommendationsButton.click();

            dlUnlimitedCartItem.getDiscountInput()
                    .shouldHave(exactValue(getNumberValueWithoutTrailingZeros(Double.valueOf(recommendedDiscountValue))));

            dlUnlimitedCartItem.getTargetPrice()
                    .shouldHave(exactTextCaseSensitive(dlUnlimitedCartItem.getYourPrice().getText()));
        });
    }
}
