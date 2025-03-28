package leads.leadconvertpage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.SubBrandsMapping__c;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.DefaultBusinessIdentityMappingFactory.createDefaultBusinessIdentityMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.SubBrandsMappingFactory.createNewSubBrandsMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.AMAZON_US_BUSINESS_IDENTITY_NAME;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.US_BILLING_COUNTRY;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.SYSTEM_ADMINISTRATOR_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("Ignite")
public class DefaultBusinessIdentityForDifferentProfilesLeadConvertPageTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private SubBrandsMapping__c testSubBrandsMapping;

    //  Test data
    private final String rcUsBrandName;
    private final String defaultBiMappingLabel;

    public DefaultBusinessIdentityForDifferentProfilesLeadConvertPageTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        rcUsBrandName = data.brandName;
        defaultBiMappingLabel = "TestDefaultBI";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.leadConvert.createPartnerAccountAndLead(salesRepUser);

        step("Create a new SubBrandsMapping__c Custom Setting record for the Partner Account " +
                "and set SubBrandsMapping__c.Brand__c = '" + rcUsBrandName + "' via API", () -> {
            testSubBrandsMapping = createNewSubBrandsMapping(steps.leadConvert.partnerAccount);
            testSubBrandsMapping.setBrand__c(rcUsBrandName);
            enterpriseConnectionUtils.update(testSubBrandsMapping);
        });

        step("Create a new 'Default Business Identity Mapping' record of custom metadata type " +
                "for 'Amazon US' business identity with random and unique Sub-Brand via API", () -> {
            var subBrand = testSubBrandsMapping.getSub_Brand__c();
            createDefaultBusinessIdentityMapping(defaultBiMappingLabel, subBrand,
                    rcUsBrandName, US_BILLING_COUNTRY, AMAZON_US_BUSINESS_IDENTITY_NAME);
        });

        step("Set the Partner Lead.LeadPartnerID__c field value to the Partner Account.Partner_ID__c via API", () -> {
            steps.leadConvert.partnerLead.setLeadPartnerID__c(steps.leadConvert.partnerAccount.getPartner_ID__c());
            enterpriseConnectionUtils.update(steps.leadConvert.partnerLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33605")
    @DisplayName("CRM-33605 - Business Identity field behavior on Lead Conversion Page")
    @Description("Verify that on Lead Conversion Page: \n" +
            "- for Lightning profiles, Business Identity field is preselected, and has only one available value \n" +
            "- for System Admin/CRM Developer/CRM QA Engineer/CRM Support Engineer profiles, " +
            "Business Identity field is preselected, and has other available BIs")
    public void test() {
        step("1. Open the Lead Convert Page for the Partner Lead, " +
                "check that the Business Identity picklist has only a single option and that it's preselected", () -> {
            checkBusinessIdentityPicklistOnLeadConvertPageTestSteps();

            leadConvertPage.businessIdentityPicklist.getOptions().shouldHave(size(1));
        });

        step("2. Re-login as a user with 'System Administrator' profile", () -> {
            var sysAdminUser = getUser().withProfile(SYSTEM_ADMINISTRATOR_PROFILE).execute();
            steps.sfdc.reLoginAsUser(sysAdminUser);
        });

        step("3. Open the Lead Convert Page for the Partner Lead, " +
                "check that the Business Identity picklist is enabled, " +
                "has a correct preselected value, and other BIs available for selection", () -> {
            checkBusinessIdentityPicklistOnLeadConvertPageTestSteps();

            leadConvertPage.businessIdentityPicklist
                    .shouldBe(enabled)
                    .getOptions().shouldHave(sizeGreaterThan(1));
        });
    }

    /**
     * <p> - Open Lead Convert page for the test Partner Lead. </p>
     * <p> - Switch the toggle into 'Create new Account' position. </p>
     * <p> - Check that the Business Identity picklist has a correct preselected value. </p>
     */
    private void checkBusinessIdentityPicklistOnLeadConvertPageTestSteps() {
        step("Open Lead Convert page for the test Lead", () ->
                leadConvertPage.openPage(steps.leadConvert.partnerLead.getId())
        );

        step("Switch the toggle into 'Create New Account' position", () ->
                leadConvertPage.newExistingAccountToggle.click()
        );

        step("Click 'Edit' in Opportunity Section " +
                "and check that Business Identity picklist has preselected value equal to '" + AMAZON_US_BUSINESS_IDENTITY_NAME + "'", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.businessIdentityPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(AMAZON_US_BUSINESS_IDENTITY_NAME));
        });
    }
}
