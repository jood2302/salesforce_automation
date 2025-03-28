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

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.LeadFactory.createCustomerLeadInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.getPrimaryContactOnAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.APPROVAL_STATUS_PENDING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.KYC_APPROVAL_RECORD_TYPE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("Approval")
@Tag("IndiaMVP")
public class IndiaNewBusinessLeadConvertKycApprovalTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account newBusinessAccount;
    private User salesRepUser;

    //  Test data
    private final String businessIdentityName;
    private final String billingCountry;
    private final String packageFolder;

    public IndiaNewBusinessLeadConvertKycApprovalTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_India_Mumbai_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        businessIdentityName = data.getBusinessIdentityName();
        billingCountry = data.getBillingCountry();
        packageFolder = data.packageFolders[0].name;
    }

    @BeforeEach
    public void setUpTest() {
        salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-23894")
    @DisplayName("CRM-23894 - Automatically KYC Approval creation in Lead Convert")
    @Description("Verify that KYC Approval is created automatically in Lead Convert")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select 'Create a new account' toggle option", () ->
                leadConvertPage.newExistingAccountToggle.click()
        );

        step("3. Click 'Edit' in Opportunity Info Section, select Country, Business Identity and Close Date fields, " +
                "check Service picklist value and click 'Apply' button", () -> {
            leadConvertPage.opportunityInfoEditButton.shouldBe(visible, ofSeconds(60)).click();
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();

            leadConvertPage.countryPicklist.selectOption(billingCountry);
            leadConvertPage.businessIdentityPicklist.shouldBe(enabled, ofSeconds(20)).selectOption(businessIdentityName);
            leadConvertPage.closeDateDatepicker.setTomorrowDate();

            leadConvertPage.servicePickList.getSelectedOption().shouldHave(exactTextCaseSensitive(packageFolder), ofSeconds(20));

            leadConvertPage.opportunityInfoApplyButton.scrollIntoView(true).click();
        });

        step("4. Click 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("5. Check created KYC Approval", () -> {
            var convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ConvertedAccountId " +
                            "FROM Lead " +
                            "WHERE Id = '" + steps.leadConvert.salesLead.getId() + "'",
                    Lead.class);
            newBusinessAccount = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name " +
                            "FROM Account " +
                            "WHERE Id = '" + convertedLead.getConvertedAccountId() + "'",
                    Account.class);
            var kycApproval = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Name, Account__c, Status__c " +
                            "FROM Approval__c " +
                            "WHERE Account__c = '" + newBusinessAccount.getId() + "'",
                    Approval__c.class);

            assertThat(kycApproval.getStatus__c())
                    .as("Approval__c.Status__c value")
                    .isEqualTo(APPROVAL_STATUS_PENDING);
            assertThat(kycApproval.getAccount__c())
                    .as("Approval__c.Account__c value")
                    .isEqualTo(newBusinessAccount.getId());
            assertThat(kycApproval.getName())
                    .as("Approval__c.Name value")
                    .isEqualTo(format("%s - %s", KYC_APPROVAL_RECORD_TYPE, newBusinessAccount.getName()));
        });

        step("6. Create new Sales Lead, link it to the previously converted Contact, " +
                "and open Lead Convert page for it", () -> {
            var newSalesLead = createCustomerLeadInSFDC(salesRepUser);

            //  to be able to select the existing account from the 'Matched Accounts' list instead of flaky Account Lookup
            step("Set the same email on the new Sales Lead and the previously converted Contact via API", () -> {
                var newBusinessAccountContact = getPrimaryContactOnAccount(newBusinessAccount);
                newSalesLead.setEmail(newBusinessAccountContact.getEmail());
                enterpriseConnectionUtils.update(newSalesLead);
            });

            leadConvertPage.openPage(newSalesLead.getId());
        });

        step("7. Select previously converted Account (from the 'Matched Accounts' table) " +
                "and click on 'Apply' button in the Account section", () ->
                leadConvertPage.selectMatchedAccount(newBusinessAccount.getId())
        );

        step("8. Click on 'Edit' in Opportunity Info Section and select 'Create New Opportunity' option", () -> {
            leadConvertPage.opportunityInfoEditButton.shouldBe(visible, ofSeconds(60)).click();
            leadConvertPage.opportunityCreateNewOppOption.click();
        });

        step("9. Click on 'Edit' in Opportunity Info Section, " +
                "select Business Identity and Close Date, and click on 'Apply' button", () -> {
            leadConvertPage.opportunityInfoEditButton.click();
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();

            leadConvertPage.businessIdentityPicklist.shouldBe(enabled, ofSeconds(20)).selectOption(businessIdentityName);
            leadConvertPage.servicePickList.getSelectedOption().shouldHave(exactTextCaseSensitive(packageFolder), ofSeconds(20));
            leadConvertPage.closeDateDatepicker.setTomorrowDate();

            leadConvertPage.opportunityInfoApplyButton.scrollIntoView(true).click();
        });

        step("10. Select default Opportunity role",
                leadConvertPage::selectDefaultOpportunityRole
        );

        step("11. Click 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("12. Check number of created KYC Approvals on Account", () -> {
            var approvalsOnAccount = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Approval__c " +
                            "WHERE Account__c = '" + newBusinessAccount.getId() + "'",
                    Approval__c.class);

            assertThat(approvalsOnAccount.size())
                    .as("Number of Approval__c records on the Account")
                    .isEqualTo(1);
        });
    }
}
