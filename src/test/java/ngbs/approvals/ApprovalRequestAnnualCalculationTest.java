package ngbs.approvals;

import base.BaseTest;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static base.Pages.invoiceApprovalCreationModal;
import static com.codeborne.selenide.Condition.exactValue;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;

@Tag("P0")
@Tag("Invoice_Approval")
@Tag("QTC-527")
@Tag("FVT")
public class ApprovalRequestAnnualCalculationTest extends BaseTest {
    private final ApprovalRequestSignUpLimitCalculationSteps approvalSignUpLimitCalculationSteps;

    public ApprovalRequestAnnualCalculationTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_PhonesAndDLs.json",
                Dataset.class);

        approvalSignUpLimitCalculationSteps = new ApprovalRequestSignUpLimitCalculationSteps(data);
    }

    @BeforeEach
    public void setUpTest() {
        approvalSignUpLimitCalculationSteps.setUpSteps();
    }

    @Test
    @TmsLink("CRM-27026")
    @TmsLink("CRM-27046")
    @DisplayName("CRM-27026 - Validate 'Sign-Up Purchase Limit' for Annual Payment \n" +
            "CRM-27046 - Validate 'Monthly Credit Limit' field value")
    @Description("CRM-27026 - Validate 'Sign-Up Purchase Limit' for Annual Payment. \n" +
            "CRM-27046 - Validate 'Monthly Credit Limit' field value in invoice request")
    public void test() {
        //  CRM-27026
        step("1. Open the Opportunity, open the modal to create 'Invoicing Request' Approval from it, " +
                "and validate Sign-Up Purchase Limit's value on the modal", () -> {
            approvalSignUpLimitCalculationSteps.validateApprovalSignUpPurchaseLimitTestSteps();
        });

        //  CRM-27046
        step("2. Validate that Monthly Credit Limit is auto-populated as per calculation", () -> {
            var quoteTotalARR = approvalSignUpLimitCalculationSteps.quote.getTotal_ARR__c();
            var monthlyCreditLimitCalculated = BigDecimal.valueOf(quoteTotalARR)
                    .divide(BigDecimal.valueOf(12))
                    .multiply(BigDecimal.valueOf(1.5));
            var monthlyCreditLimitExpectedValue = format("%,.2f", monthlyCreditLimitCalculated);

            invoiceApprovalCreationModal.monthlyCreditLimitInput.shouldHave(exactValue(monthlyCreditLimitExpectedValue));
        });
    }
}
