package leads.existingbusiness;

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
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LeadHelper.UK_BILLING_COUNTRY;
import static com.codeborne.selenide.Condition.enabled;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("NGBS")
@Tag("LeadConvert")
@Tag("Vodafone")
public class VodafonePartnerLeadExistingBusinessLeadConvertTest extends BaseTest {
    private final Steps steps;
    private final ExistingBusinessLeadConvertSteps existingBusinessLeadConvertSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account existingBusinessAccount;
    private Lead convertedLead;

    //  Test data
    private final String forecastedUsers;
    private final String vodafoneBrandName;
    private final String tierName;
    private final Integer dlExistingQuantity;

    public VodafonePartnerLeadExistingBusinessLeadConvertTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/existingbusiness/Vodafone_Office_Monthly_NonContract_129408013.json",
                Dataset.class);
        steps = new Steps(data);
        existingBusinessLeadConvertSteps = new ExistingBusinessLeadConvertSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        forecastedUsers = data.forecastedUsers;
        vodafoneBrandName = data.brandName;
        tierName = data.packageFolders[0].name;
        dlExistingQuantity = data.getProductByDataName("LC_DL-UNL_50").existingQuantity;
    }

    @BeforeEach
    public void setUpTest() {
        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.leadConvert.createPartnerAccountAndLead(dealDeskUser);
        existingBusinessLeadConvertSteps.setUpSteps(steps.leadConvert.partnerLead, dealDeskUser);

        existingBusinessAccount = existingBusinessLeadConvertSteps.existingBusinessAccount;

        steps.leadConvert.preparePartnerLeadTestSteps(existingBusinessAccount,
                existingBusinessLeadConvertSteps.existingBusinessAccountContact);

        step("Set Country value to 'United Kingdom' for the test Partner Lead via API", () -> {
            steps.leadConvert.partnerLead.setCountry__c(UK_BILLING_COUNTRY);
            enterpriseConnectionUtils.update(steps.leadConvert.partnerLead);
        });

        step("Set RC_Brand__c = 'Vodafone Business with RingCentral' and BillingCountry = 'United Kingdom' " +
                "for the Existing Business Account via API", () -> {
            existingBusinessAccount.setRC_Brand__c(vodafoneBrandName);
            existingBusinessAccount.setBillingCountry(UK_BILLING_COUNTRY);
            enterpriseConnectionUtils.update(existingBusinessAccount);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-24597")
    @DisplayName("CRM-24597 - Partner Lead Conversion for Vodafone (existing Account - Existing Business)")
    @Description("Verify that Partner Lead for Vodafone can be converted with Existing Business Account")
    public void test() {
        step("1. Open Lead Convert page for the test Partner Lead", () -> {
            leadConvertPage.openPage(steps.leadConvert.partnerLead.getId());
        });

        step("2. Select Existing Business account (from the 'Matched Account' table) " +
                "and click 'Apply' button in the Account Info section", () ->
                leadConvertPage.selectMatchedAccount(existingBusinessAccount.getId())
        );

        step("3. Click 'Edit' in the Opportunity info section, populate Forecasted users and Close Date fields, " +
                "and click 'Apply' button", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.forecastedUsersInput.shouldBe(enabled, ofSeconds(10)).setValue(forecastedUsers);
            leadConvertPage.closeDateDatepicker.setTomorrowDate();

            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("4. Click 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("5. Check that Lead is converted and Opportunity, Account and Contact are created", () ->
                steps.leadConvert.checkLeadConversion(steps.leadConvert.partnerLead)
        );

        step("6. Check that the Account selected on the Lead Convert page is the Lead's converted Account", () -> {
            convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ConvertedOpportunityId, ConvertedAccountId " +
                            "FROM Lead " +
                            "WHERE Id = '" + steps.leadConvert.partnerLead.getId() + "'",
                    Lead.class);

            assertThat(convertedLead.getConvertedAccountId())
                    .as("ConvertedLead.ConvertedAccountId value")
                    .isEqualTo(existingBusinessAccount.getId());
        });

        step("7. Check that Opportunity Name, Brand, Service and Forecasted Users fields " +
                "are populated with the same values, as were selected on the Lead Convert page", () -> {
            var opportunityFromConvertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name, Brand_Name__c, Tier_Name__c, Forecasted_Users__c " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + convertedLead.getConvertedOpportunityId() + "'",
                    Opportunity.class);

            assertThat(opportunityFromConvertedLead.getName())
                    .as("Opportunity.Name value")
                    .isEqualTo(existingBusinessAccount.getName());

            assertThat(opportunityFromConvertedLead.getBrand_Name__c())
                    .as("Opportunity.Brand_Name__c value")
                    .isEqualTo(vodafoneBrandName);

            assertThat(opportunityFromConvertedLead.getTier_Name__c())
                    .as("Opportunity.Tier_Name__c value")
                    .isEqualTo(tierName);

            var expectedForecastedUsers = Integer.parseInt(forecastedUsers) - dlExistingQuantity;
            assertThat(doubleToInteger(opportunityFromConvertedLead.getForecasted_Users__c()))
                    .as("Opportunity.Forecasted_Users__c value")
                    .isEqualTo(expectedForecastedUsers);
        });
    }
}
