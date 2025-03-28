package ngbs.quotingwizard.engage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.USER_DOES_NOT_HAVE_PERMISSION_TO_SIGN_UP_ENGAGE_ERROR;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.closeWindow;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("Buttons")
@Tag("Engage")
@Tag("SignUp")
public class SignUpNotAvailableForSalesRepEngageTest extends BaseTest {
    private final Steps steps;

    private User salesRepUser;

    //  Test data
    private final Product engageProduct;

    public SignUpNotAvailableForSalesRepEngageTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_EngageDSAndMVP_Monthly_Contract_WithProducts.json",
                Dataset.class);

        steps = new Steps(data);

        engageProduct = data.getProductByDataName("SA_SEAT_5");
    }

    @BeforeEach
    public void setUpTest() {
        salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-25149")
    @DisplayName("CRM-25149 - Sign Up for Engage is not available for Sales Rep - Lightning")
    @Description("Verify that Engage Sign Up is available only for next profiles: \n" +
            "- Deal Desk Lightning \n" +
            "- Sales 4.0 InContact Order Lightning \n" +
            "- CERT - Lightning \n" +
            "- Sales Operations Lightning \n" +
            "- System Administrator \n" +
            "- CRM Support Engineer \n" +
            "- CRM QA Engineer \n" +
            "- CRM Developer \n" +
            "- CRM Dev Team Administrator")
    public void test() {
        step("1. Open the Engage Opportunity, switch to the Quote Wizard, add a new Sales Quote, " +
                "and select a package for it", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        step("2. Open the Price tab, set up quantities and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(engageProduct);
            cartPage.saveChanges();
        });

        step("3. Open the Quote Details tab, select Payment method, set Start Date, save changes " +
                "and update it to Active Agreement via API", () -> {
            quotePage.openTab();
            quotePage.selectPaymentMethod(INVOICE_PAYMENT_METHOD);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();

            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);
            closeWindow();
        });

        step("4. Create Invoice Request Approval for the Engage Account " +
                "with related 'Accounts Payable' AccountContactRole record, " +
                "and set Approval__c.Status = 'Approved' (all via API)", () -> {
            createInvoiceApprovalApproved(steps.quoteWizard.opportunity, steps.salesFlow.account, steps.salesFlow.contact,
                    salesRepUser.getId(), false);
        });

        step("5. Click 'Process Order' button on the Opportunity record page, " +
                "and verify that the error notification is shown in the Process Order modal window", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(USER_DOES_NOT_HAVE_PERMISSION_TO_SIGN_UP_ENGAGE_ERROR),
                            ofSeconds(1));
        });
    }
}
