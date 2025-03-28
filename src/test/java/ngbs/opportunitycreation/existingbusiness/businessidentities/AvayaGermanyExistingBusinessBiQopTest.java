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
@Tag("Avaya")
public class AvayaGermanyExistingBusinessBiQopTest extends BaseTest {
    private final Steps steps;
    private final ExistingBusinessBiQopSteps existingBusinessBiQopSteps;

    //  Test data
    private final String expectedBusinessIdentityValue;

    public AvayaGermanyExistingBusinessBiQopTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/existingbusiness/Avaya_Office_Monthly_NonContract_QOP_GermanBI_201986013.json",
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
            "Brand: 'Avaya Cloud Office', BI: 'Avaya Cloud Office DE'")
    @Description("Verify that Business Identity picklist on QOP has the same value as Account in Billing")
    public void test() {
        existingBusinessBiQopSteps.openQopAndCheckPreselectedBiTestSteps(steps.salesFlow.account.getId(),
                expectedBusinessIdentityValue);
    }
}
