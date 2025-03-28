package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.wizardPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.*;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("ProServ")
public class InitiateAndCancelProServButtonsTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Quote proServQuote;

    public InitiateAndCancelProServButtonsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_NoPhones.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-28557")
    @DisplayName("CRM-28557 - Initiate/Cancel ProServ button behaviour")
    @Description("Verify that button 'Initiate ProServ' creates a Quote with 'ProServ Quote' Record Type, " +
            "'Cancel ProServ' updates ProServ Status on Quote and 'Initiate ProServ' is disabled if ProServ Status is already 'Sold'")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Click 'Initiate ProServ' button, submit data for a new ProServ Quote, " +
                "check that it's created, and that it has ProServ_Status__c = 'Created', " +
                "that 'Initiate ProServ' button is hidden, 'ProServ Created' button is disabled, " +
                "and 'X' button (for ProServ engagement) is enabled", () -> {
            wizardPage.initiateProServ();

            var proServQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id, ProServ_Status__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND RecordType.Name = '" + PROSERV_QUOTE_RECORD_TYPE + "'",
                    Quote.class);

            assertThat(proServQuotes)
                    .as("Number of created ProServ quotes")
                    .hasSize(1);

            proServQuote = proServQuotes.get(0);
            assertThat(proServQuote.getProServ_Status__c())
                    .as("ProServ Quote.ProServ_Status__c value")
                    .isEqualTo(CREATED_PROSERV_STATUS);

            wizardPage.proServCreatedButton.shouldBe(visible, disabled);
            wizardPage.cancelProServButton.shouldBe(visible, enabled);
            wizardPage.initiateProServButton.shouldBe(hidden);
        });

        step("3. Click 'X' button for ProServ engagement, " +
                "enter any text into 'Please explain the ProServ team why you are cancelling the request' field, " +
                "click 'Submit' button, check that ProServ Quote.ProServ_Status__c = 'Cancelled' " +
                "and there is an 'Initiate ProServ' button instead of 'ProServ Created' and 'X' buttons ", () -> {
            wizardPage.cancelProServ();

            var proServQuoteUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ProServ_Status__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + proServQuote.getId() + "'",
                    Quote.class);
            assertThat(proServQuoteUpdated.getProServ_Status__c())
                    .as("ProServ Quote.ProServ_Status__c value")
                    .isEqualTo(CANCELLED_PROSERV_STATUS);

            wizardPage.initiateProServButton.shouldBe(visible, enabled);
            wizardPage.proServCreatedButton.shouldBe(hidden);
            wizardPage.cancelProServButton.shouldBe(hidden);
        });

        step("4. Click 'Initiate ProServ' button, " +
                "enter any text into 'Please provide additional details that Professional Services team should know about' field " +
                "and click 'Submit' button", () -> {
            wizardPage.initiateProServ();
        });

        step("5. Set ProServ Quote.ProServ_Status__c = 'Sold' via API", () -> {
            proServQuote.setProServ_Status__c(SOLD_PROSERV_STATUS);
            enterpriseConnectionUtils.update(proServQuote);
        });

        step("6. Refresh the Quote Wizard, " +
                "and check that there is a disabled 'ProServ Sold' button " +
                "instead of 'Initiate ProServ', 'ProServ Created' and 'X' (for ProServ engagement) buttons", () -> {
            refresh();
            wizardPage.waitUntilLoaded();

            wizardPage.proServSoldButton.shouldBe(visible, disabled);
            wizardPage.proServCreatedButton.shouldBe(hidden);
            wizardPage.cancelProServButton.shouldBe(hidden);
            wizardPage.initiateProServButton.shouldBe(hidden);
        });
    }
}
