package ngbs.quotingwizard.newbusiness.quotetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.packagePage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.AGREEMENT_QUOTE_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.QUOTE_QUOTE_TYPE;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("Phoenix")
@Tag("Opportunity")
@Tag("PDFGeneration")
public class PdfTemplatesForPhoenixOpportunityTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final String initialTerm;
    private final List<String> expectedPdfTemplatesWithContract;
    private final List<String> expectedPdfTemplatesNoContract;

    public PdfTemplatesForPhoenixOpportunityTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_Meetings_Phoenix_Annual_Contract.json",
                Dataset.class);

        steps = new Steps(data);

        initialTerm = data.packageFolders[0].packages[0].contractTerms.initialTerm[0];
        expectedPdfTemplatesWithContract = List.of("MSA", "Initial Order Form");
        expectedPdfTemplatesNoContract = List.of("Initial Quote");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-23508")
    @DisplayName("CRM-23508 - Phoenix: PDF templates for Phoenix (New Business with and without contract)")
    @Description("Verify generating PDF templates for Phoenix oppty (New Business with and without contract)")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        //  CRM-23508
        step("2. Open the Quote Details tab and set Quote Stage field to 'Agreement', " +
                "fill 'Start Date' and 'Initial Term' fields, " +
                "and save changes", () -> {
            quotePage.openTab();
            quotePage.initialTermPicklist.selectOption(initialTerm);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();
            quotePage.stagePicklist.selectOption(AGREEMENT_QUOTE_TYPE);
            quotePage.saveChanges();
        });

        step("3. Click 'Generate PDF' and check the list of PDF Templates", () -> {
            quotePage.generatePdfButton.click();
            quotePage.pdfGenerateModal.pdfAllTemplateNames
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(expectedPdfTemplatesWithContract), ofSeconds(60));

            quotePage.pdfGenerateModal.cancelButton.click();
        });

        step("4. Set Quote Stage field to 'Quote', save changes, " +
                "open the Select Package tab, remove the contract, and save/confirm changes", () -> {
            quotePage.stagePicklist.selectOption(QUOTE_QUOTE_TYPE);
            quotePage.saveChanges();

            packagePage.openTab();
            packagePage.packageSelector.setContractSelected(false);
            packagePage.saveChanges();
        });

        step("5. Open the Quote Details tab, click 'Generate PDF', and check the list of PDF Templates", () -> {
            quotePage.openTab();
            quotePage.generatePdfButton.click();
            quotePage.pdfGenerateModal.pdfAllTemplateNames
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(expectedPdfTemplatesNoContract), ofSeconds(60));
        });
    }
}
