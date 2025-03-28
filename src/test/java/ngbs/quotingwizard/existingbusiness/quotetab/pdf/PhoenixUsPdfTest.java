package ngbs.quotingwizard.existingbusiness.quotetab.pdf;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getAccountInNGBS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.ACTIVE_QUOTE_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.AGREEMENT_QUOTE_TYPE;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Selenide.refresh;
import static com.codeborne.selenide.SetValueOptions.withDate;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("Phoenix")
@Tag("PDFGeneration")
public class PhoenixUsPdfTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final List<String> expectedPdfTemplatesNoContract;
    private final List<String> expectedPdfTemplatesWithContractDraft;
    private final List<String> expectedPdfTemplatesWithContractActive;

    public PhoenixUsPdfTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_Meetings_Phoenix_Monthly_NonContract_82741013.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        expectedPdfTemplatesNoContract = List.of("Quote Short | Change Order | English",
                "Quote Short | Change Order | English | Current Subscription");
        expectedPdfTemplatesWithContractDraft = List.of("MSA", "Initial Order Form");
        expectedPdfTemplatesWithContractActive = List.of("Simplified Change Order Form",
                "Incremental Change Order Form", "Detailed Change Order Form");
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-23597")
    @DisplayName("CRM-23597 - PDF templates for Phoenix (Existing Business with and without contract)")
    @Description("Verify generated PDF templates for Phoenix opportunity (Existing Business with and without contract)")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Quote Details tab, click 'Generate PDF' button and check list of PDF Templates", () -> {
            quotePage.openTab();
            quotePage.clickGeneratePdfButton();
            quotePage.pdfGenerateModal.pdfAllTemplateNames
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(expectedPdfTemplatesNoContract), ofSeconds(60));

            quotePage.pdfGenerateModal.cancelButton.click();
        });

        step("3. Open the Select Package tab, and a add contract", () -> {
            packagePage.openTab();
            packagePage.packageSelector.setContractSelected(true);
        });

        step("4. Open the Quote Details tab, fill 'Start Date' field, save changes, " +
                "set Quote Stage = 'Agreement', and save changes", () -> {
            quotePage.openTab();
            var billingDate = getAccountInNGBS(data.billingId).getMainPackage().getBillingStartDateAsLocalDate();
            quotePage.startDateInput.setValue(withDate(billingDate));
            quotePage.saveChanges();

            quotePage.stagePicklist.selectOption(AGREEMENT_QUOTE_TYPE);
            quotePage.saveChanges();
        });

        step("5. Click 'Generate PDF' button and check the list of PDF Templates", () -> {
            quotePage.clickGeneratePdfButton();
            quotePage.pdfGenerateModal.pdfAllTemplateNames
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(expectedPdfTemplatesWithContractDraft), ofSeconds(60));

            quotePage.pdfGenerateModal.cancelButton.click();
        });

        step("6. Set Quote.Status = 'Active' via API, refresh the page and open the Quote Details tab again", () -> {
            var primarySalesQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);
            primarySalesQuote.setStatus(ACTIVE_QUOTE_STATUS);

            enterpriseConnectionUtils.update(primarySalesQuote);

            //  We need to refresh the page after setting the active status to display the new list of PDF templates.
            refresh();
            wizardPage.waitUntilLoaded();
            cartPage.waitUntilLoaded();
            quotePage.openTab();
        });

        step("7. Click 'Generate PDF' button and check the list of PDF Templates", () -> {
            quotePage.clickGeneratePdfButton();
            quotePage.pdfGenerateModal.pdfAllTemplateNames
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(expectedPdfTemplatesWithContractActive), ofSeconds(60));
        });
    }
}
