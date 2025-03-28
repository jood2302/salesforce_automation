package ngbs.quotingwizard.newbusiness.quotetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.AGREEMENT_STAGE;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.INVOICE_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContactHelper.getFullName;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("P1")
@Tag("DocuSign")
@Tag("PDFGeneration")
public class EngageDigitalDocusignPdfGenerationTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final List<String> expectedPdfTemplates;

    public EngageDigitalDocusignPdfGenerationTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_ED_Standalone_Annual_Contract.json",
                Dataset.class);
        steps = new Steps(data);

        expectedPdfTemplates = List.of("Commercial MSA Office (English)", "Initial Order Form Engage");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20874")
    @TmsLink("CRM-11581")
    @DisplayName("CRM-20874 - Primary Contact Role appears as Recipient for DocuSign. \n" +
            "CRM-11581 - Template selection contains checkboxes instead of radio buttons and allows the selection of multiple PDFs")
    @Description("CRM-20874 - Verify that the Primary Contact Role presented in 'Recipient' section in 'DocuSign' modal window. \n" +
            "CRM-11581 - Verify that Template selection contains checkboxes instead of radio buttons and allows the selection of multiple PDFs")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Quote Details tab, select Payment Method and Start Date, save changes, " +
                "set Quote Stage field to 'Agreement', and save changes", () -> {
            quotePage.openTab();
            quotePage.selectPaymentMethod(INVOICE_PAYMENT_METHOD);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();

            quotePage.stagePicklist.selectOption(AGREEMENT_STAGE);
            quotePage.saveChanges();
        });

        //  CRM-11581
        step("3. Click 'Generate PDF' button and check that 'Commercial MSA Office (English)' " +
                "and 'Initial Order Form Engage' PDF templates are displayed in the modal window and presented as checkboxes", () -> {
            quotePage.clickGeneratePdfButton();
            quotePage.pdfGenerateModal.pdfAllTemplateNames.shouldHave(exactTextsCaseSensitiveInAnyOrder(expectedPdfTemplates),
                    ofSeconds(20));
            quotePage.pdfGenerateModal.pdfOptionsCheckboxes
                    .forEach(checkbox -> checkbox.shouldHave(attribute("type", "checkbox")));
        });

        //  CRM-11581
        step("4. Select all the available PDF templates and check that they are all selected", () -> {
            quotePage.pdfGenerateModal.selectAllPdfTemplates();
            quotePage.pdfGenerateModal.pdfOptionsCheckboxes
                    .forEach(checkbox -> checkbox.shouldBe(checked));
        });

        step("5. Click 'Generate and Preview' button, click 'Attach all to Opportunity' button " +
                "and close 'Attaching files' modal window", () -> {
            quotePage.pdfGenerateModal.generateAndPreviewPdfButton.click();

            quotePage.configurePlaybooksModal.attachAllToOpportunityButton.click();
            quotePage.attachingFilesModal.successAttachedIcons.shouldHave(size(expectedPdfTemplates.size()), ofSeconds(40));
            quotePage.attachingFilesModal.closeButton.click();
        });

        //  CRM-20874
        step("6. Click Send with DocuSign button and check that Recipients section of Send with Docusign modal window " +
                "includes Contact from Account with Contact Name and Contact Email", () -> {
            quotePage.sendWithDocuSignButton.click();

            quotePage.sendWithDocusignModal.recipientsListsSection.shouldBe(visible, ofSeconds(20));
            quotePage.sendWithDocusignModal.recipientContactName
                    .shouldHave(exactTextCaseSensitive(getFullName(steps.salesFlow.contact)));
            quotePage.sendWithDocusignModal.recipientContactEmail
                    .shouldHave(exactTextCaseSensitive(steps.salesFlow.contact.getEmail()));
        });
    }
}
