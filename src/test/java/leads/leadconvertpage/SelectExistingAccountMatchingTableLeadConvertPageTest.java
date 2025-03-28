package leads.leadconvertpage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.leadConvert.Datasets;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.page.lead.convert.MatchedItem;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.*;
import static com.aquiva.autotests.rc.page.lead.convert.MatchedItem.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomPositiveInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createExistingCustomerAccountInSFDC;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$$;
import static io.qameta.allure.Allure.step;
import static java.util.stream.Collectors.toList;

@Tag("P1")
@Tag("LeadConvert")
public class SelectExistingAccountMatchingTableLeadConvertPageTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUser;
    private User dealDeskUser;

    private Account newBusinessAccount;
    private Account existingBusinessAccount;

    //  Test data
    private final Dataset existingBusinessDataset;

    public SelectExistingAccountMatchingTableLeadConvertPageTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/RC_MVP_Monthly_NonContract_NB_EB.json",
                Datasets.class);
        steps = new Steps(data.dataSets[0]);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        existingBusinessDataset = data.dataSets[1];
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount(existingBusinessDataset);
        }

        salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);

        newBusinessAccount = steps.salesFlow.account;

        step("Create an additional Existing Business Account with related Contact and AccountContactRole records, " +
                "and set Account's RC_Account_Number__c and Current_Owner__c fields (all via API)", () -> {
            existingBusinessAccount = createExistingCustomerAccountInSFDC(salesRepUser, new AccountData(existingBusinessDataset));

            var rcAccountNumber = getRandomPositiveInteger();
            existingBusinessAccount.setRC_Account_Number__c(rcAccountNumber);
            existingBusinessAccount.setCurrent_Owner__c(salesRepUser.getId());

            enterpriseConnectionUtils.update(existingBusinessAccount);
        });

        step("Set the same Email value for the Lead and the New Business Account's primary Contact " +
                "and Lead.User_ID__c = Existing Business Account.RC_User_ID__c via API", () -> {
            steps.leadConvert.salesLead.setEmail(steps.salesFlow.contact.getEmail());
            steps.leadConvert.salesLead.setUser_ID__c(existingBusinessAccount.getRC_User_ID__c());

            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        step("Set Account.Current_Owner__c value for New Business Account via API", () -> {
            dealDeskUser = steps.salesFlow.getDealDeskUser();
            newBusinessAccount.setCurrent_Owner__c(dealDeskUser.getId());
            enterpriseConnectionUtils.update(newBusinessAccount);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20775")
    @DisplayName("CRM-20775 - Lead Conversion - Sales Lead - Select Existing Account - Matching Accounts Table")
    @Description("Verify that Account section functions properly when a user selects an account via Matched Accounts table")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Check accounts in the 'Matched Accounts' table", () -> {
            leadConvertPage.matchedAccountsTableLabel.shouldHave(exactTextCaseSensitive(MATCHED_ACCOUNTS_TABLE_LABEL));

            var actualMatchedAccountsNames = leadConvertPage.getMatchedAccountsList()
                    .stream()
                    .map(MatchedItem::getName)
                    .collect(toList());
            $$(actualMatchedAccountsNames).shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    newBusinessAccount.getName(),
                    existingBusinessAccount.getName()
            ));
        });

        step("3. Check the values for the New Business Account in the 'Matched Accounts' table", () -> {
            var newBusinessMatchedAccount = leadConvertPage.getMatchedAccount(newBusinessAccount.getId());

            newBusinessMatchedAccount.getElementInColumn(MATCHED_ACCOUNTS_RC_USER_ID_COLUMN)
                    .shouldBe(empty);
            newBusinessMatchedAccount.getElementInColumn(MATCHED_ACCOUNTS_RC_ACCOUNT_NUMBER_COLUMN)
                    .shouldBe(empty);
            newBusinessMatchedAccount.getElementInColumn(MATCHED_ACCOUNTS_RC_ACCOUNT_STATUS_COLUMN)
                    .shouldBe(empty);
            newBusinessMatchedAccount.getElementInColumn(MATCHED_ACCOUNTS_TYPE_COLUMN)
                    .shouldHave(exactTextCaseSensitive(MATCHED_ACCOUNT_NB_TYPE));
            newBusinessMatchedAccount.getElementInColumn(MATCHED_ACCOUNTS_CURRENT_OWNER_COLUMN)
                    .shouldHave(exactTextCaseSensitive(dealDeskUser.getName()));
        });

        step("4. Check the values for the Existing Business Account in the 'Matched Accounts' table", () -> {
            var existingBusinessMatchedAccount = leadConvertPage.getMatchedAccount(existingBusinessAccount.getId());

            existingBusinessMatchedAccount.getElementInColumn(MATCHED_ACCOUNTS_RC_USER_ID_COLUMN)
                    .shouldHave(exactTextCaseSensitive(existingBusinessAccount.getRC_User_ID__c()));
            existingBusinessMatchedAccount.getElementInColumn(MATCHED_ACCOUNTS_RC_ACCOUNT_NUMBER_COLUMN)
                    .shouldHave(exactTextCaseSensitive(existingBusinessAccount.getRC_Account_Number__c()));
            existingBusinessMatchedAccount.getElementInColumn(MATCHED_ACCOUNTS_RC_ACCOUNT_STATUS_COLUMN)
                    .shouldHave(exactTextCaseSensitive(existingBusinessAccount.getRC_Account_Status__c()));
            existingBusinessMatchedAccount.getElementInColumn(MATCHED_ACCOUNTS_TYPE_COLUMN)
                    .shouldHave(exactTextCaseSensitive(MATCHED_ACCOUNT_EB_NGBS_TYPE));
            existingBusinessMatchedAccount.getElementInColumn(MATCHED_ACCOUNTS_CURRENT_OWNER_COLUMN)
                    .shouldHave(exactTextCaseSensitive(salesRepUser.getName()));
        });

        step("5. Check that 'Matched Accounts' table contains required columns", () -> {
            leadConvertPage.matchedAccountsTableHeaders
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(MATCHED_ACCOUNTS_HEADERS));
        });

        step("6. Select the New Business Account in the 'Matched Accounts' table", () -> {
            leadConvertPage.getMatchedAccount(newBusinessAccount.getId())
                    .getSelectButton()
                    .click();

            leadConvertPage.getMatchedAccount(newBusinessAccount.getId())
                    .getSelectButtonInput()
                    .shouldBe(selected);
        });

        step("7. Select the Existing Business Account in the 'Matched Accounts' table", () -> {
            leadConvertPage.getMatchedAccount(existingBusinessAccount.getId())
                    .getSelectButton()
                    .click();

            leadConvertPage.getMatchedAccount(existingBusinessAccount.getId())
                    .getSelectButtonInput()
                    .shouldBe(selected);
            leadConvertPage.getMatchedAccount(newBusinessAccount.getId())
                    .getSelectButtonInput()
                    .shouldNotBe(selected);
        });

        step("8. Click 'Apply' and check that Search box and Matched Accounts table are disabled, " +
                "'Edit' button is displayed, and Opportunity, Contact and Lead Qualification sections are displayed", () -> {
            leadConvertPage.accountInfoApplyButton.click();

            leadConvertPage.existingAccountSearchInput.getInput().shouldBe(disabled);
            leadConvertPage.getMatchedAccountsList()
                    .forEach(matchedItem -> matchedItem.getSelectButtonInput().shouldBe(disabled));
            leadConvertPage.accountInfoEditButton.shouldBe(visible);

            leadConvertPage.opportunityInfoSection.shouldBe(visible);
            leadConvertPage.contactInfoSection.shouldBe(visible);
            leadConvertPage.leadQualificationSection.shouldBe(visible);
        });
    }
}
