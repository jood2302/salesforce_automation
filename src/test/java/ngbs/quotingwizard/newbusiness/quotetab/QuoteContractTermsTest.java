package ngbs.quotingwizard.newbusiness.quotetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.QuoteTabSteps;
import org.junit.jupiter.api.*;

import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.START_AND_END_DATE_INPUT_FORMATTER;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.SALES_QUOTE_RECORD_TYPE;
import static com.codeborne.selenide.Condition.checked;
import static io.qameta.allure.Allure.step;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("NGBS")
@Tag("QuoteTab")
public class QuoteContractTermsTest extends BaseTest {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final Steps steps;
    private final QuoteTabSteps quoteTabSteps;

    //  Test data
    private final String initialTerm;
    private final String renewalTerm;

    public QuoteContractTermsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_1PhoneAnd1DL_RegularAndPOC.json",
                Dataset.class);

        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        steps = new Steps(data);
        quoteTabSteps = new QuoteTabSteps();

        var contractTerms = data.packageFolders[0].packages[0].contractTerms;
        initialTerm = contractTerms.initialTerm[0];
        renewalTerm = contractTerms.renewalTerm;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20972")
    @DisplayName("CRM-20972 - Contract values are saved to SFDC")
    @Description("Verify that Contact terms that User populates in Quote Details tab are saved in SFDC")
    public void test() {
        step("1. Open the Quote Wizard for the New Business Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Quote Details Tab, populate Main Area Code, Start Date and Contract Terms, " +
                "make sure that Auto-Renewal checkbox is checked, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);

            quotePage.setDefaultStartDate();
            quotePage.initialTermPicklist.selectOption(initialTerm);
            quotePage.renewalTermPicklist.selectOption(renewalTerm);
            quotePage.autoRenewalCheckbox.shouldBe(checked);
            quotePage.saveChanges();
        });

        step("3. Check that Contract Terms in the database are the same, as were populated on the Quote Details tab", () -> {
            quoteTabSteps.stepCheckQuoteAutoRenewal(steps.quoteWizard.opportunity.getId(), quoteTabSteps.autoRenewalWithContract);

            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Initial_Term_months__c, Term_months__c, Start_Date__c, End_Date__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND RecordType.Name = '" + SALES_QUOTE_RECORD_TYPE + "'",
                    Quote.class);

            var expectedStartDate = quotePage.startDateInput.getValue();
            var actualStartDate = START_AND_END_DATE_INPUT_FORMATTER
                    .format(quote.getStart_Date__c().toInstant().atZone(UTC));
            var expectedEndDate = quotePage.endDateInput.getValue();
            var actualEndDate = START_AND_END_DATE_INPUT_FORMATTER
                    .format(quote.getEnd_Date__c().toInstant().atZone(UTC));

            assertThat(quote.getInitial_Term_months__c())
                    .as("Quote.Initial_Term_months__c value")
                    .isEqualTo(initialTerm);
            assertThat(quote.getTerm_months__c())
                    .as("Quote.Term_months__c value")
                    .isEqualTo(renewalTerm);
            assertThat(actualStartDate)
                    .as("Quote.Start_Date__c value")
                    .isEqualTo(expectedStartDate);
            assertThat(actualEndDate)
                    .as("Quote.End_Date__c value")
                    .isEqualTo(expectedEndDate);
        });
    }
}
