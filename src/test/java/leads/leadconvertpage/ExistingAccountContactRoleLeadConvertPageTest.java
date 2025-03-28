package leads.leadconvertpage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.SELECTED_CONTACT_ALREADY_HAS_A_ROLE_MESSAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContactHelper.getFullName;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("LeadConvert")
public class ExistingAccountContactRoleLeadConvertPageTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public ExistingAccountContactRoleLeadConvertPageTest() {
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
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);

        step("Set the same Email and Phone for the Lead and the Account's Contact via API", () -> {
            steps.leadConvert.salesLead.setEmail(steps.salesFlow.contact.getEmail());
            steps.leadConvert.salesLead.setPhone(steps.salesFlow.contact.getPhone());
            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20787")
    @DisplayName("CRM-20787 - Lead Conversion - Sales Lead - Select Existing Account - Selected Contact is Contact Role")
    @Description("Verify that Opportunity Role section functions correctly when a selected contact is a Contact Role " +
            "in the selected opportunity")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select New Business Account (from the 'Matched Accounts' table) " +
                "and press 'Apply' button in the Account section", () -> {
            leadConvertPage.selectMatchedAccount(steps.salesFlow.account.getId());

            leadConvertPage.opportunityInfoSection.shouldBe(visible);
            leadConvertPage.contactInfoSection.shouldBe(visible);
        });

        step("3. Check that 'Select Existing Opportunity' option is selected by default " +
                "and an Account's Opportunity is preselected in the Opportunity section", () -> {
            leadConvertPage.opportunitySelectExistingOppOptionInput.shouldBe(selected);
            leadConvertPage.getMatchedOpportunity(steps.quoteWizard.opportunity.getId())
                    .getSelectButtonInput()
                    .shouldBe(selected);
        });

        step("4. Check that Account's Contact is selected and Contact table is not editable in the Contact section", () -> {
            leadConvertPage.getMatchedContact(steps.salesFlow.contact.getId())
                    .getSelectButtonInput()
                    .shouldBe(selected, disabled)
                    .shouldHave(exactValue(steps.salesFlow.contact.getId()));

            leadConvertPage.contactInfoSelectedContactFullName
                    .shouldHave(exactTextCaseSensitive(getFullName(steps.salesFlow.contact)));
        });

        step("5. Check that Opportunity Role section is visible and an info message is in it", () -> {
            leadConvertPage.contactRoleSection.shouldBe(visible);
            leadConvertPage.contactRoleInfoText
                    .shouldHave(exactTextCaseSensitive(SELECTED_CONTACT_ALREADY_HAS_A_ROLE_MESSAGE));
        });
    }
}
