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
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.LeadFactory.createPartnerLeadInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.SubBrandsMappingFactory.createNewSubBrandsMapping;
import static com.codeborne.selenide.Condition.enabled;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("CustomSetting")
@Tag("Ignite")
@Tag("LeadConvert")
public class SubBrandPopulationOnOpportunityLeadConvertTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private SubBrandsMapping__c testSubBrandsMapping;
    private Lead secondPartnerLead;

    //  Test data
    private final String brandName;
    private final String tierName;

    public SubBrandPopulationOnOpportunityLeadConvertTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        brandName = data.brandName;
        tierName = data.packageFolders[0].name;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.leadConvert.createPartnerAccountAndLead(salesRepUser);

        step("Set Partner Account.RC_Brand__c = '" + brandName + "' via API", () -> {
            steps.leadConvert.partnerAccount.setRC_Brand__c(brandName);
            enterpriseConnectionUtils.update(steps.leadConvert.partnerAccount);
        });

        step("Create a new SubBrandsMapping__c Custom Setting record for the Partner Account via API", () -> {
            testSubBrandsMapping = createNewSubBrandsMapping(steps.leadConvert.partnerAccount);
        });

        step("Create another test Partner Lead via API", () -> {
            secondPartnerLead = createPartnerLeadInSFDC(salesRepUser, steps.leadConvert.partnerAccount, tierName);
        });

        step("Set both Partner Leads' LeadPartnerID__c field value to the Partner Account.Partner_ID__c via API", () -> {
            steps.leadConvert.partnerLead.setLeadPartnerID__c(steps.leadConvert.partnerAccount.getPartner_ID__c());
            secondPartnerLead.setLeadPartnerID__c(steps.leadConvert.partnerAccount.getPartner_ID__c());
            enterpriseConnectionUtils.update(steps.leadConvert.partnerLead, secondPartnerLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33520")
    @DisplayName("CRM-33520 - Sub-brand field on Opportunity is populated after Opportunity was created from Partner Lead Convert")
    @Description("Verify that Sub-brand field on Opportunity is populated after Opportunity was created from Partner Lead Convert " +
            "according to Partner ID of Partner Account and 'Sub-brands Mapping' Custom Setting")
    public void test() {
        step("1. Open Lead Convert page for the test Partner Lead, select New Account creating option, " +
                "convert the Lead, check that it's converted and check that created Opportunity.Sub_Brand__c field " +
                "is populated with related SubBrandsMapping__c.Sub_Brand__c field value", () ->
                partnerLeadConvertSubBrandMappingTestSteps(steps.leadConvert.partnerLead, testSubBrandsMapping.getSub_Brand__c())
        );

        step("2. Delete test SubBrandsMapping__c record via API", () -> {
            enterpriseConnectionUtils.delete(testSubBrandsMapping);
        });

        step("3. Open Lead Convert page for the second test Partner Lead, select New Account creating option, " +
                "convert the Lead, check that it's converted and check that created Opportunity.Sub_Brand__c field is empty", () ->
                partnerLeadConvertSubBrandMappingTestSteps(secondPartnerLead, null)
        );
    }

    /**
     * <p> - Open Lead Convert page for the test Partner Lead </p>
     * <p> - Switch the toggle into 'Create new Account' position </p>
     * <p> - Convert the Lead and check that it's converted </p>
     * <p> - Check that the created Opportunity.Sub_Brand__c field value
     * is equal to its expected value. </p>
     *
     * @param partnerLead           a Partner Lead to convert
     * @param expectedSubBrandValue an expected value of created Opportunity.Sub_Brand__c field
     *                              (related SubBrandsMapping__c.Sub_Brand__c value
     *                              or null if related SubBrandsMapping__c doesn't exist)
     */
    private void partnerLeadConvertSubBrandMappingTestSteps(Lead partnerLead, String expectedSubBrandValue) {
        step("Open Lead Convert page for the test Partner Lead", () -> {
            leadConvertPage.openPage(partnerLead.getId());
        });

        step("Switch the toggle into 'Create New Account' position", () ->
                leadConvertPage.newExistingAccountToggle.shouldBe(enabled, ofSeconds(60)).click()
        );

        step("Click 'Edit' button in the Opportunity Section, select 'Office' for Service picklist, " +
                "populate Close Date field and click 'Apply' button", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.selectService(tierName);
            leadConvertPage.closeDateDatepicker.setTomorrowDate();
            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("Press 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("Check that the Lead is converted", () ->
                steps.leadConvert.checkLeadConversion(partnerLead)
        );

        var expectedSubBrandValueForStep = expectedSubBrandValue == null
                ? "blank"
                : "equal to the related (via Partner Account) SubBrandsMapping__c.Sub_Brand__c field value";
        step("Check that created Opportunity.Sub_Brand__c field is " + expectedSubBrandValueForStep, () -> {
            var convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ConvertedOpportunityId " +
                            "FROM Lead " +
                            "WHERE Id = '" + partnerLead.getId() + "'",
                    Lead.class);
            var createdOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Sub_Brand__c " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + convertedLead.getConvertedOpportunityId() + "'",
                    Opportunity.class);
            assertThat(createdOpportunity.getSub_Brand__c())
                    .as("Opportunity.Sub_Brand__c value")
                    .isEqualTo(expectedSubBrandValue);
        });
    }
}
