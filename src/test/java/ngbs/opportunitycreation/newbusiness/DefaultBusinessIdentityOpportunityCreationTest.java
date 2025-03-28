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
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.AMAZON_US_BI_ID;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.BI_FORMAT;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.SYSTEM_ADMINISTRATOR_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Ignite")
public class DefaultBusinessIdentityOpportunityCreationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account customerAccount;
    private Account partnerAccount;
    private SubBrandsMapping__c testSubBrandsMapping;

    //  Test data
    private final String permittedBrands;
    private final String rcBrandName;
    private final String defaultBiMappingLabel;
    private final String defaultBusinessIdentity;

    public DefaultBusinessIdentityOpportunityCreationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/RC_MVP_Annual_Contract_QOP.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        permittedBrands = "RingCentral;Amazon US";
        rcBrandName = data.getBrandName();
        defaultBiMappingLabel = "Amazon US";
        defaultBusinessIdentity = "Amazon US";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        customerAccount = steps.salesFlow.account;

        step("Create a new Partner Account with RC_Brand__c = '" + rcBrandName + "', " +
                "Permitted_Brands__c = '" + permittedBrands + "' via API", () -> {
            partnerAccount = createNewPartnerAccountInSFDC(salesRepUser,
                    new AccountData(data).withPermittedBrands(permittedBrands));

            partnerAccount.setRC_Brand__c(rcBrandName);
            enterpriseConnectionUtils.update(partnerAccount);
        });

        step("Create a new SubBrandsMapping__c Custom Setting record for the Partner Account via API", () -> {
            testSubBrandsMapping = createNewSubBrandsMapping(partnerAccount);
        });

        step("Populate Customer Account's necessary fields " +
                "(RC_Brand__c, Sub_Brand__c, Partner_Account__c, Partner_Contact__c, Partner_ID__c, " +
                "Ultimate_Partner_Name__c, Ultimate_Partner_ID__c) via API", () -> {
            customerAccount.setRC_Brand__c(rcBrandName);
            customerAccount.setSub_Brand__c(testSubBrandsMapping.getSub_Brand__c());
            customerAccount.setPartner_Account__c(partnerAccount.getId());
            customerAccount.setPartner_Contact__c(partnerAccount.getPartner_Contact__c());
            customerAccount.setPartner_ID__c(partnerAccount.getPartner_ID__c());
            customerAccount.setUltimate_Partner_Name__c(partnerAccount.getName());
            customerAccount.setUltimate_Partner_ID__c(partnerAccount.getPartner_ID__c());

            enterpriseConnectionUtils.update(customerAccount);
        });

        step("Create a new 'Default Business Identity Mapping' record of custom metadata type for 'Amazon US' business identity " +
                "with Sub_Brand__c = SubBrandsMapping__c.Sub_Brand__c value via API", () -> {
            createDefaultBusinessIdentityMapping(defaultBiMappingLabel, testSubBrandsMapping.getSub_Brand__c(),
                    testSubBrandsMapping.getBrand__c(), testSubBrandsMapping.getCountry__c(), defaultBusinessIdentity);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33606")
    @DisplayName("CRM-33606 - Business Identity field behavior on Quick Opportunity Page")
    @Description("Verify that on Quick Opportunity Page: \n" +
            "- for Lightning profiles, Business Identity field is preselected, and has only one available value \n" +
            "- for System Admin/CRM Developer/CRM QA Engineer/CRM Support Engineer profiles, " +
            "Business Identity field is preselected, and has other available BIs")
    public void test() {
        step("1. Open the QOP for the Customer Account, " +
                "check that Business Identity picklist has only a single option and that it's preselected, " +
                "create a new Opportunity, and check its BusinessIdentity__c field", () ->
                businessIdentityPicklistOnQopTestSteps(false)
        );

        step("2. Re-login as a user with 'System Administrator' profile", () -> {
            var sysAdminUser = getUser().withProfile(SYSTEM_ADMINISTRATOR_PROFILE).execute();
            steps.sfdc.reLoginAsUser(sysAdminUser);
        });

        step("3. Open the QOP for the Customer Account, " +
                "check that the Business Identity picklist is enabled, " +
                "has a correct preselected value, and other BIs available for selection, " +
                "create a new Opportunity, and check its BusinessIdentity__c field", () ->
                businessIdentityPicklistOnQopTestSteps(true)
        );
    }

    /**
     * <p> - Open Quick Opportunity page for the test Customer Account </p>
     * <p> - Check that the Business Identity picklist has expected preselected value </p>
     * <p> - (for System Administrator) Check that Business Identity is enabled, and has more than 1 available option. </p>
     * <p> - Create a new Opportunity and check that Opportunity.BusinessIdentity__c field is populated with an expected value.</p>
     */
    private void businessIdentityPicklistOnQopTestSteps(boolean isAdminUser) {
        step("Open the QOP for the Customer Account " +
                "and check that Business Identity picklist has preselected value = '" + defaultBusinessIdentity + "'", () -> {
            opportunityCreationPage.openPage(customerAccount.getId());

            opportunityCreationPage.businessIdentityPicklist
                    .getSelectedOption().shouldHave(exactTextCaseSensitive(defaultBusinessIdentity));
        });

        if (isAdminUser) {
            step("Check that Business Identity picklist is enabled and has more than 1 available option to select", () -> {
                opportunityCreationPage.businessIdentityPicklist
                        .shouldBe(enabled)
                        .getOptions().shouldHave(sizeGreaterThan(1));
            });
        }

        step("Populate 'Close Date' field, click 'Continue to Opportunity' button, " +
                "and check that a new Opportunity is created with the correct BusinessIdentity__c value", () -> {
            opportunityCreationPage.populateCloseDate();
            steps.opportunityCreation.pressContinueToOpp();

            var expectedBusinessIdentityValue = String.format(BI_FORMAT, data.getCurrencyIsoCode(), AMAZON_US_BI_ID);
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
