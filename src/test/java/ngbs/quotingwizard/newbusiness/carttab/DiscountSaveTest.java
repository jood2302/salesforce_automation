package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static base.Pages.cartPage;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToInteger;
import static com.codeborne.selenide.Condition.value;
import static io.qameta.allure.Allure.step;
import static java.lang.Double.parseDouble;
import static java.lang.String.format;
import static java.math.RoundingMode.HALF_EVEN;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
public class DiscountSaveTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private QuoteLineItem dlUnlimitedWithDiscountQLI;
    private QuoteLineItem phoneWithDiscountQLI;

    //  Test data
    private final Product dlUnlimited;
    private final Product phoneToAdd;

    public DiscountSaveTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataName("LC_HD_959");
        phoneToAdd.discount = 20;
        phoneToAdd.discountType = "USD";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-7863")
    @TmsLink("CRM-23945")
    @DisplayName("CRM-7863 - New Business. Discount is applied to Discount Template. \n" +
            "CRM-23945 - Effective Price and Discount calculation")
    @Description("CRM-7863 - Verify that Discounts added on the Price tab are applied to QLIs. \n" +
            "CRM-23945 - Verify the calculation of Effective Price and Discount fields on QLI")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, save changes, and add some products on the Add Products tab", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
        });

        //  CRM-7863
        step("2. Open the Price tab, and check the initial discount on the DL Unlimited's Quote Line Item", () -> {
            cartPage.openTab();

            cartPage.getQliFromCartByDisplayName(dlUnlimited.name)
                    .getDiscountInput()
                    .shouldHave(value(String.valueOf(dlUnlimited.discount)));

            var initialQuoteLineItem = getQuoteLineItemFromDB(dlUnlimited);
            assertThat(doubleToInteger(initialQuoteLineItem.getDiscount_number__c()))
                    .as("Initial QuoteLineItem.Discount_number__c value for " + dlUnlimited.name)
                    .isEqualTo(dlUnlimited.discount);
        });

        step("3. Set new discounts for DL Unlimited and a phone, save changes, " +
                "and check the updated values of QuoteLineItem.Discount_number__c field for both products", () -> {
            steps.cartTab.setUpDiscounts(dlUnlimited, phoneToAdd);
            cartPage.saveChanges();

            //  CRM-7863
            cartPage.getQliFromCartByDisplayName(dlUnlimited.name)
                    .getDiscountInput()
                    .shouldHave(value(String.valueOf(dlUnlimited.newDiscount)));

            //  CRM-7863, CRM-23945
            dlUnlimitedWithDiscountQLI = getQuoteLineItemFromDB(dlUnlimited);
            assertThat(doubleToInteger(dlUnlimitedWithDiscountQLI.getDiscount_number__c()))
                    .as(format("QuoteLineItem.Discount_number__c value for %s after adding a new discount",
                            dlUnlimited.name))
                    .isEqualTo(dlUnlimited.newDiscount);

            //  CRM-23945
            phoneWithDiscountQLI = getQuoteLineItemFromDB(phoneToAdd);
            assertThat(doubleToInteger(phoneWithDiscountQLI.getDiscount_number__c()))
                    .as(format("QuoteLineItem.Discount_number__c value for %s after adding a new discount",
                            phoneToAdd.name))
                    .isEqualTo(phoneToAdd.discount);
        });

        //  CRM-23945
        step("4. Check the updated values of the QuoteLineItem's DiscountValueNew__c and EffectivePriceNew__c fields for both products", () -> {
            var dlUnlimitedDiscountValueNewExpected = new BigDecimal(parseDouble(dlUnlimited.price) * dlUnlimited.newDiscount / 100)
                    .setScale(2, HALF_EVEN)
                    .doubleValue();
            assertThat(dlUnlimitedWithDiscountQLI.getDiscountValueNew__c())
                    .as(format("QuoteLineItem.DiscountValueNew__c value for %s", dlUnlimited.name))
                    .isEqualTo(dlUnlimitedDiscountValueNewExpected);

            var dlUnlimitedEffectivePriceNewExpected = parseDouble(dlUnlimited.price) - dlUnlimitedDiscountValueNewExpected;
            assertThat(dlUnlimitedWithDiscountQLI.getEffectivePriceNew__c())
                    .as(format("QuoteLineItem.EffectivePriceNew__c value for %s", dlUnlimited.name))
                    .isEqualTo(dlUnlimitedEffectivePriceNewExpected)
                    .isEqualTo(parseDouble(dlUnlimited.yourPrice));

            assertThat(doubleToInteger(phoneWithDiscountQLI.getDiscountValueNew__c()))
                    .as(format("QuoteLineItem.DiscountValueNew__c value for %s", phoneToAdd.name))
                    .isEqualTo(phoneToAdd.discount);

            var phoneEffectivePriceNewExpected = parseDouble(phoneToAdd.price) - phoneToAdd.discount;
            assertThat(phoneWithDiscountQLI.getEffectivePriceNew__c())
                    .as(format("QuoteLineItem.EffectivePriceNew__c value for %s", phoneToAdd.name))
                    .isEqualTo(phoneEffectivePriceNewExpected);
        });
    }

    /**
     * Get information on Opportunity's selected quote line item from DB.
     *
     * @param product {@link Product} test data with the product for which discounts are to be checked
     * @return {@link QuoteLineItem} entity with product's external ID (e.g. "LC_DL-UNL_50" for DigitalLine Unlimited Core),
     * Discount_number__c, Discount_type__c, DiscountValueNew__c, EffectivePriceNew__c
     * @throws Exception in case of malformed query or network errors
     */
    private QuoteLineItem getQuoteLineItemFromDB(Product product) throws Exception {
        return enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, Product2.ExtID__c, Discount_number__c, " +
                        "Discount_type__c, DiscountValueNew__c, EffectivePriceNew__c " +
                        "FROM QuoteLineItem " +
                        "WHERE Quote.Opportunity.Id = '" + steps.quoteWizard.opportunity.getId() + "' " +
                        "AND Product2.ExtID__c = '" + product.dataName + "'",
                QuoteLineItem.class);
    }
}
