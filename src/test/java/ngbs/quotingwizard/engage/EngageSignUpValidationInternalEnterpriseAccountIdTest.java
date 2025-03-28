package ngbs.quotingwizard.engage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.ENGAGE_DIGITAL_SERVICE;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.YOU_NEED_AN_ACTIVE_AGREEMENT_TO_SIGN_UP_ERROR;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.closeWindow;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("Engage")
@Tag("SignUp")
public class EngageSignUpValidationInternalEnterpriseAccountIdTest extends BaseTest {
    private final LinkedAccountsNewBusinessSteps linkedAccountsNewBusinessSteps;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public EngageSignUpValidationInternalEnterpriseAccountIdTest() {
        linkedAccountsNewBusinessSteps = new LinkedAccountsNewBusinessSteps();

        var data = linkedAccountsNewBusinessSteps.data;
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
        linkedAccountsNewBusinessSteps.setUpLinkedAccountsNewBusinessSteps(
                steps.salesFlow.account, steps.salesFlow.contact, steps.quoteWizard.opportunity,
                dealDeskUser, steps.quoteWizard.localAreaCode
        );

        step("Populate 'Service_Type__c' and 'RC_Service_name__c' on Office Account with 'Office' service via API", () -> {
            linkedAccountsNewBusinessSteps.officeAccount.setService_Type__c(linkedAccountsNewBusinessSteps.officeService);
            linkedAccountsNewBusinessSteps.officeAccount.setRC_Service_name__c(linkedAccountsNewBusinessSteps.officeService);

            enterpriseConnectionUtils.update(linkedAccountsNewBusinessSteps.officeAccount);
        });
    }

    @Test
    @TmsLink("CRM-20065")
    @DisplayName("CRM-20065 - 'Internal Enterprise Account ID' is not populated - Sign Up validations failed")
    @Description("Verify that if error notification is shown after clicking 'Process Order' button " +
            "on the Engage Opportunity record page (some validations failed) " +
            "then Account's 'Internal Enterprise Account ID' field isn't populated")
    public void test() {
        step("1. Open Engage Opportunity record page, switch to the Quote Wizard, add a new Sales Quote, " +
                "and select a package for it", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        step("2. Open the Price tab, set quantity for Engage product, and save changes", () -> {
            cartPage.openTab();
            cartPage.setQuantityForQLItem(linkedAccountsNewBusinessSteps.engageProduct.name,
                    linkedAccountsNewBusinessSteps.engageProduct.quantity);
            cartPage.saveChanges();
            closeWindow();
        });

        step("3. Click 'Process Order' button on the Engage Opportunity record page, " +
                "and check error notification in the Process Order modal window", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60));
            opportunityPage.processOrderModal.errorNotifications
                    .shouldHave(exactTexts(format(YOU_NEED_AN_ACTIVE_AGREEMENT_TO_SIGN_UP_ERROR, ENGAGE_DIGITAL_SERVICE)));
            opportunityPage.processOrderModal.closeWindow();
        });

        step("4. Check that Engage Account.Internal_Enterprise_Account_ID__c field is not populated", () ->
                steps.engage.stepCheckInternalEnterpriseAccountId(null, steps.salesFlow.account.getId())
        );
    }
}
