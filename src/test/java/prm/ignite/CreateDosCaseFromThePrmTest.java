package prm.ignite;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.prm.DealRegistrationData;
import com.aquiva.autotests.rc.model.prm.PortalUserData;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;
import prm.PrmSteps;

import java.util.UUID;

import static base.Pages.*;
import static com.aquiva.autotests.rc.model.prm.DealRegistrationData.IGNITE_PARTNER_PROGRAM;
import static com.aquiva.autotests.rc.model.prm.PortalUserData.*;
import static com.aquiva.autotests.rc.page.prm.PortalNewCasePage.CASE_RECORD_WAS_CREATED_MESSAGE;
import static com.aquiva.autotests.rc.page.salesforce.cases.modal.CreateCaseModal.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.CaseHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.DqDealQualificationHelper.APPROVED_STATUS;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("P0")
@Tag("P1")
@Tag("CaseManagement")
@Tag("PRM")
@Tag("Ignite")
@Tag("LBO")
@Tag("Quote")
public class CreateDosCaseFromThePrmTest extends BaseTest {
    private final Steps steps;
    private final PrmSteps prmSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String leadId;
    private Lead convertedLead;
    private String accountOwnerName;

    //  Test data
    private final PortalUserData portalUserData;
    private final DealRegistrationData dealRegTestData;
    private final String leadSector;

    public CreateDosCaseFromThePrmTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);

        steps = new Steps(data);
        prmSteps = new PrmSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        portalUserData = prmSteps.getPortalUserData(IGNITE_PORTAL, PARTNER_HIERARCHY_LEVEL, PARTNER_FULL_ACCESS_PERSONA);
        dealRegTestData = prmSteps.getDealRegDataForIgnitePortal(IGNITE_PARTNER_PROGRAM);

        leadSector = "Banking";
    }

    @BeforeEach
    public void setUpTest() {
        prmSteps.initLoginToIgnitePrmPortal(portalUserData.getUsernameSandbox(), portalUserData.getPassword());
    }

    @Test
    @TmsLink("CRM-35291")
    @TmsLink("CRM-35277")
    @TmsLink("CRM-35294")
    @TmsLink("CRM-34943")
    @TmsLink("CRM-34776")
    @DisplayName("CRM-35291 - Create DOS case from the PRM Lead Convert page. \n" +
            "CRM-35277 - Create DOS case from the PRM Opportunity page. \n" +
            "CRM-35294 - Create DOS case from the PRM Quote. \n" +
            "CRM-34943 - All NB Sales Quotes are created at Ignite PRM portal as LBO Quotes. \n" +
            "CRM-34776 - Prm field creation and population")
    @Description("CRM-35291 - Verify that Deal and Order Support case can be created from the PRM Lead Convert page by clicking Create a Case button. \n" +
            "CRM-35277 - Verify that Deal and Order Support case can be created from the PRM Opportunity page by clicking Create a Case button. \n" +
            "CRM-35294 - Verify that Deal and Order Support case can be created from the Quoting Wizard of PRM Quote by clicking Create a Case button. \n" +
            "CRM-34943 - Verify that new NB Sales Quote with IsPRMQuote__c = true is created as LBO Quote " +
            "(Quote.Enabled_LBO__c is set to 'true'). Provision Toggle is 'Off' but can be switched 'On'. \n" +
            "CRM-34776 - Verify that the PRM (IsPRMQuote__c) field populated as: \n" +
            " - false if created in SF\n" +
            " - true if created in PRM")
    public void test() {
        step("1. Choose 'Deal Registration' option from the Sales tab, and press '+ New' button", () -> {
            portalGlobalNavBar.salesButton.click();
            portalGlobalNavBar.dealRegistrationButton.click();
            dealRegistrationListPage.newButton.shouldBe(visible, ofSeconds(90)).click();
        });

        step("2. Populate all the required fields, and press the 'Submit' button", () -> {
            dealRegistrationCreationPage.submitFormWithPartnerProgram(dealRegTestData);

            dealRegistrationRecordPage.header.shouldBe(visible, ofSeconds(30));
        });

        step("3. Set the Deal_Registration__c.Status__c = 'Approved' via API", () -> {
            var dealRegistration = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Deal_Registration__c " +
                            "WHERE Last_Name__c = '" + dealRegTestData.lastName + "'",
                    Deal_Registration__c.class);
            dealRegistration.setStatus__c(APPROVED_STATUS);
            enterpriseConnectionUtils.update(dealRegistration);
        });

        step("4. Set Lead.Sector__c = 'Banking', " +
                "and transfer the ownership of the Lead to the PRM portal user " +
                "with username = '" + portalUserData.username + "' via API", () -> {
            var portalUser = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM User " +
                            "WHERE Username = '" + portalUserData.getUsernameSandbox() + "'",
                    User.class);

            var lead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Lead " +
                            "WHERE Deal_Registration__r.Last_Name__c = '" + dealRegTestData.lastName + "'",
                    Lead.class);
            lead.setSector__c(leadSector);
            lead.setOwnerId(portalUser.getId());
            enterpriseConnectionUtils.update(lead);

            leadId = lead.getId();
        });

        //  CRM-35291
        step("5. Open the Lead Record page for the test lead, click 'Convert Lead' button, " +
                "and click 'Create a Case' button on the Lead Convert page", () -> {
            portalLeadRecordPage.openPage(leadId);
            portalLeadRecordPage.convertLeadButton.shouldBe(visible, ofSeconds(10)).click();

            //  to skip a weird empty duplicate iframe that may appear at the beginning 
            sleep(3_000);
            portalLeadRecordPage.leadConvertPageFrame.switchToIFrame();
            leadConvertPage.opportunityInfoEditButton.shouldBe(visible, ofSeconds(60));

            leadConvertPage.createCaseButton.shouldBe(visible, ofSeconds(30)).click();
        });

        //  CRM-35291
        step("6. Check all fields in the Portal Case Creation Page", () -> {
            switchTo().window(1);
            portalNewCasePage.header.shouldBe(visible, ofSeconds(30));
            portalNewCasePage.caseRecordTypeLookup.getSelectedEntity().shouldHave(exactTextCaseSensitive(DEAL_AND_ORDER_SUPPORT_CASE_RECORD_TYPE));
            portalNewCasePage.accountNameLookup.getInput().shouldBe(visible, disabled, empty);
            portalNewCasePage.csmLookup.getInput().shouldBe(visible, disabled, empty);
            portalNewCasePage.accountOwnerLookup.getInput().shouldBe(visible, disabled, empty);
            portalNewCasePage.opportunityReferenceLookup.getInput().shouldBe(visible, empty);
            portalNewCasePage.leadLookup.getSelectedEntity().shouldHave(exactTextCaseSensitive(dealRegTestData.firstName + " " + dealRegTestData.lastName));
            portalNewCasePage.leadLookup.getSelectedEntity().shouldNotHave(attribute("href"));
            portalNewCasePage.priorityPicklist.getInput().shouldHave(exactTextCaseSensitive(MEDIUM_PRIORITY_VALUE));
            portalNewCasePage.statusPicklist.getInput().shouldHave(exactTextCaseSensitive(NEW_STATUS_VALUE));
            portalNewCasePage.caseCategoryPicklist.getInput().shouldHave(exactTextCaseSensitive(IGNITE_PARTNER_CASE_CATEGORY));
            portalNewCasePage.caseOriginPicklist.getInput().shouldHave(exactTextCaseSensitive(LEAD_ORIGIN));
        });

        //  CRM-35291
        step("7. Populate required fields in the Portal Case Creation Page, click Save, " +
                "check that there's notification about that case record was created " +
                "and check that a new Lead's Case record is created with the fields are populated with proper values", () -> {
            createNewCaseRecordAndCheckFields(LEAD_ORIGIN_VALUE, portalUserData.accountName);
        });

        //  CRM-35277
        step("8. Populate the mandatory fields in the Opportunity Section on the Lead Convert Page, " +
                "press 'Convert' button, and wait for the PRM Opportunity page to be opened", () -> {
            switchTo().window(0);
            portalLeadRecordPage.leadConvertPageFrame.switchToIFrame();

            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();
            leadConvertPage.closeDateDatepicker.setTomorrowDate();
            leadConvertPage.opportunityInfoApplyButton.click();

            leadConvertPage.convertButton.click();
            leadConvertPage.switchFromIFrame();
            portalOpportunityDetailsPage.waitUntilLoaded();

            convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ConvertedAccount.Owner.Name, " +
                            "ConvertedAccountId, ConvertedContactId, ConvertedOpportunityId " +
                            "FROM Lead " +
                            "WHERE Id = '" + leadId + "'",
                    Lead.class);
            accountOwnerName = convertedLead.getConvertedAccount().getOwner().getName();
        });

        //  CRM-35277
        step("9. Click 'Create a Case' button and check all fields in the Portal Case Creation Page", () -> {
            portalOpportunityDetailsPage.createCaseButton.click();
            switchTo().window(1);
            portalNewCasePage.header.shouldBe(visible, ofSeconds(30));
            portalNewCasePage.caseRecordTypeLookup.getSelectedEntity().shouldHave(exactTextCaseSensitive(DEAL_AND_ORDER_SUPPORT_CASE_RECORD_TYPE));
            portalNewCasePage.accountNameLookup.getSelectedEntity().shouldHave(exactTextCaseSensitive(dealRegTestData.companyName));
            portalNewCasePage.csmLookup.getInput().shouldBe(visible, disabled, empty);
            portalNewCasePage.accountOwnerLookup.getSelectedEntity().shouldHave(exactTextCaseSensitive(accountOwnerName));
            portalNewCasePage.opportunityReferenceLookup.getSelectedEntity().shouldHave(exactTextCaseSensitive(dealRegTestData.companyName));
            portalNewCasePage.priorityPicklist.getInput().shouldHave(exactTextCaseSensitive(MEDIUM_PRIORITY_VALUE));
            portalNewCasePage.statusPicklist.getInput().shouldHave(exactTextCaseSensitive(NEW_STATUS_VALUE));
            portalNewCasePage.caseCategoryPicklist.getInput().shouldHave(exactTextCaseSensitive(IGNITE_PARTNER_CASE_CATEGORY));
            portalNewCasePage.caseOriginPicklist.getInput().shouldHave(exactTextCaseSensitive(OPPORTUNITY_ORIGIN));
        });

        //  CRM-35277
        step("10. Populate required fields in the Portal Case Creation Page, click Save, " +
                "check that there's notification about that case record was created " +
                "and check that a new Opportunity's Case record is created with the fields are populated with proper values", () -> {
            createNewCaseRecordAndCheckFields(OPPORTUNITY_ORIGIN_VALUE, dealRegTestData.companyName);
        });

        step("11. Add a new Sales Quote from the PRM Opportunity Record page", () -> {
            switchTo().window(0);

            //  TODO Remove this refresh action after the Known Issue BZS-15806 is resolved
            refresh();
            portalOpportunityDetailsPage.waitUntilLoaded();

            portalOpportunityDetailsPage.quoteSelectionWizardPageFrame.switchToIFrame();
            wizardBodyPage.mainQuoteSelectionWizardPage.waitUntilLoaded();
            steps.quoteWizard.addNewSalesQuote();
        });

        //  CRM-35294
        step("12. Select a package in the Quote Wizard, save changes, " +
                "and check that 'Create a Case' button is displayed and enabled on the Select Package tab", () -> {
            steps.quoteWizard.selectDefaultPackageFromTestData();

            //  TODO Remove the save with a spinner after the Spinnerless Flow is turned on for Portal Users in PRM, see PBC-20375
            packagePage.saveAndContinueButton.click();
            packagePage.spinner.shouldBe(visible);
            packagePage.spinner.shouldBe(hidden, ofSeconds(120));
            packagePage.errorNotification.shouldBe(hidden);

            packagePage.createCaseButton.shouldBe(visible, enabled);
        });

        //  CRM-34943, CRM-34776
        step("13. Check that Quote.Enabled_LBO__c = true and Quote.IsPRMQuote__c = true for the created Quote", () -> {
            var createdQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Enabled_LBO__c, IsPRMQuote__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + wizardPage.getSelectedQuoteId() + "'",
                    Quote.class);

            //  CRM-34943
            assertThat(createdQuote.getEnabled_LBO__c())
                    .as("Quote.Enabled_LBO__c value")
                    .isTrue();

            //  CRM-34776
            assertThat(createdQuote.getIsPRMQuote__c())
                    .as("Quote.IsPRMQuote__c value")
                    .isTrue();
        });

        //  CRM-35294
        step("14. Open the Add Products tab and check that 'Create a Case' button is displayed and enabled", () -> {
            productsPage.openTab();

            productsPage.createCaseButton.shouldBe(visible, enabled);
        });

        //  CRM-35294
        step("15. Open the Price tab, hover over 'More Actions' button " +
                "and check that 'Create a Case' button is displayed and enabled", () -> {
            cartPage.openTab();

            cartPage.moreActionsButton.hover();
            cartPage.createCaseButton.shouldBe(visible, enabled);
        });

        //  CRM-35294, CRM-34943
        step("16. Open the Quote Details tab, check that Provision toggle is turned off and enabled, " +
                "hover over 'More Actions' button, click 'Create a Case' button, switch to the Case creation page and check its fields", () -> {
            quotePage.openTab();

            //  CRM-34943
            steps.lbo.checkProvisionToggleOn(false);
            steps.lbo.checkProvisionToggleEnabled(true);

            quotePage.moreActionsButton.hover();
            //  CRM-35294
            quotePage.createCaseButton.shouldBe(visible, enabled).click();

            //  close Quote Wizard page as it's no longer necessary
            switchTo().window(1);
            closeWindow();
            switchTo().window(1);

            //  CRM-35294
            portalNewCasePage.header.shouldBe(visible, ofSeconds(30));
            portalNewCasePage.caseRecordTypeLookup.getSelectedEntity().shouldHave(exactTextCaseSensitive(DEAL_AND_ORDER_SUPPORT_CASE_RECORD_TYPE));
            portalNewCasePage.accountNameLookup.getSelectedEntity().shouldHave(exactTextCaseSensitive(dealRegTestData.companyName));
            portalNewCasePage.csmLookup.getInput().shouldBe(visible, disabled, empty);
            portalNewCasePage.accountOwnerLookup.getSelectedEntity().shouldHave(exactTextCaseSensitive(accountOwnerName));
            portalNewCasePage.opportunityReferenceLookup.getSelectedEntity().shouldHave(exactTextCaseSensitive(dealRegTestData.companyName));
            portalNewCasePage.priorityPicklist.getInput().shouldHave(exactTextCaseSensitive(MEDIUM_PRIORITY_VALUE));
            portalNewCasePage.statusPicklist.getInput().shouldHave(exactTextCaseSensitive(NEW_STATUS_VALUE));
            portalNewCasePage.caseCategoryPicklist.getInput().shouldHave(exactTextCaseSensitive(IGNITE_PARTNER_CASE_CATEGORY));
            portalNewCasePage.caseOriginPicklist.getInput().shouldHave(exactTextCaseSensitive(QUOTING_PAGE_ORIGIN));
        });

        //  CRM-35294
        step("17. Populate required fields in the Portal Case Creation Page, click Save, " +
                "check that there's notification about that case record was created " +
                "and check that a new Quote's Case record is created with the fields are populated with proper values", () -> {
            createNewCaseRecordAndCheckFields(QUOTING_PAGE_ORIGIN_VALUE, dealRegTestData.companyName);
        });

        step("18. Transfer the ownership of the converted Account, Contact and Opportunity " +
                "to the user with 'Sales Rep - Lightning' profile via API, " +
                "and log in as this user to CRM/SFDC", () -> {
            var salesRepUser = steps.salesFlow.getSalesRepUser();

            var convertedAccountToUpdate = new Account();
            convertedAccountToUpdate.setId(convertedLead.getConvertedAccountId());
            convertedAccountToUpdate.setOwnerId(salesRepUser.getId());

            var convertedContactToUpdate = new Contact();
            convertedContactToUpdate.setId(convertedLead.getConvertedContactId());
            convertedContactToUpdate.setOwnerId(salesRepUser.getId());

            var convertedOpportunityToUpdate = new Opportunity();
            convertedOpportunityToUpdate.setId(convertedLead.getConvertedOpportunityId());
            convertedOpportunityToUpdate.setOwnerId(salesRepUser.getId());

            enterpriseConnectionUtils.update(convertedAccountToUpdate, convertedContactToUpdate, convertedOpportunityToUpdate);

            steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
        });

        //  CRM-34776
        step("19. Open the Quote Wizard for the converted Opportunity, add a new Sales Quote, select a package, save changes, " +
                "and check that IsPRMQuote__c = false for the created Quote", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(convertedLead.getConvertedOpportunityId());

            var createdQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, IsPRMQuote__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + wizardPage.getSelectedQuoteId() + "'",
                    Quote.class);

            assertThat(createdQuote.getIsPRMQuote__c())
                    .as("Quote.IsPRMQuote__c value")
                    .isFalse();
        });
    }

    /**
     * <p> - Populate required fields in the Portal Case Creation Page and save the case </p>
     * <p> - Check that there's a notification about new case record is created </p>
     * <p> - Check that a new Case record is created </p>
     * <p> - Check that Case record fields are populated with proper values </p>
     *
     * @param caseOriginValue          Expected value for the Case.Origin field
     * @param accountNameExpectedValue Expected value for the Case.Account_Name__c field
     */
    private void createNewCaseRecordAndCheckFields(String caseOriginValue, String accountNameExpectedValue) {
        step("Populate Case Subcategory, Subject and Description fields, click Save, " +
                "check that there's success notification about created case record, " +
                "and close the tab", () -> {
            portalNewCasePage.caseSubcategoryPicklist.selectOption(ELA_TERMS_SUBCATEGORY);
            portalNewCasePage.descriptionInput.setValue(TEST_STRING + UUID.randomUUID());
            portalNewCasePage.subjectInput.setValue(TEST_STRING + UUID.randomUUID());
            portalNewCasePage.saveButton.click();
            portalNewCasePage.notificationBar
                    .shouldBe(visible, ofSeconds(30))
                    .shouldHave(exactTextCaseSensitive(CASE_RECORD_WAS_CREATED_MESSAGE), ofSeconds(1));

            closeWindow();
        });

        step("Check that a PRM " + caseOriginValue + "'s Case record is created and that Case record fields are populated with proper values", () -> {
            var createdCase = step("Wait until the required Case is created", () ->
                    assertWithTimeout(() -> {
                        var prmCase = enterpriseConnectionUtils.query(
                                "SELECT Id, RecordType.Name, Account_Name__c, " +
                                        "Priority, Status, Case_Category__c, " +
                                        "Case_Subcategory__c, Origin " +
                                        "FROM Case " +
                                        "WHERE Origin = '" + caseOriginValue + "' " +
                                        "AND " +
                                        "(" +
                                        "   Account_Name__c = '" + dealRegTestData.companyName + "' " +
                                        "   OR " +
                                        "   Lead__r.Name = '" + dealRegTestData.getFullName() + "'" +
                                        ")",
                                Case.class);
                        assertEquals(1, prmCase.size(),
                                "Number of Cases for the created Lead with Origin = " + caseOriginValue);
                        return prmCase.get(0);
                    }, ofSeconds(30))
            );

            assertThat(createdCase.getRecordType().getName())
                    .as("Case.RecordType.Name value")
                    .isEqualTo(DEAL_AND_ORDER_SUPPORT_RECORD_TYPE);
            assertThat(createdCase.getAccount_Name__c())
                    .as("Case.Account_Name__c value")
                    .isEqualTo(accountNameExpectedValue);
            assertThat(createdCase.getPriority())
                    .as("Case.Priority value")
                    .isEqualTo(MEDIUM_PRIORITY);
            assertThat(createdCase.getStatus())
                    .as("Case.Status value")
                    .isEqualTo(NEW_STATUS);
            assertThat(createdCase.getCase_Category__c())
                    .as("Case.Case_Category__c value")
                    .isEqualTo(IGNITE_PARTNER_CATEGORY);
            assertThat(createdCase.getOrigin())
                    .as("Case.Origin value")
                    .isEqualTo(caseOriginValue);
            assertThat(createdCase.getCase_Subcategory__c())
                    .as("Case.Case_Subcategory__c value")
                    .isEqualTo(ELA_TERMS_SUBCATEGORY);
        });
    }
}
