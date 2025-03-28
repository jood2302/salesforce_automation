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

import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewPartnerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.SubBrandsMappingFactory.createNewSubBrandsMapping;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Opportunity")
@Tag("Ignite")
public class SubBrandPopulationOpportunityCreationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account customerAccount;
    private Account partnerAccount;
    private SubBrandsMapping__c testSubBrandsMapping;

    public SubBrandPopulationOpportunityCreationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/RC_MVP_Annual_Contract_QOP.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        customerAccount = steps.salesFlow.account;

        step("Create a new Partner Account with RC_Brand__c = '" + data.brandName + "' via API", () -> {
            partnerAccount = createNewPartnerAccountInSFDC(salesRepUser, new AccountData(data));
        });

        step("Create a new SubBrandsMapping__c Custom Setting record for the Partner Account via API", () -> {
            testSubBrandsMapping = createNewSubBrandsMapping(partnerAccount);
        });

        step("Populate Customer Account's Partner_Account__c, Partner_ID__c fields " +
                "with Partner Account field values (all via API)", () -> {
            customerAccount.setPartner_Account__c(partnerAccount.getId());
            customerAccount.setPartner_ID__c(partnerAccount.getPartner_ID__c());
            enterpriseConnectionUtils.update(customerAccount);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33658")
    @DisplayName("CRM-33658 - Sub-brand field on Opportunity is populated after Opportunity was created from QOP")
    @Description("Verify that Sub-brand field on Opportunity is populated after Opportunity was created from QOP " +
            "according to Partner ID of Partner Account and 'Sub-brands mapping' Custom Setting")
    public void test() {
        step("1. Open the QOP for the Customer Account, create a new Opportunity " +
                "and check that created Opportunity.Sub_Brand__c field value " +
                "is equal to the test SubBrandsMapping__c.Sub_Brand__c field value", () ->
                checkOpportunitySubBrandFieldTestSteps(testSubBrandsMapping.getSub_Brand__c())
        );

        step("2. Delete the test SubBrandsMapping__c record via API", () -> {
            enterpriseConnectionUtils.delete(testSubBrandsMapping);
        });

        step("3. Open the QOP for the Customer Account, create a new Opportunity " +
                "and check that created Opportunity.Sub_Brand__c field value is empty", () ->
                checkOpportunitySubBrandFieldTestSteps(null)
        );
    }

    /**
     * <p> - Open Quick Opportunity page for the test Customer Account </p>
     * <p> - Populate required fields and click 'Continue to Opportunity' button </p>
     * <p> - Check that created Opportunity.Sub_Brand__c field is populated with an expected value.</p>
     *
     * @param expectedSubBrandValue an expected value of Opportunity.Sub_Brand__c value
     */
    private void checkOpportunitySubBrandFieldTestSteps(String expectedSubBrandValue) {
        step("Open the QOP for the test Customer Account and populate 'Close Date' field", () -> {
            steps.opportunityCreation.openQopAndPopulateRequiredFields(customerAccount.getId());
        });

        step("Click 'Continue to Opportunity' button " +
                "and check that a new Opportunity is created with the correct Sub_Brand__c value", () -> {
            steps.opportunityCreation.pressContinueToOpp();

            var createdOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Sub_Brand__c " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + opportunityPage.getCurrentRecordId() + "'",
                    Opportunity.class);
            assertThat(createdOpportunity.getSub_Brand__c())
                    .as("Opportunity.Sub_Brand__c value")
                    .isEqualTo(expectedSubBrandValue);
        });
    }
}
