package leads.newbusiness;

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
import static com.aquiva.autotests.rc.page.salesforce.account.AccountHighlightsPage.CRITICAL_ACCOUNT_BANNER_MESSAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.ELA_BILLING_ACCOUNT_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.ELA_SERVICE_ACCOUNT_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountRelationHelper.ELA_TYPE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("LeadConvert")
@Tag("ELALeads")
public class ElaLeadConvertFlowTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private AccountRelation__c accountRelation;
    private Lead convertedLead;
    private String billingAccountId;
    private String serviceAccountId;

    //  Test data
    private final String ringCentralBI;
    private final String numberOfElaAccounts;
    private final String accountRelationNamePattern;

    public ElaLeadConvertFlowTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        ringCentralBI = data.getBusinessIdentityName();
        numberOfElaAccounts = "1";
        //  This RegExp is a pattern of Account Relation Name (e.g. A-0001, A-1234 etc.)
        accountRelationNamePattern = "A-\\d+";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-21625")
    @TmsLink("CRM-36883")
    @DisplayName("CRM-21625 - ELA on Lead Convert [New Accounts] .\n" +
            "CRM-36883 - 'Critical Account' banner for ELA Accounts")
    @Description("CRM-21625 - Verify that user is able to work with ELA Leads and have Accounts created automatically from them. \n" +
            "CRM-36883 - Verify that there is a banner for accounts where ELA_Account_Type__c is not empty. \n" +
            "Text of the banner is : 'Critical account, do not touch, contact the Deal Management team for assistance with opportunities'")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Switch the toggle into 'Create New Account' position in Account Info section, " +
                "enable ELA and set number of ELA Accounts", () -> {
            leadConvertPage.newExistingAccountToggle.shouldBe(enabled, ofSeconds(60)).click();
            leadConvertPage.accountInfoEditButton.click();

            leadConvertPage.elaCheckbox.shouldBe(enabled, ofSeconds(30)).click();
            leadConvertPage.elaServiceAccountsNumberInput.setValue(numberOfElaAccounts);
            leadConvertPage.accountInfoApplyButton.click();
        });

        step("3. Click 'Edit' in Opportunity Section, check that Business Identity = 'RingCentral Inc.', " +
                "populate Close Date field, and click 'Apply'", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.businessIdentityPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(ringCentralBI));
            leadConvertPage.closeDateDatepicker.setTomorrowDate();
            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("4. Press 'Convert' button", () -> {
            leadConvertPage.convertButton.click();

            //  this is a particularly long conversion, that's why LeadConvertSteps.pressConvertButton() is not suitable
            opportunityPage.entityTitle.shouldBe(visible, ofSeconds(240));
            opportunityPage.waitUntilLoaded();
        });

        //  CRM-21625
        step("5. Verify that Account, Account Relation, and Account Contact Role records have expected values " +
                "after ELA Lead conversion", () -> {
            convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ConvertedAccountId, ConvertedContactId, ConvertedOpportunityId " +
                            "FROM Lead " +
                            "WHERE Id = '" + steps.leadConvert.salesLead.getId() + "'",
                    Lead.class);

            var billingAccount = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ELA_Account_Type__c " +
                            "FROM Account " +
                            "WHERE Id = '" + convertedLead.getConvertedAccountId() + "'",
                    Account.class);
            billingAccountId = billingAccount.getId();

            assertThat(billingAccount.getELA_Account_Type__c())
                    .as("Billing Account.ELA_Account_Type__c value")
                    .isEqualTo(ELA_BILLING_ACCOUNT_TYPE);

            accountRelation = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name, Child_Account__c " +
                            "FROM AccountRelation__c " +
                            "WHERE Relation_Type__c = '" + ELA_TYPE + "' " +
                            "AND Parent_Account__c = '" + billingAccount.getId() + "'",
                    AccountRelation__c.class);
            assertThat(accountRelation.getName())
                    .as("AccountRelation__c.Name value")
                    .matches(accountRelationNamePattern);

            var serviceAccount = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ELA_Account_Type__c " +
                            "FROM Account " +
                            "WHERE Id = '" + accountRelation.getChild_Account__c() + "'",
                    Account.class);
            serviceAccountId = serviceAccount.getId();

            assertThat(serviceAccount.getELA_Account_Type__c())
                    .as("Service Account.ELA_Account_Type__c value ")
                    .isEqualTo(ELA_SERVICE_ACCOUNT_TYPE);

            var convertedContact = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, AccountId " +
                            "FROM Contact " +
                            "WHERE Id = '" + convertedLead.getConvertedContactId() + "'",
                    Contact.class);
            assertThat(convertedContact.getAccountId())
                    .as("Contact.AccountId value")
                    .isEqualTo(billingAccount.getId());

            var accountContactRoles = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM AccountContactRole " +
                            "WHERE ContactId = '" + convertedContact.getId() + "' " +
                            "AND AccountId = '" + billingAccount.getId() + "'",
                    AccountContactRole.class);
            assertThat(accountContactRoles)
                    .as("List of AccountContactRole records for the Contact converted from ELA Lead " +
                            "and Billing Account")
                    .isNotEmpty();
        });

        //  CRM-21625
        step("6. Check that the Opportunity was created after Lead conversion", () -> {
            assertThat(convertedLead.getConvertedOpportunityId())
                    .as("ConvertedLead.ConvertedOpportunityId value")
                    .isNotNull();
        });

        //  CRM-36883
        step("7. Open the Account record page with ELA_Account_Type__c = 'ELA Billing', " +
                "and check that 'Details' section has 'Critical Account' banner", () -> {
            checkCriticalAccountBannerVisibility(billingAccountId, convertedLead.getConvertedContactId());
        });

        //  CRM-36883
        step("8. Open the Account record page with ELA_Account_Type__c = 'ELA Service', " +
                "and check that 'Details' section has 'Critical Account' banner", () -> {
            checkCriticalAccountBannerVisibility(serviceAccountId, convertedLead.getConvertedContactId());
        });

        step("9. Set Service Account's ELA_Account_Type__c = null via API", () -> {
            var serviceAccountToUpdate = new Account();
            serviceAccountToUpdate.setId(serviceAccountId);
            serviceAccountToUpdate.setFieldsToNull(new String[]{"ELA_Account_Type__c"});
            enterpriseConnectionUtils.update(serviceAccountToUpdate);
        });

        //  CRM-36883
        step("10. Open the Account record page for the Service Account, " +
                "and verify that 'Critical Account' banner is not displayed", () -> {
            accountRecordPage.openPage(serviceAccountId);
            accountHighlightsPage.switchToIFrame();
            accountHighlightsPage.miscHighlightsWarningMessage.shouldNotBe(visible);
        });
    }

    /**
     * Open the Account's record page,
     * and check if the 'Critical Account...' banner is displayed on it.
     *
     * @param accountId ID of the Account to check
     * @param contactId ID of the Account's Contact
     */
    private void checkCriticalAccountBannerVisibility(String accountId, String contactId) {
        //  Workaround for automatic Account Page refresh logic in VF page under test
        step("Update the Account.Most_Recent_Implementation_Contact__c via API", () -> {
            var accountToUpdate = new Account();
            accountToUpdate.setId(accountId);
            accountToUpdate.setMost_Recent_Implementation_Contact__c(contactId);
            enterpriseConnectionUtils.update(accountToUpdate);
        });

        accountRecordPage.openPage(accountId);
        accountHighlightsPage.switchToIFrame();
        accountHighlightsPage.miscHighlightsWarningMessage.shouldHave(exactTextCaseSensitive(CRITICAL_ACCOUNT_BANNER_MESSAGE));
    }
}
