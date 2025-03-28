package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static base.Pages.wizardPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory.createOpportunity;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.getTechQuote;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("Account")
@Tag("Opportunity")
@Tag("Quote")
@Tag("LTR-569")
@Tag("MultiProduct-UB")
public class TechAccAndOppCreationNewBusinessMpUbTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUserWithEnabledMpUbFT;
    private User salesRepUserWithoutEnabledMpUbFT;
    private Opportunity newMasterOpportunity;
    private Opportunity newTechRcCcOpportunity;
    private Opportunity newTechEdOpportunity;

    //  Test data
    private final String officeServiceName;
    private final String engageDigitalServiceName;
    private final String rcCcServiceName;
    private final List<String> allSelectedServices;
    private final Map<String, Package> officeRcCcPackageFolderNameToPackageMap;
    private final Map<String, Package> officeRcCcEdPackageFolderNameToPackageMap;

    public TechAccAndOppCreationNewBusinessMpUbTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_Annual_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        officeServiceName = data.packageFolders[0].name;
        engageDigitalServiceName = data.packageFolders[2].name;
        rcCcServiceName = data.packageFolders[3].name;
        allSelectedServices = List.of(officeServiceName, engageDigitalServiceName, rcCcServiceName);

        var officePackage = data.packageFolders[0].packages[0];
        var engageDigitalPackage = data.packageFolders[2].packages[0];
        var rcCcPackage = data.packageFolders[3].packages[0];

        officeRcCcPackageFolderNameToPackageMap = Map.of(
                officeServiceName, officePackage,
                rcCcServiceName, rcCcPackage
        );

        officeRcCcEdPackageFolderNameToPackageMap = Map.of(
                officeServiceName, officePackage,
                rcCcServiceName, rcCcPackage,
                engageDigitalServiceName, engageDigitalPackage
        );
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Sales Rep - Lightning' profile and WITHOUT 'Enable Multi-Product Unified Billing' Feature Toggle", () -> {
            salesRepUserWithoutEnabledMpUbFT = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withFeatureToggles(Map.of(ENABLE_MULTIPRODUCT_UNIFIED_BILLING_FT, false))
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithoutEnabledMpUbFT);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithoutEnabledMpUbFT);

        step("Log in as a user with 'Sales Rep - Lightning' profile and WITHOUT 'Enable Multi-Product Unified Billing' Feature Toggle", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(salesRepUserWithoutEnabledMpUbFT)
        );
    }

    @Test
    @TmsLink("CRM-36633")
    @DisplayName("CRM-36633 - UB. Technical Accounts and Opportunities are created while creating Quote " +
            "by user with enabled FT 'Enable Multi-Product Unified Billing' on Account with existing linked Technical Accounts. New Business")
    @Description("Verify that required technical Accounts and Opportunities are created while creating Multi-Product Quote " +
            "on Account with existing linked Technical Accounts by user with enabled FT 'Enable Multi-Product Unified Billing'")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select Office and RC Contact Center packages for it, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityForMultiProduct(steps.quoteWizard.opportunity.getId(), officeRcCcPackageFolderNameToPackageMap);
        });

        step("2. Transfer the ownership of the test Account and Contact " +
                "to the user with 'Sales Rep - Lightning' profile and WITH 'Enable Multi-Product Unified Billing' Feature Toggle, " +
                "and re-login as this user into SFDC", () -> {
            step("Find a user with 'Sales Rep - Lightning' profile and WITH 'Enable Multi-Product Unified Billing' Feature Toggle", () -> {
                salesRepUserWithEnabledMpUbFT = getUser()
                        .withProfile(SALES_REP_LIGHTNING_PROFILE)
                        .withFeatureToggles(List.of(ENABLE_MULTIPRODUCT_UNIFIED_BILLING_FT))
                        //  to avoid issues with records sharing during MultiProduct Quote creation 
                        //  (access to the Tech Accounts to correctly identify MPL/MPUB flow)
                        .withGroupMembership(NON_GSP_GROUP)
                        .execute();
            });

            steps.salesFlow.account.setOwnerId(salesRepUserWithEnabledMpUbFT.getId());
            steps.salesFlow.contact.setOwnerId(salesRepUserWithEnabledMpUbFT.getId());
            enterpriseConnectionUtils.update(steps.salesFlow.account, steps.salesFlow.contact);

            steps.sfdc.reLoginAsUser(salesRepUserWithEnabledMpUbFT);
        });

        step("3. Create a new Opportunity for the same test Account via API", () -> {
            newMasterOpportunity = createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, true,
                    data.getBrandName(), data.businessIdentity.id, salesRepUserWithEnabledMpUbFT, data.getCurrencyIsoCode(),
                    officeServiceName);
        });

        step("4. Open the Quote Wizard for the NEW test Opportunity to add a new Sales Quote, " +
                "select Office, Engage Digital and RingCentral Contact Center packages for it, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityForMultiProduct(newMasterOpportunity.getId(), officeRcCcEdPackageFolderNameToPackageMap);
        });

        step("5. Check that there is 1 Technical Account created for ED Service", () -> {
            var edTechAccounts = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Account " +
                            "WHERE Master_Account__c = '" + steps.salesFlow.account.getId() + "' " +
                            "AND Service_Type__c = '" + engageDigitalServiceName + "'",
                    Account.class);
            assertThat(edTechAccounts.size())
                    .as("Number of Technical Accounts for Engage Digital service")
                    .isEqualTo(1);
        });

        step("6. Check that there are 2 Technical Opportunities created for the RC CC and ED Services (1 for each) " +
                "linked to the new (Master) Opportunity", () -> {
            var edTechOpportunities = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Opportunity " +
                            "WHERE MasterOpportunity__c = '" + newMasterOpportunity.getId() + "' " +
                            "AND Tier_Name__c = '" + engageDigitalServiceName + "'",
                    Opportunity.class);
            assertThat(edTechOpportunities.size())
                    .as("Number of Technical Opportunities for Engage Digital service")
                    .isEqualTo(1);

            newTechEdOpportunity = edTechOpportunities.get(0);

            var rcCcTechOpportunities = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Opportunity " +
                            "WHERE MasterOpportunity__c = '" + newMasterOpportunity.getId() + "' " +
                            "AND Tier_Name__c = '" + rcCcServiceName + "'",
                    Opportunity.class);
            assertThat(rcCcTechOpportunities.size())
                    .as("Number of Technical Opportunities for RC CC service")
                    .isEqualTo(1);

            newTechRcCcOpportunity = rcCcTechOpportunities.get(0);
        });

        step("7. Check that there are 1 Master and 3 Technical Quotes created, " +
                "and that they are linked to the correct Opportunities", () -> {
            var masterQuoteId = wizardPage.getSelectedQuoteId();
            var allTechQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE MasterQuote__c = '" + masterQuoteId + "'",
                    Quote.class);
            assertThat(allTechQuotes.size())
                    .as("Number of Technical Quotes")
                    .isEqualTo(allSelectedServices.size());

            step("Check that the Master Quote and Office Technical Quote are linked " +
                    "to the new (Master) Opportunity", () -> {
                var masterQuote = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, OpportunityId " +
                                "FROM Quote " +
                                "WHERE Id = '" + masterQuoteId + "'",
                        Quote.class);
                assertThat(masterQuote.getOpportunityId())
                        .as("Master Quote.OpportunityId value")
                        .isEqualTo(newMasterOpportunity.getId());

                var officeTechQuote = getTechQuote(masterQuoteId, officeServiceName);
                assertThat(officeTechQuote.getOpportunityId())
                        .as("Tech Office Quote.OpportunityId value")
                        .isEqualTo(newMasterOpportunity.getId());
            });

            step("Check that the Technical Quotes for RC CC and ED are linked " +
                    "to the Technical Opportunities for the corresponding Service", () -> {
                var rcCcTechQuote = getTechQuote(masterQuoteId, rcCcServiceName);
                assertThat(rcCcTechQuote.getOpportunityId())
                        .as("Tech RC CC Quote.OpportunityId value")
                        .isEqualTo(newTechRcCcOpportunity.getId());

                var edTechQuote = getTechQuote(masterQuoteId, engageDigitalServiceName);
                assertThat(edTechQuote.getOpportunityId())
                        .as("Tech ED Quote.OpportunityId value")
                        .isEqualTo(newTechEdOpportunity.getId());
            });
        });
    }
}
