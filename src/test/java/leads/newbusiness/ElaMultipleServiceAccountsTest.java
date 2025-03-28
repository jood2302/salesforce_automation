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
import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToIntToString;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.BI_FORMAT;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("ELALeads")
public class ElaMultipleServiceAccountsTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Lead convertedLead;
    private Account firstServiceAccount;
    private Account secondServiceAccount;

    //  Test data
    private final String currencyIsoCode;
    private final String ringCentralBrand;
    private final String ringCentralBiName;
    private final String ringCentralBiId;

    private final String numberOfElaAccounts;
    private final String numberOfForecastedUsers;

    public ElaMultipleServiceAccountsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        currencyIsoCode = data.currencyISOCode;
        ringCentralBrand = data.brandName;
        ringCentralBiName = data.businessIdentity.name;
        ringCentralBiId = data.businessIdentity.id;

        numberOfElaAccounts = "2";
        numberOfForecastedUsers = "12";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-22225")
    @DisplayName("CRM-22225 - Creating Opportunities and Quotes for Service Accounts after Lead Conversion")
    @Description("Verify that after Lead Conversion where ELA = true will be created Service Accounts with " +
            "and all Accounts will have Forecasted Users as it was set on Lead Convert")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Switch the toggle into 'Create New Account' position in Account Info section, " +
                "enable ELA, set number of ELA Accounts and click 'Apply' button", () -> {
            leadConvertPage.newExistingAccountToggle.shouldBe(enabled, ofSeconds(60)).click();
            leadConvertPage.accountInfoEditButton.click();

            leadConvertPage.elaCheckbox.click();
            leadConvertPage.elaServiceAccountsNumberInput.setValue(numberOfElaAccounts);
            leadConvertPage.accountInfoApplyButton.click();
        });

        step("3. Click 'Edit' in Opportunity Section and check that Business Identity = 'RingCentral Inc.'", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.businessIdentityPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(ringCentralBiName));
        });

        step("4. Set number of forecasted users and Close Date fields, and click 'Apply' in Opportunity Info section", () -> {
            leadConvertPage.forecastedUsersInput.shouldBe(editable, ofSeconds(10)).setValue(numberOfForecastedUsers);
            leadConvertPage.closeDateDatepicker.setTomorrowDate();

            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("5. Press 'Convert' button", () -> {
            leadConvertPage.convertButton.click();

            //  this is a particularly long conversion, that's why LeadConvertSteps.pressConvertButton() is not suitable
            opportunityPage.entityTitle.shouldBe(visible, ofSeconds(240));
            opportunityPage.waitUntilLoaded();
        });

        step("6. Check that Billable Account and Opportunity were created correctly after Lead conversion", () -> {
            convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ConvertedAccountId, ConvertedOpportunity.Id, ConvertedAccount.Name, " +
                            "ConvertedOpportunity.Brand_Name__c, ConvertedOpportunity.Forecasted_Users__c, " +
                            "ConvertedOpportunity.BusinessIdentity__c " +
                            "FROM Lead " +
                            "WHERE Id = '" + steps.leadConvert.salesLead.getId() + "'",
                    Lead.class);

            stepCheckFieldsOnOpportunityViaApi(convertedLead.getConvertedOpportunity());

            step("Check Billable Account after Lead Conversion", () -> {
                assertThat(convertedLead.getConvertedAccountId())
                        .as("ConvertedLead.ConvertedAccountId value")
                        .isNotNull();

                assertThat(convertedLead.getConvertedAccount().getName())
                        .as("ConvertedLead.ConvertedAccount.Name value")
                        .contains("Billable");
            });
        });

        step("7. Check that correct number of Service Accounts were created", () -> {
            var serviceAccounts = enterpriseConnectionUtils.query(
                    "SELECT Id, Child_Account__r.Id, Child_Account__r.Name " +
                            "FROM AccountRelation__c " +
                            "WHERE Parent_Account__c = '" + convertedLead.getConvertedAccountId() + "'",
                    AccountRelation__c.class);
            assertThat(serviceAccounts.size())
                    .as("Number of 'AccountRelation__c' records (Service Accounts) " +
                            "linked with the converted account (Parent)")
                    .isEqualTo(Integer.parseInt(numberOfElaAccounts));

            firstServiceAccount = serviceAccounts.get(0).getChild_Account__r();
            secondServiceAccount = serviceAccounts.get(1).getChild_Account__r();
        });

        step("8. Check that Service Accounts doesn't contain 'Billable' in their names", () -> {
            assertThat(firstServiceAccount.getName())
                    .as("First Service Account.Name value")
                    .doesNotContain("Billable");

            assertThat(secondServiceAccount.getName())
                    .as("Second Service Account.Name value")
                    .doesNotContain("Billable");
        });

        step("9. Check that first Service Opportunity was created correctly after Lead conversion", () -> {
            var firstServiceOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name, Brand_Name__c, Forecasted_Users__c, BusinessIdentity__c " +
                            "FROM Opportunity " +
                            "WHERE AccountId = '" + firstServiceAccount.getId() + "'",
                    Opportunity.class);

            stepCheckFieldsOnOpportunityViaApi(firstServiceOpportunity);
        });

        step("10. Check that second Service Opportunity was created correctly after Lead conversion", () -> {
            var secondServiceOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name, Brand_Name__c, Forecasted_Users__c, BusinessIdentity__c " +
                            "FROM Opportunity " +
                            "WHERE AccountId = '" + secondServiceAccount.getId() + "'",
                    Opportunity.class);

            stepCheckFieldsOnOpportunityViaApi(secondServiceOpportunity);
        });
    }

    /**
     * Check that converted Opportunity fields are populated correctly after Lead conversion.
     *
     * @param opportunity Opportunity to be checked
     */
    private void stepCheckFieldsOnOpportunityViaApi(Opportunity opportunity) {
        step("Check that Business Identity, Brand and Forecasted users fields are populated with values from Converted Lead", () -> {
            var expectedBusinessIdentityValue = String.format(BI_FORMAT, currencyIsoCode, ringCentralBiId);

            assertThat(opportunity.getId())
                    .as("Opportunity.Id value")
                    .isNotNull();
            assertThat(opportunity.getBusinessIdentity__c())
                    .as("Opportunity.BusinessIdentity__c value")
                    .isEqualTo(expectedBusinessIdentityValue);
            assertThat(opportunity.getBrand_Name__c())
                    .as("Opportunity.Brand_Name__c value")
                    .isEqualTo(ringCentralBrand);
            assertThat(doubleToIntToString(opportunity.getForecasted_Users__c()))
                    .as("Opportunity.Forecasted_Users__c value")
                    .isEqualTo(numberOfForecastedUsers);
        });
    }
}
