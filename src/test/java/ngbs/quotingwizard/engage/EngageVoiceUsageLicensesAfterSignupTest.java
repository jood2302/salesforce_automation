package ngbs.quotingwizard.engage;

import base.BaseTest;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.newbusiness.signup.MultiProductSignUpSteps;
import org.junit.jupiter.api.*;

import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("Engage")
@Tag("SignUp")
public class EngageVoiceUsageLicensesAfterSignupTest extends BaseTest {
    private final EngageUsageLicensesAfterSignupSteps engageUsageLicensesAfterSignupSteps;
    private final MultiProductSignUpSteps multiProductSignUpSteps;

    public EngageVoiceUsageLicensesAfterSignupTest() {
        engageUsageLicensesAfterSignupSteps = new EngageUsageLicensesAfterSignupSteps(1);
        multiProductSignUpSteps = new MultiProductSignUpSteps();
    }

    @BeforeEach
    public void setUpTest() {
        engageUsageLicensesAfterSignupSteps.setUpTest();
    }

    @Test
    @TmsLink("CRM-37576")
    @DisplayName("CRM-37576 - EV - Usage licenses send to BAP with Monthly Service Plan (Sign Up)")
    @Description("Verify that with going through Sign Up for EV Account where Service Plan on Contract is Annual, " +
            "Usage licenses will be sent with Monthly Service plan")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity, " +
                "create a new Sales Quote with MVP and Engage Voice packages, " +
                "update it to the Active Agreement, prepare it for signing up its Engage Voice service, " +
                "and open the Process Order modal for the Opportunity", () -> {
            engageUsageLicensesAfterSignupSteps.prepareMultiproductQuoteWithEngageForSignup();
        });

        step("2. Expand the Engage Voice service service's section, " +
                "sign up 'Engage Voice' service via the Process Order modal window, " +
                "and check that the Engage Voice package is added to the account in NGBS", () -> {
            multiProductSignUpSteps.signUpEngageVoiceServiceStep(engageUsageLicensesAfterSignupSteps.officeAccountBillingId);
        });

        step("3. Check Service plan values (Billing Cycle Duration) " +
                "for Recurring and Usage licenses in NGBS for Engage Voice Service", () ->
                engageUsageLicensesAfterSignupSteps.checkEngageLicensesAfterSignup(engageUsageLicensesAfterSignupSteps.engageServiceName)
        );
    }
}
