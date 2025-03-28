package ngbs.quotingwizard.newbusiness.quotetab.specialterms;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

@Tag("P0")
@Tag("Lambda")
@Tag("NGBS")
@Tag("QuoteTab")
public class FreeServiceCreditAndFreeServiceTaxesAnnualTest extends BaseTest {
    private final Steps steps;
    private final SpecialTermsSteps specialTermsSteps;

    public FreeServiceCreditAndFreeServiceTaxesAnnualTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_AllTypesOfDLs.json",
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
    @TmsLink("CRM-7472")
    @TmsLink("CRM-7547")
    @DisplayName("CRM-7472 - FSC for 'Annual' plan with Special Terms. \n" +
            "CRM-7547 - 'Free Service Taxes' for 'Annual' plan with Special Terms")
    @Description("CRM-7472 - Verify that 'Free Service Credit Amount' is shown for Customers with 'Annual' service plan. \n" +
            "CRM-7547 - Verify 'Free Service Taxes' calculation for New Customers with 'Annual' service plan " +
            "and 'Special Terms' set to 'N Free Month of Service'")
    public void test() {
        specialTermsSteps.freeServiceCreditAndFreeServiceTaxesTestSteps(steps.quoteWizard.opportunity.getId());
    }
}
