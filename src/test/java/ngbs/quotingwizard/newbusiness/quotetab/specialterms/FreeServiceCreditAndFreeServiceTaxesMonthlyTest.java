package ngbs.quotingwizard.newbusiness.quotetab.specialterms;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

@Tag("P0")
@Tag("PDV")
@Tag("Lambda")
@Tag("NGBS")
@Tag("QuoteTab")
public class FreeServiceCreditAndFreeServiceTaxesMonthlyTest extends BaseTest {
    private final Steps steps;
    private final SpecialTermsSteps specialTermsSteps;

    public FreeServiceCreditAndFreeServiceTaxesMonthlyTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_NoPhones.json",
                Dataset.class);
        steps = new Steps(data);
        specialTermsSteps = new SpecialTermsSteps(data);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-7546")
    @TmsLink("CRM-7559")
    @DisplayName("CRM-7546 - FSC for 'Monthly - Contract' plan with Special Terms. \n" +
            "CRM-7559 - 'Free Service Taxes' for 'Monthly - Contract' plan with Special Terms")
    @Description("CRM-7546 - Verify that 'Free Service Credit Amount' is shown for Customers with 'Monthly - Contract' service plan. \n" +
            "CRM-7559 - Verify 'Free Service Taxes' calculation for New Customers with 'Monthly - Contract' service plan " +
            "and 'Special Terms' set to 'N Free Month of Service'")
    public void test() {
        specialTermsSteps.freeServiceCreditAndFreeServiceTaxesTestSteps(steps.quoteWizard.opportunity.getId());
    }
}
