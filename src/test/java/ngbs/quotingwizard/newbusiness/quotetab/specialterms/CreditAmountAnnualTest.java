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
@Tag("PDV")
@Tag("Lambda")
@Tag("NGBS")
@Tag("QuoteTab")
public class CreditAmountAnnualTest extends BaseTest {
    private final Steps steps;
    private final SpecialTermsSteps specialTermsSteps;

    //  Test data
    private final Product annualChargeTermProduct;

    public CreditAmountAnnualTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_NoPhones.json",
                Dataset.class);
        steps = new Steps(data);
        specialTermsSteps = new SpecialTermsSteps(data);

        annualChargeTermProduct = data.getProductByDataName("LC_HDR_619");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-7553")
    @DisplayName("CRM-7553 - 'Credit Amount' for 'Annual' plan with Special Terms")
    @Description("Verify 'Credit Amount' calculation for New Customers " +
            "with 'Annual' service plan and 'Special Terms' set to 'N Free Month of Service'")
    public void test() {
        specialTermsSteps.creditAmountTestSteps(steps.quoteWizard.opportunity.getId(), annualChargeTermProduct);
    }
}
