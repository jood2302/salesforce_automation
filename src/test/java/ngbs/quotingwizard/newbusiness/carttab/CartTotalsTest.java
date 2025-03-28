package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static base.Pages.cartPage;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.math.BigDecimal.valueOf;
import static java.math.RoundingMode.DOWN;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("NGBS")
@Tag("PriceTab")
@Tag("Totals")
public class CartTotalsTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product digitalLineUnlimited;
    private final Product complianceFee;
    private final Product e911ServiceFee;

    public CartTotalsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_NoPhones.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        complianceFee = data.getProductByDataName("LC_CRF_51");
        e911ServiceFee = data.getProductByDataName("LC_E911_52");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-13125")
    @DisplayName("CRM-13125 - 'New Monthly Recurring Charges' from the Price tab is equal to 'Total MRR New' from the Quote")
    @Description("To check that 'New Monthly Recurring Charges' from the Price tab " +
            "(that calculated as sum of 'Total Prices' for all Non Zero Priced Recurring Items) " +
            "is equal to the 'Total MRR New' from the Quote")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "and select a package for it", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        step("2. Open the Price tab, set up quantity and discount for DL Unlimited, and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(digitalLineUnlimited);
            steps.cartTab.setUpDiscounts(digitalLineUnlimited);
            cartPage.saveChanges();
        });

        step("3. Check that 'New Monthly Recurring Charges' value from the Price Tab is correct " +
                "and is equal to the corresponding field Quote.Total_MRR_New__c in DB", () -> {
            //  Total prices for all recurring items (using discounted prices where applicable)
            var dlUnlimitedTotalPriceExpected = new BigDecimal(digitalLineUnlimited.yourPrice)
                    .multiply(valueOf(digitalLineUnlimited.quantity));
            var complianceFeeTotalPriceExpected = new BigDecimal(complianceFee.price)
                    .multiply(valueOf(complianceFee.quantity));
            var e911ServiceTotalPriceExpected = new BigDecimal(e911ServiceFee.price)
                    .multiply(valueOf(e911ServiceFee.quantity));
            var totalMrrNewExpected = dlUnlimitedTotalPriceExpected
                    .add(complianceFeeTotalPriceExpected)
                    .add(e911ServiceTotalPriceExpected)
                    .setScale(2, DOWN)
                    .toString();

            var quoteWithMRR = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Total_MRR_New__c " +
                            "FROM Quote " +
                            "WHERE Opportunity.Id = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);

            cartPage.footer.newRecurringCharges.shouldHave(exactTextCaseSensitive(totalMrrNewExpected));

            assertThat(quoteWithMRR.getTotal_MRR_New__c())
                    .as("Quote.Total_MRR_New__c value")
                    .isEqualTo(Double.valueOf(totalMrrNewExpected));
        });
    }
}
