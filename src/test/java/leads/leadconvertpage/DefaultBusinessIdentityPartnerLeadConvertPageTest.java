package leads.leadconvertpage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.leadConvert.Datasets;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.Default_Business_Identity_Mapping__mdt;
import com.sforce.soap.enterprise.sobject.SubBrandsMapping__c;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewPartnerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.DefaultBusinessIdentityMappingFactory.createDefaultBusinessIdentityMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.SubBrandsMappingFactory.createNewSubBrandsMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Ignite")
public class DefaultBusinessIdentityPartnerLeadConvertPageTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private SubBrandsMapping__c testSubBrandsMapping;

    //  Test data
    private final String usdCurrencyIsoCode;
    private final String permittedBrandsOnParentPartnerAccount;
    private final String permittedBrandsOnPartnerAccount;
    private final String subBrandsMappingCountry;
    private final String subBrandsMappingBrand;

    private final String defaultBiMappingLabel;
    private final String defaultBiMappingBrand;
    private final String defaultBiMappingCountry;
    private final String defaultBusinessIdentity;

    private final String expectedPreselectedBusinessIdentity;

    public DefaultBusinessIdentityPartnerLeadConvertPageTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_USAndCanada_NB.json",
                Datasets.class);
        steps = new Steps(data.dataSets[0]);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        usdCurrencyIsoCode = data.dataSets[0].currencyISOCode;
        permittedBrandsOnParentPartnerAccount = "RingCentral;RingCentral Canada";
        permittedBrandsOnPartnerAccount = "RingCentral;RingCentral EU";
        subBrandsMappingCountry = "Canada";
        subBrandsMappingBrand = data.dataSets[1].brandName;

        defaultBiMappingLabel = "CA RingCentral Canada";
        defaultBiMappingBrand = subBrandsMappingBrand;
        defaultBiMappingCountry = subBrandsMappingCountry;
        defaultBusinessIdentity = data.dataSets[1].getBusinessIdentityName();

        expectedPreselectedBusinessIdentity = data.dataSets[0].getBusinessIdentityName();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.leadConvert.createPartnerAccountAndLead(salesRepUser);

        step("Create an Ultimate Partner Account for an existing Partner Account " +
                "and populate Parent Account's Parent_Partner_Account__c and Permitted_Brands__c fields (all via API)", () -> {
            var ultimateParentPartnerAccount = createNewPartnerAccountInSFDC(salesRepUser,
                    new AccountData().withCurrencyIsoCode(usdCurrencyIsoCode).withPermittedBrands(permittedBrandsOnParentPartnerAccount));

            steps.leadConvert.partnerAccount.setParent_Partner_Account__c(ultimateParentPartnerAccount.getId());
            steps.leadConvert.partnerAccount.setPermitted_Brands__c(permittedBrandsOnPartnerAccount);
            enterpriseConnectionUtils.update(steps.leadConvert.partnerAccount);
        });

        step("Set Partner Lead.LeadPartnerID__c = Partner Account.Partner_ID__c via API", () -> {
            steps.leadConvert.partnerLead.setLeadPartnerID__c(steps.leadConvert.partnerAccount.getPartner_ID__c());
            enterpriseConnectionUtils.update(steps.leadConvert.partnerLead);
        });

        step("Create a new SubBrandsMapping__c Custom Setting record for the Partner Account " +
                "and set its fields Country__c = 'Canada' and Brand__c = 'RingCentral Canada' (all via API)", () -> {
            testSubBrandsMapping = createNewSubBrandsMapping(steps.leadConvert.partnerAccount);

            testSubBrandsMapping.setCountry__c(subBrandsMappingCountry);
            testSubBrandsMapping.setBrand__c(subBrandsMappingBrand);
            enterpriseConnectionUtils.update(testSubBrandsMapping);
        });

        step("Check that 'Default Business Identity Mapping' record of custom metadata type exists " +
                "for 'RingCentral Inc.' business identity", () -> {
            var rcUsDefaultBiMappingCustomMetadataRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Sub_Brand__c = null " +
                            "AND Brand__c = '" + RINGCENTRAL_RC_BRAND + "' " +
                            "AND Country__c = '" + US_BILLING_COUNTRY + "' " +
                            "AND Default_Business_Identity__c = '" + RC_US_BUSINESS_IDENTITY_NAME + "'",
                    Default_Business_Identity_Mapping__mdt.class);
            assertThat(rcUsDefaultBiMappingCustomMetadataRecords.size())
                    .as("Quantity of Default_Business_Identity_Mapping__mdt records for 'RingCentral Inc.'")
                    .isEqualTo(1);
        });

        step("Create a new 'Default Business Identity Mapping' record of custom metadata type for RC Canada business identity " +
                "with Sub_Brand__c = SubBrandsMapping__c.Sub_Brand__c value", () -> {
            var defaultBiSubBrand = testSubBrandsMapping.getSub_Brand__c();
            createDefaultBusinessIdentityMapping(defaultBiMappingLabel, defaultBiSubBrand,
                    defaultBiMappingBrand, defaultBiMappingCountry, defaultBusinessIdentity);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33611")
    @DisplayName("CRM-33611 - Business Identity field on Partner Lead's Lead Conversion Page populated by Default Business Identity " +
            "based on Lead's Country and Brand")
    @Description("Verify that on Lead Conversion Page:\n" +
            "- If no sub-brand is found based on Partner ID (and all related Parents), " +
            "then Default Business Identity is mapped based on Country and Brand")
    public void test() {
        step("1. Open the Lead Convert page for the test Partner Lead", () ->
                leadConvertPage.openPage(steps.leadConvert.partnerLead.getId())
        );

        step("2. Switch the toggle into 'Create New Account' position", () ->
                leadConvertPage.newExistingAccountToggle.click()
        );

        step("3. Click 'Edit' in Opportunity section and check that Business Identity picklist is enabled " +
                "and has preselected value = 'RingCentral Inc.'", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.businessIdentityPicklist
                    .shouldBe(enabled)
                    .getSelectedOption().shouldHave(exactTextCaseSensitive(expectedPreselectedBusinessIdentity));
        });
    }
}
