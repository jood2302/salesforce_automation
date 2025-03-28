package leads.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Lead;
import com.sforce.soap.enterprise.sobject.OpportunityContactRole;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.SIGNATORY_ROLE;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("LeadConvert")
public class ExistingAccountWithoutContactRoleLeadConvertFlowTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public ExistingAccountWithoutContactRoleLeadConvertFlowTest() {
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

        step("Set the same Email and Phone for the Lead and the Contact (all via API)", () -> {
            steps.leadConvert.salesLead.setEmail(steps.salesFlow.contact.getEmail());
            steps.leadConvert.salesLead.setPhone(steps.salesFlow.contact.getPhone());

            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20793")
    @DisplayName("CRM-20793 - Lead Conversion - Sales Lead - Convert a lead creating an opportunity.")
    @Description("Verify that a Sales Lead is converted creating an opportunity.")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select New Business Account (from the 'Matched Accounts' table), " +
                "and click 'Apply' button in Account Info section", () ->
                leadConvertPage.selectMatchedAccount(steps.salesFlow.account.getId())
        );

        step("3. Click 'Edit' in Opportunity Section and select 'Create new Opportunity' radiobutton", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.opportunityCreateNewOppOption.shouldBe(visible, ofSeconds(10)).click();
        });

        step("4. Populate required fields in Opportunity Section and click 'Apply' button", () -> {
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.opportunityNameInput.setValue(steps.leadConvert.salesLead.getCompany());
            leadConvertPage.closeDateDatepicker.setTomorrowDate();
            leadConvertPage.opportunityInfoApplyButton.scrollIntoView(true).click();
        });

        step("5. Check selected Contact", () ->
                steps.leadConvert.checkSelectedContact(steps.salesFlow.contact)
        );

        step("6. Select Contact Role and click 'Apply' button in Contact Role section",
                leadConvertPage::selectDefaultOpportunityRole
        );

        step("7. Press 'Convert' button", ()->
                steps.leadConvert.pressConvertButton()
        );

        step("8. Check converted Lead and Contact Role", () -> {
            var convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT ConvertedAccountId, ConvertedOpportunityId, ConvertedContactId " +
                            "FROM Lead " +
                            "WHERE Id = '" + steps.leadConvert.salesLead.getId() + "'",
                    Lead.class);

            var opportunityContactRole = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT ContactId, IsPrimary, Role " +
                            "FROM OpportunityContactRole " +
                            "WHERE OpportunityId = '" + convertedLead.getConvertedOpportunityId() + "'",
                    OpportunityContactRole.class);

            step("Check that Lead is converted", () -> {
                assertThat(convertedLead.getConvertedAccountId())
                        .as("ConvertedLead.ConvertedAccountId value")
                        .isEqualTo(steps.salesFlow.account.getId());

                assertThat(convertedLead.getConvertedContactId())
                        .as("ConvertedLead.ConvertedContactId value")
                        .isEqualTo(steps.salesFlow.contact.getId());

                assertThat(convertedLead.getConvertedOpportunityId())
                        .as("ConvertedLead.ConvertedOpportunityId value")
                        .isNotNull();
            });

            step("Check Opportunity Contact Role", () -> {
                assertThat(opportunityContactRole.getContactId())
                        .as("OpportunityContactRole.ContactId value")
                        .isEqualTo(steps.salesFlow.contact.getId());

                assertThat(opportunityContactRole.getIsPrimary())
                        .as("OpportunityContactRole.IsPrimary value")
                        .isTrue();

                assertThat(opportunityContactRole.getRole())
                        .as("OpportunityContactRole.Role value")
                        .isEqualTo(SIGNATORY_ROLE);
            });
        });
    }
}
