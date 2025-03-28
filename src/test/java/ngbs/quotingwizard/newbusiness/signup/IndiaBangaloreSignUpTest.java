package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.RC_INDIA_BANGALORE_BUSINESS_IDENTITY_ID;

@Tag("P0")
@Tag("NGBS")
@Tag("SignUp")
@Tag("IndiaMVP")
public class IndiaBangaloreSignUpTest extends BaseTest {
    private final IndiaSignUpSteps indiaSignUpSteps;

    //  Test data
    private final String indiaBusinessIdentityId;

    public IndiaBangaloreSignUpTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_Contract_163077013_RC_India_NB.json",
                Dataset.class);
        indiaSignUpSteps = new IndiaSignUpSteps(data);

        indiaBusinessIdentityId = RC_INDIA_BANGALORE_BUSINESS_IDENTITY_ID;
    }

    @BeforeEach
    public void setUpTest() {
        indiaSignUpSteps.setUpIndiaSignUpTest(indiaBusinessIdentityId);
    }

    @Test
    @TmsLink("CRM-24797")
    @DisplayName("CRM-24797 - Sign Up India (Bangalore) opportunity")
    @Description("Verify that Indian (Bangalore) opportunity can be signed up and proper data is transferred to the funnel")
    public void test() {
        indiaSignUpSteps.indiaSignUpMainTestSteps(indiaBusinessIdentityId);
    }
}
