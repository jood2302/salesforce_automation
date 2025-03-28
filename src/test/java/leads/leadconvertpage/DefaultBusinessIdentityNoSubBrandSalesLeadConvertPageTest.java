package leads.leadconvertpage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.leadConvert.Datasets;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.MetadataConnectionUtils;
import com.sforce.soap.enterprise.sobject.Default_Business_Identity_Mapping__mdt;
import com.sforce.soap.enterprise.sobject.Lead;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomPositiveInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.DefaultBusinessIdentityMappingFactory.createDefaultBusinessIdentityMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.DefaultBusinessIdentityMappingHelper.getCustomMetadataToUpdateDefaultBiMapping;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Ignite")
@Tag("LeadConvert")
@Tag("QTC-944")
public class DefaultBusinessIdentityNoSubBrandSalesLeadConvertPageTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final MetadataConnectionUtils metadataConnectionUtils;

    private Lead salesLead;
    private String defaultBiMappingGermanyFullName;
    private String defaultBiMappingCanadaFullName;

    //  Test data
    private final String rcUsBrand;
    private final String rcEuBrand;
    private final String zimbabweCountry;
    private final String rcUsCountry;
    private final String finlandCountry;
    private final String jamaicaCountry;
    private final String rcUsBusinessIdentity;
    private final String rcGermanyBusinessIdentity;
    private final String rcCanadaBusinessIdentity;
    private final String defaultBiMappingLabelRcGermany;
    private final String defaultBiMappingLabelRcCanada;
    private final List<String> defaultBiMappingFullNamesToDelete;

    public DefaultBusinessIdentityNoSubBrandSalesLeadConvertPageTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/RC_MVP_Monthly_NonContract_NB_EB_DifferentBIs.json",
                Datasets.class);
        steps = new Steps(data.dataSets[0]);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        metadataConnectionUtils = MetadataConnectionUtils.getInstance();

        rcUsBrand = data.dataSets[0].brandName;
        rcEuBrand = data.dataSets[1].brandName;
        zimbabweCountry = "Zimbabwe";
        rcUsCountry = "United States";
        finlandCountry = "Finland";
        jamaicaCountry = "Jamaica";
        rcUsBusinessIdentity = data.dataSets[0].getBusinessIdentityName();
        rcGermanyBusinessIdentity = "RingCentral Germany";
        rcCanadaBusinessIdentity = "RingCentral Canada";
        defaultBiMappingLabelRcGermany = "TEST Zimbabwe 1_" + getRandomPositiveInteger();
        defaultBiMappingLabelRcCanada = "TEST Zimbabwe 2_" + getRandomPositiveInteger();

        defaultBiMappingFullNamesToDelete = new ArrayList<>();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.leadConvert.createSalesLead(salesRepUser);
        salesLead = steps.leadConvert.salesLead;

        step("Set the test Sales Lead.Country__c = '" + rcUsCountry + "' via API", () -> {
            salesLead.setCountry__c(rcUsCountry);
            enterpriseConnectionUtils.update(salesLead);
        });

        step("Check that 'Default Business Identity Mapping' records of custom metadata type exist " +
                "for 'Finland'/'RingCentral EU' and 'United States'/'RingCentral' brand/country combos (both for Core Brand)", () -> {
            var rcEuFinlandDefaultBiMappingCustomMetadataRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Sub_Brand__c = null " +
                            "AND Brand__c = '" + rcEuBrand + "' " +
                            "AND Country__c = '" + finlandCountry + "' " +
                            "AND Is_Core_Brand__c = true",
                    Default_Business_Identity_Mapping__mdt.class);
            assertThat(rcEuFinlandDefaultBiMappingCustomMetadataRecords.size())
                    .as("Quantity of Default_Business_Identity_Mapping__mdt records for 'Finland'/'RingCentral EU'")
                    .isEqualTo(1);

            var rcUsUnitedStatesDefaultBiMappingCustomMetadataRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Sub_Brand__c = null " +
                            "AND Brand__c = '" + rcUsBrand + "' " +
                            "AND Country__c = '" + rcUsCountry + "' " +
                            "AND Is_Core_Brand__c = true",
                    Default_Business_Identity_Mapping__mdt.class);
            assertThat(rcUsUnitedStatesDefaultBiMappingCustomMetadataRecords.size())
                    .as("Quantity of Default_Business_Identity_Mapping__mdt records for 'United States'/'RingCentral'")
                    .isEqualTo(1);
        });

        step("Check that 'Default Business Identity Mapping' records of custom metadata type do NOT exist " +
                "for 'Jamaica' country", () -> {
            var jamaicaDefaultBiMappingCustomMetadataRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Country__c = '" + jamaicaCountry + "'",
                    Default_Business_Identity_Mapping__mdt.class);
            assertThat(jamaicaDefaultBiMappingCustomMetadataRecords.size())
                    .as("Quantity of Default_Business_Identity_Mapping__mdt records for 'Jamaica' country")
                    .isEqualTo(0);
        });

        step("Create new 'Default Business Identity Mapping' record of custom metadata type " +
                "for '" + rcGermanyBusinessIdentity + "' business identity via Metadata API", () -> {
            defaultBiMappingGermanyFullName = createDefaultBusinessIdentityMapping(defaultBiMappingLabelRcGermany, EMPTY_STRING,
                    rcEuBrand, zimbabweCountry, rcGermanyBusinessIdentity, EMPTY_STRING, true);
            defaultBiMappingFullNamesToDelete.add(defaultBiMappingGermanyFullName);
        });

        step("Create new 'Default Business Identity Mapping' record of custom metadata type " +
                "for '" + rcCanadaBusinessIdentity + "' business identity via Metadata API", () -> {
            defaultBiMappingCanadaFullName = createDefaultBusinessIdentityMapping(defaultBiMappingLabelRcCanada, EMPTY_STRING,
                    rcUsBrand, zimbabweCountry, rcCanadaBusinessIdentity, EMPTY_STRING, false);
            defaultBiMappingFullNamesToDelete.add(defaultBiMappingCanadaFullName);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @AfterEach
    public void tearDownTest() {
        step("Delete the created 'Default Business Identity Mapping' records via Metadata API", () -> {
            metadataConnectionUtils.deleteCustomMetadataRecords(defaultBiMappingFullNamesToDelete);
        });
    }

    @Test
    @TmsLink("CRM-33615")
    @TmsLink("CRM-35285")
    @DisplayName("CRM-33615 - Brand and Business Identity fields on Sales Lead's Lead Conversion Page " +
            "populated according to Default BI mapping metadata records. \n" +
            "CRM-35285 - 'Country' picklist preselect logic for LCP with BI mapping FT")
    @Description("CRM-33615 - Verify that on Lead Conversion Page the value in Business Identity selector is selected " +
            "depending on the Country and Brand according only Default BI mapping metadata records with 'Core Brand' = true. \n" +
            "CRM-35285 - Verify that: \n" +
            "- if Lead.Country__c != null AND has a corresponding record in 'Default Business Identity Mapping' Custom Metadata with Is_Core_Brand__c = true, " +
            "then this country is preselected in the Country picklist on the Lead Convert Page\n" +
            "- if Lead.Country__c = null \n" +
            "OR Lead.Country__c != null AND has NO corresponding record in 'Default Business Identity Mapping' Custom Metadata with Is_Core_Brand__c = true \n" +
            "OR Lead.Country__c != null AND has a corresponding record in 'Default Business Identity Mapping' Custom Metadata with Is_Core_Brand__c = false, " +
            "then 'United States' is preselected in the Country picklist on the Lead Convert Page")
    public void test() {
        step("1. Open the Lead Convert page for the test Lead and switch the toggle into 'Create New Account' position", () -> {
            leadConvertPage.openPage(salesLead.getId());
            leadConvertPage.newExistingAccountToggle.click();
        });

        //  CRM-33615
        step("2. Click 'Edit' in Opportunity section " +
                "and check Country and Business Identity picklists and Brand field value", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.countryPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(rcUsCountry));
            leadConvertPage.brandNameOutputField.shouldHave(exactTextCaseSensitive(rcUsBrand));
            leadConvertPage.businessIdentityPicklist
                    .getSelectedOption().shouldHave(exactTextCaseSensitive(rcUsBusinessIdentity));
        });

        //  CRM-33615
        step("3. Change value in Country picklist to " + zimbabweCountry + " " +
                "and check Business Identity picklist and Brand field value", () -> {
            leadConvertPage.countryPicklist.selectOption(zimbabweCountry);

            leadConvertPage.brandNameOutputField.shouldHave(exactTextCaseSensitive(rcEuBrand));
            leadConvertPage.businessIdentityPicklist
                    .getSelectedOption().shouldHave(exactTextCaseSensitive(rcGermanyBusinessIdentity));
        });

        step("4. Set Is_Core_Brand__c = false for RC Germany Default BI Mapping Metadata Record via Metadata API", () -> {
            var defaultBusinessIdentityMapping = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, DeveloperName, MasterLabel " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Label LIKE '" + defaultBiMappingLabelRcGermany + "%'",
                    Default_Business_Identity_Mapping__mdt.class);
            var customMetadata = getCustomMetadataToUpdateDefaultBiMapping(defaultBusinessIdentityMapping);
            var newFieldsMapping = Map.of("Is_Core_Brand__c", false);

            metadataConnectionUtils.updateCustomMetadataRecords(customMetadata, newFieldsMapping);
        });

        //  CRM-33615
        step("5. Refresh the Lead Convert page, switch the toggle into 'Create New Account' position, " +
                "click 'Edit' button in Opportunity section, search for 'Zimbabwe' value in Country picklist " +
                "and check that it's absent in the picklist", () -> {
            leadConvertPage.openPage(salesLead.getId());
            leadConvertPage.newExistingAccountToggle.click();
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();

            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.countryPicklist
                    .getOptions()
                    .shouldHave(sizeGreaterThan(0))
                    .filter(exactTextCaseSensitive(zimbabweCountry))
                    .shouldHave(size(0));
        });

        step("6. Set Is_Core_Brand__c = true for RC Canada Default BI Mapping Metadata Record via Metadata API", () -> {
            var defaultBusinessIdentityMapping = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, DeveloperName, MasterLabel " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Label LIKE '" + defaultBiMappingLabelRcCanada + "%'",
                    Default_Business_Identity_Mapping__mdt.class);
            var customMetadata = getCustomMetadataToUpdateDefaultBiMapping(defaultBusinessIdentityMapping);
            var newFieldsMapping = Map.of("Is_Core_Brand__c", true);

            metadataConnectionUtils.updateCustomMetadataRecords(customMetadata, newFieldsMapping);
        });

        //  CRM-33615
        step("7. Refresh the Lead Convert page, switch the toggle into 'Create New Account' position, " +
                "click 'Edit' button in Opportunity section, set Country to '" + zimbabweCountry + "' " +
                "and check Business Identity picklist and Brand field value", () -> {
            leadConvertPage.openPage(salesLead.getId());
            leadConvertPage.newExistingAccountToggle.click();
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();

            leadConvertPage.opportunityInfoEditButton.click();
            leadConvertPage.countryPicklist.selectOption(zimbabweCountry);

            leadConvertPage.brandNameOutputField.shouldHave(exactTextCaseSensitive(rcUsBrand));
            leadConvertPage.businessIdentityPicklist
                    .getSelectedOption().shouldHave(exactTextCaseSensitive(rcCanadaBusinessIdentity));
        });

        //  to prepare a precise precondition for CRM-35285
        step("8. Delete one of the created 'Default Business Identity Mapping' records " +
                "with Label = '" + defaultBiMappingCanadaFullName + "' via Metadata API", () -> {
            metadataConnectionUtils.deleteCustomMetadataRecords(List.of(defaultBiMappingCanadaFullName));
            //  to avoid an error in tearDownTest() method when deleting the same record ('no CustomMetadata named ... found')
            defaultBiMappingFullNamesToDelete.remove(defaultBiMappingCanadaFullName);
        });

        //  CRM-35285
        step("9. Update the test Lead via API (Country__c = '" + finlandCountry + "', Lead_Brand_Name__c = null), " +
                "re-open the Lead Convert page, " +
                "and check the preselected Country and Brand value in the Opportunity Info section", () -> {
            updateLeadAndCheckCountryAndBrandOnLeadConvertPage(finlandCountry, null, finlandCountry, rcEuBrand);
        });

        //  CRM-35285
        step("10. Update the test Lead via API (Country__c = null, Lead_Brand_Name__c = '" + rcEuBrand + "'), " +
                "and check the preselected Country and Brand value in the Opportunity Info section", () -> {
            updateLeadAndCheckCountryAndBrandOnLeadConvertPage(null, rcEuBrand, rcUsCountry, rcUsBrand);
        });

        //  CRM-35285
        step("11. Update the test Lead via API (Country__c = '" + zimbabweCountry + "', Lead_Brand_Name__c = '" + rcEuBrand + "'), " +
                "and check the preselected Country and Brand value in the Opportunity Info section", () -> {
            updateLeadAndCheckCountryAndBrandOnLeadConvertPage(zimbabweCountry, rcEuBrand, rcUsCountry, rcUsBrand);
        });

        //  CRM-35285
        step("12. Update the test Lead via API (Country__c = '" + jamaicaCountry + "', Lead_Brand_Name__c = '" + rcUsBrand + "'), " +
                "and check the preselected Country and Brand value in the Opportunity Info section", () -> {
            updateLeadAndCheckCountryAndBrandOnLeadConvertPage(jamaicaCountry, rcUsBrand, rcUsCountry, rcUsBrand);
        });
    }

    /**
     * Update the test Lead's Country__c and Lead_Brand_Name__c via API,
     * open Lead Convert page for the Lead,
     * and check the preselected Country and Brand in the Opportunity Info section.
     *
     * @param leadCountryToSet     Country__c value to set for the test Lead (or {@code null} if the field should be empty)
     * @param leadBrandToSet       Lead_Brand_Name__c value to set for the test Lead (or {@code null} if the field should be empty)
     * @param expectedCountryOnLCP expected preselected Country's value on the Lead Convert page
     * @param expectedBrandOnLCP   expected Brand's value on the Lead Convert page
     */
    private void updateLeadAndCheckCountryAndBrandOnLeadConvertPage(String leadCountryToSet, String leadBrandToSet,
                                                                    String expectedCountryOnLCP, String expectedBrandOnLCP) {
        step("Update the test Lead's Country__c = " + leadCountryToSet + ", Lead_Brand_Name__c = " + leadBrandToSet + " via API", () -> {
            var leadToUpdate = new Lead();
            leadToUpdate.setId(salesLead.getId());

            if (leadCountryToSet != null) {
                leadToUpdate.setCountry__c(leadCountryToSet);
            } else {
                leadToUpdate.setFieldsToNull(new String[]{"Country__c"});
            }

            if (leadBrandToSet != null) {
                leadToUpdate.setLead_Brand_Name__c(leadBrandToSet);
            } else {
                leadToUpdate.setFieldsToNull(new String[]{"Lead_Brand_Name__c"});
            }

            enterpriseConnectionUtils.update(leadToUpdate);
        });

        step("Open the Lead Convert page, switch the toggle into 'Create New Account' position, " +
                "click 'Edit' button in Opportunity section, " +
                "and check that preselected Country = '" + expectedCountryOnLCP + "' and Brand value = '" + expectedBrandOnLCP + "' in it", () -> {
            leadConvertPage.openPage(salesLead.getId());
            leadConvertPage.newExistingAccountToggle.click();
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();

            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.countryPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(expectedCountryOnLCP));
            leadConvertPage.brandNameOutputField.shouldHave(exactTextCaseSensitive(expectedBrandOnLCP));
        });
    }
}
