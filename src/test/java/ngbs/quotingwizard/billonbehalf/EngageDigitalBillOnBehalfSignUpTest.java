package ngbs.quotingwizard.billonbehalf;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

@Tag("P1")
@Tag("Bill-on-Behalf")
@Tag("Engage")
public class EngageDigitalBillOnBehalfSignUpTest extends BaseTest {
    private final Steps steps;
    private final EngageWithoutOfficeBillOnBehalfSteps engageWithoutOfficeBillOnBehalfSteps;

    public EngageDigitalBillOnBehalfSignUpTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_ED_Standalone_Annual_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        engageWithoutOfficeBillOnBehalfSteps = new EngageWithoutOfficeBillOnBehalfSteps(data);
    }

    @BeforeEach
    public void setUpTest() {
        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);

        steps.billOnBehalf.setUpBillOnBehalfSteps(steps.salesFlow.account, steps.salesFlow.contact,
                steps.quoteWizard.opportunity, dealDeskUser);

        engageWithoutOfficeBillOnBehalfSteps.setUpBaseEngageWithoutOfficeBillOnBehalfTest(steps.salesFlow.account,
                steps.billOnBehalf.invoiceOnBehalfApproval);

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-25700")
    @DisplayName("CRM-25700 - Sign Up new business ED no master (Bill-on-Behalf)")
    @Description("Verify that Engage Digital BoB opportunity can be signed up. Account is not linked to the master account in this case")
    public void test() {
        engageWithoutOfficeBillOnBehalfSteps.checkEngageBillOnBehalfSignUpTestSteps(null,
                steps.quoteWizard.opportunity);
    }
}
