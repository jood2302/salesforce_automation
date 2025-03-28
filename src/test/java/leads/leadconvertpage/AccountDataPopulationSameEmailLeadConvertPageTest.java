package leads.leadconvertpage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.leadConvert.Datasets;
import com.aquiva.autotests.rc.page.lead.convert.MatchedItem;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.Account;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createExistingCustomerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewCustomerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.getPrimaryContactOnAccount;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$$;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

@Tag("P1")
@Tag("LeadConvert")
public class AccountDataPopulationSameEmailLeadConvertPageTest extends BaseTest {
    private final Datasets data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account newBusinessAccount;
    private Account existingBusinessPaidAccount;
    private Account existingBusinessTrialAccount;

    //  Test data
    private final String forecastedUsersForNewAccount;
    private final Integer forecastedUsersForPaidAccount;
    private final Integer forecastedUsersForTrialAccount;
    private final String ringCentralBI;
    private final String rcFranceBI;
    private final String rcSingaporeBI;

    public AccountDataPopulationSameEmailLeadConvertPageTest() {
        data = JsonUtils.readConfigurationResource(
                "data/leadConvert/RC_MVP_Monthly_NonContract_NB_EB_DifferentBIs.json",
                Datasets.class);
        steps = new Steps(data.dataSets[0]);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        forecastedUsersForNewAccount = data.dataSets[0].forecastedUsers;
        forecastedUsersForPaidAccount = data.dataSets[1].packageFolders[0].packages[0].productsFromBilling[5].quantity;
        forecastedUsersForTrialAccount = data.dataSets[2].packageFolders[0].packages[0].productsFromBilling[3].quantity;
        ringCentralBI = data.dataSets[0].businessIdentity.name;
        rcFranceBI = data.dataSets[1].businessIdentity.name;
        rcSingaporeBI = data.dataSets[2].businessIdentity.name;
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount(data.dataSets[1]);
            steps.ngbs.generateBillingAccount(data.dataSets[2]);
        }

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);

        step("Create New Business Account with related Contact and AccountContactRole via API", () -> {
            newBusinessAccount = createNewCustomerAccountInSFDC(salesRepUser, new AccountData(data.dataSets[0]));
        });

        step("Create Existing Business Accounts (standard Paid and Trial) " +
                "with related Contacts and AccountContactRoles via API", () -> {
            existingBusinessPaidAccount = createExistingCustomerAccountInSFDC(salesRepUser, new AccountData(data.dataSets[1]));
            existingBusinessTrialAccount = createExistingCustomerAccountInSFDC(salesRepUser, new AccountData(data.dataSets[2]));
        });

        step("Set the same email to all of the test Accounts' Contacts and to the Lead via API", () -> {
            var testEmail = UUID.randomUUID() + "@example.com";

            var newBusinessAccountContact = getPrimaryContactOnAccount(newBusinessAccount);
            var existingBusinessPaidAccountContact = getPrimaryContactOnAccount(existingBusinessPaidAccount);
            var existingBusinessTrialAccountContact = getPrimaryContactOnAccount(existingBusinessTrialAccount);

            newBusinessAccountContact.setEmail(testEmail);
            existingBusinessPaidAccountContact.setEmail(testEmail);
            existingBusinessTrialAccountContact.setEmail(testEmail);
            steps.leadConvert.salesLead.setEmail(testEmail);

            enterpriseConnectionUtils.update(
                    newBusinessAccountContact,
                    existingBusinessPaidAccountContact,
                    existingBusinessTrialAccountContact
            );
            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        step("Populate 'Number_of_Users__c' field on test Lead via API", () -> {
            steps.leadConvert.salesLead.setNumber_of_Users__c(Double.valueOf(forecastedUsersForNewAccount));
            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-13079")
    @DisplayName("CRM-13079 - Switching between Accounts in the Matched Accounts list")
    @Description("Verify that switching between Accounts in Matched Accounts list selects Accounts and loads data for them correctly")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Check accounts in the Matched Accounts list", () -> {
            var actualMatchedAccountNames = leadConvertPage.getMatchedAccountsList()
                    .stream()
                    .map(MatchedItem::getName)
                    .collect(toList());

            $$(actualMatchedAccountNames).shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    newBusinessAccount.getName(),
                    existingBusinessPaidAccount.getName(),
                    existingBusinessTrialAccount.getName()
            ));
        });

        step("3. Select the New Business Account from the Matched Account list and click 'Apply'", () ->
                leadConvertPage.selectMatchedAccount(newBusinessAccount.getId())
        );

        step("4. Click on 'Edit' in Opportunity Info Section, " +
                "check that First Business Identity from the picklist is selected " +
                "and Forecasted Users value is taken from Lead", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.businessIdentityPicklist.getOptions().shouldHave(itemWithText(ringCentralBI), ofSeconds(30));
            leadConvertPage.businessIdentityPicklist.getOptions().first()
                    .shouldHave(exactTextCaseSensitive(ringCentralBI), ofSeconds(30))
                    .shouldBe(selected);
            leadConvertPage.forecastedUsersInput.shouldHave(exactValue(forecastedUsersForNewAccount));
        });

        step("5. Select Existing Business NGBS Account from Matched Account list", () ->
                leadConvertPage.reselectMatchedAccount(existingBusinessPaidAccount.getId())
        );

        step("6. Click on 'Edit' in Opportunity Section and check that '" + rcFranceBI + "' Business Identity is preselected " +
                "and Forecasted Users value equals number of DL on Account", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.businessIdentityPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(rcFranceBI), ofSeconds(30));
            leadConvertPage.forecastedUsersInput.shouldHave(exactValue(valueOf(forecastedUsersForPaidAccount)));
        });

        step("7. Select Existing Business Trial Account in Account info section", () ->
                leadConvertPage.reselectMatchedAccount(existingBusinessTrialAccount.getId())
        );

        step("8. Click on 'Edit' in Opportunity Info Section Button and check that '" + rcSingaporeBI + "' Business Identity is preselected " +
                "and Forecasted Users value equals number of DLs on the Account", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.businessIdentityPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(rcSingaporeBI), ofSeconds(30));
            leadConvertPage.forecastedUsersInput.shouldHave(exactValue(valueOf(forecastedUsersForTrialAccount)));
        });
    }
}
