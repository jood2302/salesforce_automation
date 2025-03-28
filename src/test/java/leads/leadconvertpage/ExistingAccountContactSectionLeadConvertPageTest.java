package leads.leadconvertpage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.page.lead.convert.MatchedItem;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Contact;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.MATCHED_CONTACTS_HEADERS;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.MATCHED_CONTACTS_TABLE_LABEL;
import static com.aquiva.autotests.rc.utilities.StringHelper.NONE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContactFactory.createContactForAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContactHelper.getFullName;
import static com.codeborne.selenide.CollectionCondition.containExactTextsCaseSensitive;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$$;
import static io.qameta.allure.Allure.step;
import static java.util.stream.Collectors.toList;

@Tag("P1")
@Tag("LeadConvert")
public class ExistingAccountContactSectionLeadConvertPageTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Contact primaryAccountContact;
    private Contact nonPrimaryAccountContact;

    public ExistingAccountContactSectionLeadConvertPageTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);

        primaryAccountContact = steps.salesFlow.contact;

        step("Create an additional non-primary Contact for the New Business Account via API", () -> {
            nonPrimaryAccountContact = createContactForAccount(steps.salesFlow.account, salesRepUser);
        });

        step("Set the same Phone value for the Lead and the non-primary Contact " +
                "and the same Email value for the Lead and the primary Contact via API", () -> {
            steps.leadConvert.salesLead.setEmail(primaryAccountContact.getEmail());
            steps.leadConvert.salesLead.setPhone(nonPrimaryAccountContact.getPhone());

            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20783")
    @DisplayName("CRM-20783 - Lead Conversion - Sales Lead - Select Existing Account - Contact section")
    @Description("Verify that Contact section functions correctly when contacts are selectable")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select New Business Account (from the 'Matched Accounts' table) " +
                "and press 'Apply' button", () ->
                leadConvertPage.selectMatchedAccount(steps.salesFlow.account.getId())
        );

        step("3. Check that the Account section is disabled, " +
                "and Opportunity, Contact and Lead Qualification sections are displayed", () -> {
            leadConvertPage.newExistingAccountToggle.$x(".//input").shouldBe(disabled);
            leadConvertPage.existingAccountSearchInput.getInput().shouldBe(disabled);

            leadConvertPage.opportunityInfoSection.shouldBe(visible);
            leadConvertPage.contactInfoSection.shouldBe(visible);
            leadConvertPage.leadQualificationSection.shouldBe(visible);
        });

        step("4. Check that 'Selected Contact' = 'None' in the Contact section", () -> {
            leadConvertPage.contactInfoSelectedContactName.shouldHave(exactTextCaseSensitive(NONE));
        });

        step("5. Check that 'Matched Contacts' table is labeled with 'Choose from matched Contacts from the chosen Account' " +
                "and includes records related to Lead and selected Account", () -> {
            leadConvertPage.matchedContactsTableLabel.shouldHave(exactTextCaseSensitive(MATCHED_CONTACTS_TABLE_LABEL));

            var actualMatchedContactsNames = leadConvertPage.getMatchedContactsList()
                    .stream()
                    .map(MatchedItem::getName)
                    .collect(toList());
            $$(actualMatchedContactsNames).shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    getFullName(primaryAccountContact),
                    getFullName(nonPrimaryAccountContact)
            ));
        });

        step("6. Check that 'Matched Contacts' table contains required columns", () -> {
            leadConvertPage.matchedContactsTableHeaders.should(containExactTextsCaseSensitive(MATCHED_CONTACTS_HEADERS));
        });

        step("7. Select a non-primary Contact of the selected Account, " +
                "and check the 'Selected Contact' value and the selection in the table", () -> {
            leadConvertPage.getMatchedContact(nonPrimaryAccountContact.getId())
                    .getSelectButton()
                    .click();

            leadConvertPage.getMatchedContact(nonPrimaryAccountContact.getId())
                    .getSelectButtonInput()
                    .shouldBe(selected);
            leadConvertPage.contactInfoSelectedContactName
                    .shouldHave(exactTextCaseSensitive(getFullName(nonPrimaryAccountContact)));
        });

        step("8. Select a primary Contact of the selected Account, " +
                "and check the 'Selected Contact' value and the selection in the table", () -> {
            leadConvertPage.getMatchedContact(primaryAccountContact.getId())
                    .getSelectButton()
                    .click();

            leadConvertPage.getMatchedContact(primaryAccountContact.getId())
                    .getSelectButtonInput()
                    .shouldBe(selected);
            leadConvertPage.getMatchedContact(nonPrimaryAccountContact.getId())
                    .getSelectButtonInput()
                    .shouldNotBe(selected);

            leadConvertPage.contactInfoSelectedContactName
                    .shouldHave(exactText(getFullName(primaryAccountContact)));
        });

        step("9. Click 'Apply' and check that 'Matched Contacts' table is not editable, " +
                "and that 'Opportunity Role' section is visible", () -> {
            leadConvertPage.contactInfoApplyButton.click();

            leadConvertPage.getMatchedContactsList()
                    .forEach(matchedItem -> matchedItem.getSelectButtonInput().shouldBe(disabled));

            leadConvertPage.contactRoleSection.shouldBe(visible);
        });
    }
}
