package ngbs.opportunitycreation.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.MetadataConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Map;

import static base.Pages.opportunityCreationPage;
import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewPartnerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.DefaultBusinessIdentityMappingFactory.createDefaultBusinessIdentityMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.SubBrandsMappingFactory.createNewSubBrandsMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.getPrimaryContactOnAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.DefaultBusinessIdentityMappingHelper.getCustomMetadataToUpdateDefaultBiMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.AMAZON_US_BI_ID;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.BI_FORMAT;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Ignite")
public class BusinessIdentityPicklistOpportunityCreationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final MetadataConnectionUtils metadataConnectionUtils;

    private Account customerAccount;
    private Account ultimatePartnerAccount;
    private Account firstPartnerAccount;
    private Account secondPartnerAccount;
    private SubBrandsMapping__c firstTestSubBrandsMapping;
    private SubBrandsMapping__c secondTestSubBrandsMapping;
    private SubBrandsMapping__c thirdTestSubBrandsMapping;

    //  Test data
    private final String rcCanadaBrandName;
    private final String rcUsBusinessIdentity;
    private final String rcUsBiId;
    private final String rcCanadaBusinessIdentity;
    private final String countryFrance;
    private final String defaultAmazonBusinessIdentity;
    private final String defaultBiMappingLabelAmazon;
    private final String defaultBiMappingLabelRcFrance;
    private final String defaultBiMappingLabelRcCanada;
    private final String permittedBrandsOnPartnerAccount;

    public BusinessIdentityPicklistOpportunityCreationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/RC_MVP_Annual_Contract_QOP.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        metadataConnectionUtils = MetadataConnectionUtils.getInstance();

        rcCanadaBrandName = "RingCentral Canada";
        rcUsBusinessIdentity = data.businessIdentity.name;
        rcUsBiId = data.businessIdentity.id;
        rcCanadaBusinessIdentity = "RingCentral Canada";

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

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        customerAccount = steps.salesFlow.account;

        step("Create three test Partner Accounts " +
                "with Permitted_Brands__c = '" + permittedBrandsOnPartnerAccount + "' brands via API", () -> {
            var accountData = new AccountData(data).withPermittedBrands(permittedBrandsOnPartnerAccount);
            ultimatePartnerAccount = createNewPartnerAccountInSFDC(salesRepUser, accountData);
            firstPartnerAccount = createNewPartnerAccountInSFDC(salesRepUser, accountData);
            secondPartnerAccount = createNewPartnerAccountInSFDC(salesRepUser, accountData);
        });

        step("Set Partner_Account__c and Parent_Partner_Account__c fields " +
                "for the Partner Account with the Name = '" + firstPartnerAccount.getName() + "' " +
                "with Id of the Ultimate Partner Account with the Name = '" + ultimatePartnerAccount.getName() + "' via API", () -> {
            firstPartnerAccount.setPartner_Account__c(ultimatePartnerAccount.getId());
            firstPartnerAccount.setParent_Partner_Account__c(ultimatePartnerAccount.getId());
            enterpriseConnectionUtils.update(firstPartnerAccount);
        });

        step("Set Partner_Account__c and Parent_Partner_Account__c fields " +
                "for the Partner Account with the Name = '" + secondPartnerAccount.getName() + "' " +
                "with Id of the 1st test Partner Account with the Name = '" + firstPartnerAccount.getId() + "' via API", () -> {
            secondPartnerAccount.setPartner_Account__c(firstPartnerAccount.getId());
            secondPartnerAccount.setParent_Partner_Account__c(firstPartnerAccount.getId());
            enterpriseConnectionUtils.update(secondPartnerAccount);
        });

        step("Populate Customer Account's necessary fields " +
                "(Partner_Account__c, Partner_Contact__c, Partner_ID__c, " +
                "Ultimate_Partner_Name__c, Ultimate_Partner_ID__c) via API", () -> {
            customerAccount.setPartner_Account__c(secondPartnerAccount.getId());
            customerAccount.setPartner_Contact__c(getPrimaryContactOnAccount(secondPartnerAccount).getId());
            customerAccount.setPartner_ID__c(secondPartnerAccount.getPartner_ID__c());
            customerAccount.setUltimate_Partner_Name__c(secondPartnerAccount.getName());
            customerAccount.setUltimate_Partner_ID__c(secondPartnerAccount.getPartner_ID__c());

            enterpriseConnectionUtils.update(customerAccount);
        });

        step("Create three SubBrandsMapping__c Custom Setting records for the Partner Accounts " +
                "and set their Country__c and/or Brand__c fields (all via API)", () -> {
            firstTestSubBrandsMapping = createNewSubBrandsMapping(ultimatePartnerAccount);
            secondTestSubBrandsMapping = createNewSubBrandsMapping(firstPartnerAccount);
            thirdTestSubBrandsMapping = createNewSubBrandsMapping(secondPartnerAccount);

            firstTestSubBrandsMapping.setBrand__c(data.brandName);
            secondTestSubBrandsMapping.setCountry__c(countryFrance);
            secondTestSubBrandsMapping.setBrand__c(data.brandName);
            thirdTestSubBrandsMapping.setBrand__c(rcCanadaBrandName);
            enterpriseConnectionUtils.update(firstTestSubBrandsMapping, secondTestSubBrandsMapping, thirdTestSubBrandsMapping);
        });

        step("Create new 'Default Business Identity Mapping' records of custom metadata type " +
                "for 'Amazon US', 'RingCentral Inc.', and 'RingCentral Canada' business identities " +
                "with Sub_Brand__c = SubBrandsMapping__c.Sub_Brand__c value for each created SubBrandsMapping__c record via API", () -> {
            createDefaultBusinessIdentityMapping(defaultBiMappingLabelAmazon, firstTestSubBrandsMapping.getSub_Brand__c(),
                    data.brandName, firstTestSubBrandsMapping.getCountry__c(), defaultAmazonBusinessIdentity);
            createDefaultBusinessIdentityMapping(defaultBiMappingLabelRcFrance, secondTestSubBrandsMapping.getSub_Brand__c(),
                    data.brandName, secondTestSubBrandsMapping.getCountry__c(), rcUsBusinessIdentity);
            createDefaultBusinessIdentityMapping(defaultBiMappingLabelRcCanada, thirdTestSubBrandsMapping.getSub_Brand__c(),
                    rcCanadaBrandName, thirdTestSubBrandsMapping.getCountry__c(), rcCanadaBusinessIdentity);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33610")
    @DisplayName("CRM-33610 - Business Identity field on Quick Opportunity Page populated based on Country and Brand and Sub-brand")
    @Description("Verify that on Quick Opportunity Page: \n" +
            " - When QOP is opened, system checks for Default Business Identity based on Country, Brand and Sub-brand " +
            "to map correct Business Identity in BI selector")
    public void test() {
        step("1. Open the QOP for the Customer Account, " +
                "check that Business Identity picklist has only one available and preselected value = '" + defaultAmazonBusinessIdentity + "', " +
                "create a new Opportunity and check its BusinessIdentity__c field value", () ->
                checkBusinessIdentityPicklistOnQopTestSteps(defaultAmazonBusinessIdentity, AMAZON_US_BI_ID)
        );

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
            var defaultBusinessIdentityMapping = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, DeveloperName, MasterLabel " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Sub_Brand__c = '" + secondTestSubBrandsMapping.getSub_Brand__c() + "'",
                    Default_Business_Identity_Mapping__mdt.class);
            var customMetadata = getCustomMetadataToUpdateDefaultBiMapping(defaultBusinessIdentityMapping);
            var newFieldsMapping = Map.of("Country__c", secondTestSubBrandsMapping.getCountry__c());

            metadataConnectionUtils.updateCustomMetadataRecords(customMetadata, newFieldsMapping);
        });

        step("4. Open the QOP for the Customer Account, " +
                "check that Business Identity picklist has only one available and preselected value = '" + rcUsBusinessIdentity + "', " +
                "create a new Opportunity and check its BusinessIdentity__c field value", () ->
                checkBusinessIdentityPicklistOnQopTestSteps(rcUsBusinessIdentity, rcUsBiId)
        );
    }

    /**
     * <p> - Open Quick Opportunity page for the test Customer Account </p>
     * <p> - Check that Business Identity picklist has only one available preselected value </p>
     * <p> - Create a new Opportunity and check that Opportunity.BusinessIdentity__c field
     * is populated with an expected value.</p>
     *
     * @param expectedBusinessIdentity   an expected value of Business Identity picklist
     * @param expectedBusinessIdentityId an expected value of Business Identity ID
     */
    private void checkBusinessIdentityPicklistOnQopTestSteps(String expectedBusinessIdentity, String expectedBusinessIdentityId) {
        step("Open the QOP for the Customer Account and check that Business Identity picklist is disabled " +
                "and has preselected value = '" + expectedBusinessIdentity + "'", () -> {
            opportunityCreationPage.openPage(customerAccount.getId());

            opportunityCreationPage.businessIdentityPicklist
                    .getSelectedOption().shouldHave(exactTextCaseSensitive(expectedBusinessIdentity));
            opportunityCreationPage.businessIdentityPicklist.getOptions().shouldHave(size(1));
        });

        step("Populate 'Close Date' field, click 'Continue to Opportunity' button " +
                "and check that a new Opportunity is created with the correct BusinessIdentity__c value", () -> {
            opportunityCreationPage.populateCloseDate();
            steps.opportunityCreation.pressContinueToOpp();

            var expectedBusinessIdentityValue = String.format(BI_FORMAT, data.getCurrencyIsoCode(), expectedBusinessIdentityId);
            var createdOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, BusinessIdentity__c " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + opportunityPage.getCurrentRecordId() + "'",
                    Opportunity.class);
            assertThat(createdOpportunity.getBusinessIdentity__c())
                    .as("Opportunity.BusinessIdentity__c value")
                    .isEqualTo(expectedBusinessIdentityValue);
        });
    }
}
