package ngbs.quotingwizard.engage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.modal.AccountManagerModal.ACCOUNT_DOESNT_MEET_REQUIREMENTS_ERROR;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.INFLUENCER_ROLE;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("PDV")
@Tag("Engage")
@Tag("AccountBindings")
public class EngageManageAccountBindingsWithoutPermissionTest extends BaseTest {
    private final Steps steps;
    private final AccountBindingsSteps accountBindingsSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account engageAccount;
    private Contact engageContact;
    private Opportunity engageOpportunity;
    private Account officeAccount;

    public EngageManageAccountBindingsWithoutPermissionTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_EngageDSAndMVP_Monthly_Contract_WithProducts.json",
                Dataset.class);

        steps = new Steps(data);
        accountBindingsSteps = new AccountBindingsSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        engageAccount = steps.salesFlow.account;
        engageContact = steps.salesFlow.contact;
        
        steps.quoteWizard.createOpportunity(engageAccount, engageContact, dealDeskUser);
        engageOpportunity = steps.quoteWizard.opportunity;

        accountBindingsSteps.createOfficeAccountRecordsForBinding(dealDeskUser);
        officeAccount = accountBindingsSteps.officeAccount;

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-20487")
    @DisplayName("CRM-20487 - Link Accounts validation - User don't have Custom permission")
    @Description("Verify that MVP and Engage Accounts can be linked from the Quote Wizard " +
            "if their Contact Roles are linked to the same Contact record")
    public void test() {
        step("1. Open the Quote Wizard for the New Business Engage Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(engageOpportunity.getId())
        );

        step("2. Open the Quote Details tab, click on 'Manage Account Bindings' button, " +
                "select Office Account for binding and check that error message is shown", () -> {
            quotePage.openTab();
            quotePage.manageAccountBindingsButton.click();
            quotePage.manageAccountBindings.accountSearchInput.selectItemInCombobox(officeAccount.getName());
            quotePage.manageAccountBindings.notifications
                    .shouldHave(exactTexts(ACCOUNT_DOESNT_MEET_REQUIREMENTS_ERROR), ofSeconds(10));
        });

        step("3. Create the second contact role for Office Account with the same Contact " +
                "as on Engage Account's Primary Contact Role via API and reopen the Quote Wizard", () -> {
            createAccountContactRole(officeAccount, engageContact, INFLUENCER_ROLE, false);

            refresh();
            wizardPage.waitUntilLoaded();
            packagePage.packageSelector.waitUntilLoaded();
        });

        step("4. Open the Quote Details tab, click on 'Manage Account Bindings' button, " +
                "check that Office Account is selected without errors, " +
                "click 'Submit' button and check that Engage Account.Master_Account__c = Office Account.Id", () -> {
            quotePage.openTab();

            quotePage.manageAccountBindingsButton.click();
            quotePage.manageAccountBindings.accountSearchInput.selectItemInCombobox(officeAccount.getName());
            quotePage.manageAccountBindings.notifications.shouldHave(size(0));
            quotePage.submitAccountBindingChanges();

            var engageAccountUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Master_Account__c " +
                            "FROM Account " +
                            "WHERE Id = '" + engageAccount.getId() + "'",
                    Account.class);
            assertThat(engageAccountUpdated.getMaster_Account__c())
                    .as("Engage Account.Master_Account__c value (should be equal to Office Account.Id)")
                    .isEqualTo(officeAccount.getId());
        });
    }
}
