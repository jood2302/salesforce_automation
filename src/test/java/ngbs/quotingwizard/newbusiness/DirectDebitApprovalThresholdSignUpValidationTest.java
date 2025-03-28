package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.DirectDebitApprovalThreshold__c;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.DIRECT_DEBIT_THRESHOLD_LIMIT_EXCEEDED_ERROR;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.MVP_SERVICE;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage.YOU_DO_NOT_HAVE_APPROVED_DIRECT_DEBIT_REQUEST_APPROVAL;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.DIRECT_DEBIT_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("DirectDebit")
public class DirectDebitApprovalThresholdSignUpValidationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product digitalLineUnlimited;
    private final Double newDlPrice;
    private final AreaCode euAreaCode;

    public DirectDebitApprovalThresholdSignUpValidationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_EU_MVP_Annual_Contract_Regular.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        newDlPrice = digitalLineUnlimited.priceRater[0].raterPrice.doubleValue();
        euAreaCode = new AreaCode("Local", "France", EMPTY_STRING, EMPTY_STRING, "1");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-24830")
    @DisplayName("CRM-24830 - Opportunity can't proceed to Sign Up with Direct Debit PM and Threshold Limits violated. Annual")
    @Description("Verify that Opportunity can't proceed to Sign Up with Direct Debit PM and Threshold Limits violated")
    public void test() {
        step("1. Open the Opportunity record page, switch to the Quote Wizard, " +
                "add a new Sales Quote, and select a package for it", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        step("2. Open the Price tab and increase the quantity of DLs so that Quote.Total_MRR_with_Taxes__c value " +
                "is just above the DirectDebitApprovalThreshold__c value for the brand, and save changes", () -> {
            cartPage.openTab();

            var directDebitThresholdValue = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, ThresholdValue__c " +
                                    "FROM DirectDebitApprovalThreshold__c " +
                                    "WHERE Brand__c = '" + data.brandName + "'",
                            DirectDebitApprovalThreshold__c.class)
                    .getThresholdValue__c();
            assertThat(directDebitThresholdValue)
                    .as("DirectDebitApprovalThreshold__c.ThresholdValue__c value")
                    .isNotNull();

            var dlQuantityAboveThreshold = (int) Math.ceil(directDebitThresholdValue / newDlPrice);
            cartPage.setQuantityForQLItem(digitalLineUnlimited.name, dlQuantityAboveThreshold);
            cartPage.saveChanges();

            //  make sure that the totals exceed DD Approval threshold
            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Total_ARR_with_Taxes__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);
            assertThat(quote.getTotal_ARR_with_Taxes__c())
                    .as("Quote.Total_ARR_with_Taxes__c value")
                    .isGreaterThan(directDebitThresholdValue);
        });

        step("3. Open the Quote Details tab, set Payment Method = 'Direct Debit', " +
                "set Main Area Code and Start Date, save changes, " +
                "and check that notification about required Direct Debit Approval appeared in the Quote Wizard", () -> {
            quotePage.openTab();
            quotePage.selectPaymentMethod(DIRECT_DEBIT_PAYMENT_METHOD);
            quotePage.setMainAreaCode(euAreaCode);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();

            cartPage.openTab();
            //  to make all notifications displayed
            cartPage.notificationBar.click();
            cartPage.notifications.shouldHave(itemWithText(YOU_DO_NOT_HAVE_APPROVED_DIRECT_DEBIT_REQUEST_APPROVAL));
        });

        step("4. Update the Quote to Active Agreement via API", () ->
                steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity)
        );

        step("5. Click 'Process Order' button on the Opportunity record page, " +
                "check that the error message is shown in the Process Order modal window", () -> {
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications.shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    format(DIRECT_DEBIT_THRESHOLD_LIMIT_EXCEEDED_ERROR, MVP_SERVICE)));
        });
    }
}
