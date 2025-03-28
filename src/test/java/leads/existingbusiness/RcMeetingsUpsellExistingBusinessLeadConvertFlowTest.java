package leads.existingbusiness;

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
import static base.Pages.opportunityPage;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("LeadConvert")
public class RcMeetingsUpsellExistingBusinessLeadConvertFlowTest extends BaseTest {
    private final Steps steps;
    private final ExistingBusinessLeadConvertSteps existingBusinessLeadConvertSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String rcBrand;

    public RcMeetingsUpsellExistingBusinessLeadConvertFlowTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/existingbusiness/RC_Meetings_Monthly_Contract_82666013.json",
                Dataset.class);
        steps = new Steps(data);
        existingBusinessLeadConvertSteps = new ExistingBusinessLeadConvertSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        rcBrand = data.brandName;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);
        existingBusinessLeadConvertSteps.setUpSteps(steps.leadConvert.salesLead, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-13033")
    @DisplayName("CRM-13033 - RC Meetings Lead Conversion for Existing Business.")
    @Description("Verify that Leads can be converted into Opportunities with RC Meetings")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select Existing Business Account (from the 'Matched Accounts' table) and click on 'Apply' button", () ->
                leadConvertPage.selectMatchedAccount(existingBusinessLeadConvertSteps.existingBusinessAccount.getId())
        );

        step("3. Click 'Edit' button in the Opportunity Info section, select Close Date field " +
                "and click 'Apply' button in Opportunity Info section", () -> {
            //  Additional wait for correct Opportunity role selection
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();

            leadConvertPage.opportunityInfoEditButton.click();
            leadConvertPage.closeDateDatepicker.setTomorrowDate();
            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("4. Select Contact Role and click 'Apply' button in Contact Role section", () -> {
            leadConvertPage.selectDefaultOpportunityRole();
        });

        step("5. Press 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("6. Check that Lead is converted, and Opportunity is created with correct brand", () -> {
            steps.leadConvert.checkLeadConversion(steps.leadConvert.salesLead);

            step("Check Brand on the created Opportunity", () -> {
                var convertedOpportunity = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, Brand_Name__c " +
                                "FROM Opportunity " +
                                "WHERE Id = '" + opportunityPage.getCurrentRecordId() + "'",
                        Opportunity.class);

                assertThat(convertedOpportunity.getBrand_Name__c())
                        .as("Opportunity.Brand_Name__c value")
                        .isEqualTo(rcBrand);
            });
        });
    }
}
