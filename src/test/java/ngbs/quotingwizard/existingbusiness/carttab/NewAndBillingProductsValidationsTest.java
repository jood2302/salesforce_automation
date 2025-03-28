package ngbs.quotingwizard.existingbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.Arrays;

import static base.Pages.cartPage;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToInteger;
import static com.aquiva.autotests.rc.utilities.StringHelper.PERCENT;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.DISCOUNT_TYPE_CURRENCY;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.DISCOUNT_TYPE_PERCENTAGE;
import static io.qameta.allure.Allure.step;
import static java.util.stream.Collectors.toMap;

@Tag("P0")
@Tag("P1")
@Tag("PDV")
@Tag("NGBS")
public class NewAndBillingProductsValidationsTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public NewAndBillingProductsValidationsTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_Meetings_Monthly_NonContract_82740013.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
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
    @TmsLink("CRM-11320")
    @TmsLink("CRM-8175")
    @TmsLink("CRM-8176")
    @TmsLink("CRM-8177")
    @TmsLink("CRM-8178")
    @DisplayName("CRM-11320 - User can add a new Item to Cart. Existing Business. \n" +
            "CRM-8175 - Existing Business. Verify that Preselected products on the Price tab equal to Billing products. \n" +
            "CRM-8176 - Existing Business. Verify that New quantity for preselected products equals to Existing quantity. \n" +
            "CRM-8177 - Quantity can be changed in Cart. Existing Business. \n" +
            "CRM-8178 - Discount can be changed in Cart. Existing Business.")
    @Description("CRM-11320 - To check that user can add the new product from the Add Products tab to the Price tab by pressing 'Add' Button. \n" +
            "CRM-8175 - To check that preselected products are equal to Billing Info. \n" +
            "CRM-8176 - To check that preselected products' quantity is equal to Billing Info. \n" +
            "CRM-8177 - To check that product quantity can be changed in Cart and it is applied after saving on Existing Business Opportunity. \n" +
            "CRM-8178 - To check that product discount can be changed in Cart and it is applied after saving on Existing Business Opportunity")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select package for it, save changes, and add products on the Add Products tab", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
        });

        step("2. Open the Price tab, change quantities and discounts for the new products, and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(data.getNewProductsToAdd());
            steps.cartTab.setUpDiscounts(data.getNewProductsToAdd());
            cartPage.saveChanges();
        });

        //  For CRM-8177, CRM-8178, CRM-11320
        step("3. Check added products, their new quantities and applied discounts on the Price tab", () ->
                steps.cartTab.checkProductsInCartExistingBusiness(data.getNewProductsToAdd())
        );

        //  For CRM-8175, CRM-8176
        step("4. Check products from billing on the Price tab", () ->
                steps.cartTab.checkProductsInCartExistingBusiness(data.getProductsFromBilling())
        );

        //  For CRM-8177, CRM-8178
        step("5. Check new quantities and discounts for the Opportunity's Quote Line Items in SFDC " +
                "for the added products", () -> {
            var quoteLineItems = enterpriseConnectionUtils.query(
                    "SELECT Id, Display_Name__c, NewQuantity__c, Discount_number__c, Discount_type__c " +
                            "FROM QuoteLineItem " +
                            "WHERE Quote.OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    QuoteLineItem.class);

            //  Additional mapping of 'Product Name <-> Product' for assertions against Quote Line Items collection above
            var productsMap = Arrays.stream(data.getNewProductsToAdd())
                    .collect(toMap(product -> product.name, product -> product));

            //  Filter to check only added products and skip others (from billing)
            var quoteLineItemsAddedNew = quoteLineItems.stream()
                    .filter(qli -> productsMap.get(qli.getDisplay_Name__c()) != null)
                    .toList();

            var softly = new SoftAssertions();

            for (var qli : quoteLineItemsAddedNew) {
                step("Check discount for '" + qli.getDisplay_Name__c() + "' Quote Line Item", () -> {
                    var qliName = qli.getDisplay_Name__c();

                    var qliNewQuantityActual = doubleToInteger(qli.getNewQuantity__c());
                    var qliDiscountNumberActual = BigDecimal.valueOf(qli.getDiscount_number__c());
                    var qliDiscountTypeActual = qli.getDiscount_type__c();

                    var qliNewQuantityExpected = productsMap.get(qliName).quantity;
                    var qliDiscountNumberExpected = BigDecimal.valueOf(productsMap.get(qliName).discount);
                    var qliDiscountTypeExpected = productsMap.get(qliName).discountType.equals(PERCENT) ?
                            DISCOUNT_TYPE_PERCENTAGE :
                            DISCOUNT_TYPE_CURRENCY;

                    softly.assertThat(qliNewQuantityActual)
                            .as("QuoteLineItem.NewQuantity__c value")
                            .isEqualTo(qliNewQuantityExpected);

                    softly.assertThat(qliDiscountNumberActual)
                            .as("QuoteLineItem.Discount_number__c value")
                            .isEqualByComparingTo(qliDiscountNumberExpected);

                    softly.assertThat(qliDiscountTypeActual)
                            .as("QuoteLineItem.Discount_type__c value")
                            .isEqualTo(qliDiscountTypeExpected);
                });
            }

            softly.assertAll();
        });
    }
}
