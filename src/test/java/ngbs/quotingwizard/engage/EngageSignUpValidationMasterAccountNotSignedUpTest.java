package ngbs.quotingwizard.engage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.Opportunity;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.PAID_RC_ACCOUNT_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.setRandomEnterpriseAccountId;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.OFFICE_SERVICE;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.closeWindow;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("P1")
@Tag("Engage")
@Tag("SignUp")
public class EngageSignUpValidationMasterAccountNotSignedUpTest extends BaseTest {
    private final LinkedAccountsNewBusinessSteps linkedAccountsNewBusinessSteps;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account officeAccount;
    private Account engageAccount;
    private Opportunity officeOpportunity;
    private Opportunity engageOpportunity;

    public EngageSignUpValidationMasterAccountNotSignedUpTest() {
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

        officeAccount = linkedAccountsNewBusinessSteps.officeAccount;
        engageAccount = steps.salesFlow.account;
        officeOpportunity = linkedAccountsNewBusinessSteps.officeOpportunity;
        engageOpportunity = steps.quoteWizard.opportunity;

        step("Populate 'Service_Type__c' and 'RC_Service_name__c' on Office Account with 'Office' service via API", () -> {
            officeAccount.setService_Type__c(linkedAccountsNewBusinessSteps.officeService);
            officeAccount.setRC_Service_name__c(linkedAccountsNewBusinessSteps.officeService);

            enterpriseConnectionUtils.update(officeAccount);
        });
    }

    @Test
    @TmsLink("CRM-20513")
    @DisplayName("CRM-20513 - Engage Account - Sign Up Validation - Master Account is not Signed Up")
    @Description("Verify that if Master Account isn't Signed Up then child Engage Account can't be Signed Up too")
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
        });

        step("3. Open the Quote Details tab, select Payment method = 'Invoice', " +
                "populate Start Date field, and save changes", () -> {
            quotePage.openTab();
            quotePage.selectPaymentMethod(INVOICE_PAYMENT_METHOD);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();

            closeWindow();
        });

        step("4. Link Engage and Office Quotes and set Engage Quote as 'Active Agreement' via API", () ->
                linkedAccountsNewBusinessSteps.stepPrepareEngageQuoteForOpportunityCloseOrSignUp(
                        engageOpportunity.getId(), officeOpportunity.getId())
        );

        step("5. Click 'Process Order' button on the Engage Opportunity's record page " +
                        "and verify that error notification is shown in the Process Order modal window",
                this::checkSignUpErrorMessage
        );

        step("6. Set 'RC_Account_Status__c' = 'Paid' for Office Account via API, " +
                "click 'Process Order' button on the Engage Opportunity's record page " +
                "and verify that error notification is shown in the Process Order modal window", () -> {
            officeAccount.setRC_Account_Status__c(PAID_RC_ACCOUNT_STATUS);
            enterpriseConnectionUtils.update(officeAccount);

            checkSignUpErrorMessage();
        });

        step("7. Set 'Offer_Type__c' = 'Office' for Office Account via API, " +
                "click 'Process Order' button on the Engage Opportunity's record page " +
                "and verify that error notification is shown in the Process Order modal window", () -> {
            officeAccount.setOffer_Type__c(OFFICE_SERVICE);
            enterpriseConnectionUtils.update(officeAccount);

            checkSignUpErrorMessage();
        });

        step("8. Populate 'RC_User_ID__c' field for Office Account via API, " +
                "click 'Process Order' button on the Engage Opportunity's record page, " +
                "and check that 'Preparing Data' step on the Process Order modal is passed successfully, " +
                "and that Engage Account's Internal_Enterprise_Account_ID__c is populated", () -> {
            //  imitate that Master Account is already signed up
            setRandomEnterpriseAccountId(officeAccount);
            enterpriseConnectionUtils.update(officeAccount);

            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.engagePreparingDataActiveStep
                    .shouldHave(exactTextCaseSensitive(SYNCED_STEP), ofSeconds(60));

            assertWithTimeout(() -> {
                var updatedAccount = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, Internal_Enterprise_Account_ID__c " +
                                "FROM Account " +
                                "WHERE Id = '" + engageAccount.getId() + "'",
                        Account.class);
                assertNotNull(updatedAccount.getInternal_Enterprise_Account_ID__c(),
                        "Account.Internal_Enterprise_Account_ID__c value on the Engage Account");
            }, ofSeconds(60));
        });
    }

    /**
     * Click 'Process Order' button, check error notification when Master Account isn't signed up
     * and close Process Order modal window.
     */
    @Step("Check error message while trying to Sign Up Engage Opportunity")
    private void checkSignUpErrorMessage() {
        opportunityPage.clickProcessOrderButton();

        opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60));
        opportunityPage.processOrderModal.errorNotifications
                .shouldHave(exactTextsCaseSensitiveInAnyOrder(format(LINKED_MASTER_ACCOUNT_SHOULD_BE_SIGNED_UP_ERROR, ENGAGE_DIGITAL_SERVICE)));
        opportunityPage.processOrderModal.closeWindow();
    }
}
