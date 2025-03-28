package leads.leadconvertpage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Contact;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.DECISION_MAKER_OPPORTUNITY_CONTACT_ROLE;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.INFLUENCER_OPPORTUNITY_CONTACT_ROLE;
import static com.aquiva.autotests.rc.utilities.StringHelper.NONE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContactFactory.createContactForAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContactHelper.getFullName;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("LeadConvert")
public class ExistingAccountOpportunityRoleSectionLeadConvertPageTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Contact primaryAccountContact;
    private Contact nonPrimaryAccountContact;

    public ExistingAccountOpportunityRoleSectionLeadConvertPageTest() {
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

        step("Create New Business Opportunity for the test Account and its additional non-primary Contact via API", () -> {
            steps.quoteWizard.createOpportunity(steps.salesFlow.account, nonPrimaryAccountContact, salesRepUser);
        });

        step("Set the same Email and Phone for the Lead and the Account's primary Contact via API", () -> {
            steps.leadConvert.salesLead.setEmail(primaryAccountContact.getEmail());
            steps.leadConvert.salesLead.setPhone(primaryAccountContact.getPhone());
            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20786")
    @DisplayName("CRM-20786 - Lead Conversion - Sales Lead - Select Existing Account - Opportunity Role section")
    @Description("Verify that Opportunity Role section functions correctly when an Opportunity Role is selected")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select New Business Account (from the 'Matched Accounts' table) " +
                "and press 'Apply' button", () -> {
            leadConvertPage.selectMatchedAccount(steps.salesFlow.account.getId());

            leadConvertPage.opportunityInfoSection.shouldBe(visible);
            leadConvertPage.contactInfoSection.shouldBe(visible);
        });

        step("3. Check that in the Opportunity section there are 3 options to select " +
                "and 'Select existing Opportunity' option is preselected ", () -> {
            leadConvertPage.opportunityCreateNewOppOption.shouldBe(visible);
            leadConvertPage.opportunityDoNotCreateOppOption.shouldBe(visible);
            leadConvertPage.opportunitySelectExistingOppOption.shouldBe(visible);
            leadConvertPage.opportunitySelectExistingOppOptionInput.shouldBe(selected);
        });

        step("4. Check that in the Contact section there's 'Matched Contacts' table, it's not editable, " +
                "and primary Contact on the selected Account is preselected", () -> {
            var matchedContact = leadConvertPage.getMatchedContact(primaryAccountContact.getId());
            matchedContact.getSelectButtonInput().shouldBe(disabled, selected);
            matchedContact.getName().shouldHave(exactTextCaseSensitive(getFullName(primaryAccountContact)));

            leadConvertPage.contactInfoSelectedContactName
                    .shouldHave(exactTextCaseSensitive(getFullName(primaryAccountContact)));
        });

        step("5. Check that in the Opportunity Role section 'Select Role' picklist has 'None' value preselected " +
                "and 2 options to select: 'Decision Maker' and 'Influencer'", () -> {
            leadConvertPage.contactRolePickList.getInput().shouldHave(textCaseSensitive(NONE));

            leadConvertPage.contactRolePickList.getInput().click();
            leadConvertPage.contactRolePickList.getOptions().shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    INFLUENCER_OPPORTUNITY_CONTACT_ROLE, DECISION_MAKER_OPPORTUNITY_CONTACT_ROLE)
            );
            leadConvertPage.contactRolePickList.getInput().click();
        });

        step("6. Select an 'Influencer' value in the picklist and click 'Apply' button", () -> {
            leadConvertPage.selectOpportunityRole(INFLUENCER_OPPORTUNITY_CONTACT_ROLE);

            leadConvertPage.contactRolePickList.getInput().shouldBe(hidden);
            leadConvertPage.contactRoleInfoText.shouldHave(exactTextCaseSensitive(INFLUENCER_OPPORTUNITY_CONTACT_ROLE));
        });

        step("7. Click 'Edit' in the Opportunity Role section and check that picklist is visible and enabled again", () -> {
            leadConvertPage.contactRoleEditButton.click();

            leadConvertPage.contactRolePickList.getInput().shouldBe(visible, enabled);
        });
    }
}
