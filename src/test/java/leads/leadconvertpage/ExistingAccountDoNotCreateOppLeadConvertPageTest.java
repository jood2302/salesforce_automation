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
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.MATCHED_OPPORTUNITIES_TABLE_LABEL;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.NO_OPPORTUNITY_WILL_BE_CREATED_MESSAGE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("LeadConvert")
public class ExistingAccountDoNotCreateOppLeadConvertPageTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public ExistingAccountDoNotCreateOppLeadConvertPageTest() {
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

        step("Create two New Business Opportunities via API", () -> {
            steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
            steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        });

        //  to be able to select the existing account from the 'Matched Accounts' list instead of flaky Account Lookup
        step("Set the same email on the Sales Lead and the test Account's Contact via API", () -> {
            steps.leadConvert.salesLead.setEmail(steps.salesFlow.contact.getEmail());
            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20779")
    @DisplayName("CRM-20779 - Lead Conversion - Sales Lead - Select Existing Account - Opportunity section - Do not create Opportunity")
    @Description("Verify that Opportunity section functions properly when an opportunity is not created for a lead that is being converted")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select New Business Account (from the 'Matched Accounts' table) " +
                "and press 'Apply' button in the Account section", () -> {
            leadConvertPage.selectMatchedAccount(steps.salesFlow.account.getId());

            leadConvertPage.opportunityInfoSection.shouldBe(visible);
            leadConvertPage.contactInfoSection.shouldBe(visible);
            leadConvertPage.leadQualificationSection.shouldBe(visible);
        });

        step("3. Check that there's a table called 'Choose from matched Opportunities', " +
                "there are 3 options to select and 'Select existing Opportunity' option is preselected", () -> {
            leadConvertPage.matchedOpportunitiesTableLabel.shouldHave(exactText(MATCHED_OPPORTUNITIES_TABLE_LABEL));
            leadConvertPage.opportunityCreateNewOppOption.shouldBe(visible);
            leadConvertPage.opportunityDoNotCreateOppOption.shouldBe(visible);
            leadConvertPage.opportunitySelectExistingOppOption.shouldBe(visible);
            leadConvertPage.opportunitySelectExistingOppOptionInput.shouldBe(selected);
        });

        step("4. Select 'Do not create Opportunity' radio-button " +
                "and check that all options in Opportunity section are disabled, " +
                "that message 'No Opportunity will be created' is shown, " +
                "and that 'Opportunity Role' section is hidden", () -> {
            leadConvertPage.opportunityDoNotCreateOppOption.click();

            leadConvertPage.opportunityDoNotCreateOppOptionInput.shouldBe(disabled);
            leadConvertPage.opportunityCreateNewOppOptionInput.shouldBe(disabled);
            leadConvertPage.opportunitySelectExistingOppOptionInput.shouldBe(disabled);

            leadConvertPage.opportunityInfoLabel.shouldHave(exactTextCaseSensitive(NO_OPPORTUNITY_WILL_BE_CREATED_MESSAGE));
            leadConvertPage.contactRoleSection.shouldBe(hidden);
        });

        step("5. Press 'Edit' button in the Opportunity section, " +
                "check that all options for Opportunity section are enabled", () -> {
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.opportunityDoNotCreateOppOptionInput.shouldBe(enabled);
            leadConvertPage.opportunityCreateNewOppOptionInput.shouldBe(enabled);
            leadConvertPage.opportunitySelectExistingOppOptionInput.shouldBe(enabled);
        });
    }
}
