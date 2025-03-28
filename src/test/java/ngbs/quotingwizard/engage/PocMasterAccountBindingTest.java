package ngbs.quotingwizard.engage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.packagePage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.AccountManagerModal.MASTER_ACCOUNT_CANT_BE_POC_ERROR;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewCustomerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory.createOpportunity;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.BUSINESS_USER_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.POC_RC_ACCOUNT_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.getPrimaryContactOnAccount;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Condition.disabled;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("Engage")
public class PocMasterAccountBindingTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account officeAccount;
    private Account engageAccount;
    private Contact engageAccountContact;
    private Opportunity engageOpportunity;

    //  Test data
    private final String engageAccountService;
    private final Package engagePackage;

    public PocMasterAccountBindingTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_POC_Monthly_Contract_163082013_ED_Standalone_NB.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        engageAccountService = data.packageFolders[1].name;
        engagePackage = data.packageFolders[1].packages[0];
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
        }

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        officeAccount = steps.salesFlow.account;

        steps.quoteWizard.createOpportunity(officeAccount, steps.salesFlow.contact, dealDeskUser);

        step("Set Office Account.RC_Account_Status__c = 'POC' via API", () -> {
            officeAccount.setRC_Account_Status__c(POC_RC_ACCOUNT_STATUS);
            enterpriseConnectionUtils.update(officeAccount);
        });

        step("Create New Business Engage Account with Service_Type__c and RC_Service_name__c = '" +
                engageAccountService + "' with related Contact and AccountContactRole records via API", () -> {
            engageAccount = createNewCustomerAccountInSFDC(dealDeskUser, new AccountData(data));
            engageAccount.setService_Type__c(engageAccountService);
            engageAccount.setRC_Service_name__c(engageAccountService);
            enterpriseConnectionUtils.update(engageAccount);

            engageAccountContact = getPrimaryContactOnAccount(engageAccount);
        });

        step("Add second contact role for Office Account with the same Contact " +
                "as on Engage Account's Primary Contact Role via API", () -> {
            createAccountContactRole(officeAccount, engageAccountContact, BUSINESS_USER_ROLE, false);
        });

        step("Create New Business Engage Opportunity via API", () -> {
            engageOpportunity = createOpportunity(engageAccount, engageAccountContact, true,
                    data.getBrandName(), data.businessIdentity.id, dealDeskUser, data.getCurrencyIsoCode(), engageAccountService);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-20819")
    @DisplayName("CRM-20819 - POC Office Account binding")
    @Description("Verify that if Office Account is POC then with Link from Engage to Office " +
            "will be shown explanation message: 'Master Account can't be POC'")
    public void test() {
        step("1. Open the Quote Wizard for Engage Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(engageOpportunity.getId());

            packagePage.packageSelector.selectPackage(data.chargeTerm, engageAccountService, engagePackage);
            packagePage.saveChanges();
        });

        step("2. Open the Quote Details tab, open Account binding modal " +
                "and verify that POC Account can't be linked to Engage Account", () -> {
            quotePage.openTab();
            quotePage.manageAccountBindingsButton.click();

            quotePage.manageAccountBindings.accountSearchInput.selectItemInCombobox(officeAccount.getName());
            //  Expand notification list to view all notifications
            quotePage.manageAccountBindings.notificationBlock.click();
            quotePage.manageAccountBindings.notifications.shouldHave(itemWithText(MASTER_ACCOUNT_CANT_BE_POC_ERROR));
            quotePage.manageAccountBindings.submitButton.shouldBe(disabled);
        });
    }
}
