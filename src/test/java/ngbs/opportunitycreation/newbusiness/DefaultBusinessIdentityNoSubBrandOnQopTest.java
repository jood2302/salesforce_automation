package ngbs.opportunitycreation.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.opportunityCreationPage;
import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewPartnerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.DefaultBusinessIdentityMappingFactory.createDefaultBusinessIdentityMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.SubBrandsMappingFactory.createNewSubBrandsMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.BI_FORMAT;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Ignite")
public class DefaultBusinessIdentityNoSubBrandOnQopTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account customerAccount;
    private Account ultimatePartnerAccount;
    private Account partnerAccount;
    private SubBrandsMapping__c testSubBrandsMapping;

    //  Test data
    private final String permittedRcBrandsCanada;
    private final String permittedRcBrandsEU;
    private final String rcBrandName;
    private final String rcBrandNameCanada;
    private final String rcCanadaBusinessIdentity;
    private final String defaultBiMappingLabel;
    private final String rcUsBusinessIdentityName;
    private final String rcCanadaCountry;
    private final String rsUsBusinessIdentityId;

    public DefaultBusinessIdentityNoSubBrandOnQopTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/RC_MVP_Annual_Contract_QOP.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        permittedRcBrandsCanada = "RingCentral;RingCentral Canada";
        permittedRcBrandsEU = "RingCentral;RingCentral EU";
        rcBrandName = data.getBrandName();
        rcBrandNameCanada = "RingCentral Canada";
        rcCanadaBusinessIdentity = rcBrandNameCanada;
        defaultBiMappingLabel = "RingCentral Canada";
        rcUsBusinessIdentityName = data.getBusinessIdentityName();
        rcCanadaCountry = "Canada";
        rsUsBusinessIdentityId = data.businessIdentity.id;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        customerAccount = steps.salesFlow.account;

        step("Create a new Ultimate Partner Account with Permitted_Brands__c = '" + permittedRcBrandsCanada + "' via API", () -> {
            ultimatePartnerAccount = createNewPartnerAccountInSFDC(salesRepUser,
                    new AccountData(data).withPermittedBrands(permittedRcBrandsCanada));

            enterpriseConnectionUtils.update(ultimatePartnerAccount);
        });

        step("Create a new test Partner Account with Permitted_Brands__c = '" + permittedRcBrandsEU + "' via API", () -> {
            partnerAccount = createNewPartnerAccountInSFDC(salesRepUser,
                    new AccountData(data).withPermittedBrands(permittedRcBrandsEU));
        });

        step("Populate Partner Account's Parent_Account_Id__c, Parent_Partner_Account__c and Partner_Account__c fields " +
                "with Ultimate Partner Account.Parent_Account_ID__c field value via API", () -> {
            partnerAccount.setParent_Account_ID__c(ultimatePartnerAccount.getParent_Account_ID__c());
            partnerAccount.setParent_Partner_Account__c(ultimatePartnerAccount.getParent_Account_ID__c());
            partnerAccount.setPartner_Account__c(ultimatePartnerAccount.getParent_Account_ID__c());

            enterpriseConnectionUtils.update(partnerAccount);
        });

        step("Create a new SubBrandsMapping__c Custom Setting record for the Partner Account " +
                "and set its fields Country__c = 'Canada' and Brand__c = 'RingCentral Canada' (all via API)", () -> {
            testSubBrandsMapping = createNewSubBrandsMapping(ultimatePartnerAccount);
            testSubBrandsMapping.setCountry__c(rcCanadaCountry);
            testSubBrandsMapping.setBrand__c(rcBrandNameCanada);

            enterpriseConnectionUtils.update(partnerAccount);
        });

        step("Populate Customer Account's necessary fields " +
                "(RC_Brand__c, Partner_Account__c, Partner_Contact__c, Partner_ID__c, " +
                "Ultimate_Partner_Name__c, Ultimate_Partner_ID__c) via API", () -> {
            customerAccount.setRC_Brand__c(rcBrandName);
            customerAccount.setPartner_Account__c(partnerAccount.getId());
            customerAccount.setPartner_Contact__c(partnerAccount.getPartner_Contact__c());
            customerAccount.setPartner_ID__c(partnerAccount.getPartner_ID__c());
            customerAccount.setUltimate_Partner_Name__c(partnerAccount.getName());
            customerAccount.setUltimate_Partner_ID__c(partnerAccount.getPartner_ID__c());

            enterpriseConnectionUtils.update(customerAccount);
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

        step("Create a new 'Default Business Identity Mapping' record of custom metadata type for 'RingCentral Canada' business identity " +
                "with Sub_Brand__c = SubBrandsMapping__c.Sub_Brand__c value via API", () -> {
            createDefaultBusinessIdentityMapping(defaultBiMappingLabel, testSubBrandsMapping.getSub_Brand__c(),
                    rcBrandNameCanada, rcCanadaCountry, rcCanadaBusinessIdentity);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33612")
    @DisplayName("CRM-33612 - Business Identity field on Quick Opportunity Page populated by Default Business Identity " +
            "based on Country and Brand")
    @Description("Verify that on Quick Opportunity Page: " +
            " - If no sub-brand is found based on Partner ID (and all related Parents), " +
            "then Default Business Identity is mapped based on Country and Brand")
    public void test() {
        step("1. Open the QOP for the Customer Account, " +
                "check that Business Identity picklist has preselected value = '" + rcUsBusinessIdentityName + "'", () -> {
            opportunityCreationPage.openPage(customerAccount.getId());

            opportunityCreationPage.businessIdentityPicklist
                    .getSelectedOption().shouldHave(exactTextCaseSensitive(rcUsBusinessIdentityName));
        });

        step("2. Populate 'Close Date' field, click 'Continue to Opportunity' button, " +
                "and check that a new Opportunity is created with the correct BusinessIdentity__c value", () -> {
            opportunityCreationPage.populateCloseDate();
            steps.opportunityCreation.pressContinueToOpp();

            var expectedBusinessIdentityValue = String.format(BI_FORMAT, data.getCurrencyIsoCode(), rsUsBusinessIdentityId);
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
