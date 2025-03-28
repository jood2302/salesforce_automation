package clm;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Contact;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.contactRecordPage;
import static com.aquiva.autotests.rc.page.salesforce.contact.ContactEditModal.CONTACT_CANNOT_BE_MODIFIED_ERROR;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContactFactory.createContactForAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.QuoteFactory.createActiveSalesAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.DECISION_MAKER_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContactHelper.getFullName;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("P1")
@Tag("CLM-Phase1")
@Tag("Contact")
@Tag("FVT")
public class EditContactsForExistingBusinessAccountTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUserWithoutEditPermissionSets;
    private Contact nonPrimaryAccountContact;

    //  Test data
    private final String initialTerm;
    private final String primaryContactNewFirstName;
    private final String nonPrimaryContactNewFirstName;

    public EditContactsForExistingBusinessAccountTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_Contract_163075013.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        initialTerm = data.getInitialTerm();
        primaryContactNewFirstName = "Primary Contact New First Name";
        nonPrimaryContactNewFirstName = "Non-Primary Contact New First Name";
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.stepCreateContractInNGBS();
        }

        step("Find a user with 'Sales Rep - Lightning' profile " +
                "and w/o 'Edit Account Contact Role' and 'Edit Opportunity Contact' permission sets", () -> {
            salesRepUserWithoutEditPermissionSets = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withoutPermissionSet(EDIT_ACCOUNT_CONTACT_ROLE_PS)
                    .withoutPermissionSet(EDIT_OPPORTUNITY_CONTACT_PS)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithoutEditPermissionSets);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithoutEditPermissionSets);

        step("Create an additional Contact record " +
                "and non-primary AccountContactRole (Role = 'Decision Maker') for the test Account via API", () -> {
            nonPrimaryAccountContact = createContactForAccount(steps.salesFlow.account, salesRepUserWithoutEditPermissionSets);
            createAccountContactRole(steps.salesFlow.account, nonPrimaryAccountContact, DECISION_MAKER_ROLE, false);
        });

        step("Create an Active Sales Agreement for the test Account's Opportunity via API", () -> {
            createActiveSalesAgreement(steps.quoteWizard.opportunity, initialTerm);
        });

        step("Login as a user with 'Sales Rep - Lightning' profile " +
                "and w/o 'Edit Account Contact Role' and 'Edit Opportunity Contact' permission sets", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(salesRepUserWithoutEditPermissionSets)
        );
    }

    @Test
    @TmsLink("CRM-29093")
    @TmsLink("CRM-29094")
    @DisplayName("CRM-29093 - Primary Contact should not be editable on any of the matching condition. \n" +
            "CRM-29094 - Non-Primary Contact should be editable on any of the matching condition")
    @Description("CRM-29093 - Primary Contact should not be editable on any of the matching condition. \n" +
            "CRM-29094 - Non-Primary Contact should be editable on any of the matching condition")
    public void test() {
        //  CRM-29093
        step("1. Open the Contact record page for the primary Contact, " +
                "update First Name field value, save changes, and check the error message", () -> {
            contactRecordPage.openPage(steps.salesFlow.contact.getId());
            contactRecordPage.clickEditButton();

            var contactEditModal = contactRecordPage.getContactEditModal(getFullName(steps.salesFlow.contact));
            contactEditModal.firstNameInput.shouldBe(visible, ofSeconds(20)).setValue(primaryContactNewFirstName);
            contactEditModal.getSaveButton().click();

            contactEditModal.errorsPopUpModal.getErrorsList()
                    .shouldHave(itemWithText(CONTACT_CANNOT_BE_MODIFIED_ERROR), ofSeconds(30));
        });

        //  CRM-29094
        step("2. Open the Contact record page for the additional non-primary Contact, " +
                "update First Name field value, save changes, and check that the save was successful", () -> {
            contactRecordPage.openPage(nonPrimaryAccountContact.getId());
            contactRecordPage.clickEditButton();

            var contactEditModal = contactRecordPage.getContactEditModal(getFullName(nonPrimaryAccountContact));
            contactEditModal.firstNameInput.shouldBe(visible, ofSeconds(20)).setValue(nonPrimaryContactNewFirstName);
            contactEditModal.saveChanges();
            contactEditModal.errorsPopUpModal.getErrorsList().shouldHave(size(0));
            contactRecordPage.notification.shouldBe(hidden, ofSeconds(10));

            var updatedContact = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, FirstName " +
                            "FROM Contact " +
                            "WHERE Id = '" + nonPrimaryAccountContact.getId() + "'",
                    Contact.class);
            assertThat(updatedContact.getFirstName())
                    .as("Non-primary Contact.FirstName value")
                    .isEqualTo(nonPrimaryContactNewFirstName);
        });
    }
}
