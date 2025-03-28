package ngbs.quotingwizard.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.ServiceInfo__c;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.packagePage;
import static base.Pages.wizardPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("Account")
@Tag("Opportunity")
@Tag("Quote")
@Tag("LTR-569")
@Tag("MultiProduct-UB")
public class ServiceInfoSingleProductTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUser;
    private String masterQuoteId;

    //  Test data
    private final String rcCcServiceName;
    private final Package rcCcPackage;
    private final String engageDigitalServiceName;
    private final Package engageDigitalPackage;

    private final List<String> allSelectedServicesOnUpgrade;

    public ServiceInfoSingleProductTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Annual_Contract_196103013_CC_ED_NB.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        rcCcServiceName = data.packageFolders[1].name;
        rcCcPackage = data.packageFolders[1].packages[0];
        engageDigitalServiceName = data.packageFolders[2].name;
        engageDigitalPackage = data.packageFolders[2].packages[0];
        var officeServiceName = data.packageFolders[0].name;

        allSelectedServicesOnUpgrade = List.of(officeServiceName, rcCcServiceName, engageDigitalServiceName);
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.stepCreateContractInNGBS();
        }

        step("Find a user with 'Sales Rep - Lightning' profile and 'Enable Multi-Product Unified Billing' Feature Toggle", () -> {
            salesRepUser = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withFeatureToggles(List.of(ENABLE_MULTIPRODUCT_UNIFIED_BILLING_FT))
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);

        step("Log in as a user with 'Sales Rep - Lightning' profile and 'Enable Multi-Product Unified Billing' Feature Toggle", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser)
        );
    }

    @Test
    @TmsLink("CRM-36669")
    @DisplayName("CRM-36669 - UB. Record on 'Service Info' object is created for Quote while creating SP Quote " +
            "by user with enabled FT 'Enable Multi-Product Unified Billing'. Existing Business")
    @Description("Verify that record on 'Service Info' object is created for Quote while creating SP Quote " +
            "by user with enabled FT 'Enable Multi-Product Unified Billing'")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Quote, " +
                "keep the current package selected, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());

            masterQuoteId = wizardPage.getSelectedQuoteId();
        });

        step("2. Check that only one ServiceInfo__c record is created with the CurrencyIsoCode value from the Quote " +
                "and with Quote__r.IsMultiProductTechnicalQuote__c = false", () -> {
            var createdServiceInfoRecords = enterpriseConnectionUtils.query(
                    "SELECT Id, CurrencyIsoCode, Quote__r.IsMultiProductTechnicalQuote__c " +
                            "FROM ServiceInfo__c " +
                            "WHERE Quote__c = '" + masterQuoteId + "' ",
                    ServiceInfo__c.class);

            assertThat(createdServiceInfoRecords.size())
                    .as("Number of ServiceInfo__c records")
                    .isEqualTo(1);
            assertThat(createdServiceInfoRecords.get(0).getCurrencyIsoCode())
                    .as("ServiceInfo__c.CurrencyIsoCode value")
                    .isEqualTo(data.currencyISOCode);
            assertThat(createdServiceInfoRecords.get(0).getQuote__r().getIsMultiProductTechnicalQuote__c())
                    .as("ServiceInfo__c.Quote__r.IsMultiProductTechnicalQuote__c value")
                    .isEqualTo(false);
        });

        step("3. Open the Select Package tab, select Engage Digital and RC CC packages, and save changes", () -> {
            packagePage.openTab();
            packagePage.packageSelector.selectPackage(data.chargeTerm, rcCcServiceName, rcCcPackage);
            packagePage.packageSelector.selectPackageWithoutSeatsSetting(data.chargeTerm, engageDigitalServiceName, engageDigitalPackage);
            packagePage.saveChanges();
        });

        step("4. Check that ServiceInfo__c record is not created for Master Quote and records are created only for Technical Quotes, " +
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
                        "SELECT Id, Quote__r.IsMultiProductTechnicalQuote__c, Quote__r.ServiceName__c " +
                                "FROM ServiceInfo__c " +
                                "WHERE Quote__r.MasterQuote__c = '" + masterQuoteId + "'",
                        ServiceInfo__c.class);
                assertThat(serviceInfoForTechQuotes.size())
                        .as("Number of ServiceInfo__c records for the Tech Quotes")
                        .isEqualTo(allSelectedServicesOnUpgrade.size());

                //  this way we can check that the ServiceInfo__c records are created for all Tech Quotes
                var serviceNamesOnTechQuotes = serviceInfoForTechQuotes.stream()
                        .map(serviceInfo -> serviceInfo.getQuote__r().getServiceName__c())
                        .toList();
                assertThat(serviceNamesOnTechQuotes)
                        .as("ServiceInfo__c.Quote__r.ServiceName__c values (should contain all selected services)")
                        .containsExactlyInAnyOrderElementsOf(allSelectedServicesOnUpgrade);

                var isMultiProductTechnicalQuoteValuesOnTechQuotes = serviceInfoForTechQuotes.stream()
                        .map(serviceInfo -> serviceInfo.getQuote__r().getIsMultiProductTechnicalQuote__c())
                        .toList();
                assertThat(isMultiProductTechnicalQuoteValuesOnTechQuotes)
                        .as("ServiceInfo__c.Quote__r.IsMultiProductTechnicalQuote__c values (should be true for all records)")
                        .containsOnly(true);
            });
        });
    }
}
