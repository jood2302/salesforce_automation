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
public class EngageDigitalUsageLicensesAfterSignupTest extends BaseTest {
    private final EngageUsageLicensesAfterSignupSteps engageUsageLicensesAfterSignupSteps;
    private final MultiProductSignUpSteps multiProductSignUpSteps;

    public EngageDigitalUsageLicensesAfterSignupTest() {
        engageUsageLicensesAfterSignupSteps = new EngageUsageLicensesAfterSignupSteps(2);
        multiProductSignUpSteps = new MultiProductSignUpSteps();
    }

    @BeforeEach
    public void setUpTest() {
        engageUsageLicensesAfterSignupSteps.setUpTest();
    }

    @Test
    @TmsLink("CRM-37578")
    @DisplayName("CRM-37578 - ED - Usage licenses send to BAP with Monthly Service Plan (Sign Up)")
    @Description("Verify that with going through Sign Up for ED Account where Service Plan on Contract is Annual, " +
            "Usage licenses will be sent with Monthly Service plan")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity, " +
                "create a new Sales Quote with MVP and Engage Digital packages, " +
                "update it to the Active Agreement, prepare it for signing up its Engage Digital service, " +
                "and open the Process Order modal for the Opportunity", () -> {
            engageUsageLicensesAfterSignupSteps.prepareMultiproductQuoteWithEngageForSignup();
        });

        step("2. Expand the Engage Digital service service's section, " +
                "sign up 'Engage Digital' service via the Process Order modal window, " +
                "and check that the Engage Digital package is added to the account in NGBS", () -> {
            multiProductSignUpSteps.signUpEngageDigitalServiceStep(engageUsageLicensesAfterSignupSteps.officeAccountBillingId);
        });

        step("3. Check Service plan values (Billing Cycle Duration) " +
                "for Recurring and Usage licenses in NGBS for Engage Digital Service", () ->
                engageUsageLicensesAfterSignupSteps.checkEngageLicensesAfterSignup(engageUsageLicensesAfterSignupSteps.engageServiceName)
        );
    }
}
