package leads.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.accountRecordPage;
import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.NO_CONTACTS_MATCH_THE_CHOSEN_ACCOUNT_MESSAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewCustomerAccountWithoutContactInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.SIGNATORY_ROLE;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("LeadConvert")
public class CreatingContactLeadConvertFlowTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Account newBusinessAccount;

    public CreatingContactLeadConvertFlowTest() {
        data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);

        step("Create New Business Account (without Contact record) via API", () -> {
            newBusinessAccount = createNewCustomerAccountWithoutContactInSFDC(salesRepUser, new AccountData(data));
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);

        //  this will help stabilize the behavior of Account lookup on the Lead Convert page 
        //  as it returns the "recent" accounts in the search results
        step("Open the record page for the test New Business Account", () -> {
            accountRecordPage.openPage(newBusinessAccount.getId());
        });
    }

    @Test
    @TmsLink("CRM-20802")
    @DisplayName("CRM-20802 - Lead Conversion - Sales Lead - Convert a lead creating a contact")
    @Description("Verify that a lead is converted creating a contact")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select account that exists in SFDC, and click 'Apply' button in Account Info section " +
                "and verify that Contact section is empty. No matching contacts found.", () -> {
            leadConvertPage.existingAccountSearchInput.selectItemInCombobox(newBusinessAccount.getName());
            leadConvertPage.accountInfoApplyButton.click();

            leadConvertPage.contactInfoSection.shouldBe(visible, ofSeconds(30));
            leadConvertPage.contactDetailsInfoSection.shouldHave(
                    exactTextCaseSensitive(NO_CONTACTS_MATCH_THE_CHOSEN_ACCOUNT_MESSAGE));
        });

        step("3. Click 'Edit' in Opportunity Section, check that Business Identity = 'RingCentral Inc.', " +
                "populate Close Date field, and click 'Apply'", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.businessIdentityPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(data.getBusinessIdentityName()));
            leadConvertPage.closeDateDatepicker.setTomorrowDate();

            leadConvertPage.opportunityInfoApplyButton.scrollIntoView(true).click();
        });

        step("4. Select Contact Role and click 'Apply' button in Contact Role section",
                leadConvertPage::selectDefaultOpportunityRole
        );

        step("5. Press 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("6. Check converted Lead and created Contact", () -> {
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

            step("Check that Lead is converted for Account and new Contact is created", () -> {
                assertThat(convertedLead.getConvertedAccountId())
                        .as("ConvertedLead.ConvertedAccountId value")
                        .isEqualTo(newBusinessAccount.getId());

                assertThat(convertedLead.getConvertedContactId())
                        .as("ConvertedLead.ConvertedContactId value")
                        .isNotNull();

                assertThat(convertedLead.getConvertedOpportunityId())
                        .as("ConvertedLead.ConvertedOpportunityId value")
                        .isNotNull();
            });

            step("Check that created Contact has become Primary Contact Role for created Opportunity", () -> {
                assertThat(opportunityContactRole.getContactId())
                        .as("OpportunityContactRole.ContactId value")
                        .isEqualTo(convertedLead.getConvertedContactId());

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
