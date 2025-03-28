package ngbs;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static com.aquiva.autotests.rc.model.ngbs.dto.account.PaymentMethodTypeDTO.CREDITCARD_PAYMENT_METHOD_TYPE;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getPaymentMethodTypeFromNGBS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.INVOICE_PAYMENT_METHOD;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("P0")
@Tag("NGBS")
@Tag("Approval")
public class AccountPaymentMethodTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    private User salesRepUser;

    public AccountPaymentMethodTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_163076013.json",
                Dataset.class);
        steps = new Steps(data);

        steps.ngbs.isGenerateAccountsForSingleTest = true;
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
        }

        salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
    }

    @Test
    @TmsLink("CRM-21908")
    @DisplayName("CRM-21908 - Account Payment Method is always synced")
    @Description("Check that the Account Payment Method is always synced with NGBS")
    public void test() {
        step("1. Check Office's Account payment method in NGBS", () -> {
            var officeAccountPaymentMethodType = getPaymentMethodTypeFromNGBS(data.billingId);
            assertThat(officeAccountPaymentMethodType.currentType)
                    .as("Account Payment Method in NGBS")
                    .isEqualTo(CREDITCARD_PAYMENT_METHOD_TYPE);
        });

        step("2. Create Invoice Request Approval for Office Account and set its status = 'Approved' via API", () -> {
            createInvoiceApprovalApproved(steps.quoteWizard.opportunity, steps.salesFlow.account, steps.salesFlow.contact,
                    salesRepUser.getId(), false);
        });

        step("3. Check Office's Account payment method in NGBS", () ->
                assertWithTimeout(() -> {
                    var officeAccountPaymentMethodType = getPaymentMethodTypeFromNGBS(data.billingId);
                    assertEquals(INVOICE_PAYMENT_METHOD, officeAccountPaymentMethodType.currentType,
                            "Account's Payment Method in NGBS");
                }, ofSeconds(60))
        );
    }
}
