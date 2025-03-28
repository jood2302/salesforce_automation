package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.Account;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.opportunityPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.CREDIT_CARD_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomPositiveInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createElaServiceAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountRelationFactory.createElaAccountRelation;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.ELA_BILLING_ACCOUNT_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.getPrimaryContactOnAccount;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.closeWindow;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("ELALeads")
@Tag("SignUp")
public class ElaAccountsSignUpValidationsTest extends BaseTest {
    private final SalesQuoteSignUpSteps salesQuoteSignUpSteps;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account firstServiceAccount;
    private Account secondServiceAccount;

    public ElaAccountsSignUpValidationsTest() {
        salesQuoteSignUpSteps = new SalesQuoteSignUpSteps();
        steps = new Steps(salesQuoteSignUpSteps.data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUserWithPermissionSet = salesQuoteSignUpSteps.getSalesRepUserWithAllowedProcessOrderWithoutShipping();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithPermissionSet);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithPermissionSet);

        step("Set Account.ELA_Account_Type__c = 'ELA Billing' for parent Account via API", () -> {
            steps.salesFlow.account.setELA_Account_Type__c(ELA_BILLING_ACCOUNT_TYPE);
            enterpriseConnectionUtils.update(steps.salesFlow.account);
        });

        step("Create two service Accounts with related AccountContactRoles (using parent Account's Contact) " +
                "and Account Relations for them via API", () -> {
            var primaryContact = getPrimaryContactOnAccount(steps.salesFlow.account);
            firstServiceAccount = createElaServiceAccountInSFDC(salesRepUserWithPermissionSet, primaryContact, new AccountData(salesQuoteSignUpSteps.data));
            secondServiceAccount = createElaServiceAccountInSFDC(salesRepUserWithPermissionSet, primaryContact, new AccountData(salesQuoteSignUpSteps.data));

            createElaAccountRelation(steps.salesFlow.account, firstServiceAccount);
            createElaAccountRelation(steps.salesFlow.account, secondServiceAccount);
        });

        salesQuoteSignUpSteps.loginAsSalesRepUserWithAllowedProcessOrderWithoutShipping();
    }

    @Test
    @TmsLink("CRM-21705")
    @DisplayName("CRM-21705 - ELA Billing Account - Sign Up Validation - All ELA Service Accounts are signed up and active")
    @Description("Verify that when user clicks 'Process Order' button on ELA Billing Account's Opportunity " +
            "there's a validation for all related ELA Service Accounts to be signed up and active")
    public void test() {
        step("1. Open the New Business Opportunity, switch to the Quote Wizard, add a new Sales quote, " +
                "add some Products, assign devices, and save changes", () ->
                salesQuoteSignUpSteps.prepareSalesQuoteWithAssignedDevicesSteps(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Quote Details tab, populate Main Area Code, Payment Method and Start Date, " +
                "save changes, and set it to Active Agreement via API", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.selectPaymentMethod(CREDIT_CARD_PAYMENT_METHOD);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();

            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);
            closeWindow();
        });

        step("3. Click 'Process Order' button on the Opportunity record page, " +
                        "verify that the error notification 'Please, review linked Service ELA Accounts...' " +
                        "is shown in the Process Order modal window, and close the modal window",
                this::stepCheckSignUpErrorMessage
        );

        step("4. Populate 'Billing_ID__c' field on the first Service Account via API", () -> {
            firstServiceAccount.setBilling_ID__c(getRandomPositiveInteger());
            enterpriseConnectionUtils.update(firstServiceAccount);
        });

        step("5. Click 'Process Order' button on the Opportunity record page, " +
                        "verify that the error notification 'Please, review linked Service ELA Accounts...' " +
                        "is shown in the Process Order modal window, and close the modal window",
                this::stepCheckSignUpErrorMessage
        );

        step("6. Populate 'Billing_ID__c' field on second Service Account via API", () -> {
            secondServiceAccount.setBilling_ID__c(getRandomPositiveInteger());
            enterpriseConnectionUtils.update(secondServiceAccount);
        });

        step("7. Click 'Process Order' button on the Opportunity record page, " +
                "verify that there are no error notifications in the Process Order modal window, " +
                "'Preparing Data' step is completed, Timezone selector is enabled for a user, " +
                "and 'Sign Up MVP' button is disabled", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();

            opportunityPage.processOrderModal.selectTimeZonePicklist.getInput().shouldBe(enabled);
            opportunityPage.processOrderModal.signUpButton.shouldBe(disabled);
        });
    }

    /**
     * Check that error message is shown
     * after the user clicks 'Process Order' button on the Opportunity record page
     * when the related ELA Service Accounts are not signed up.
     */
    private void stepCheckSignUpErrorMessage() {
        step("Click 'Process Order' button, verify that error notification is shown in the Process Order modal window, " +
                "and close the modal window", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();

            opportunityPage.processOrderModal.errorNotifications
                    .shouldHave(exactTexts(format(REVIEW_LINKED_SERVICE_ELA_ACCOUNTS_ERROR, MVP_SERVICE)), ofSeconds(1));
            opportunityPage.processOrderModal.mvpPreparingDataActiveStep
                    .shouldHave(exactTextCaseSensitive(DATA_VALIDATION_STEP));
            opportunityPage.processOrderModal.closeWindow();
        });
    }
}
