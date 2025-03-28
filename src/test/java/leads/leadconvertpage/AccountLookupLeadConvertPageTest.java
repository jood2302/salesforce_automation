package leads.leadconvertpage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.leadConvert.Datasets;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.Account;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createExistingCustomerAccountInSFDC;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("P1")
@Tag("NGBS")
@Tag("LeadConvert")
public class AccountLookupLeadConvertPageTest extends BaseTest {
    private final Datasets data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account existingBusinessAccount;

    public AccountLookupLeadConvertPageTest() {
        data = JsonUtils.readConfigurationResource(
                "data/leadConvert/RC_MVP_Monthly_NonContract_NB_EB.json",
                Datasets.class);
        steps = new Steps(data.dataSets[0]);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount(data.dataSets[1]);
        }

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);

        //  to bypass Lead Qualification-related validations when the user clicks "Convert" on the Lead Record page
        step("Populate Sales Lead's Lead_Qualification_Next_Steps__c, Lead_Qualification_Current_SItuation__c, " +
                "Lead_Qualification_Problems__c and Decision_Making_Process__c fields via API", () -> {
            steps.leadConvert.salesLead.setLead_Qualification_Next_Steps__c(TEST_STRING);
            steps.leadConvert.salesLead.setLead_Qualification_Current_SItuation__c(TEST_STRING);
            steps.leadConvert.salesLead.setLead_Qualification_Problems__c(TEST_STRING);
            steps.leadConvert.salesLead.setDecision_Making_Process__c(TEST_STRING);
            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        step("Create Existing Business Account with related Contact and AccountContactRole via API", () -> {
            existingBusinessAccount = createExistingCustomerAccountInSFDC(salesRepUser, new AccountData(data.dataSets[1]));
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);

        //  this will help stabilize the behavior of Account lookup on the Lead Convert page 
        //  as it returns the "recent" accounts in the search results
        step("Open the record pages for the test New Business Account and Existing Business Account", () -> {
            accountRecordPage.openPage(steps.salesFlow.account.getId());
            accountRecordPage.openPage(existingBusinessAccount.getId());
        });
    }

    @Test
    @TmsLink("CRM-11030")
    @DisplayName("CRM-11030 - Account Lookup on Lead conversion page")
    @Description("Verify that looking up accounts through lookup field in Account section gets correct results")
    public void test() {
        step("1. Open Lead Record Page and click 'Convert' button", () -> {
            leadRecordPage.openPage(steps.leadConvert.salesLead.getId());
            leadRecordPage.clickConvertButton();

            leadConvertPage.switchToIFrame();
            leadConvertPage.newExistingAccountToggle.shouldBe(visible, ofSeconds(60));
            leadConvertPage.existingAccountSearchInput.getSelf().shouldBe(visible);
        });

        step("2. Select New Business Account by its Name, and check Account lookup", () -> {
            leadConvertPage.existingAccountSearchInput.selectItemInCombobox(steps.salesFlow.account.getName());

            leadConvertPage.existingAccountSearchInput.getSelectedEntity()
                    .shouldHave(exactTextCaseSensitive(steps.salesFlow.account.getName()));
        });

        step("3. Reload the Lead Convert page, search and select the Existing Business Account by its RC_User_ID__c, " +
                "and check the Account lookup", () -> {
            //  reloading the page instead of clearing the lookup for more stable results
            leadConvertPage.reloadIFrame();
            leadConvertPage.existingAccountSearchInput.getInput().shouldBe(visible, ofSeconds(60));

            leadConvertPage.existingAccountSearchInput.selectItemInCombobox(
                    existingBusinessAccount.getRC_User_ID__c(), existingBusinessAccount.getName());

            leadConvertPage.existingAccountSearchInput.getSelectedEntity()
                    .shouldHave(exactTextCaseSensitive(existingBusinessAccount.getName()));
        });
    }
}
