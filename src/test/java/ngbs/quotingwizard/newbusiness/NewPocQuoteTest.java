package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.POC_QUOTE_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.QUOTE_QUOTE_TYPE;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Selenide.*;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("P1")
@Tag("PDV")
@Tag("NGBS")
@Tag("QuoteTab")
@Tag("POC")
public class NewPocQuoteTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String salesQuoteId;
    private String pocQuoteId;

    //  Test data
    private final Package pocPackage;
    private final String packageFolder;

    public NewPocQuoteTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_2TypesOfDLs_RegularAndPOC.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        pocPackage = data.packageFolders[0].packages[1];
        packageFolder = data.packageFolders[0].name;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-10938")
    @TmsLink("CRM-9262")
    @TmsLink("CRM-9263")
    @DisplayName("CRM-10938 - Quote Stage is populated after creating a new POC Quote. \n" +
            "CRM-9262 - POC Quote creation. \n" +
            "CRM-9263 - Quote type is shown in Quote Selector.")
    @Description("CRM-10938 - To check that after User clicks 'POC Quote' - 'Add New' and creates the New POC Quote the Quote Stage is Quote. \n" +
            "CRM-9262 - Verify that if Add New button on the POC Quote section is clicked " +
            "then a Quote is created with 'POC Quote' Record Type. \n" +
            "CRM-9263 - Verify that Quote Type is shown in Quote selector (Sales/POC Quote).")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity, add a new Sales Quote, " +
                "select a package for it, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectPackageFromTestDataAndCreateQuote();
            
            salesQuoteId = wizardPage.getSelectedQuoteId();

            closeWindow();
            switchTo().window(0);
        });

        step("2. Click 'POC Quote' - 'Add New' button on the Quote selection page, " +
                "select POC package and create a new POC Quote with it", () -> {
            steps.quoteWizard.addNewPocQuote();
            packagePage.packageSelector.selectPackage(data.chargeTerm, packageFolder, pocPackage);
            packagePage.saveChanges();

            pocQuoteId = wizardPage.getSelectedQuoteId();
        });

        //  For CRM-9263, CRM-9262
        step("3. Check that there are Sales and POC quotes on Quote selection page separated by their type", () -> {
            var salesQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name " +
                            "FROM Quote " +
                            "WHERE Id = '" + salesQuoteId + "' ",
                    Quote.class);
            var pocQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name, RecordType.Name " +
                            "FROM Quote " +
                            "WHERE Id = '" + pocQuoteId + "' ",
                    Quote.class);

            switchTo().window(0);
            refresh();
            quoteSelectionWizardPage.waitUntilLoaded();

            quoteSelectionWizardPage.getQuoteName(salesQuoteId).shouldHave(exactTextCaseSensitive(salesQuote.getName()));
            quoteSelectionWizardPage.getQuoteName(pocQuoteId).shouldHave(exactTextCaseSensitive(pocQuote.getName()));

            assertThat(pocQuote.getRecordType().getName())
                    .as("POC Quote.RecordType.Name value")
                    .isEqualTo(POC_QUOTE_RECORD_TYPE);
        });

        //  For CRM-10938
        step("4. Open the Quote Details tab for the POC Quote and check the Quote Stage", () -> {
            switchTo().window(1);
            quotePage.openTab();
            quotePage.stagePicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(QUOTE_QUOTE_TYPE));
        });
    }
}