package leads.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Opportunity;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Lead")
@Tag("Fields")
@Tag("LeadConvert")
@Tag("Opportunity")
@Tag("OpportunityCreation")
public class RcMeetingsForecastedUsersFieldsLeadConvertTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String usBillingCountry;
    private final String rcMeetingsServiceName;
    private final String forecastedRcVideoUsers;

    public RcMeetingsForecastedUsersFieldsLeadConvertTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_Meetings_Phoenix_Annual_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        usBillingCountry = data.getBillingCountry();
        rcMeetingsServiceName = data.packageFolders[0].name;
        forecastedRcVideoUsers = "15";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33069")
    @DisplayName("CRM-33069 - Meetings Forecasted Users fields on LCP are mapped to related fields on Opportunity after converting Lead")
    @Description("Verify that Forecasted Users fields on LCP are mapped to related fields on Opportunity after converting Lead")
    public void test() {
        step("1. Open Lead Convert page for the test Sales Lead, " + 
                "and switch the toggle into 'Create New Account' position in the Account Info section", () -> {
            leadConvertPage.openPage(steps.leadConvert.salesLead.getId());

            leadConvertPage.newExistingAccountToggle.click();
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
        });

        step("2. Click 'Edit' in Opportunity Section, select Country = '" + usBillingCountry + "', Service = '" + rcMeetingsServiceName + "', " +
                "populate Forecasted RingCentral Video Users and Close Date, and click the 'Apply' button", () -> {
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.countryPicklist.setValue(usBillingCountry);
            leadConvertPage.selectService(rcMeetingsServiceName);
            leadConvertPage.forecastedRCVideoUsersInput.setValue(forecastedRcVideoUsers);
            leadConvertPage.closeDateDatepicker.setTomorrowDate();

            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("3. Click 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("4. Check that the Lead has been converted, and check that converted Opportunity's " +
                "Forecasted_RingCentral_Video_Users__c field value is matched with the value set on the Lead Convert page " +
                "and check that Forecasted_Users__c, Forecast_Contact_Center_Users__c, Forecast_Connect_First_Users__c, " +
                "ForcastedDimeloUsers__c, Forecasted_Global_Office_Users__c field values are equal to 0", () -> {
            var convertedLead = steps.leadConvert.checkLeadConversion(steps.leadConvert.salesLead);
            var opportunityFromLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Forecasted_Users__c, Forecast_Contact_Center_Users__c, Forecast_Connect_First_Users__c, " +
                            "ForcastedDimeloUsers__c, Forecasted_Global_Office_Users__c, Forecasted_RingCentral_Video_Users__c " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + convertedLead.getConvertedOpportunityId() + "' ",
                    Opportunity.class);

            assertThat(opportunityFromLead.getForecasted_Users__c())
                    .as("Opportunity.Forecasted_Users__c value")
                    .isEqualTo(0);
            assertThat(opportunityFromLead.getForecast_Contact_Center_Users__c())
                    .as("Opportunity.Forecast_Contact_Center_Users__c value")
                    .isEqualTo(0);
            assertThat(opportunityFromLead.getForecast_Connect_First_Users__c())
                    .as("Opportunity.Forecast_Connect_First_Users__c value")
                    .isEqualTo(0);
            assertThat(opportunityFromLead.getForcastedDimeloUsers__c())
                    .as("Opportunity.ForcastedDimeloUsers__c value")
                    .isEqualTo(0);
            assertThat(opportunityFromLead.getForecasted_Global_Office_Users__c())
                    .as("Opportunity.Forecasted_Global_Office_Users value")
                    .isEqualTo(0);
            assertThat(opportunityFromLead.getForecasted_RingCentral_Video_Users__c())
                    .as("Opportunity.Forecasted_RingCentral_Video_Users__c value")
                    .isEqualTo(Integer.parseInt(forecastedRcVideoUsers));
        });
    }
}
