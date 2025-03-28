package leads.leadconvertpage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.leadConvert.Datasets;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.MetadataConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Map;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewPartnerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.DefaultBusinessIdentityMappingFactory.createDefaultBusinessIdentityMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.SubBrandsMappingFactory.createNewSubBrandsMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.DefaultBusinessIdentityMappingHelper.getCustomMetadataToUpdateDefaultBiMapping;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("Ignite")
public class DefaultBusinessIdentityWithSubBrandPartnerLeadConvertPageTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final MetadataConnectionUtils metadataConnectionUtils;

    private Account firstPartnerAccount;
    private Account secondPartnerAccount;
    private SubBrandsMapping__c firstTestSubBrandsMapping;
    private SubBrandsMapping__c secondTestSubBrandsMapping;
    private SubBrandsMapping__c thirdTestSubBrandsMapping;

    //  Test data
    private final String usdCurrencyIsoCode;
    private final String rcUsBrandName;
    private final String rcCanadaBrandName;
    private final String rcUsBusinessIdentity;
    private final String rcCanadaBusinessIdentity;
    private final String countryUnitedStates;
    private final String countryFrance;
    private final String defaultAmazonBusinessIdentity;
    private final String defaultBiMappingLabelAmazon;
    private final String defaultBiMappingLabelRcFrance;
    private final String defaultBiMappingLabelRcCanada;
    private final String permittedBrandsOnPartnerAccount;

    public DefaultBusinessIdentityWithSubBrandPartnerLeadConvertPageTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_USAndCanada_NB.json",
                Datasets.class);
        steps = new Steps(data.dataSets[0]);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        metadataConnectionUtils = MetadataConnectionUtils.getInstance();

        usdCurrencyIsoCode = data.dataSets[0].currencyISOCode;
        rcUsBrandName = data.dataSets[0].brandName;
        rcCanadaBrandName = data.dataSets[1].brandName;
        rcUsBusinessIdentity = data.dataSets[0].businessIdentity.name;
        rcCanadaBusinessIdentity = data.dataSets[1].businessIdentity.name;

        countryUnitedStates = data.dataSets[0].getBillingCountry();
        countryFrance = "France";
        defaultAmazonBusinessIdentity = "Amazon US";
        defaultBiMappingLabelAmazon = defaultAmazonBusinessIdentity;
        defaultBiMappingLabelRcFrance = "RC France";
        defaultBiMappingLabelRcCanada = "RC Canada";
        permittedBrandsOnPartnerAccount = "RingCentral;RingCentral EU";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.leadConvert.createPartnerAccountAndLead(salesRepUser);

        step("Create two test Partner Accounts with Permitted_Brands__c = '" + permittedBrandsOnPartnerAccount + "' brands via API", () -> {
            var accountData = new AccountData()
                    .withCurrencyIsoCode(usdCurrencyIsoCode)
                    .withPermittedBrands(permittedBrandsOnPartnerAccount)
                    .withBillingCountry(countryUnitedStates);
            firstPartnerAccount = createNewPartnerAccountInSFDC(salesRepUser, accountData);
            secondPartnerAccount = createNewPartnerAccountInSFDC(salesRepUser, accountData);
        });

        step("Set Permitted_Brands__c = '" + permittedBrandsOnPartnerAccount + "' " +
                "for the Ultimate Partner Account with Name = '" + steps.leadConvert.partnerAccount.getName() + "' via API", () -> {
            steps.leadConvert.partnerAccount.setPermitted_Brands__c(permittedBrandsOnPartnerAccount);
            enterpriseConnectionUtils.update(steps.leadConvert.partnerAccount);
        });

        step("Set Partner_Account__c and Parent_Partner_Account__c fields " +
                "for the Partner Account with the Name = '" + firstPartnerAccount.getName() + "' " +
                "with Id of the Ultimate Partner Account with the Name '" + steps.leadConvert.partnerAccount.getName() + "' via API", () -> {
            firstPartnerAccount.setPartner_Account__c(steps.leadConvert.partnerAccount.getId());
            firstPartnerAccount.setParent_Partner_Account__c(steps.leadConvert.partnerAccount.getId());
            enterpriseConnectionUtils.update(firstPartnerAccount);
        });

        step("Set Partner_Account__c and Parent_Partner_Account__c fields " +
                "for the Partner Account with the Name = '" + secondPartnerAccount.getName() + "' " +
                "with Id of the 1st test Partner Account with the Name = '" + firstPartnerAccount.getId() + "' via API", () -> {
            secondPartnerAccount.setPartner_Account__c(firstPartnerAccount.getId());
            secondPartnerAccount.setParent_Partner_Account__c(firstPartnerAccount.getId());
            enterpriseConnectionUtils.update(secondPartnerAccount);
        });

        step("Set Partner Lead's Partner_Account__c, Partner_Contact__c, LeadPartnerID__c fields " +
                "using the values from Id, Partner_Contact__c, and Partner_ID__c fields " +
                "on the 2nd test Partner Account with the Name = '" + secondPartnerAccount.getName() + "', " +
                "and set Lead_Brand_Name__c = 'RingCentral' (all via API)", () -> {
            steps.leadConvert.partnerLead.setPartner_Account__c(secondPartnerAccount.getId());
            steps.leadConvert.partnerLead.setPartner_Contact__c(secondPartnerAccount.getPartner_Contact__c());
            steps.leadConvert.partnerLead.setLeadPartnerID__c(secondPartnerAccount.getPartner_ID__c());
            steps.leadConvert.partnerLead.setLead_Brand_Name__c(rcUsBrandName);
            enterpriseConnectionUtils.update(steps.leadConvert.partnerLead);
        });

        step("Create three SubBrandsMapping__c Custom Setting records for each of the Partner Accounts " +
                "and update their Country__c and/or Brand__c fields (all via API)", () -> {
            firstTestSubBrandsMapping = createNewSubBrandsMapping(steps.leadConvert.partnerAccount);
            secondTestSubBrandsMapping = createNewSubBrandsMapping(firstPartnerAccount);
            thirdTestSubBrandsMapping = createNewSubBrandsMapping(secondPartnerAccount);

            firstTestSubBrandsMapping.setBrand__c(rcUsBrandName);
            secondTestSubBrandsMapping.setCountry__c(countryFrance);
            secondTestSubBrandsMapping.setBrand__c(rcUsBrandName);
            thirdTestSubBrandsMapping.setBrand__c(rcCanadaBrandName);
            enterpriseConnectionUtils.update(firstTestSubBrandsMapping, secondTestSubBrandsMapping, thirdTestSubBrandsMapping);
        });

        step("Create three 'Default Business Identity Mapping' records of custom metadata type " +
                "for 'Amazon US', 'RingCentral Inc.', and 'RingCentral Canada' business identities " +
                "with Sub_Brand__c = SubBrandsMapping__c.Sub_Brand__c value for each created SubBrandsMapping__c record via API", () -> {
            createDefaultBusinessIdentityMapping(defaultBiMappingLabelAmazon, firstTestSubBrandsMapping.getSub_Brand__c(),
                    rcUsBrandName, firstTestSubBrandsMapping.getCountry__c(), defaultAmazonBusinessIdentity);
            createDefaultBusinessIdentityMapping(defaultBiMappingLabelRcFrance, secondTestSubBrandsMapping.getSub_Brand__c(),
                    rcUsBrandName, secondTestSubBrandsMapping.getCountry__c(), rcUsBusinessIdentity);
            createDefaultBusinessIdentityMapping(defaultBiMappingLabelRcCanada, thirdTestSubBrandsMapping.getSub_Brand__c(),
                    rcCanadaBrandName, thirdTestSubBrandsMapping.getCountry__c(), rcCanadaBusinessIdentity);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33608")
    @DisplayName("CRM-33608 - Business Identity field on Partner Lead's Lead Conversion Page populated based on Lead Partner Id, Country and brand")
    @Description("Verify that on Lead Conversion Page: " +
            " - When LCP is opened, system checks for Default Business Identity based on Country, " +
            "Brand and Sub-brand to map correct Business Identity in BI selector, without ability to change it")
    public void test() {
        step("1. Open the Lead Convert page for the test Partner Lead, " +
                "check that Business Identity picklist has only one available and preselected value = '" + defaultAmazonBusinessIdentity + "'", () -> {
            checkBusinessIdentityFieldOnLeadConvertPageTestSteps(defaultAmazonBusinessIdentity);
        });

        step("2. Set SubBrandsMapping__c's Country__c and PartnerID__c fields " +
                "for the test record with Sub_Brand__c = '" + secondTestSubBrandsMapping.getSub_Brand__c() + "' " +
                "with the values from 2nd test Partner Account's BillingCountry and Partner_ID__c " +
                "that has the name = '" + secondPartnerAccount.getName() + "' via API", () -> {
            secondTestSubBrandsMapping.setCountry__c(secondPartnerAccount.getBillingCountry());
            secondTestSubBrandsMapping.setPartnerID__c(secondPartnerAccount.getPartner_ID__c());
            enterpriseConnectionUtils.update(secondTestSubBrandsMapping);
        });

        step("3. Set Default_Business_Identity_Mapping__mdt.Country__c = '" + secondTestSubBrandsMapping.getCountry__c() + "' " +
                "for the test record with Sub_Brand__c = '" + secondTestSubBrandsMapping.getSub_Brand__c() + "' via API", () -> {
            var defaultBiMapping = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, DeveloperName, MasterLabel " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Sub_Brand__c = '" + secondTestSubBrandsMapping.getSub_Brand__c() + "'",
                    Default_Business_Identity_Mapping__mdt.class);
            var customMetadata = getCustomMetadataToUpdateDefaultBiMapping(defaultBiMapping);
            var newFieldsMapping = Map.of("Country__c", secondTestSubBrandsMapping.getCountry__c());

            metadataConnectionUtils.updateCustomMetadataRecords(customMetadata, newFieldsMapping);
        });

        step("4. Re-open the Lead Convert page for the test Partner Lead, " +
                "check that Business Identity picklist has only one available and preselected value = '" + rcUsBusinessIdentity + "'", () -> {
            checkBusinessIdentityFieldOnLeadConvertPageTestSteps(rcUsBusinessIdentity);
        });
    }

    /**
     * <p> - Open Lead Convert page for the test Partner Lead. </p>
     * <p> - Switch the toggle into 'Create new Account' position. </p>
     * <p> - Check preselected value of Business Identity picklist and that it's the only one available. </p>
     *
     * @param expectedBI expected preselected value of Business Identity picklist
     */
    private void checkBusinessIdentityFieldOnLeadConvertPageTestSteps(String expectedBI) {
        step("Open Lead Convert page for the test Partner Lead", () ->
                leadConvertPage.openPage(steps.leadConvert.partnerLead.getId())
        );

        step("Switch the toggle into 'Create New Account' position", () ->
                leadConvertPage.newExistingAccountToggle.click()
        );

        step("Click 'Edit' in Opportunity Section, " +
                "and check that Business Identity picklist has only one available and preselected value = '" + expectedBI + "'", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.businessIdentityPicklist.getOptions().shouldHave(size(1));
            leadConvertPage.businessIdentityPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(expectedBI));
        });
    }
}

