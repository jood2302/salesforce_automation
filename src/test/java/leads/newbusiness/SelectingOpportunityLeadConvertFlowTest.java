package leads.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.INFLUENCER_OPPORTUNITY_CONTACT_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContactFactory.createContactForAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.SIGNATORY_ROLE;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("LeadConvert")
public class SelectingOpportunityLeadConvertFlowTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Contact nonPrimaryAccountContact;

    public SelectingOpportunityLeadConvertFlowTest() {
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

        step("Create an additional non-primary Contact record, " +
                "and set the same Email value for the Lead and the primary Contact " +
                "and the same Phone value for the Lead and the non-primary Contact (all via API)", () -> {
            nonPrimaryAccountContact = createContactForAccount(steps.salesFlow.account, salesRepUser);

            steps.leadConvert.salesLead.setEmail(steps.salesFlow.contact.getEmail());
            steps.leadConvert.salesLead.setPhone(nonPrimaryAccountContact.getPhone());

            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20792")
    @DisplayName("CRM-20792 - Lead Conversion - Sales Lead - Convert a lead selecting an opportunity.")
    @Description("Verify that a Sales Lead is converted selecting an opportunity.")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select account with created Opportunity (from the 'Matched Accounts' table) " +
                "and click 'Apply' button in Account Info section", () ->
                leadConvertPage.selectMatchedAccount(steps.salesFlow.account.getId())
        );

        step("3. Select a contact that is not the Primary contact role in the account and click 'Apply' button", () -> {
            leadConvertPage.contactInfoSection.shouldBe(visible, ofSeconds(30));
            leadConvertPage.selectMatchedContact(nonPrimaryAccountContact.getId());
        });

        step("4. Select Contact Role and click 'Apply' button in Contact Role section",
                leadConvertPage::selectDefaultOpportunityRole
        );

        step("5. Press 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("6. Check converted Lead and Opportunity Contact Roles", () -> {
            var convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT ConvertedAccountId, ConvertedOpportunityId, ConvertedContactId " +
                            "FROM Lead " +
                            "WHERE Id = '" + steps.leadConvert.salesLead.getId() + "'",
                    Lead.class);

            var nonPrimaryOpportunityContactRole = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT IsPrimary, Role " +
                            "FROM OpportunityContactRole " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND ContactId = '" + nonPrimaryAccountContact.getId() + "'",
                    OpportunityContactRole.class);

            var primaryOpportunityContactRole = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT IsPrimary, Role " +
                            "FROM OpportunityContactRole " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "' " +
                            "AND ContactId = '" + steps.salesFlow.contact.getId() + "'",
                    OpportunityContactRole.class);

            step("Check that Lead is converted", () -> {
                assertThat(convertedLead.getConvertedAccountId())
                        .as("ConvertedLead.ConvertedAccountId value")
                        .isEqualTo(steps.salesFlow.account.getId());

                assertThat(convertedLead.getConvertedContactId())
                        .as("ConvertedLead.ConvertedContactId value")
                        .isEqualTo(nonPrimaryAccountContact.getId());

                assertThat(convertedLead.getConvertedOpportunityId())
                        .as("ConvertedLead.ConvertedOpportunityId value")
                        .isEqualTo(steps.quoteWizard.opportunity.getId());
            });

            step("Check Non-Primary Opportunity Contact Role", () -> {
                assertThat(nonPrimaryOpportunityContactRole.getIsPrimary())
                        .as("OpportunityContactRole.IsPrimary value")
                        .isFalse();

                assertThat(nonPrimaryOpportunityContactRole.getRole())
                        .as("OpportunityContactRole.Role value")
                        .isEqualTo(INFLUENCER_OPPORTUNITY_CONTACT_ROLE);
            });

            step("Check Primary Opportunity Contact Role", () -> {
                assertThat(primaryOpportunityContactRole.getIsPrimary())
                        .as("OpportunityContactRole.IsPrimary value")
                        .isTrue();

                assertThat(primaryOpportunityContactRole.getRole())
                        .as("OpportunityContactRole.Role value")
                        .isEqualTo(SIGNATORY_ROLE);
            });
        });
    }
}
