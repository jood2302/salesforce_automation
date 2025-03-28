package ngbs.quotingwizard.newbusiness.quotetab.specialterms;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

@Tag("P0")
@Tag("Lambda")
@Tag("NGBS")
@Tag("QuoteTab")
public class CreditAmountMonthlyTest extends BaseTest {
    private final Steps steps;
    private final SpecialTermsSteps specialTermsSteps;

    //  Test data
    private final Product monthlyChargeTermProduct;

    public CreditAmountMonthlyTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_2TypesOfDLs_RegularAndPOC.json",
                Dataset.class);
        steps = new Steps(data);
        specialTermsSteps = new SpecialTermsSteps(data);

        monthlyChargeTermProduct = data.getProductByDataName("LC_HDR_619");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-7464")
    @DisplayName("CRM-7464 - 'Credit Amount' for 'Monthly - Contract' plan with Special Terms")
    @Description("Verify 'Credit Amount' calculation for New Customers " +
            "with 'Monthly - Contract' service plan and Free Months 'Special Terms'")
    public void test() {
        specialTermsSteps.creditAmountTestSteps(steps.quoteWizard.opportunity.getId(), monthlyChargeTermProduct);
    }
}
