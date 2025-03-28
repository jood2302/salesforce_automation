package ngbs.quotingwizard.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Approval__c;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.cartPage;
import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage.MONTHLY_CREDIT_LIMIT_EXCEEDED_ERROR;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage.MONTHLY_CREDIT_LIMIT_EXCEEDED;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createDirectDebitApproval;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("DirectDebit")
public class DirectDebitCloseValidationTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Approval__c ddApprovalRequest;

    //  Test data
    private final Product digitalLineUnlimited;
    private final Double newDlPrice;

    public DirectDebitCloseValidationTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_EU_MVP_Monthly_NonContract_DirectDebitPM_82738013.json",
                Dataset.class);
        
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        newDlPrice = digitalLineUnlimited.priceRater[0].raterPrice.doubleValue();
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);

        step("Create a new Direct Debit Approval request for the test Account via API", () -> {
            ddApprovalRequest = createDirectDebitApproval(steps.salesFlow.account, steps.quoteWizard.opportunity, salesRepUser.getId());
        });

        step("Submit the Direct Debit Approval record for approval, and approve it via API", () -> {
            enterpriseConnectionUtils.submitRecordForApproval(ddApprovalRequest.getId());

            //  Number of necessary approvals according to DD Approval Process
            var maxDirectDebitApprovalLevel = 4;

            for (int i = 0; i < maxDirectDebitApprovalLevel; i++) {
                enterpriseConnectionUtils.approveSingleRecord(ddApprovalRequest.getId());
            }
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-24108")
    @DisplayName("CRM-24108 - Ex. Business. Opportunity can't be closed with Direct Debit PM and Approval Limits violated. Monthly")
    @Description("Verify that Opportunity can't be closed with Direct Debit PM and Direct Debit Approval Limits violated")
    public void test() {
        step("1. Open the Opportunity record page, switch to the Quote Wizard, " +
                "add a new Sales Quote, and keep the same preselected package for it", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        step("2. Open the Price tab and increase quantity of DLs to make totals " +
                "(Total_MRR_with_Taxes_c + Total_One_Time_with_Taxes__c) " +
                "just above Monthly Credit Limit, and save changes", () -> {
            cartPage.openTab();

            var directDebitMonthlyCreditLimitValue = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, Monthly_Credit_Limit__c " +
                                    "FROM Approval__c " +
                                    "WHERE Id = '" + ddApprovalRequest.getId() + "'",
                            Approval__c.class)
                    .getMonthly_Credit_Limit__c();
            assertThat(directDebitMonthlyCreditLimitValue)
                    .as("Direct Debit Approval__c.Monthly_Credit_Limit__c value")
                    .isNotNull();

            var dlQuantityAboveLimit = (int) Math.ceil(directDebitMonthlyCreditLimitValue / newDlPrice) +
                    digitalLineUnlimited.existingQuantity;
            cartPage.setNewQuantityForQLItem(digitalLineUnlimited.name, dlQuantityAboveLimit);
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

        step("3. Check that that notification about required Direct Debit Approval appeared in the Quote Wizard", () -> {
            //  to make all notifications in QW displayed
            cartPage.notificationBar.click();
            cartPage.notifications.shouldHave(itemWithText(MONTHLY_CREDIT_LIMIT_EXCEEDED));
        });

        step("4. Click 'Close' button on the Opportunity record page " +
                "and check that error notification about exceeded Monthly credit limit is appeared", () -> {
            opportunityPage.clickCloseButton();

            opportunityPage.alertNotificationBlock.shouldBe(visible, ofSeconds(40));
            opportunityPage.notifications.shouldHave(itemWithText(MONTHLY_CREDIT_LIMIT_EXCEEDED_ERROR), ofSeconds(1));
        });
    }
}
