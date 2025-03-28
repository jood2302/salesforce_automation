package leads.leadconvertpage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.NONE_BUSINESS_IDENTITY;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.NO_MATCHING_BUSINESS_IDENTITY_WAS_FOUND;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.SYSTEM_ADMINISTRATOR_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("Ignite")
public class DefaultBusinessIdentityNoCountryLeadConvertPageTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String rcUsBrandName;
    private final String rcCanadaBrandName;
    private final String countryWithoutDefaultBiMappingRecord;

    public DefaultBusinessIdentityNoCountryLeadConvertPageTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        rcUsBrandName = data.brandName;
        rcCanadaBrandName = "RingCentral Canada";
        countryWithoutDefaultBiMappingRecord = "Egypt";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.leadConvert.createPartnerAccountAndLead(salesRepUser);

        step("Set Partner Account.Permitted_Brands__c = '" + rcCanadaBrandName + "' via API", () -> {
            steps.leadConvert.partnerAccount.setPermitted_Brands__c(rcCanadaBrandName);
            enterpriseConnectionUtils.update(steps.leadConvert.partnerAccount);
        });

        step("Set the Partner Lead.LeadPartnerID__c field value to the Partner Account.Partner_ID__c, " +
                "Country__c = '" + countryWithoutDefaultBiMappingRecord + "' " +
                "and Lead_Brand_Name__c = '" + rcUsBrandName + "' via API", () -> {
            steps.leadConvert.partnerLead.setCountry__c(countryWithoutDefaultBiMappingRecord);
            steps.leadConvert.partnerLead.setLead_Brand_Name__c(rcUsBrandName);
            steps.leadConvert.partnerLead.setLeadPartnerID__c(steps.leadConvert.partnerAccount.getPartner_ID__c());
            enterpriseConnectionUtils.update(steps.leadConvert.partnerLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33613")
    @DisplayName("CRM-33613 - Business Identity field on Partner Lead's Lead Conversion Page populated by 'None' " +
            "if Lead's country is missing in 'Default Business Identity Mappings' Custom MetaData")
    @Description("Verify that on Partner Lead's Conversion Page Business Identity field filled by 'None' " +
            "if Lead's Country is missing in 'Default Business Identity Mappings' Custom MetaData")
    public void test() {
        step("1. Open Lead Convert page for the test Partner Lead with brand = " + rcCanadaBrandName + ", " +
                "check that the Business Identity picklist is enabled and has preselected value = '" + NONE_BUSINESS_IDENTITY + "', " +
                "check that 'Convert' button is disabled and has a correct tooltip message on hover", () ->
                checkBusinessIdentityPicklistOnLeadConvertPageTestSteps(steps.leadConvert.partnerLead.getId())
        );

        step("2. Login as a user with 'System Administrator' profile", () -> {
            var sysAdminUser = getUser().withProfile(SYSTEM_ADMINISTRATOR_PROFILE).execute();
            steps.sfdc.reLoginAsUser(sysAdminUser);
        });

        step("3. Open Lead Convert page for the test Partner Lead with brand = " + rcCanadaBrandName + ", " +
                "check that the Business Identity picklist is enabled and has preselected value = '" + NONE_BUSINESS_IDENTITY + "'" +
                "check that 'Convert' button is disabled and has a correct tooltip message on hover", () -> {
            checkBusinessIdentityPicklistOnLeadConvertPageTestSteps(steps.leadConvert.partnerLead.getId());
        });
    }

    /**
     * <p> - Open Lead Convert page for the test Partner Lead </p>
     * <p> - Switch the toggle into 'Create new Account' position </p>
     * <p> - Check preselected value of Business Identity picklist and that it's enabled </p>
     * <p> - Check that the 'Convert' button is disabled and its tooltip message on hover </p>
     *
     * @param partnerLeadID a Partner Lead ID to convert
     */
    private void checkBusinessIdentityPicklistOnLeadConvertPageTestSteps(String partnerLeadID) {
        step("Open Lead Convert page for the Partner Lead", () ->
                leadConvertPage.openPage(partnerLeadID)
        );

        step("Switch the toggle into 'Create New Account' position", () ->
                leadConvertPage.newExistingAccountToggle.click()
        );

        step("Click 'Edit' in Opportunity Section, " +
                "check that Business Identity picklist is enabled and has preselected value = '" + NONE_BUSINESS_IDENTITY + "'", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.businessIdentityPicklist
                    .shouldBe(enabled)
                    .getSelectedOption().shouldHave(exactTextCaseSensitive(NONE_BUSINESS_IDENTITY));
        });

        step("Check that the 'Convert' button is disabled, hover over it, and check the displayed tooltip for it", () -> {
            leadConvertPage.convertButton.shouldBe(disabled);
            leadConvertPage.convertButton.hover();
            leadConvertPage.convertButtonTooltip.shouldHave(exactTextCaseSensitive(NO_MATCHING_BUSINESS_IDENTITY_WAS_FOUND), ofSeconds(10));
        });
    }
}
