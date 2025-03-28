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
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.HashMap;

import static base.Pages.cartPage;
import static com.aquiva.autotests.rc.utilities.StringHelper.PERCENT;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.DISCOUNT_TYPE_CURRENCY;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.DISCOUNT_TYPE_PERCENTAGE;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
public class QliUpdateTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public QliUpdateTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_AnnualAndMonthly_Contract_PhonesAndDLs_Promos.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-7852")
    @DisplayName("CRM-7852 - New Business. QLIs update")
    @Description("Verify that Quote Line items are updated via Cart")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and add some products on the Add Products tab", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
        });

        step("2. Open the Price tab, update quantities and discounts for the added products, and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(data.getNewProductsToAdd());
            steps.cartTab.setUpQuantities(data.getProductsDefault());
            steps.cartTab.setUpDiscounts(data.getNewProductsToAdd());
            steps.cartTab.setUpDiscounts(data.getProductsDefault());

            cartPage.saveChanges();
        });

        step("3. Check the displayed products on the Price tab", () -> {
            steps.cartTab.checkProductsInCartNewBusiness(data.getNewProductsToAdd());
            steps.cartTab.checkProductsInCartNewBusiness(data.getProductsDefault());
        });

        step("4. Check Quote Line Items in DB against Products data", () -> {
            var productsMap = new HashMap<String, Product>();
            for (var currentProduct : data.getNewProductsToAdd()) {
                productsMap.put(currentProduct.name, currentProduct);
            }
            for (var currentProduct : data.getProductsDefault()) {
                productsMap.put(currentProduct.name, currentProduct);
            }

            var quoteLineItems = enterpriseConnectionUtils.query(
                    "SELECT Id, Display_Name__c, Quantity, Discount_number__c, Discount_type__c " +
                            "FROM QuoteLineItem " +
                            "WHERE Quote.Opportunity.Id = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND IsHiddenLicense__c = false",
                    QuoteLineItem.class);

            assertThat(quoteLineItems.size())
                    .as("Quote Line Items List size")
                    .isEqualTo(productsMap.size());

            var softly = new SoftAssertions();

            for (var qli : quoteLineItems) {
                var qliName = qli.getDisplay_Name__c();

                var qliQuantityActual = BigDecimal.valueOf(qli.getQuantity());
                var qliDiscountNumberActual = BigDecimal.valueOf(qli.getDiscount_number__c());
                var qliDiscountTypeActual = qli.getDiscount_type__c();

                var qliQuantityExpected = new BigDecimal(productsMap.get(qliName).quantity);
                var qliDiscountNumberExpected = new BigDecimal(productsMap.get(qliName).discount);
                var qliDiscountTypeExpected = productsMap.get(qliName).discountType.equals(PERCENT) ?
                        DISCOUNT_TYPE_PERCENTAGE :
                        DISCOUNT_TYPE_CURRENCY;

                softly.assertThat(qliQuantityActual)
                        .as("Quantity on QLI '" + qliName
                                + "' expected:  " + qliQuantityExpected
                                + ", actual:  " + qliQuantityActual)
                        .isEqualByComparingTo(qliQuantityExpected);

                softly.assertThat(qliDiscountNumberActual)
                        .as("Discount Number on QLI '" + qliName
                                + "' expected:  " + qliDiscountNumberExpected
                                + ", actual:  " + qliDiscountNumberActual)
                        .isEqualByComparingTo(qliDiscountNumberExpected);

                softly.assertThat(qliDiscountTypeActual)
                        .as("Discount Type on QLI '" + qliName
                                + "' expected:  " + qliDiscountTypeExpected
                                + ", actual:  " + qliDiscountTypeActual)
                        .isEqualTo(qliDiscountTypeExpected);
            }

            softly.assertAll();
        });
    }
}
