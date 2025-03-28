package ngbs.opportunitycreation.existingbusiness.businessidentities;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

@Tag("P1")
@Tag("QOP")
public class RcExistingBusinessBiQopTest extends BaseTest {
    private final Steps steps;
    private final ExistingBusinessBiQopSteps existingBusinessBiQopSteps;

    //  Test data
    private final String expectedBusinessIdentityValue;

    public RcExistingBusinessBiQopTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/existingbusiness/RC_MVP_Monthly_NonContract_QOP_163067013.json",
                Dataset.class);

        steps = new Steps(data);
        existingBusinessBiQopSteps = new ExistingBusinessBiQopSteps();

        expectedBusinessIdentityValue = data.getBusinessIdentityName();
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-22456")
    @DisplayName("CRM-22456 - Business Identities for Existing Business Accounts. " +
            "Brand: 'RingCentral', BI: 'RingCentral Inc.'")
    @Description("Verify that Business Identity picklist on QOP has the same value as Account in Billing")
    public void test() {
        existingBusinessBiQopSteps.openQopAndCheckPreselectedBiTestSteps(steps.salesFlow.account.getId(),
                expectedBusinessIdentityValue);
    }
}
