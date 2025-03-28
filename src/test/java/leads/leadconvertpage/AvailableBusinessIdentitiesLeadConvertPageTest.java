package leads.leadconvertpage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.leadConvert.Datasets;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Default_Business_Identity_Mapping__mdt;
import com.sforce.soap.enterprise.sobject.SubBrandsMapping__c;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.DefaultBusinessIdentityMappingFactory.createDefaultBusinessIdentityMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.SubBrandsMappingFactory.createNewSubBrandsMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.US_BILLING_COUNTRY;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.SYSTEM_ADMINISTRATOR_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.CollectionCondition.containExactTextsCaseSensitive;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Ignite")
public class AvailableBusinessIdentitiesLeadConvertPageTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private SubBrandsMapping__c testSubBrandsMapping;

    //  Test data
    private final String rcUsBrandName;
    private final String rcEuBrandName;
    private final String permittedRcBrands;
    private final String countryGermany;
    private final String rcUsBusinessIdentity;
    private final String rcGermanyBusinessIdentity;
    private final String rcCanadaBusinessIdentity;
    private final String rcFranceBusinessIdentity;
    private final List<String> otherBusinessIdentities;
    private final String defaultBiMappingLabel;

    public AvailableBusinessIdentitiesLeadConvertPageTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/RC_MVP_Monthly_NonContract_NB_EB_DifferentBIs.json",
                Datasets.class);
        steps = new Steps(data.dataSets[0]);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        rcUsBrandName = data.dataSets[0].brandName;
        rcEuBrandName = data.dataSets[1].brandName;
        permittedRcBrands = "RingCentral;RingCentral EU";
        countryGermany = "Germany";
        rcUsBusinessIdentity = data.dataSets[0].businessIdentity.name;
        rcGermanyBusinessIdentity = "RingCentral Germany";
        rcCanadaBusinessIdentity = "RingCentral Canada";
        rcFranceBusinessIdentity = data.dataSets[1].businessIdentity.name;
        otherBusinessIdentities = List.of(rcUsBusinessIdentity, "Amazon US", "Avaya Cloud Office",
                "Rainbow Office US", "Unify Office US");
        defaultBiMappingLabel = rcGermanyBusinessIdentity;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.leadConvert.createSalesLead(salesRepUser);
        steps.leadConvert.createPartnerAccountAndLead(salesRepUser);

        step("Set the test Sales Lead.Country__c = '" + US_BILLING_COUNTRY + "' via API", () -> {
            steps.leadConvert.salesLead.setCountry__c(US_BILLING_COUNTRY);

            steps.leadConvert.salesLead.setOwnerId(salesRepUser.getId());
            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        step("Check that 'Default Business Identity Mapping' records of custom metadata type exists " +
                "for '" + rcGermanyBusinessIdentity + "', '" + rcUsBusinessIdentity + " business identities", () -> {
            var rcGermanyDefaultBiMappingCustomMetadataRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Sub_Brand__c = null " +
                            "AND Brand__c = '" + rcEuBrandName + "' " +
                            "AND Country__c = 'Germany' " +
                            "AND Default_Business_Identity__c = '" + rcGermanyBusinessIdentity + "' " +
                            "AND Available_Business_Identities__c = '" + rcFranceBusinessIdentity + "' " +
                            "AND Is_Core_Brand__c = true",
                    Default_Business_Identity_Mapping__mdt.class);

            assertThat(rcGermanyDefaultBiMappingCustomMetadataRecords.size())
                    .as("Quantity of Default_Business_Identity_Mapping__mdt records for '" + rcGermanyBusinessIdentity + "', " +
                            "'" + rcFranceBusinessIdentity + " business identities")
                    .isEqualTo(1);

            var rcUsaDefaultBiMappingCustomMetadataRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Sub_Brand__c = null " +
                            "AND Brand__c = '" + rcUsBrandName + "' " +
                            "AND Country__c = '" + US_BILLING_COUNTRY + "' " +
                            "AND Default_Business_Identity__c = '" + rcUsBusinessIdentity + "' " +
                            "AND Available_Business_Identities__c = '" + rcCanadaBusinessIdentity + "' " +
                            "AND Is_Core_Brand__c = true",
                    Default_Business_Identity_Mapping__mdt.class);

            assertThat(rcUsaDefaultBiMappingCustomMetadataRecords.size())
                    .as("Quantity of Default_Business_Identity_Mapping__mdt records for '" + rcUsBusinessIdentity + "', " +
                            "'" + rcCanadaBusinessIdentity + " business identities")
                    .isEqualTo(1);
        });

        step("Set Permitted_Brands__c = '" + permittedRcBrands + "' for the Partner Account via API", () -> {
            steps.leadConvert.partnerAccount.setPermitted_Brands__c(permittedRcBrands);
            enterpriseConnectionUtils.update(steps.leadConvert.partnerAccount);
        });

        step("Set Partner Lead's Country = '" + countryGermany + "', Lead_Brand_Name__c = '" + rcEuBrandName + "' " +
                "and LeadPartnerID__c = Partner Account's Partner_ID__c via API", () -> {
            steps.leadConvert.partnerLead.setCountry__c(countryGermany);
            steps.leadConvert.partnerLead.setLead_Brand_Name__c(rcEuBrandName);
            steps.leadConvert.partnerLead.setLeadPartnerID__c(steps.leadConvert.partnerAccount.getPartner_ID__c());

            enterpriseConnectionUtils.update(steps.leadConvert.partnerLead);
        });

        step("Create a new SubBrandsMapping__c Custom Setting record for the Partner Account " +
                "and set its field Brand__c = '" + rcEuBrandName + "' (all via API)", () -> {
            testSubBrandsMapping = createNewSubBrandsMapping(steps.leadConvert.partnerAccount);
            testSubBrandsMapping.setBrand__c(rcEuBrandName);
            enterpriseConnectionUtils.update(testSubBrandsMapping);
        });

        step("Create a new 'Default Business Identity Mapping' record of custom metadata " +
                "for '" + rcGermanyBusinessIdentity + "' business identity " +
                "with Sub_Brand__c = SubBrandsMapping__c.Sub_Brand__c value via API", () -> {
            createDefaultBusinessIdentityMapping(defaultBiMappingLabel, testSubBrandsMapping.getSub_Brand__c(),
                    rcEuBrandName, countryGermany, rcGermanyBusinessIdentity, rcFranceBusinessIdentity);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-34725")
    @TmsLink("CRM-33688")
    @DisplayName("CRM-34725 - Availability of 'Available Business Identity' from 'Business Identity Mapping' Custom MetaData " +
            "on Lead Conversion Page for Sales Lead. \n " +
            "CRM-33688 - Availability of 'Available Business Identity' from 'Business Identity Mapping' Custom MetaData " +
            "on Lead Conversion Page for Partner Lead")
    @Description("CRM-34725 - Verify that 'Available Business Identity' from 'Business Identity Mapping' Custom MetaData " +
            "on Sales Lead Conversion Page. \n " +
            "CRM-33688 - Verify that 'Available Business Identity' from 'Business Identity Mapping' Custom MetaData " +
            "on Lead Conversion Page of Partner Lead")
    public void test() {
        //  CRM-34725
        step("1. Open the Lead Convert page for the test Sales Lead, check preselected Country picklist value, " +
                "check Brand field value, check preselected Business Identity picklist value " +
                "and that '" + rcUsBusinessIdentity + "', '" + rcCanadaBusinessIdentity + "' business identities " +
                "are available to select in Business Identity picklist", () -> {
            checkBusinessIdentityPicklistLeadConvertPageTestSteps(steps.leadConvert.salesLead.getId(),
                    US_BILLING_COUNTRY, rcUsBrandName, rcUsBusinessIdentity);
            leadConvertPage.businessIdentityPicklist.getOptions()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(rcUsBusinessIdentity, rcCanadaBusinessIdentity));
        });

        //  CRM-34725
        step("2. Select Country = '" + countryGermany + ", " +
                "check Brand field value, check preselected Business Identity picklist value " +
                "and that only '" + rcFranceBusinessIdentity + "', " +
                "'" + rcGermanyBusinessIdentity + "' business identities " +
                "are available to select in Business Identity picklist", () -> {
            leadConvertPage.countryPicklist.selectOption(countryGermany);

            leadConvertPage.brandNameOutputField.shouldHave(exactTextCaseSensitive(rcEuBrandName), ofSeconds(20));
            leadConvertPage.businessIdentityPicklist
                    .shouldBe(enabled)
                    .getSelectedOption().shouldHave(exactTextCaseSensitive(rcGermanyBusinessIdentity));
            leadConvertPage.businessIdentityPicklist.getOptions().shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    rcFranceBusinessIdentity, rcGermanyBusinessIdentity));
            leadConvertPage.businessIdentityPicklist.getOptions().asDynamicIterable()
                    .forEach(businessIdentity -> businessIdentity.shouldBe(enabled));
        });

        //  CRM-33688
        step("3. Open the Lead Convert page for the test Partner Lead, " +
                "check Brand field value, check preselected Business Identity picklist value " +
                "and that '" + rcFranceBusinessIdentity + "', '" + rcGermanyBusinessIdentity + "' business identities " +
                "are available to select in Business Identity picklist", () -> {
            checkBusinessIdentityPicklistLeadConvertPageTestSteps(steps.leadConvert.partnerLead.getId(),
                    null, rcEuBrandName, rcGermanyBusinessIdentity);
            leadConvertPage.businessIdentityPicklist.getOptions().shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    rcFranceBusinessIdentity, rcGermanyBusinessIdentity));
        });

        step("4. Re-login as a user with 'System Administrator' profile", () -> {
            var sysAdminUser = getUser().withProfile(SYSTEM_ADMINISTRATOR_PROFILE).execute();
            steps.sfdc.reLoginAsUser(sysAdminUser);
        });

        //  CRM-34725
        step("5. Open the Lead Convert page for the test Sales Lead, check preselected Country picklist value, " +
                "check Brand field value, check preselected Business Identity picklist value " +
                "and that all business identities are available to select in Business Identity picklist", () -> {
            checkBusinessIdentityPicklistLeadConvertPageTestSteps(steps.leadConvert.salesLead.getId(),
                    US_BILLING_COUNTRY, rcUsBrandName, rcUsBusinessIdentity);
            leadConvertPage.businessIdentityPicklist.getOptions().should(containExactTextsCaseSensitive(otherBusinessIdentities));
        });

        //  CRM-33688
        step("6. Open the Lead Convert page for the test Partner Lead, " +
                "check Brand field value, check preselected Business Identity picklist value " +
                "and that all business identities are available to select in Business Identity picklist", () -> {
            checkBusinessIdentityPicklistLeadConvertPageTestSteps(steps.leadConvert.partnerLead.getId(),
                    null, rcEuBrandName, rcGermanyBusinessIdentity);
            leadConvertPage.businessIdentityPicklist.getOptions().should(containExactTextsCaseSensitive(otherBusinessIdentities));
        });
    }

    /**
     * <p> - Open Lead Convert page for test Sales Lead </p>
     * <p> - Switch the toggle into 'Create new Account' position </p>
     * <p> - Check that Brand field has expected value </p>
     * <p> - Check preselected value of Business Identity picklist and that it's enabled </p>
     * <p> - Check that Business Identity picklist contains only expected values
     * and all of them are available to select </p>
     * <p> - Sales Lead only: Check preselected value of Country picklist </p>
     *
     * @param leadId                            ID of the Lead record that is to be checked
     * @param expectedCountry                   an expected value of Country picklist.
     *                                          Country picklist is visible only for Sales Lead
     * @param expectedBrandName                 an expected value of Brand field
     * @param expectedRcDefaultBusinessIdentity an expected value of Business Identity picklist
     */
    private void checkBusinessIdentityPicklistLeadConvertPageTestSteps(String leadId,
                                                                       String expectedCountry,
                                                                       String expectedBrandName,
                                                                       String expectedRcDefaultBusinessIdentity) {
        step("Open the Lead Convert page for the test Lead", () ->
                leadConvertPage.openPage(leadId)
        );

        step("Switch the toggle into 'Create New Account' position", () ->
                leadConvertPage.newExistingAccountToggle.click()
        );

        step("Click 'Edit' in Opportunity section, check that Brand = '" + expectedBrandName + "'", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.brandNameOutputField.shouldHave(exactTextCaseSensitive(expectedBrandName), ofSeconds(10));
        });

        if (leadId.equals(steps.leadConvert.salesLead.getId())) {
            step("Check that preselected Country = '" + expectedCountry + "'", () -> {
                leadConvertPage.countryPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(expectedCountry));
            });
        }

        step("Check that BI picklist is enabled and has a preselected value = '" + expectedRcDefaultBusinessIdentity + "'", () -> {
            leadConvertPage.businessIdentityPicklist
                    .shouldBe(enabled)
                    .getSelectedOption().shouldHave(exactTextCaseSensitive(expectedRcDefaultBusinessIdentity));
        });

        step("Check that all of BI picklist's options are available to select", () -> {
            leadConvertPage.businessIdentityPicklist.getOptions().asDynamicIterable()
                    .forEach(businessIdentity -> businessIdentity.shouldBe(enabled));
        });
    }
}
