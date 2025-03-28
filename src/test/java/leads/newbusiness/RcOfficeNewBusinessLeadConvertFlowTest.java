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
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.NEW_ACCOUNT_WILL_BE_CREATED_MESSAGE;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.TYPE_NEW_BUSINESS;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("PDV")
@Tag("LeadConvert")
@Tag("NGBS")
public class RcOfficeNewBusinessLeadConvertFlowTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Lead convertedLead;

    //  Test data
    private final String officeService;
    private final String forecastedOfficeUsers;
    private final String forecastedContactCenterUsers;
    private final String forecastedEngageVoiceUsers;
    private final String forecastedEngageDigitalUsers;
    private final String forecastedGlobalOfficeUsers;

    public RcOfficeNewBusinessLeadConvertFlowTest() {
        data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        officeService = data.packageFolders[0].name;
        forecastedOfficeUsers = "11";
        forecastedContactCenterUsers = "12";
        forecastedEngageVoiceUsers = "13";
        forecastedEngageDigitalUsers = "14";
        forecastedGlobalOfficeUsers = "10";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20773")
    @TmsLink("CRM-33038")
    @DisplayName("CRM-20773 - Lead Conversion - Sales Lead - Convert with a new account. " +
            "CRM-33038 - Forecasted Users fields on LCP are mapped to related fields on Opportunity after converting Lead")
    @Description("CRM-20773 - Verify that a Sales Lead can be converted creating a new account. " +
            "CRM-33038 - Verify that Forecasted Users fields on LCP are mapped to related fields on Opportunity after converting Lead")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Switch the toggle into 'Create New Account' position in Account Info section, " +
                "and check the section's elements", () -> {
            leadConvertPage.newExistingAccountToggle
                    .shouldBe(enabled, ofSeconds(60))
                    .click();

            leadConvertPage.accountInfoLabel.shouldHave(exactTextCaseSensitive(NEW_ACCOUNT_WILL_BE_CREATED_MESSAGE));
            leadConvertPage.newAccountName.shouldHave(exactTextCaseSensitive(steps.leadConvert.salesLead.getCompany()));
            leadConvertPage.newAccountType.shouldHave(exactTextCaseSensitive(TYPE_NEW_BUSINESS));
            leadConvertPage.accountInfoApplyButton.shouldBe(hidden);
        });

        //  CRM-20773
        step("3. Check the 'Opportunity' section", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();

            leadConvertPage.opportunityNameNonEditable
                    .shouldHave(exactTextCaseSensitive(steps.leadConvert.salesLead.getCompany()), ofSeconds(10));
            leadConvertPage.opportunityBrandNonEditable.shouldHave(exactTextCaseSensitive(data.brandName), ofSeconds(10));
            leadConvertPage.opportunityBusinessIdentityNonEditable
                    .shouldHave(exactTextCaseSensitive(data.getBusinessIdentityName()), ofSeconds(10));
            leadConvertPage.opportunityInfoEditButton.shouldBe(visible);
        });

        step("4. Click Edit button, select Service = 'Office', set Forecasted Office Users, Forecasted Contact Center Users, " +
                "Forecasted Engage Voice Users, Forecasted Engage Digital Users, " +
                "Forecasted Global Office Users and Close Date fields and click Apply button", () -> {
            leadConvertPage.opportunityInfoEditButton.click();
            leadConvertPage.selectService(officeService);

            leadConvertPage.forecastedUsersInput.setValue(forecastedOfficeUsers);
            leadConvertPage.forecastedContactCenterUsersInput.setValue(forecastedContactCenterUsers);
            leadConvertPage.forecastedEngageVoiceUsersInput.setValue(forecastedEngageVoiceUsers);
            leadConvertPage.forecastedEngageDigitalUsersInput.setValue(forecastedEngageDigitalUsers);
            leadConvertPage.forecastedGlobalOfficeUsersInput.setValue(forecastedGlobalOfficeUsers);

            leadConvertPage.closeDateDatepicker.setTomorrowDate();
            leadConvertPage.opportunityInfoApplyButton.click();
        });

        //  CRM-20773
        step("5. Check the 'Lead Qualification' section", () -> {
            leadConvertPage.leadQualificationSection.shouldBe(visible);
            leadConvertPage.selectedQualification.shouldBe(visible);
        });

        step("6. Press 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        //  CRM-20773
        step("7. Check that Lead is converted correctly and check 'Name' field on the Account and Opportunity records", () -> {
            convertedLead = steps.leadConvert.checkLeadConversion(steps.leadConvert.salesLead);

            var accountFromLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name " +
                            "FROM Account " +
                            "WHERE Id = '" + convertedLead.getConvertedAccountId() + "'",
                    Account.class);
            assertThat(accountFromLead.getName())
                    .as("Account.Name value")
                    .isEqualTo(steps.leadConvert.salesLead.getCompany());

            var opportunityFromLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Name " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + convertedLead.getConvertedOpportunityId() + "'",
                    Opportunity.class);
            assertThat(opportunityFromLead.getName())
                    .as("Opportunity.Name value")
                    .isEqualTo(steps.leadConvert.salesLead.getCompany());
        });

        //  CRM-33038
        step("8. Check that Forecasted_Users__c, Forecast_Contact_Center_Users__c, Forecast_Connect_First_Users__c, " +
                "ForcastedDimeloUsers__c, Forecasted_Global_Office_Users__c, Forecasted_RingCentral_Video_Users__c field values " +
                "are matched with the values set on the Lead Convert page", () -> {
            var opportunityFromLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Forecasted_Users__c, Forecast_Contact_Center_Users__c, Forecast_Connect_First_Users__c, " +
                            "ForcastedDimeloUsers__c, Forecasted_Global_Office_Users__c, Forecasted_RingCentral_Video_Users__c " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + convertedLead.getConvertedOpportunityId() + "'",
                    Opportunity.class);

            assertThat(opportunityFromLead.getForecasted_Users__c())
                    .as("Opportunity.Forecasted_Users__c value")
                    .isEqualTo(Integer.parseInt(forecastedOfficeUsers));
            assertThat(opportunityFromLead.getForecast_Contact_Center_Users__c())
                    .as("Opportunity.Forecast_Contact_Center_Users__c value")
                    .isEqualTo(Integer.parseInt(forecastedContactCenterUsers));
            assertThat(opportunityFromLead.getForecast_Connect_First_Users__c())
                    .as("Opportunity.Forecast_Connect_First_Users__c value")
                    .isEqualTo(Integer.parseInt(forecastedEngageVoiceUsers));
            assertThat(opportunityFromLead.getForcastedDimeloUsers__c())
                    .as("Opportunity.ForcastedDimeloUsers__c value")
                    .isEqualTo(Integer.parseInt(forecastedEngageDigitalUsers));
            assertThat(opportunityFromLead.getForecasted_Global_Office_Users__c())
                    .as("Opportunity.Forecasted_Global_Office_Users__c value")
                    .isEqualTo(Integer.parseInt(forecastedGlobalOfficeUsers));
            assertThat(opportunityFromLead.getForecasted_RingCentral_Video_Users__c())
                    .as("Opportunity.Forecasted_RingCentral_Video_Users__c value")
                    .isEqualTo(0);
        });
    }
}
