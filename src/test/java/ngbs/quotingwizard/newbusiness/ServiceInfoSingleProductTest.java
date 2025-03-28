package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.ServiceInfo__c;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

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
    private String createdQuoteId;

    public ServiceInfoSingleProductTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_2TypesOfDLs_RegularAndPOC.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
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

        step("Login as a user with 'Sales Rep - Lightning' profile and 'Enable Multi-Product Unified Billing' Feature Toggle", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
        });
    }

    @Test
    @TmsLink("CRM-36635")
    @DisplayName("CRM-36635 - UB. Record on 'Service Info' object is created for Quote while creating SP Quote " +
            "by user with enabled FT 'Enable Multi-Product Unified Billing'. New Business")
    @Description("Verify that record on 'Service Info' object is created for Quote while creating SP Quote " +
            "by user with enabled FT 'Enable Multi-Product Unified Billing'")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());

            createdQuoteId = wizardPage.getSelectedQuoteId();
        });

        step("2. Check that only one ServiceInfo__c record is created with the CurrencyIsoCode value from the Quote " +
                "and with Quote__r.IsMultiProductTechnicalQuote__c = false", () -> {
            var createdServiceInfoRecords = enterpriseConnectionUtils.query(
                    "SELECT Id, CurrencyIsoCode, Quote__r.IsMultiProductTechnicalQuote__c " +
                            "FROM ServiceInfo__c " +
                            "WHERE Quote__c = '" + createdQuoteId + "' ",
                    ServiceInfo__c.class);
            assertThat(createdServiceInfoRecords.size())
                    .as("Number of ServiceInfo__c records")
                    .isEqualTo(1);

            assertThat(createdServiceInfoRecords.get(0).getCurrencyIsoCode())
                    .as("ServiceInfo__c.CurrencyIsoCode value")
                    .isEqualTo(data.currencyISOCode);
            assertThat(createdServiceInfoRecords.get(0).getQuote__r().getIsMultiProductTechnicalQuote__c())
                    .as("ServiceInfo__c.Quote__r.IsMultiProductTechnicalQuote__c value")
                    .isFalse();
        });
    }
}
