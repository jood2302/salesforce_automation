package leads.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

@Tag("P1")
@Tag("LeadConvert")
public class PartnerLeadNewAccountLeadConvertFlowTest extends BaseTest {
    private final Steps steps;
    private final RcNewBusinessPartnerLeadConvertSteps rcNewBusinessPartnerLeadConvertSteps;

    public PartnerLeadNewAccountLeadConvertFlowTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        rcNewBusinessPartnerLeadConvertSteps = new RcNewBusinessPartnerLeadConvertSteps(data);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createPartnerAccountAndLead(salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-10813")
    @TmsLink("CRM-20789")
    @DisplayName("CRM-10813 - New Business. Partner Leads get converted with new opportunity creation. \n" +
            "CRM-20789 - Lead Conversion - Partner Lead - RingCentral - New Account.")
    @Description("CRM-10813 - Verify that Partner Leads get converted, with Create New Opportunity checkbox disabled. \n" +
            "CRM-20789 - Verify that a Partner Lead with a RingCentral brand can be converted with a new account.")
    public void test() {
        rcNewBusinessPartnerLeadConvertSteps.newBusinessPartnerLeadConvertTestSteps(steps.leadConvert.partnerLead, null);
    }
}