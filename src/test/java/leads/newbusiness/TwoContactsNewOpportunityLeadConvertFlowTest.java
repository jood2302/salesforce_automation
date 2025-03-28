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
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.LOADING_SERVICE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContactFactory.createContactForAccount;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.SIGNATORY_ROLE;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("LeadConvert")
public class TwoContactsNewOpportunityLeadConvertFlowTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Contact nonPrimaryAccountContact;

    public TwoContactsNewOpportunityLeadConvertFlowTest() {
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

        step("Create an additional non-primary Contact record, " +
                "and set the same Email value for the Lead and the non-primary Contact " +
                "and the same Phone value for the Lead and the primary Contact (all via API)", () -> {
            nonPrimaryAccountContact = createContactForAccount(steps.salesFlow.account, salesRepUser);

            steps.leadConvert.salesLead.setEmail(nonPrimaryAccountContact.getEmail());
            steps.leadConvert.salesLead.setPhone(steps.salesFlow.contact.getPhone());

            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20798")
    @DisplayName("CRM-20798 - Lead Conversion - Sales Lead - Primary Contact Role in Account reflected on created " +
            "Opportunity upon Conversion.")
    @Description("Verify that a Primary Contact Role in the selected Account is reflected on the created Opportunity " +
            "upon Lead Conversion.")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select New Business Account (from the 'Matched Accounts' table), " +
                "and click 'Apply' button in Account Info section", () ->
                leadConvertPage.selectMatchedAccount(steps.salesFlow.account.getId())
        );

        step("3. Click 'Edit' in the Opportunity Section and select 'Create new Opportunity' radiobutton", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.opportunityCreateNewOppOption.shouldBe(visible, ofSeconds(10)).click();
        });

        step("4. Populate required fields in Opportunity Section and click 'Apply' button", () -> {
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.opportunityNameInput.setValue(steps.leadConvert.salesLead.getCompany());
            leadConvertPage.servicePickList.getSelectedOption().shouldNotHave(text(LOADING_SERVICE), ofSeconds(10));
            leadConvertPage.closeDateDatepicker.setTomorrowDate();
            leadConvertPage.opportunityInfoApplyButton.scrollIntoView(true).click();
        });

        step("5. Select a contact that is not the Primary contact role in the account and click 'Apply' button", () ->
                leadConvertPage.selectMatchedContact(nonPrimaryAccountContact.getId())
        );

        step("6. Select Contact Role and click 'Apply' button in Contact Role section",
                leadConvertPage::selectDefaultOpportunityRole
        );

        step("7. Press 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("8. Check converted Lead and Opportunity Contact Roles", () -> {
            var convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT ConvertedAccountId, ConvertedOpportunityId, ConvertedContactId " +
                            "FROM Lead " +
                            "WHERE Id = '" + steps.leadConvert.salesLead.getId() + "'",
                    Lead.class);

            var nonPrimaryOpportunityContactRole = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT IsPrimary, Role " +
                            "FROM OpportunityContactRole " +
                            "WHERE OpportunityId = '" + convertedLead.getConvertedOpportunityId() + "' " +
                            "AND ContactId = '" + nonPrimaryAccountContact.getId() + "'",
                    OpportunityContactRole.class);

            var primaryOpportunityContactRole = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT IsPrimary, Role " +
                            "FROM OpportunityContactRole " +
                            "WHERE OpportunityId = '" + convertedLead.getConvertedOpportunityId() + "' " +
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
                        .isNotNull();
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
