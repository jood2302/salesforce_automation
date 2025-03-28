package ngbs.quotingwizard.engage;

import base.BaseTest;
import base.Steps;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.OpportunityRecordPage.MASTER_ACCOUNT_IS_NOT_PAID_ERROR;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.INVOICE_PAYMENT_METHOD;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.closeWindow;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("Engage")
@Tag("OpportunityClose")
public class EngageCloseOppMasterNotSignedUpTest extends BaseTest {
    private final LinkedAccountsNewBusinessSteps linkedAccountsNewBusinessSteps;
    private final Steps steps;

    private String opportunityId;

    public EngageCloseOppMasterNotSignedUpTest() {
        linkedAccountsNewBusinessSteps = new LinkedAccountsNewBusinessSteps();

        var data = linkedAccountsNewBusinessSteps.data;
        steps = new Steps(data);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);

        opportunityId = steps.quoteWizard.opportunity.getId();

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);

        linkedAccountsNewBusinessSteps.setUpLinkedAccountsNewBusinessSteps(
                steps.salesFlow.account, steps.salesFlow.contact, steps.quoteWizard.opportunity,
                salesRepUser, steps.quoteWizard.localAreaCode
        );
    }

    @Test
    @TmsLink("CRM-20016")
    @DisplayName("CRM-20016 - Master Account is not Signed Up")
    @Description("Verify that if Master (Office) Account is not Signed Up then with click to Close button will be shown Error banner")
    public void test() {
        step("1. Open Engage Opportunity record page, switch to the Quote Wizard, add a new Sales Quote, " +
                "and select a package for it", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(opportunityId);
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        step("2. Open the Price tab, set the quantity for the Engage product, and save changes", () -> {
            cartPage.openTab();
            var engageProduct = linkedAccountsNewBusinessSteps.engageProduct;
            cartPage.setQuantityForQLItem(engageProduct.name, engageProduct.quantity);
            cartPage.saveChanges();
        });

        step("3. Open the Quote Details tab, select Payment Method = 'Invoice', populate Start Date, and save changes", () -> {
            quotePage.openTab();
            quotePage.selectPaymentMethod(INVOICE_PAYMENT_METHOD);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();

            closeWindow();
        });

        step("4. Link Engage and Office Quotes and set Engage Quote to Active Agreement via API", () -> {
            var engageOpportunityId = steps.quoteWizard.opportunity.getId();
            var officeOpportunityId = linkedAccountsNewBusinessSteps.officeOpportunity.getId();
            linkedAccountsNewBusinessSteps.stepPrepareEngageQuoteForOpportunityCloseOrSignUp(engageOpportunityId, officeOpportunityId);
        });

        step("5. Click 'Close' button on the Engage Opportunity's record page, " +
                "and check the error message 'Master account is not Paid...'", () -> {
            opportunityPage.clickCloseButton();

            opportunityPage.alertNotificationBlock.shouldBe(visible, ofSeconds(20));
            opportunityPage.notifications
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(MASTER_ACCOUNT_IS_NOT_PAID_ERROR), ofSeconds(1));
        });
    }
}
