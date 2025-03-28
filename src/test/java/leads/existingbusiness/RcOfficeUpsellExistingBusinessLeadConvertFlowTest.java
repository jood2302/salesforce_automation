package leads.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Opportunity;
import com.sforce.soap.enterprise.sobject.OpportunityContactRole;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.INFLUENCER_OPPORTUNITY_CONTACT_ROLE;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToInteger;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
@Tag("LeadConvert")
@Tag("PackageTab")
@Tag("PriceTab")
@Tag("QuoteTab")
public class RcOfficeUpsellExistingBusinessLeadConvertFlowTest extends BaseTest {
    private final Steps steps;
    private final ExistingBusinessLeadConvertSteps existingBusinessLeadConvertSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String convertedOpportunityId;

    //  Test data
    private final String forecastedUsersToBeSet;
    private final String rcBrand;
    private final String officeService;
    private final Integer dlExistingQuantity;

    public RcOfficeUpsellExistingBusinessLeadConvertFlowTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/existingbusiness/RC_MVP_Monthly_Contract_163058013.json",
                Dataset.class);
        steps = new Steps(data);
        existingBusinessLeadConvertSteps = new ExistingBusinessLeadConvertSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        forecastedUsersToBeSet = data.forecastedUsers;
        rcBrand = data.brandName;
        officeService = data.packageFolders[0].name;
        dlExistingQuantity = data.getProductByDataName("LC_DL-UNL_50").existingQuantity;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);
        existingBusinessLeadConvertSteps.setUpSteps(steps.leadConvert.salesLead, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-10769")
    @TmsLink("CRM-21406")
    @DisplayName("CRM-10769 - Existing Business. Sales Lead Conversion. \n" +
            "CRM-21406 - Existing Business | Lead Convert | No buttons are enabled after Upsell Quote creation.")
    @Description("CRM-10769 - Verify that Lead Conversion is functional with Opportunity with NGBS Quote. \n" +
            "CRM-21406 - Verify that Save/Discard buttons behave correctly. No buttons are enabled after Lead Convert Upsell")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select Existing Business Account (from the 'Matched Accounts' table) and click on 'Apply' button", () ->
                leadConvertPage.selectMatchedAccount(existingBusinessLeadConvertSteps.existingBusinessAccount.getId())
        );

        step("3. Click 'Edit' in the Opportunity section, increase the number of users, populate Close Date field " +
                "and click 'Apply' button", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.forecastedUsersInput
                    .shouldHave(exactValue(valueOf(dlExistingQuantity)), ofSeconds(30))
                    .shouldBe(enabled, ofSeconds(30))
                    .setValue(forecastedUsersToBeSet);
            leadConvertPage.closeDateDatepicker.setTomorrowDate();

            leadConvertPage.opportunityInfoApplyButton.scrollIntoView(true).click();
        });

        step("4. Select Contact Role and click 'Apply' button in Contact Role section",
                leadConvertPage::selectDefaultOpportunityRole
        );

        step("5. Press 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        //  CRM-21406
        step("6. Switch to the Quote Wizard on the Opportunity record page, " +
                "and create a new Sales Quote with the package from the NGBS account", () -> {
            opportunityPage.switchToNGBSQW();
            steps.quoteWizard.addNewSalesQuote();
            packagePage.saveChanges();
        });

        step("7. Switch between Price and Quote Details tabs, " +
                "verify that switching performs without any confirmation windows " +
                "and 'Save Changes' button is disabled", () -> {
            cartPage.openTab();
            cartPage.saveButton.shouldBe(disabled);

            quotePage.openTab();
            quotePage.saveButton.shouldBe(disabled);
        });

        //  CRM-10769
        step("8. Check that Lead is converted; " +
                "Check Name, Brand, Service (Tier_Name__c), Forecasted Users fields on the converted Opportunity; " +
                "Check the related OpportunityContactRole record", () -> {
            steps.leadConvert.checkLeadConversion(steps.leadConvert.salesLead);

            step("Check fields on the converted Opportunity", () -> {
                switchTo().window(0);

                convertedOpportunityId = opportunityPage.getCurrentRecordId();
                var convertedOpportunity = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, Name, Brand_Name__c, Tier_Name__c, Forecasted_Users__c, Primary_Opportunity_Contact__c " +
                                "FROM Opportunity " +
                                "WHERE Id = '" + convertedOpportunityId + "'",
                        Opportunity.class);

                assertThat(convertedOpportunity.getName())
                        .as("Opportunity.Name value")
                        .isEqualTo(existingBusinessLeadConvertSteps.existingBusinessAccount.getName());

                assertThat(convertedOpportunity.getBrand_Name__c())
                        .as("Opportunity.Brand_Name__c value")
                        .isEqualTo(rcBrand);

                assertThat(convertedOpportunity.getTier_Name__c())
                        .as("Opportunity.Tier_Name__c value")
                        .isEqualTo(officeService);

                var expectedForecastedUsers = Integer.parseInt(forecastedUsersToBeSet) - dlExistingQuantity;
                assertThat(doubleToInteger(convertedOpportunity.getForecasted_Users__c()))
                        .as("Opportunity.Forecasted_Users__c value")
                        .isEqualTo(expectedForecastedUsers);
            });

            step("Check the OpportunityContactRole record after Lead Conversion", () -> {
                var opportunityContactRole = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, Role " +
                                "FROM OpportunityContactRole " +
                                "WHERE OpportunityId = '" + convertedOpportunityId + "' " +
                                "AND ContactId = '" + existingBusinessLeadConvertSteps.existingBusinessAccountContact.getId() + "'",
                        OpportunityContactRole.class);
                assertThat(opportunityContactRole.getRole())
                        .as("OpportunityContactRole.Role value")
                        .isEqualTo(INFLUENCER_OPPORTUNITY_CONTACT_ROLE);
            });
        });
    }
}
