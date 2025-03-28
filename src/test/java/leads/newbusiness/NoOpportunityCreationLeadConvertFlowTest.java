package leads.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Lead;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.contactRecordPage;
import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.NO_OPPORTUNITY_WILL_BE_CREATED_MESSAGE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("LeadConvert")
public class NoOpportunityCreationLeadConvertFlowTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public NoOpportunityCreationLeadConvertFlowTest() {
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

        step("Update Lead's Email and Phone with values from the Account's Contact via API", () -> {
            steps.leadConvert.salesLead.setEmail(steps.salesFlow.contact.getEmail());
            steps.leadConvert.salesLead.setPhone(steps.salesFlow.contact.getPhone());

            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20795")
    @DisplayName("CRM-20795 - Lead Conversion - Sales Lead - Convert a lead without an opportunity")
    @Description("Verify that a Sales Lead is converted without an opportunity")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select New Business Account (from the 'Matched Accounts' table), " +
                "and click 'Apply' button in Account Info section", () ->
                leadConvertPage.selectMatchedAccount(steps.salesFlow.account.getId())
        );

        step("3. Click 'Edit' in Opportunity Section, select 'Do not create Opportunity' option and check message", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.opportunityDoNotCreateOppOption.click();
            leadConvertPage.opportunityInfoLabel.shouldHave(exactTextCaseSensitive(NO_OPPORTUNITY_WILL_BE_CREATED_MESSAGE));
        });

        step("4. Check selected Contact", () ->
                steps.leadConvert.checkSelectedContact(steps.salesFlow.contact)
        );

        step("5. Press 'Convert' button", () -> {
            leadConvertPage.convertButton.click();
            leadConvertPage.spinner
                    .shouldBe(visible)
                    .shouldBe(hidden, ofSeconds(100));
            contactRecordPage.contactInfoSectionTitle.shouldBe(visible, ofSeconds(100));
        });

        step("6. Check converted Lead and Contact", () -> {
            var convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT ConvertedAccountId, ConvertedContactId, ConvertedOpportunityId " +
                            "FROM Lead " +
                            "WHERE Id = '" + steps.leadConvert.salesLead.getId() + "'",
                    Lead.class);

            assertThat(convertedLead.getConvertedAccountId())
                    .as("ConvertedLead.ConvertedAccountId value")
                    .isEqualTo(steps.salesFlow.account.getId());

            assertThat(convertedLead.getConvertedContactId())
                    .as("ConvertedLead.ConvertedContactId value")
                    .isEqualTo(steps.salesFlow.contact.getId());

            assertThat(convertedLead.getConvertedOpportunityId())
                    .as("ConvertedLead.ConvertedOpportunityId value")
                    .isNull();
        });
    }
}
