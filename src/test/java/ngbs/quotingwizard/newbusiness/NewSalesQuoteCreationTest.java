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

import static base.Pages.quoteSelectionWizardPage;
import static base.Pages.wizardPage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NgbsQuoteSelectionQuoteWizardPage.NO_POC_QUOTES_MESSAGE;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NgbsQuoteSelectionQuoteWizardPage.NO_SALES_QUOTES_MESSAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.SALES_QUOTE_RECORD_TYPE;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("P1")
@Tag("PDV")
@Tag("NGBS")
@Tag("UQT")
public class NewSalesQuoteCreationTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String createdQuoteId;

    public NewSalesQuoteCreationTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_2TypesOfDLs_RegularAndPOC.json",
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
    @TmsLink("CRM-1452")
    @TmsLink("CRM-9261")
    @TmsLink("CRM-26430")
    @DisplayName("CRM-1452 - New Quote button creates Quote and its available in the quotes list. \n" +
            "CRM-9261 - New Sales Quote creation\n" +
            "CRM-26430 - Opportunity without quotes")
    @Description("CRM-1452 - To check that after pressing 'New Quote' button, new Quote is created and it is available in the quotes list. \n" +
            "CRM-9261 - Verify that if Add New button on the Sales Quote section is clicked then a Quote is created " +
            "with 'Sales Quote v2' Record Type\n" +
            "CRM-26430 - Verify that the message 'No Sales (POC) Quotes found' is shown in UQT if there are no Sales/POC quotes")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity, " +
                "and check that notification messages and 'Add New' buttons are available", () -> {
            steps.quoteWizard.openQuoteWizardDirect(steps.quoteWizard.opportunity.getId());

            //  For CRM-26430
            quoteSelectionWizardPage.addNewSalesQuoteButton.shouldBe(visible, enabled);
            quoteSelectionWizardPage.addNewPocQuoteButton.shouldBe(visible, enabled);
            quoteSelectionWizardPage.noSalesQuotesNotification.shouldHave(exactTextCaseSensitive(NO_SALES_QUOTES_MESSAGE));
            quoteSelectionWizardPage.noPocQuotesNotification.shouldHave(exactTextCaseSensitive(NO_POC_QUOTES_MESSAGE));
        });

        step("2. Click 'Add New' for the Sales Quote, " +
                "check that the new tab with UQT on the Select Package tab is opened, " +
                "select a package for the quote, and save changes", () -> {
            //  For CRM-26430
            steps.quoteWizard.addNewSalesQuote();

            steps.quoteWizard.selectPackageFromTestDataAndCreateQuote();

            createdQuoteId = wizardPage.getSelectedQuoteId();
            closeWindow();
        });

        step("3. Check that created Quote is displayed in Quotes list on the Quote Selection page " +
                "and created with RecordType = 'Sales Quote v2'", () -> {
            switchTo().window(0);
            refresh();
            quoteSelectionWizardPage.waitUntilLoaded();

            //  For CRM-1452
            quoteSelectionWizardPage.getQuoteName(createdQuoteId).shouldBe(visible);
            quoteSelectionWizardPage.salesQuotes.shouldHave(size(1));

            var createdQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, RecordType.Name " +
                            "FROM Quote " +
                            "WHERE Id = '" + createdQuoteId + "' ",
                    Quote.class);

            //  For CRM-9261
            assertThat(createdQuote.getRecordType().getName())
                    .as("New Quote.RecordType.Name value")
                    .isEqualTo(SALES_QUOTE_RECORD_TYPE);
        });
    }
}
