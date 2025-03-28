package leads.currency;

import com.aquiva.autotests.rc.model.leadConvert.Datasets;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Lead;
import leads.LeadConvertSteps;

import static base.Pages.leadConvertPage;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods related to checking the CurrencyIsoCode on the Opportunity
 * after the successful Lead Conversion.
 */
public class CurrencyIsoCodeLeadConvertSteps {
    public final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final LeadConvertSteps leadConvertSteps;

    /**
     * New instance for the class with the test methods/steps related to
     * checking the CurrencyIsoCode on the Opportunity after the successful Lead Conversion.
     *
     * @param index index number of the dataset in the object parsed JSON file with the test data
     *              (e.g. 0 for the 1st one, 1 for the 2nd one)
     */
    public CurrencyIsoCodeLeadConvertSteps(int index) {
        var datasets = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_USAndCanada_NB.json",
                Datasets.class);

        data = datasets.dataSets[index];
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        leadConvertSteps = new LeadConvertSteps(data);
    }

    /**
     * Test steps for checking CurrencyIsoCode on the Opportunity record after successful Lead Conversion.
     *
     * @param leadId ID of the lead to be converted
     */
    protected void currencyIsoCodeAfterLeadConvertTestSteps(String leadId) {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(leadId)
        );

        step("2. Switch the toggle into 'Create new account position'", () ->
                leadConvertPage.newExistingAccountToggle
                        .shouldBe(enabled, ofSeconds(60))
                        .click()
        );

        step("3. Click 'Edit' in Opportunity Section, select Country, check the Business Identity, " +
                "populate Close Date field and press 'Apply' button", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.countryPicklist.selectOption(data.getBillingCountry());
            leadConvertPage.businessIdentityPicklist.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(data.getBusinessIdentityName()));

            leadConvertPage.closeDateDatepicker.setTomorrowDate();

            leadConvertPage.opportunityInfoApplyButton.scrollIntoView(true).click();
        });

        step("4. Press 'Convert' button", () ->
                leadConvertSteps.pressConvertButton()
        );

        //  Check Opportunity record in DB only after Opportunity Record Page fully loads
        step("5. Check Opportunity.CurrencyIsoCode field after the Lead Conversion", () -> {
            var convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ConvertedOpportunity.CurrencyIsoCode " +
                            "FROM Lead " +
                            "WHERE Id = '" + leadId + "'",
                    Lead.class);
            assertThat(convertedLead.getConvertedOpportunity().getCurrencyIsoCode())
                    .as("ConvertedLead.ConvertedOpportunity.CurrencyIsoCode value")
                    .isEqualTo(data.getCurrencyIsoCode());
        });
    }
}
