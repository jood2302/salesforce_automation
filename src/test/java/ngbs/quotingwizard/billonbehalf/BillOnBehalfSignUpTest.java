package ngbs.quotingwizard.billonbehalf;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.newbusiness.signup.SalesQuoteSignUpSteps;
import org.junit.jupiter.api.*;

import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.MVP_SERVICE;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getPaymentMethodsFromNGBS;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.searchAccountsByContactLastNameInNGBS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_APPROVED;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Tag("P0")
@Tag("Bill-on-Behalf")
public class BillOnBehalfSignUpTest extends BaseTest {
    private final Steps steps;
    private final SalesQuoteSignUpSteps salesQuoteSignUpSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product dlUnlimited;
    private final Product phoneToAdd;
    private final String renewalTerm;

    public BillOnBehalfSignUpTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Advanced_Monthly_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        salesQuoteSignUpSteps = new SalesQuoteSignUpSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataName("LC_HD_959");
        renewalTerm = data.packageFolders[0].packages[0].contractTerms.renewalTerm;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUserWithPermissionSet = salesQuoteSignUpSteps.getSalesRepUserWithAllowedProcessOrderWithoutShipping();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithPermissionSet);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithPermissionSet);

        steps.billOnBehalf.setUpBillOnBehalfSteps(steps.salesFlow.account, steps.salesFlow.contact,
                steps.quoteWizard.opportunity, salesRepUserWithPermissionSet);
        salesQuoteSignUpSteps.loginAsSalesRepUserWithAllowedProcessOrderWithoutShipping();
    }

    @Test
    @TmsLink("CRM-23977")
    @DisplayName("CRM-23977 - SignUp for Bill-on-behalf Accounts (not POC)")
    @Description("Verify that NGBS Partner ID and Payment Method are sent to the Funnel/NGBS " +
            "when user signs up the BoB Customer Account")
    public void test() {
        step("1. Open the test Opportunity, create a new Sales Quote from the Quote Wizard, " +
                "add phones, assign phones to DLs, and set up a quote to become an Active Agreement", () ->
                steps.billOnBehalf.prepareOpportunityToBeClosedAndSignedUp(steps.quoteWizard.opportunity, phoneToAdd,
                        dlUnlimited, renewalTerm)
        );

        step("2. Set status of 'Invoice-on-behalf Request' approval to 'Approved' via API", () -> {
            steps.billOnBehalf.invoiceOnBehalfApproval.setStatus__c(APPROVAL_STATUS_APPROVED);
            enterpriseConnectionUtils.update(steps.billOnBehalf.invoiceOnBehalfApproval);
        });

        step("3. Press 'Process Order' button on the Opportunity record page, " +
                "verify that 'Preparing Data' step is completed, select Timezone, " +
                "click 'Sign Up MVP', and check that the account is processed for signing up", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();

            opportunityPage.processOrderModal.selectDefaultTimezone();
            opportunityPage.processOrderModal.signUpButton.click();

            opportunityPage.processOrderModal.signUpMvpStatus
                    .shouldHave(exactTextCaseSensitive(format(YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE, MVP_SERVICE)), ofSeconds(60));
        });

        step("4. Check that account is created in NGBS after a sign up, " +
                "and check the customer's Partner ID in NGBS", () -> {
            var accountNgbsDTO = step("Check that the account is created in NGBS via NGBS API", () -> {
                return assertWithTimeout(() -> {
                    var accounts = searchAccountsByContactLastNameInNGBS(steps.salesFlow.contact.getLastName());
                    assertEquals(1, accounts.size(),
                            "Number of NGBS accounts found by the related Contact's Last Name");
                    return accounts.get(0);
                }, ofSeconds(90));
            });

            step("Check the customer's Partner ID (Partner Account.NGBS_Partner_ID__c) in NGBS", () ->
                    assertWithTimeout(() -> {
                        var paymentMethodsFromNGBS = getPaymentMethodsFromNGBS(accountNgbsDTO.id);

                        assertNotEquals(0, paymentMethodsFromNGBS.size(),
                                "Number of payment methods on the NGBS account");

                        var invoiceOnBehalfPaymentMethod = paymentMethodsFromNGBS.stream()
                                .filter(paymentMethodDTO -> paymentMethodDTO.invoiceOnBehalfInfo != null)
                                .findFirst()
                                .orElseThrow(() ->
                                        new AssertionError("No payment method with Invoice on Behalf info found on the NGBS account!"));

                        assertEquals(steps.billOnBehalf.ngbsPartner.id, invoiceOnBehalfPaymentMethod.invoiceOnBehalfInfo.partnerId,
                                "Customer's Partner ID (Partner Account.NGBS_Partner_ID__c) in NGBS");
                    }, ofSeconds(60))
            );
        });
    }
}
