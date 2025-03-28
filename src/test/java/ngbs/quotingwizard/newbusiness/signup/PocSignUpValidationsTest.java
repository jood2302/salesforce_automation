package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Dsfs__DocuSign_Status__c;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.Map;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.DocuSignStatusFactory.createDocuSignStatus;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.DocuSignStatusHelper.COMPLETED_ENVELOPE_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.DocuSignStatusHelper.NEW_ENVELOPE_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.POC_QUOTE_RECORD_TYPE;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.closeWindow;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Tag("P0")
@Tag("P1")
@Tag("NGBS")
@Tag("SignUp")
@Tag("LTR-569")
public class PocSignUpValidationsTest extends BaseTest {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final Steps steps;
    private final PocSignUpSteps pocSignUpSteps;

    private Quote pocQuote;
    private Dsfs__DocuSign_Status__c docuSignStatus;

    //  Test data
    private final Product digitalLineUnlimited;
    private final Product polycomPhone;
    private final Map<String, Package> packageFolderNameToPackageMap;

    public PocSignUpValidationsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_Annual_RegularAndPOC.json",
                Dataset.class);

        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        steps = new Steps(data);
        pocSignUpSteps = new PocSignUpSteps();

        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        polycomPhone = data.getProductByDataName("LC_HD_687");
        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[1],
                data.packageFolders[1].name, data.packageFolders[1].packages[0]
        );
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-11679")
    @TmsLink("CRM-29369")
    @TmsLink("CRM-12115")
    @DisplayName("CRM-11679 - Sign Up POC validations. \n" +
            "CRM-29369 - Multiproduct - Sign Up Validation - POC validations on Sign Up. \n" +
            "CRM-12115 - Quote Selector for the Sign Up")
    @Description("CRM-11679 - Verify that Completed Docusign Status is required for Sign Up. \n" +
            "CRM-29369 - Verify that POC Sign Up shows an error " +
            "if the DocuSign object for the POC Quote has an EnvelopeStatus other than 'Completed'. \n" +
            "CRM-12115 - Verify that the Quote Selector with Sales and POC Quotes is shown " +
            "on the Process Order modal window for the NB Opportunity that has both POC and MP Sales Quotes.\n " +
            "Verify that for the selected Sales Quote the sections " +
            "with all selected services are shown (MVP + any other services).")
    public void test() {
        step("1. Open the Opportunity record page, switch to the Quote Wizard, " +
                "add a new POC Quote and select a package for it", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewPocQuote();
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        //  CRM-11679
        step("2. Open the Add Products tab, add a phone, " +
                "set its quantity and assign it to the DL on the Price tab, save changes, " +
                "and check that there are no errors on the Quote", () -> {
            steps.quoteWizard.addProductsOnProductsTab(polycomPhone);

            cartPage.openTab();
            steps.cartTab.setUpQuantities(digitalLineUnlimited, polycomPhone);
            steps.cartTab.assignDevicesToDLAndSave(polycomPhone.name, digitalLineUnlimited.name, steps.quoteWizard.localAreaCode,
                    polycomPhone.quantity);

            pocQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, isQuoteHasErrors__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND RecordType.Name = '" + POC_QUOTE_RECORD_TYPE + "'",
                    Quote.class);
            assertThat(pocQuote.getIsQuoteHasErrors__c())
                    .as("Quote.isQuoteHasErrors__c value")
                    .isFalse();
        });

        //  CRM-11679
        step("3. Click 'Process Order' button on the Opportunity's record page, " +
                "and check that the error notification is shown on 'Data validation' step", () -> {
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications
                    .shouldHave(exactTexts(format(YOU_NEED_TO_HAVE_COMPLETED_ENVELOPE_ERROR, MVP_SERVICE)));
            opportunityPage.processOrderModal.mvpPreparingDataActiveStep.shouldHave(exactTextCaseSensitive(DATA_VALIDATION_STEP));
            opportunityPage.processOrderModal.closeWindow();
        });

        step("4. Open the Quote Wizard to add a new Sales Quote, select MVP and Engage Digital packages for it, and save changes", () -> {
            switchTo().window(1);
            steps.quoteWizard.prepareOpportunityForMultiProduct(steps.quoteWizard.opportunity.getId(), packageFolderNameToPackageMap);
        });

        //  CRM-29369
        step("5. Click 'Process Order' button on the Opportunity's record page, select POC Quote in the modal window, " +
                "click 'Continue' button and check that the error notification about missing Completed Envelope is shown", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(10));
            opportunityPage.processOrderModal.pocQuoteRadioButton.click();
            opportunityPage.processOrderModal.continueButton.click();

            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications
                    .shouldHave(exactTexts(format(YOU_NEED_TO_HAVE_COMPLETED_ENVELOPE_ERROR, MVP_SERVICE)));
            opportunityPage.processOrderModal.closeWindow();
        });

        step("6. Create a new DocuSign Status object for the POC Quote with Envelope Status = 'New' via API", () -> {
            docuSignStatus = createDocuSignStatus(steps.salesFlow.account.getId(), steps.quoteWizard.opportunity.getId(),
                    pocQuote.getId(), NEW_ENVELOPE_STATUS);
        });

        //  CRM-29369
        step("7. Click 'Process Order' button on the Opportunity's record page, select POC Quote in the modal window, " +
                "click 'Continue' button and check that the error notification about missing Completed Envelope is shown", () -> {
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(10));
            opportunityPage.processOrderModal.pocQuoteRadioButton.click();
            opportunityPage.processOrderModal.continueButton.click();

            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60)).click();
            opportunityPage.processOrderModal.errorNotifications
                    .shouldHave(exactTexts(format(YOU_NEED_TO_HAVE_COMPLETED_ENVELOPE_ERROR, MVP_SERVICE)));
            opportunityPage.processOrderModal.closeWindow();
        });

        step("8. Set Envelope Status = 'Completed' on the DocuSign Status via API", () -> {
            docuSignStatus.setDsfs__Envelope_Status__c(COMPLETED_ENVELOPE_STATUS);
            enterpriseConnectionUtils.update(docuSignStatus);
        });

        step("9. Open the POC Quote in the Quote Wizard, create POC Approval, " +
                "and set Envelope Status = 'Completed' on the DocuSign Status via API", () -> {
            switchTo().window(1);
            wizardPage.openPage(steps.quoteWizard.opportunity.getId(), pocQuote.getId());
            quotePage.openTab();
            quotePage.createPocApproval(pocSignUpSteps.linkToSignedEvaluationAgreement);
            closeWindow();

            docuSignStatus.setDsfs__Envelope_Status__c(COMPLETED_ENVELOPE_STATUS);
            enterpriseConnectionUtils.update(docuSignStatus);
        });

        //  CRM-12115
        step("10. Click 'Process Order' button on the Opportunity record page, " +
                "check that Quote Selector is shown with Sales Quote preselected " +
                "and both MVP and Engage Digital sections are displayed for Sales Quote in the modal window", () -> {
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.salesQuoteRadioButtonInput.shouldBe(selected);

            opportunityPage.processOrderModal.mvpSection.shouldBe(visible);
            opportunityPage.processOrderModal.engageDigitalSection.shouldBe(visible);
        });

        //  CRM-12115
        step("11. Select POC Quote and check that only MVP section is displayed in the Process Order modal window", () -> {
            opportunityPage.processOrderModal.pocQuoteRadioButton.click();
            opportunityPage.processOrderModal.mvpSection.shouldBe(visible);
            opportunityPage.processOrderModal.engageDigitalSection.shouldNotBe(visible);
        });
    }
}
