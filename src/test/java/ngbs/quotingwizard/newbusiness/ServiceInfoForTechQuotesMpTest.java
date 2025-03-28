package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static base.Pages.*;
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
public class ServiceInfoForTechQuotesMpTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUser;
    private String opportunityId;
    private String masterQuoteId;

    //  Test data
    private final String officeServiceName;
    private final String engageDigitalServiceName;
    private final String rcCcServiceName;
    private final Map<String, Package> packageFolderNameToPackageMap;
    private final List<String> allSelectedServices;
    private final Product phoneToAdd;
    private final Product digitalLineUnlimited;

    public ServiceInfoForTechQuotesMpTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_Annual_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        var officePackage = data.packageFolders[0].packages[0];
        var engageDigitalPackage = data.packageFolders[2].packages[0];
        var rcCcPackage = data.packageFolders[3].packages[0];
        officeServiceName = data.packageFolders[0].name;
        engageDigitalServiceName = data.packageFolders[2].name;
        rcCcServiceName = data.packageFolders[3].name;

        packageFolderNameToPackageMap = Map.of(
                officeServiceName, officePackage,
                engageDigitalServiceName, engageDigitalPackage,
                rcCcServiceName, rcCcPackage
        );

        allSelectedServices = List.of(officeServiceName, engageDigitalServiceName, rcCcServiceName);
        phoneToAdd = data.getProductByDataName("LC_HD_523", officePackage);
        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50", officePackage);
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Sales Rep - Lightning' profile and 'Enable Multi-Product Unified Billing' Feature Toggle", () -> {
            salesRepUser = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withFeatureToggles(List.of(ENABLE_MULTIPRODUCT_UNIFIED_BILLING_FT))
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        opportunityId = steps.quoteWizard.opportunity.getId();

        step("Log in as a user with 'Sales Rep - Lightning' profile and 'Enable Multi-Product Unified Billing' Feature Toggle", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser)
        );
    }

    @Test
    @TmsLink("CRM-36634")
    @TmsLink("CRM-36313")
    @TmsLink("CRM-36637")
    @TmsLink("CRM-36636")
    @DisplayName("CRM-36634 - UB. Records on 'Service Info' object are created for all Technical Quotes while creating MP Quote " +
            "by user with enabled FT 'Enable Multi-Product Unified Billing'. New Business.\n" +
            "CRM-36313 - UB. Technical Accounts and Opportunities are not created while creating Quote " +
            "by user with enabled FT 'Enable Multi-Product Unified Billing'. New Business.\n" +
            "CRM-36637 - All MP UB Quotes (Master and Technical) are created under same Opportunity. (New Business).\n" +
            "CRM-36636 - Only Master Quote is Primary for UB MP Quotes. New Business")
    @Description("CRM-36634 - Verify that Records on 'Service Info' object are created for all Technical Quotes while creating Quote " +
            "by user with enabled FT 'Enable Multi-Product Unified Billing'.\n" +
            "CRM-36313 - Verify that technical Accounts and Opportunities are not created while creating Multi-Product Quote " +
            "by user with enabled FT 'Enable Multi-Product Unified Billing'.\n" +
            "CRM-36637 - Verify that all MP UB Quotes (Master and Technical) are created under same Opportunity.\n" +
            "CRM-36636 - Verify that only Master Quote has IsPrimary__c = 'true' and all Technical Quotes " +
            "have IsPrimary__c = 'false' for UB MP Quotes")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select MVP, Engage Digital and RC Contact Center packages for it, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityForMultiProduct(opportunityId, packageFolderNameToPackageMap);

            masterQuoteId = wizardPage.getSelectedQuoteId();
        });

        //  CRM-36634
        step("2. Check that ServiceInfo__c record is not created for Master Quote and records are created only for Technical Quotes, " +
                "and that Quote__r.IsMultiProductTechnicalQuote__c = true for all ServiceInfo__c records", () -> {
            step("Check the there's no ServiceInfo__c record for the Master Quote", () -> {
                var serviceInfoForMasterQuote = enterpriseConnectionUtils.query(
                        "SELECT Id " +
                                "FROM ServiceInfo__c " +
                                "WHERE Quote__r.Id = '" + masterQuoteId + "'",
                        ServiceInfo__c.class);
                assertThat(serviceInfoForMasterQuote.size())
                        .as("Number of ServiceInfo__c records for the Master Quote")
                        .isEqualTo(0);
            });

            step("Check that ServiceInfo__c record is created for the each Tech Quote, " +
                    "and all corresponding Tech Quote.IsMultiProductTechnicalQuote__c = true", () -> {
                var serviceInfoForTechQuotes = enterpriseConnectionUtils.query(
                        "SELECT Id, Quote__r.IsMultiProductTechnicalQuote__c, Quote__r.ServiceName__c, Quote__c " +
                                "FROM ServiceInfo__c " +
                                "WHERE Quote__r.MasterQuote__c = '" + masterQuoteId + "'",
                        ServiceInfo__c.class);
                assertThat(serviceInfoForTechQuotes.size())
                        .as("Number of ServiceInfo__c records for the Tech Quotes")
                        .isEqualTo(allSelectedServices.size());

                //  this way we can check that the ServiceInfo__c records are created for all Tech Quotes
                var serviceNamesOnTechQuotes = serviceInfoForTechQuotes.stream()
                        .map(serviceInfo -> serviceInfo.getQuote__r().getServiceName__c())
                        .toList();
                assertThat(serviceNamesOnTechQuotes)
                        .as("ServiceInfo__c.Quote__r.ServiceName__c values (should contain all selected services)")
                        .containsExactlyInAnyOrderElementsOf(allSelectedServices);

                var isMultiProductTechnicalQuoteValuesOnTechQuotes = serviceInfoForTechQuotes.stream()
                        .map(serviceInfo -> serviceInfo.getQuote__r().getIsMultiProductTechnicalQuote__c())
                        .toList();
                assertThat(isMultiProductTechnicalQuoteValuesOnTechQuotes)
                        .as("ServiceInfo__c.Quote__r.IsMultiProductTechnicalQuote__c values (should be true for all records)")
                        .containsOnly(true);
            });
        });

        step("3. Check the OpportunityId and isPrimary__c values " +
                "for the Master Quote and the Office, EV, and RC CC Technical Quotes", () -> {
            var masterQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, OpportunityId, isPrimary__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + masterQuoteId + "'",
                    Quote.class);
            var officeTechQuote = getTechQuote(masterQuoteId, officeServiceName);
            var rcCcTechQuote = getTechQuote(masterQuoteId, rcCcServiceName);
            var edTechQuote = getTechQuote(masterQuoteId, engageDigitalServiceName);

            //  CRM-36637
            step("Check that the Master Quote and the Office, ED, and RC CC Technical Quotes " +
                    "are linked to the same test Opportunity", () -> {
                assertThat(masterQuote.getOpportunityId())
                        .as("Master Quote.OpportunityId value")
                        .isEqualTo(opportunityId);
                assertThat(officeTechQuote.getOpportunityId())
                        .as("Tech office Quote.OpportunityId value")
                        .isEqualTo(opportunityId);
                assertThat(rcCcTechQuote.getOpportunityId())
                        .as("Tech RC CC Quote.OpportunityId value")
                        .isEqualTo(opportunityId);
                assertThat(edTechQuote.getOpportunityId())
                        .as("Tech ED Quote.OpportunityId value")
                        .isEqualTo(opportunityId);
            });

            //  CRM-36636
            step("Check that Master Quote has isPrimary__c = 'true' " +
                    "and the Office, ED, and RC CC Technical Quotes have isPrimary__c = 'false'", () -> {
                assertThat(masterQuote.getIsPrimary__c())
                        .as("Master Quote.isPrimary__c value")
                        .isTrue();
                assertThat(officeTechQuote.getIsPrimary__c())
                        .as("Tech office Quote.isPrimary__c value")
                        .isFalse();
                assertThat(rcCcTechQuote.getIsPrimary__c())
                        .as("Tech RC CC Quote.isPrimary__c value")
                        .isFalse();
                assertThat(edTechQuote.getIsPrimary__c())
                        .as("Tech ED Quote.isPrimary__c value")
                        .isFalse();
            });
        });

        step("4. Add necessary products on the Add Products tab, " +
                "open the Price Tab, and assign phone to DL", () -> {
            steps.quoteWizard.addProductsOnProductsTab(phoneToAdd);
            cartPage.openTab();
            steps.cartTab.assignDevicesToDL(phoneToAdd.name, digitalLineUnlimited.name, steps.quoteWizard.localAreaCode,
                    phoneToAdd.quantity);
        });

        step("5. Open the Shipping tab, and assign the '" + phoneToAdd.name + "' phone to the default shipping group, " +
                "and save changes", () -> {
            shippingPage.openTab();

            var defaultShippingGroup = shippingPage.getFirstShippingGroup();
            var shippingDevice = shippingPage.getShippingDevice(phoneToAdd.name);
            shippingPage.assignDeviceToShippingGroup(shippingDevice, defaultShippingGroup);

            shippingPage.saveChanges();
        });

        //  CRM-36313
        step("6. Check that Technical Accounts and Opportunities are not created", () -> {
            var techAccounts = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Account " +
                            "WHERE Master_Account__c = '" + steps.salesFlow.account.getId() + "' ",
                    Account.class);
            assertThat(techAccounts)
                    .as("List of Technical Accounts")
                    .isEmpty();

            var techOpportunities = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Opportunity " +
                            "WHERE MasterOpportunity__c = '" + opportunityId + "'",
                    Opportunity.class);
            assertThat(techOpportunities)
                    .as("List of Technical Opportunities")
                    .isEmpty();
        });
    }
}
