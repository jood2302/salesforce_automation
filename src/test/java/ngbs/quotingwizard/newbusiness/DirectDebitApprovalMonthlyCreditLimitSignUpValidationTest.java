package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.DIRECT_DEBIT_MONTHLY_CREDIT_LIMIT_EXCEEDED_ERROR;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.MVP_SERVICE;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage.YOU_DO_NOT_HAVE_APPROVED_DIRECT_DEBIT_REQUEST_APPROVAL;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.DIRECT_DEBIT_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createDirectDebitApproval;
import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("DirectDebit")
public class DirectDebitApprovalMonthlyCreditLimitSignUpValidationTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUser;
    private Approval__c ddApprovalRequest;

    //  Test data
    private final Product digitalLineUnlimited;
    private final Double newDlPrice;
    private final AreaCode euAreaCode;

    public DirectDebitApprovalMonthlyCreditLimitSignUpValidationTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_EU_MVP_Monthly_Contract_RegularAndPOC.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        newDlPrice = digitalLineUnlimited.priceRater[0].raterPrice.doubleValue();
        euAreaCode = new AreaCode("Local", "France", EMPTY_STRING, EMPTY_STRING, "1");

        //  New Quantity to exceed the Monthly Recurring Charges above 10,000 USD (DD Approval threshold)
        digitalLineUnlimited.quantity = (int) Math.ceil(10_000 / newDlPrice);
    }

    @BeforeEach
    public void setUpTest() {
        salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-23958")
    @DisplayName("CRM-23958 - Opportunity can't proceed to Sign Up with Direct Debit PM and Package Limits violated. Monthly")
    @Description("Verify that Opportunity can't proceed to Sign Up with Direct Debit PM and Threshold Limits violated")
    public void test() {
        step("1. Open the Opportunity record page, switch to the Quote Wizard, " +
                "add a new sales quote, select a package for it and save changes", () ->
                steps.quoteWizard.prepareOpportunity(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Quote Details tab and select the Direct Debit payment method", () -> {
            quotePage.openTab();
            quotePage.selectPaymentMethod(DIRECT_DEBIT_PAYMENT_METHOD);
        });

        step("3. Open the Price tab, check that there are no displayed notifications in the Notification bar, " +
                "and increase the quantity for DL Unlimited so that Quote's MRR exceeds DD Approval threshold", () -> {
            cartPage.openTab();
            cartPage.notificationBar.shouldBe(hidden);
            cartPage.notifications.shouldBe(empty);

            cartPage.setQuantityForQLItem(digitalLineUnlimited.name, digitalLineUnlimited.quantity);
        });

        step("4. Open the Quote Details tab, set Main Area Code and Start Date, save changes, " +
                "open the Price tab and check that the notification about required Direct Debit Approval is displayed", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(euAreaCode);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();

            cartPage.openTab();
            //  to make all notifications displayed
            cartPage.notificationBar.click();
            cartPage.notifications.shouldHave(itemWithText(YOU_DO_NOT_HAVE_APPROVED_DIRECT_DEBIT_REQUEST_APPROVAL));
        });

        step("5. Create a new Direct Debit Approval request for the test Account via API", () -> {
            ddApprovalRequest = createDirectDebitApproval(steps.salesFlow.account, steps.quoteWizard.opportunity, salesRepUser.getId());
        });

        step("6. Increase the quantity of DLs so that the totals (Total_MRR_with_Taxes__c + Total_One_Time_with_Taxes__c) " +
                "are just above the Monthly Credit Limit on Direct Debit Approval and save changes", () -> {
            var directDebitMonthlyCreditLimitValue = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, Monthly_Credit_Limit__c " +
                                    "FROM Approval__c " +
                                    "WHERE Id = '" + ddApprovalRequest.getId() + "'",
                            Approval__c.class)
                    .getMonthly_Credit_Limit__c();
            assertThat(directDebitMonthlyCreditLimitValue)
                    .as("Direct Debit Approval__c.Monthly_Credit_Limit__c value")
                    .isNotNull();

            //  New Quantity to exceed the Monthly Recurring Charges above the Monthly Credit Limit of DD Approval
            var dlQuantityAboveLimit = (int) Math.ceil(directDebitMonthlyCreditLimitValue / newDlPrice);

            cartPage.setQuantityForQLItem(digitalLineUnlimited.name, dlQuantityAboveLimit);
            cartPage.saveChanges();

            //  make sure that the totals exceed DD Monthly Credit Limit
            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Total_MRR_with_Taxes__c, Total_One_Time_with_Taxes__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);
            assertThat(List.of(quote.getTotal_MRR_with_Taxes__c(), quote.getTotal_One_Time_with_Taxes__c()))
                    .as("Quote's Total_MRR_with_Taxes__c and Total_One_Time_with_Taxes__c values")
                    .doesNotContainNull();

            var totalsSum = quote.getTotal_MRR_with_Taxes__c() + quote.getTotal_One_Time_with_Taxes__c();
            assertThat(totalsSum)
                    .as("Sum of Quote's Total_MRR_with_Taxes__c and Total_One_Time_with_Taxes__c")
                    .isGreaterThan(directDebitMonthlyCreditLimitValue);
        });

        step("7. Update the Quote to Active Agreement via API", () ->
                steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity)
        );

        step("8. Click 'Process Order' button on the Opportunity record page, " +
                "check that the error message is shown in the Process Order modal window", () -> {
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications.shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    format(DIRECT_DEBIT_MONTHLY_CREDIT_LIMIT_EXCEEDED_ERROR, MVP_SERVICE)));
        });
    }
}
