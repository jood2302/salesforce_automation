package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

@Tag("P0")
@Tag("Atos/UnifyOffice")
@Tag("Lambda")
@Tag("SignUp")
public class UnifyOfficePocSignUpTest extends BaseTest {
    private final Steps steps;
    private final NoErrorPocSignupSteps noErrorPocSignupSteps;

    public UnifyOfficePocSignUpTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/Unify_Office_Monthly_RegularAndPOC.json",
                Dataset.class);

        steps = new Steps(data);
        noErrorPocSignupSteps = new NoErrorPocSignupSteps(data);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-21434")
    @DisplayName("CRM-21434 - Skip Completed Envelope Check for Unify Office POC Sign Up")
    @Description("Verify that Sign Up POC Quote for Unify Office brands doesn't require completed envelope")
    public void test() {
        noErrorPocSignupSteps.noErrorsPocSignUpTestSteps(steps.quoteWizard.opportunity.getId());
    }
}
