package ngbs.approvals;

import base.BaseTest;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("Invoice_Approval")
@Tag("QTC-527")
@Tag("FVT")
public class ApprovalRequestMonthlyCalculationTest extends BaseTest {
    private final ApprovalRequestSignUpLimitCalculationSteps approvalSignUpLimitCalculationSteps;

    public ApprovalRequestMonthlyCalculationTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_NonContract_1TypeOfDL.json",
                Dataset.class);

        approvalSignUpLimitCalculationSteps = new ApprovalRequestSignUpLimitCalculationSteps(data);
    }

    @BeforeEach
    public void setUpTest() {
        approvalSignUpLimitCalculationSteps.setUpSteps();
    }

    @Test
    @TmsLink("CRM-27027")
    @DisplayName("CRM-27027 - Validate 'Sign-Up Purchase Limit' for Monthly payment")
    @Description("Validate 'Sign-Up Purchase Limit' for Monthly payment")
    public void test() {
        step("1. Open the Opportunity, open the modal to create 'Invoicing Request' Approval from it, " +
                "and validate Sign-Up Purchase Limit's value on the modal", () ->
                approvalSignUpLimitCalculationSteps.validateApprovalSignUpPurchaseLimitTestSteps()
        );
    }
}
