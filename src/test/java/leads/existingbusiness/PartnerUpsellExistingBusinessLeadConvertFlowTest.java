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
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("PDV")
@Tag("LeadConvert")
public class PartnerUpsellExistingBusinessLeadConvertFlowTest extends BaseTest {
    private final Steps steps;
    private final ExistingBusinessLeadConvertSteps existingBusinessLeadConvertSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String rcBusinessIdentity;

    public PartnerUpsellExistingBusinessLeadConvertFlowTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/existingbusiness/RC_MVP_Monthly_Contract_163058013.json",
                Dataset.class);
        steps = new Steps(data);
        existingBusinessLeadConvertSteps = new ExistingBusinessLeadConvertSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        rcBusinessIdentity = data.businessIdentity.name;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createPartnerAccountAndLead(salesRepUser);
        existingBusinessLeadConvertSteps.setUpSteps(steps.leadConvert.partnerLead, salesRepUser);
        steps.leadConvert.preparePartnerLeadTestSteps(existingBusinessLeadConvertSteps.existingBusinessAccount,
                existingBusinessLeadConvertSteps.existingBusinessAccountContact);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-10853")
    @DisplayName("CRM-10853 - Existing Business. Upsell. Partner Leads get converted with new opportunity creation")
    @Description("Verify that Partner Leads get converted")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.partnerLead.getId())
        );

        step("2. Select Existing Business Account (from the 'Matched Accounts' table) and click on 'Apply' button", () ->
                leadConvertPage.selectMatchedAccount(existingBusinessLeadConvertSteps.existingBusinessAccount.getId())
        );

        step("3. Check preselected Business Identity in the Opportunity Info section, click 'Edit' button, " +
                "populate Close Date field and click 'Apply' button", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();

            leadConvertPage.opportunityBusinessIdentityNonEditable
                    .shouldHave(exactTextCaseSensitive(rcBusinessIdentity), ofSeconds(60));

            leadConvertPage.opportunityInfoEditButton.click();
            leadConvertPage.closeDateDatepicker.setTomorrowDate();
            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("4. Press 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("5. Check that the Lead is converted; check the created Opportunity's Contact", () -> {
            steps.leadConvert.checkLeadConversion(steps.leadConvert.partnerLead);

            step("Check the created Opportunity Contact", () -> {
                var convertedOpportunity = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, Primary_Opportunity_Contact__c " +
                                "FROM Opportunity " +
                                "WHERE Id = '" + opportunityPage.getCurrentRecordId() + "'",
                        Opportunity.class);

                //  Account's Contact should be used as Contact for Opportunity
                assertThat(convertedOpportunity.getPrimary_Opportunity_Contact__c())
                        .as("Opportunity.Primary_Opportunity_Contact__c value")
                        .isEqualTo(existingBusinessLeadConvertSteps.existingBusinessAccountContact.getId());
            });
        });
    }
}
