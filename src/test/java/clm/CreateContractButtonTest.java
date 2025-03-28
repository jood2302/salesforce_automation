package clm;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.AccountContactRole;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.quotePage;
import static base.Pages.wizardPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.ACCOUNTS_PAYABLE_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.SIGNATORY_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("FVT")
@Tag("CLM-Phase2")
@Tag("Contract")
public class CreateContractButtonTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesUserWithPermissionSet;
    private AccountContactRole accountContactRole;

    //  Test data
    private final String brandName;

    public CreateContractButtonTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_NoPhones.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        brandName = data.brandName;
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Sales Rep - Lightning' profile and with 'Enable DocuSign CLM Access' permission set", () -> {
            salesUserWithPermissionSet = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withPermissionSet(ENABLE_DOCUSIGN_CLM_ACCESS_PS)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesUserWithPermissionSet);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesUserWithPermissionSet);

        step("Set Account.RC_Brand__c = '" + brandName + "' via API", () -> {
            steps.salesFlow.account.setRC_Brand__c(brandName);
            enterpriseConnectionUtils.update(steps.salesFlow.account);
        });

        step("Set AccountContactRole.Role = 'Accounts Payable' via API", () -> {
            accountContactRole = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, IsPrimary, Role " +
                            "FROM AccountContactRole " +
                            "WHERE AccountId = '" + steps.salesFlow.account.getId() + "' " +
                            "AND ContactId = '" + steps.salesFlow.contact.getId() + "'",
                    AccountContactRole.class);

            accountContactRole.setRole(ACCOUNTS_PAYABLE_ROLE);
            enterpriseConnectionUtils.update(accountContactRole);
        });

        step("Set Opportunity.IsDocusignEnabled__c = true via API", () -> {
            steps.quoteWizard.opportunity.setIsDocusignCLMEnabled__c(true);
            enterpriseConnectionUtils.update(steps.quoteWizard.opportunity);
        });

        step("Login as a user with 'Sales Rep - Lightning' profile and with 'Enable DocuSign CLM Access' permission set", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(salesUserWithPermissionSet)
        );
    }

    @Test
    @TmsLink("CRM-28745")
    @DisplayName("CRM-28745 - Validate 'Create Contract' button on Opportunity Page Layout is disabled " +
            "& error on-hover when Oppty contact role != Primary + Signatory")
    @Description("Verify that 'Create Contract' button on Opportunity Page Layout is disabled " +
            "& error on-hover when Oppty contact role != Primary + Signatory")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Quote Details tab, and verify that 'Create Contract' button is disabled", () -> {
            quotePage.openTab();
            quotePage.createContractButton.shouldBe(disabled);
        });

        step("3. Set AccountContactRole's Role = 'Signatory' and IsPrimary = false via API", () -> {
            accountContactRole.setRole(SIGNATORY_ROLE);
            accountContactRole.setIsPrimary(false);
            enterpriseConnectionUtils.update(accountContactRole);
        });

        step("4. Re-open the Quote Wizard with a created quote, open the Quote Details tab, " +
                "and verify that 'Create Contract' button is disabled", () -> {
            refresh();
            wizardPage.waitUntilLoaded();

            quotePage.openTab();
            quotePage.createContractButton.shouldBe(disabled);
        });
    }
}
