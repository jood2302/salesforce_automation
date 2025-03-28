package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.opportunityPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.model.ngbs.dto.account.AccountFieldsDTO.CREDITCARD_INTENDED_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.CREDIT_CARD_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getIntendedPaymentMethodFromNGBS;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.searchAccountsByContactLastNameInNGBS;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.closeWindow;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
@Tag("SignUp")
public class SalesQuoteSignUpTest extends BaseTest {
    private final SalesQuoteSignUpSteps salesQuoteSignUpSteps;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Contact contact;
    private Opportunity opportunity;

    //  Test data
    private final String brandName;

    public SalesQuoteSignUpTest() {
        salesQuoteSignUpSteps = new SalesQuoteSignUpSteps();
        var data = salesQuoteSignUpSteps.data;
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        brandName = data.brandName;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUserWithPermissionSet = salesQuoteSignUpSteps.getSalesRepUserWithAllowedProcessOrderWithoutShipping();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithPermissionSet);
        contact = steps.salesFlow.contact;
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, contact, salesRepUserWithPermissionSet);
        opportunity = steps.quoteWizard.opportunity;

        salesQuoteSignUpSteps.loginAsSalesRepUserWithAllowedProcessOrderWithoutShipping();
    }

    @Test
    @TmsLink("CRM-22993")
    @TmsLink("CRM-22986")
    @DisplayName("CRM-22993 - Validation for Sign Up with a None payment method. RC US. New Business. \n" +
            "CRM-22986 - Sign Up with a Credit Card payment method. New Business")
    @Description("CRM-22993 - Verify that validation is correctly triggered if the payment method sent as None. \n" +
            "CRM-22986 - Verify that PaymentMethod__c field is correctly sent to the Funnel if payment method is Credit Card")
    public void test() {
        step("1. Check 'Is Quoting Available (New Business)' value in NGBS Brand Mapping Custom Setting " +
                "for 'RingCentral' brand", () -> {
            var ngbsBrandMappingCustomSetting = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT IsQuotingAvailableNewBusiness__c " +
                            "FROM LeadConversionConfiguration__c " +
                            "WHERE Name = '" + brandName + "'",
                    LeadConversionConfiguration__c.class);
            assertThat(ngbsBrandMappingCustomSetting.getIsQuotingAvailableNewBusiness__c())
                    .as("LeadConversionConfiguration__c.IsQuotingAvailableNewBusiness__c value")
                    .isTrue();
        });

        step("2. Open the test Opportunity, switch to the Quote Wizard, add a new Sales quote, " +
                "add some Products, assign devices, and save changes", () ->
                salesQuoteSignUpSteps.prepareSalesQuoteWithAssignedDevicesSteps(opportunity.getId())
        );

        step("3. Open the Quote Details tab, populate Main Area Code and Start Date, save changes " +
                "and set current quote to Active Agreement via API", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();

            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(opportunity);
        });

        //  CRM-22993
        step("4. Press 'Process Order' button on the Opportunity's record page, " +
                "check that error message is shown on the 'Preparing Data - Data validation' step in the Process Order modal window, " +
                "and close the modal window", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications
                    .shouldHave(exactTexts(format(ACCOUNT_SHOULD_HAVE_PAYMENT_METHOD_ERROR, MVP_SERVICE)), ofSeconds(1));
            opportunityPage.processOrderModal.mvpPreparingDataActiveStep.shouldHave(exactTextCaseSensitive(DATA_VALIDATION_STEP));

            opportunityPage.processOrderModal.closeWindow();
        });

        step("5. Populate 'Payment Method' field on the Quote Details tab, save changes, " +
                "then set the Quote to Active Agreement via API", () -> {
            switchTo().window(1);
            quotePage.selectPaymentMethod(CREDIT_CARD_PAYMENT_METHOD);
            quotePage.saveChanges();

            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(opportunity);
            closeWindow();
        });

        step("6. Press 'Process Order' button on the Opportunity's record page, " +
                "verify that 'Preparing Data' step is completed, " +
                "select the timezone, click 'Sign Up MVP', and check that the account is processed for signing up", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();

            opportunityPage.processOrderModal.selectDefaultTimezone();
            opportunityPage.processOrderModal.signUpButton.click();

            opportunityPage.processOrderModal.signUpMvpStatus
                    .shouldHave(exactTextCaseSensitive(format(YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE, MVP_SERVICE)), ofSeconds(60));
        });

        //  CRM-22986
        step("7. Check that the Payment Method was transmitted to NGBS correctly", () -> {
            //  polling is used here because the created NGBS account might be obtained a bit later
            var accountNgbsDTO = step("Check that the account is created in NGBS via NGBS API", () -> {
                return assertWithTimeout(() -> {
                    var accounts = searchAccountsByContactLastNameInNGBS(contact.getLastName());
                    assertEquals(1, accounts.size(),
                            "Number of NGBS accounts found by the related Contact's Last Name");
                    return accounts.get(0);
                }, ofSeconds(90));
            });

            //  polling is used here because initially payment method info might not be correct or available right away
            step("Check that there's 'Credit Card' intended payment method on the NGBS account", () -> {
                assertWithTimeout(() -> {
                    var paymentMethods = getIntendedPaymentMethodFromNGBS(accountNgbsDTO.id);
                    assertEquals(CREDITCARD_INTENDED_PAYMENT_METHOD, paymentMethods,
                            "Intended payment method on the NGBS account");
                }, ofSeconds(20));
            });
        });
    }
}
