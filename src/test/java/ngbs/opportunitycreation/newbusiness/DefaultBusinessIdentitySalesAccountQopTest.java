package ngbs.opportunitycreation.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Default_Business_Identity_Mapping__mdt;
import com.sforce.soap.enterprise.sobject.Opportunity;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.opportunityCreationPage;
import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.utilities.StringHelper.CAD_CURRENCY_ISO_CODE;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomPositiveInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.DefaultBusinessIdentityMappingFactory.createDefaultBusinessIdentityMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.BI_FORMAT;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.RC_CA_BI_ID;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Ignite")
public class DefaultBusinessIdentitySalesAccountQopTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String officeService;
    private final String usdCurrencyIsoCode;
    private final String rcUsBrandName;
    private final String rcUsBiId;
    private final String rcUsBiName;

    public DefaultBusinessIdentitySalesAccountQopTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/RC_MVP_Annual_Contract_QOP.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        officeService = data.packageFolders[0].name;
        usdCurrencyIsoCode = data.getCurrencyIsoCode();
        rcUsBrandName = data.getBrandName();
        rcUsBiId = data.businessIdentity.id;
        rcUsBiName = data.getBusinessIdentityName();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);

        step("Set Account.RC_Brand__c = '" + rcUsBrandName + "' via API", () -> {
            steps.salesFlow.account.setRC_Brand__c(rcUsBrandName);
            enterpriseConnectionUtils.update(steps.salesFlow.account);
        });

        step("Check that 'Default Business Identity Mapping' records of custom metadata type exist " +
                "for 'RingCentral Inc.' and 'RingCentral Canada' business identities", () -> {
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

            var rcCaDefaultBiMappingCustomMetadataRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Sub_Brand__c = null " +
                            "AND Brand__c = '" + RINGCENTRAL_CANADA_RC_BRAND + "' " +
                            "AND Country__c = '" + CA_BILLING_COUNTRY + "' " +
                            "AND Default_Business_Identity__c = '" + RC_CA_BUSINESS_IDENTITY_NAME + "'",
                    Default_Business_Identity_Mapping__mdt.class);
            assertThat(rcCaDefaultBiMappingCustomMetadataRecords.size())
                    .as("Quantity of Default_Business_Identity_Mapping__mdt records for 'RingCentral Canada'")
                    .isEqualTo(1);
        });

        step("Create a new 'Default Business Identity Mapping' record of custom metadata type " +
                "for 'Amazon US' business identity with random and unique Sub-Brand via API", () -> {
            var subBrand = "Test SubBrand " + getRandomPositiveInteger();
            createDefaultBusinessIdentityMapping(RINGCENTRAL_RC_BRAND, subBrand,
                    rcUsBrandName, US_BILLING_COUNTRY, AMAZON_US_BUSINESS_IDENTITY_NAME);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33617")
    @DisplayName("CRM-33617 - Business Identity field on Sales Accounts Quick Opportunity Page " +
            "populated by Default Business Identity based on BillingCountry and Brand")
    @Description("Verify that on Quick Opportunity Page Default Business Identity is mapped based on " +
            "Account's BillingCountry and Brand, " +
            "and only Default Business Identity Mapping record with sub-brand = null is used for mapping")
    public void test() {
        step("1. Open the Quick Opportunity Page for the test Account with RC_Brand__c = '" + rcUsBrandName + "' " +
                "and BillingCountry = '" + US_BILLING_COUNTRY + "', check the Business Identity picklist on it, " +
                "create a new Opportunity from there, and check Opportunity.BusinessIdentity__c field", () ->
                businessIdentitySalesAccountQopTestSteps(rcUsBiId, rcUsBiName, usdCurrencyIsoCode)
        );

        step("2. Set Account's BillingCountry = '" + CA_BILLING_COUNTRY + "' " +
                "and RC_Brand__c = '" + RINGCENTRAL_CANADA_RC_BRAND + "' via API", () -> {
            steps.salesFlow.account.setBillingCountry(CA_BILLING_COUNTRY);
            steps.salesFlow.account.setRC_Brand__c(RINGCENTRAL_CANADA_RC_BRAND);
            enterpriseConnectionUtils.update(steps.salesFlow.account);
        });

        step("3. Open the Quick Opportunity Page for the test Account with RC_Brand__c = '" + RINGCENTRAL_CANADA_RC_BRAND + "' " +
                "and BillingCountry = '" + CA_BILLING_COUNTRY + "', check the Business Identity picklist on it, " +
                "create a new Opportunity from there, and check Opportunity.BusinessIdentity__c field", () ->
                businessIdentitySalesAccountQopTestSteps(RC_CA_BI_ID, RC_CA_BUSINESS_IDENTITY_NAME, CAD_CURRENCY_ISO_CODE)
        );
    }

    /**
     * <p> - Open a Quick Opportunity creation page and check that the Business Identity field has the expected preselected value. </p>
     * <p> - Populate required fields on QOP and continue with opportunity creation. </p>
     * <p> - Check the 'BusinessIdentity__c' field on the created Opportunity to ensure it matches the expected value. </p>
     *
     * @param biId            expected ID of the Business Identity on the created Opportunity
     * @param biName          expected Name of the Business Identity to be preselected on the QOP
     * @param currencyIsoCode expected value of currencyIsoCode on the created Opportunity
     */
    private void businessIdentitySalesAccountQopTestSteps(String biId, String biName, String currencyIsoCode) {
        step("Open Quick Opportunity creation Page (QOP) for a test Account " +
                "and check that Business Identity field has preselected value = '" + biName + "'", () -> {
            opportunityCreationPage.openPage(steps.salesFlow.account.getId());

            opportunityCreationPage.businessIdentityPicklist
                    .getSelectedOption().shouldHave(exactTextCaseSensitive(biName));
        });

        step("Select Service, set Close Date, and press 'Continue to Opportunity' button", () -> {
            opportunityCreationPage.servicePicklist.getOptions().shouldHave(itemWithText(officeService), ofSeconds(10));
            opportunityCreationPage.servicePicklist.selectOption(officeService);
            opportunityCreationPage.populateCloseDate();
            steps.opportunityCreation.pressContinueToOpp();
        });

        step("Check the 'BusinessIdentity__c' field on the created Opportunity", () -> {
            var expectedBusinessIdentityValue = String.format(BI_FORMAT, currencyIsoCode, biId);

            var createdOpportunityId = opportunityPage.getCurrentRecordId();
            var createdOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, BusinessIdentity__c " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + createdOpportunityId + "'",
                    Opportunity.class);

            assertThat(createdOpportunity.getBusinessIdentity__c())
                    .as("Opportunity.BusinessIdentity__c value")
                    .isEqualTo(expectedBusinessIdentityValue);
        });
    }
}
