package ngbs.quotingwizard;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.salesforce.cases.modal.CreateCaseModal.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewPartnerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.CaseHelper.*;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.closeWindow;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("P1")
@Tag("CaseManagement")
@Tag("CaseCreationPage")
public class DosCaseCreationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUser;
    private Account account;
    private Opportunity opportunity;
    private String csmExpectedValue;
    private String accountOwnerName;
    private String parentPartnerAccountName;

    public DosCaseCreationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_NoDLs.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        account = steps.salesFlow.account;
        accountOwnerName = dealDeskUser.getName();

        steps.quoteWizard.createOpportunity(account, steps.salesFlow.contact, dealDeskUser);
        opportunity = steps.quoteWizard.opportunity;

        step("Create a new Partner Account, populate Customer Account.Parent_Partner_Account__c field with its ID, " +
                "and populate Account.CSM__c field (all via API)", () -> {
            var partnerAccount = createNewPartnerAccountInSFDC(dealDeskUser, new AccountData(data));
            account.setParent_Partner_Account__c(partnerAccount.getId());
            parentPartnerAccountName = partnerAccount.getName();

            //  Retrieve ID of a user with different from current user's profile
            var salesRepUser = steps.salesFlow.getSalesRepUser();
            account.setCSM__c(salesRepUser.getId());
            csmExpectedValue = salesRepUser.getName();

            enterpriseConnectionUtils.update(account);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-32813")
    @TmsLink("CRM-32955")
    @TmsLink("CRM-32866")
    @TmsLink("CRM-32878")
    @DisplayName("CRM-32813 - Create DOS case from the Opportunity page. \n" +
            "CRM-32955 - Assignment rules for cases with category = 'System Issue', from 'Case'. \n" +
            "CRM-32866 - Create DOS case from the Quoting Wizard. \n" +
            "CRM-32878 - Create DOS case from the Process Order page")
    @Description("CRM-32813 - Verify that Deal and Order Support case can be created from the opportunity page " +
            "by clicking Create a Case button. \n" +
            "CRM-32955 - Verify that DOS cases are assigned to the Biz Serv Innovation Team queue " +
            "and change their record type to Internal Business Services when specific conditions are met:\n" +
            " - Case category = System Issue\n" +
            " - Case origin = Case. \n\n" +
            "CRM-32866 - Verify that Deal and Order Support case can be created from the quoting wizard by clicking Create a Case button. \n" +
            "CRM-32878 - Verify that Deal and Order Support case can be created from the process order page by clicking Create a Case button")
    public void test() {
        step("1. Open the Opportunity record page, click 'Create a Case' button, " +
                "and check that Case creation modal is opened in the new tab", () -> {
            opportunityPage.openPage(opportunity.getId());

            opportunityPage.clickCreateCaseButton();
            createCaseModal.waitUntilLoaded();
        });

        //  CRM-32813
        step("2. Check field values in Case creation modal", () -> {
            createCaseModal.caseRecordTypeLookupInput.shouldHave(exactValue(DEAL_AND_ORDER_SUPPORT_RECORD_TYPE));
            createCaseModal.opportunityReferenceLookup.getInput().shouldHave(exactValue(opportunity.getName()));
            createCaseModal.caseCategoryPicklist.getInput().shouldHave(exactTextCaseSensitive(APPROVAL_REQUEST_CATEGORY));
            createCaseModal.caseOriginLookupInput.shouldHave(exactTextCaseSensitive(OPPORTUNITY_ORIGIN));

            createCaseModal.accountLookup.getInput().shouldHave(exactValue(account.getName()));
            createCaseModal.csmLookup.getInput().shouldHave(exactValue(csmExpectedValue));
            createCaseModal.accountOwnerLookup.getInput().shouldHave(exactValue(accountOwnerName));
            createCaseModal.parentPartnerAccountLookup.getInput().shouldHave(exactValue(parentPartnerAccountName));
        });

        //  CRM-32813
        step("3. Populate Case Subcategory, Subject and Description fields, click Save button " +
                "and check that the new Case is created with the correct Record Type and the Owner", () -> {
            createCaseModal.caseSubcategoryPicklist.selectOption(DEAL_REVIEW_REQUEST_SUBCATEGORY);
            createCaseModal.subjectInput.setValue(TEST_STRING + UUID.randomUUID());
            createCaseModal.description.setValue(TEST_STRING + UUID.randomUUID());
            //  to bypass a validation rule (see PBC-16363)
            createCaseModal.caseDependenciesPicklist.selectOption(NONE_CASE_DEPENDENCIES);
            createCaseModal.saveChanges();

            casePage.waitUntilLoaded();

            checkCreatedDosCaseFields();
        });

        step("4. Click 'Create a Case' button on the Case record page to open Case creation modal window", () -> {
            casePage.clickCreateCaseButton();
            createCaseModal.waitUntilLoaded();
        });

        //  CRM-32955
        step("5. Populate necessary fields in Case creation modal window, click 'Save' button " +
                "and check that a new Case is created with 'Internal Business Services' record type " +
                "and 'Biz Serv Innovation Team' owner", () -> {
            createCaseModal.caseCategoryPicklist.selectOption(SYSTEM_ISSUE_CATEGORY);
            createCaseModal.caseSubcategoryPicklist.selectOption(DEMAND_FUNNEL_SUBCATEGORY);

            createCaseModal.subjectInput.setValue(TEST_STRING + UUID.randomUUID());
            createCaseModal.affectedUserLookup.selectItemInCombobox(dealDeskUser.getName());
            createCaseModal.stepsToReproduceTextArea.setValue(TEST_STRING + UUID.randomUUID());
            createCaseModal.actualResultTextArea.setValue(TEST_STRING + UUID.randomUUID());
            createCaseModal.expectedResultTextArea.setValue(TEST_STRING + UUID.randomUUID());
            createCaseModal.linkToAffectedRecordInput.setValue(TEST_STRING);

            createCaseModal.saveChanges();

            casePage.waitUntilLoaded();

            var createdCase = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, RecordType.Name, Owner.Name " +
                            "FROM Case " +
                            "WHERE Id = '" + casePage.getCurrentRecordId() + "'",
                    Case.class);
            assertThat(createdCase.getRecordType().getName())
                    .as("Case.RecordType.Name value")
                    .isEqualTo(INTERNAL_BUSINESS_SERVICES_RECORD_TYPE);
            assertThat(((Name) createdCase.getOwner()).getName())
                    .as("Case.Owner.Name value")
                    .isEqualTo(BIZ_SERV_INNOVATION_TEAM_OWNER);
        });

        //  CRM-32866
        step("6. Open the Quote Wizard for the test Opportunity " +
                "and check that 'Create a Case' button is displayed and enabled", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(opportunity.getId());

            packagePage.createCaseButton.shouldBe(visible, enabled);
        });

        //  CRM-32866
        step("7. Select a package, save changes and check that 'Create a Case' button is displayed and enabled", () -> {
            steps.quoteWizard.selectPackageFromTestDataAndCreateQuote();

            packagePage.createCaseButton.shouldBe(visible, enabled);
        });

        //  CRM-32866
        step("8. Open the Add Products tab and check that 'Create a Case' button is displayed and enabled", () -> {
            productsPage.openTab();

            productsPage.createCaseButton.shouldBe(visible, enabled);
        });

        //  CRM-32866
        step("9. Open the Price tab, hover over 'More Actions' button " +
                "and check that 'Create a Case' button is displayed and enabled", () -> {
            cartPage.openTab();

            cartPage.moreActionsButton.hover();
            cartPage.createCaseButton.shouldBe(visible, enabled);
        });

        //  CRM-32866
        step("10. Open the Quote Details tab, hover over 'More Actions' button, click 'Create a Case' button, " +
                "switch to the Case creation modal window and check its fields", () -> {
            quotePage.openTab();
            quotePage.moreActionsButton.hover();
            quotePage.createCaseButton.shouldBe(visible, enabled).click();

            switchTo().window(1);
            createCaseModal.waitUntilLoaded();

            createCaseModal.caseRecordTypeLookupInput.shouldHave(exactValue(DEAL_AND_ORDER_SUPPORT_RECORD_TYPE));
            createCaseModal.caseCategoryPicklist.getInput().shouldHave(exactTextCaseSensitive(QUOTING_ASSISTANCE_CATEGORY));
            createCaseModal.caseOriginLookupInput.shouldHave(exactTextCaseSensitive(QUOTING_PAGE_ORIGIN));
            createCaseModal.accountLookup.getInput().shouldHave(exactValue(account.getName()));
            createCaseModal.csmLookup.getInput().shouldHave(exactValue(csmExpectedValue));
            createCaseModal.accountOwnerLookup.getInput().shouldHave(exactValue(accountOwnerName));
            createCaseModal.parentPartnerAccountLookup.getInput().shouldHave(exactValue(parentPartnerAccountName));
        });

        //  CRM-32866
        step("11. Populate all required fields (Case Subcategory, Subject and Description), click Save button " +
                "and check that the new Case is created with the correct Record Type and the Owner", () -> {
            createCaseModal.caseSubcategoryPicklist.selectOption(CONTACT_CENTER_SUBCATEGORY);
            createCaseModal.subjectInput.setValue(TEST_STRING + UUID.randomUUID());
            createCaseModal.description.setValue(TEST_STRING + UUID.randomUUID());
            //  to bypass a validation rule (see PBC-16363)
            createCaseModal.caseDependenciesPicklist.selectOption(NONE_CASE_DEPENDENCIES);
            createCaseModal.saveChanges();

            casePage.waitUntilLoaded();

            checkCreatedDosCaseFields();

            closeWindow();
            switchTo().window(0);
        });

        //  CRM-32878
        step("12. Open the Opportunity record page, click 'Process Order' button, " +
                "click 'Create a Case' button in the Process Order modal window and check Case creation modal window fields", () -> {
            opportunityPage.openPage(opportunity.getId());
            opportunityPage.clickProcessOrderButton();

            opportunityPage.processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(20));
            opportunityPage.processOrderModal.spinner.shouldBe(hidden, ofSeconds(20));
            opportunityPage.processOrderModal.createCaseButton.click();
            switchTo().window(1);
            createCaseModal.waitUntilLoaded();

            createCaseModal.caseRecordTypeLookupInput.shouldHave(exactValue(DEAL_AND_ORDER_SUPPORT_RECORD_TYPE));
            createCaseModal.accountLookup.getInput().shouldHave(exactValue(account.getName()));
            createCaseModal.opportunityReferenceLookup.getInput().shouldHave(exactValue(opportunity.getName()));
            createCaseModal.caseCategoryPicklist.getInput().shouldHave(exactTextCaseSensitive(PROVISIONING_ASSISTANCE_CATEGORY));
            createCaseModal.caseOriginLookupInput.shouldHave(exactTextCaseSensitive(PROCESS_ORDER_ORIGIN));
            createCaseModal.csmLookup.getInput().shouldHave(exactValue(csmExpectedValue));
            createCaseModal.accountOwnerLookup.getInput().shouldHave(exactValue(accountOwnerName));
            createCaseModal.parentPartnerAccountLookup.getInput().shouldHave(exactValue(parentPartnerAccountName));
        });

        //  CRM-32878
        step("13. Populate all required fields (Case Subcategory, Subject and Description), click Save button " +
                "and check that the new Case is created with the correct Record Type and the Owner", () -> {
            createCaseModal.caseSubcategoryPicklist.selectOption(CONTACT_CENTER_SUBCATEGORY);
            createCaseModal.subjectInput.setValue(TEST_STRING + UUID.randomUUID());
            createCaseModal.description.setValue(TEST_STRING + UUID.randomUUID());
            //  to bypass a validation rule (see PBC-16363)
            createCaseModal.caseDependenciesPicklist.selectOption(NONE_CASE_DEPENDENCIES);
            createCaseModal.saveChanges();

            casePage.waitUntilLoaded();

            checkCreatedDosCaseFields();
        });
    }

    /**
     * Check that a new Case record is created
     * with 'Deal and Order Support' record type
     * and populated OwnerId field.
     *
     * @throws ConnectionException in case of errors while accessing API
     */
    private void checkCreatedDosCaseFields() throws ConnectionException {
        var createdCase = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, RecordType.Name, OwnerId " +
                        "FROM Case " +
                        "WHERE Id = '" + casePage.getCurrentRecordId() + "'",
                Case.class);
        assertThat(createdCase.getRecordType().getName())
                .as("Case.RecordType.Name value")
                .isEqualTo(DEAL_AND_ORDER_SUPPORT_RECORD_TYPE);
        assertThat(createdCase.getOwnerId())
                .as("Case.OwnerId value")
                .isNotBlank();
    }
}
