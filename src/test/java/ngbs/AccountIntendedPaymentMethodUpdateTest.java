package ngbs;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Approval__c;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Random;

import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getAccountInNGBS;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getPaymentMethodsFromNGBS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createDirectDebitApproval;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_APPROVED;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("P1")
@Tag("DirectDebit")
public class AccountIntendedPaymentMethodUpdateTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Approval__c ddApprovalRequest;

    //  Test data
    private final int monthlyCreditLimitOnDirectDebitApproval;

    public AccountIntendedPaymentMethodUpdateTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_EU_MVP_Monthly_NonContract_DirectDebitPM_82737013.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        monthlyCreditLimitOnDirectDebitApproval = new Random().nextInt(100_000);
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
        }

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);

        step("Create a new Direct Debit Approval request for the test Account via API", () -> {
            ddApprovalRequest = createDirectDebitApproval(steps.salesFlow.account, steps.quoteWizard.opportunity,
                    salesRepUser.getId());
        });

        step("Update Monthly_Credit_Limit__c on Direct Debit Approval via API", () -> {
            ddApprovalRequest.setMonthly_Credit_Limit__c((double) monthlyCreditLimitOnDirectDebitApproval);
            enterpriseConnectionUtils.update(ddApprovalRequest);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-23765")
    @DisplayName("CRM-23765 - Sending Intended Payment Method for Regular flow")
    @Description("Verify that Intended Payment Method Limits' value is updated " +
            "and Intended Payment Method isn't changed on the Account in NGBS " +
            "when DD Request Approval in SFDC (with non-default Monthly_Credit_Limit__c value) is approved")
    public void test() {
        step("1. Approve Direct Debit Request Approval via API", () -> {
            ddApprovalRequest.setStatus__c(APPROVAL_STATUS_APPROVED);
            enterpriseConnectionUtils.update(ddApprovalRequest);
        });

        step("2. Check that Payment Method on the account in NGBS remained unchanged (as Direct Debit)", () -> {
            var paymentMethodsFromNGBS = getPaymentMethodsFromNGBS(data.billingId);
            var directDebitPaymentMethod = paymentMethodsFromNGBS.stream()
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("There's no payment method on Account"));
            assertThat(directDebitPaymentMethod.directDebitInfo)
                    .as("'directDebitInfo' value on the Direct Debit payment method on NGBS Account")
                    .isNotNull();

            assertThat(directDebitPaymentMethod.defaultVal)
                    .as("'default' value on the Direct Debit payment method on NGBS Account")
                    .isTrue();
        });

        step("3. Check that Approved Package limits' value on the account in NGBS is updated " +
                "(with a value from DD Approval in SFDC)", () ->
                assertWithTimeout(() -> {
                    var accountDataInNGBS = getAccountInNGBS(data.billingId);
                    assertEquals(monthlyCreditLimitOnDirectDebitApproval,
                            accountDataInNGBS.packages[0].packageLimits[0].value,
                            "Customer Account's Approved Package limits' value in NGBS");
                }, ofSeconds(30))
        );
    }
}
