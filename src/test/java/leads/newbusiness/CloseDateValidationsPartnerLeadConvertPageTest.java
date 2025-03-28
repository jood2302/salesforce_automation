package leads.newbusiness;

import base.BaseTest;
import base.Steps;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

@Tag("P1")
@Tag("LeadConvert")
@Tag("LTR-732")
public class CloseDateValidationsPartnerLeadConvertPageTest extends BaseTest {
    private final Steps steps;
    private final CloseDateValidationsLeadConvertPageSteps closeDateValidationsLeadConvertPageSteps;

    public CloseDateValidationsPartnerLeadConvertPageTest() { 
        closeDateValidationsLeadConvertPageSteps = new CloseDateValidationsLeadConvertPageSteps();
        steps = new Steps(closeDateValidationsLeadConvertPageSteps.data);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createPartnerAccountAndLead(salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @Tag("KnownIssue")
    @Issue("PBC-25078")
    @TmsLink("CRM-37318")
    @DisplayName("CRM-37318 - Close Date validations on LCP (Partner Lead)")
    @Description("Verify that Close Date field has validations on Partner Lead Convert Page")
    public void test() {
        closeDateValidationsLeadConvertPageSteps.checkCloseDateValidationsOnLeadConvertPageTestSteps(steps.leadConvert.partnerLead);
    }
}
