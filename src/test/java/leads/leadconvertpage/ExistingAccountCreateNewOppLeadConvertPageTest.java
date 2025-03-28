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
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("LeadConvert")
public class ExistingAccountCreateNewOppLeadConvertPageTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String ringCentralBrand;

    public ExistingAccountCreateNewOppLeadConvertPageTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        ringCentralBrand = data.brandName;
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
    @TmsLink("CRM-20778")
    @DisplayName("CRM-20778 - Lead Conversion - Sales Lead - Select Existing Account - Opportunity section - Create New Opportunity")
    @Description("Verify that Opportunity section functions properly when creating an opportunity for a lead that is being converted")
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

        step("3. Check that a table is called 'Choose from matched Opportunities', " +
                "there are 3 options to select and 'Select existing Opportunity' option is preselected", () -> {
            leadConvertPage.matchedOpportunitiesTableLabel.shouldHave(exactText(MATCHED_OPPORTUNITIES_TABLE_LABEL));
            leadConvertPage.opportunityCreateNewOppOption.shouldBe(visible);
            leadConvertPage.opportunityDoNotCreateOppOption.shouldBe(visible);
            leadConvertPage.opportunitySelectExistingOppOption.shouldBe(visible);
            leadConvertPage.opportunitySelectExistingOppOptionInput.shouldBe(selected);
        });

        step("4. Select 'Create New Opportunity' radio-button, " +
                "and check that all options in Opportunity section are disabled, " +
                "data for the creation of the opportunity is displayed " +
                "and Opportunity Name is the same as Account name value", () -> {
            leadConvertPage.opportunityCreateNewOppOption.shouldBe(visible).click();
            leadConvertPage.opportunityCreateNewOppOption.shouldBe(hidden);
            leadConvertPage.opportunityCreateNewOppOption.shouldBe(visible, ofSeconds(30));

            leadConvertPage.opportunityDoNotCreateOppOptionInput.shouldBe(disabled);
            leadConvertPage.opportunityCreateNewOppOptionInput.shouldBe(disabled);
            leadConvertPage.opportunitySelectExistingOppOptionInput.shouldBe(disabled);

            leadConvertPage.opportunityNameNonEditable
                    .shouldHave(exactTextCaseSensitive(steps.salesFlow.account.getName()), ofSeconds(10));
            leadConvertPage.opportunityBrandNonEditable
                    .shouldHave(exactTextCaseSensitive(ringCentralBrand), ofSeconds(10));
        });

        step("5. Press 'Edit' button in the Opportunity section, " +
                "check that all options for Opportunity section are enabled", () -> {
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.opportunityDoNotCreateOppOptionInput.shouldBe(enabled, ofSeconds(10));
            leadConvertPage.opportunityCreateNewOppOptionInput.shouldBe(enabled);
            leadConvertPage.opportunitySelectExistingOppOptionInput.shouldBe(enabled);
        });
    }
}
