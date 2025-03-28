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
public class PartnerLeadExistingAccountLeadConvertTest extends BaseTest {
    private final Steps steps;
    private final RcNewBusinessPartnerLeadConvertSteps rcNewBusinessPartnerLeadConvertSteps;

    public PartnerLeadExistingAccountLeadConvertTest() {
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
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.leadConvert.preparePartnerLeadTestSteps(steps.salesFlow.account, steps.salesFlow.contact);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20790")
    @DisplayName("CRM-20790 - Lead Conversion - Partner Lead - RingCentral - Existing Account.")
    @Description("Verify that a Partner Lead with a RingCentral brand can be converted with an existing account.")
    public void test() {
        rcNewBusinessPartnerLeadConvertSteps.newBusinessPartnerLeadConvertTestSteps(steps.leadConvert.partnerLead, steps.salesFlow.account);
    }
}
