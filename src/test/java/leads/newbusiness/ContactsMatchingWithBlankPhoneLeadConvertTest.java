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

import static base.Pages.accountRecordPage;
import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.NO_CONTACTS_MATCH_THE_CHOSEN_ACCOUNT_MESSAGE;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("LeadConvert")
public class ContactsMatchingWithBlankPhoneLeadConvertTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public ContactsMatchingWithBlankPhoneLeadConvertTest() {
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

        step("Set a blank phone for the Contact and Sales Lead via API", () -> {
            steps.salesFlow.contact.setPhone(EMPTY_STRING);
            steps.leadConvert.salesLead.setPhone(EMPTY_STRING);

            enterpriseConnectionUtils.update(steps.leadConvert.salesLead, steps.salesFlow.contact);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);

        //  this will help stabilize the behavior of Account lookup on the Lead Convert page 
        //  as it returns the "recent" accounts in the search results
        step("Open the record page for the test New Business Account", () -> {
            accountRecordPage.openPage(steps.salesFlow.account.getId());
        });
    }

    @Test
    @TmsLink("CRM-21664")
    @DisplayName("CRM-21664 - Matching contacts when Sales Lead has blank phone number")
    @Description("Verify that contacts are not matched if both lead and contact have blank phone number")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select a New Business Account that exists in SFDC, click 'Apply' button in Account Info section " +
                "and verify that Contact section is empty. No matching contacts found.", () -> {
            leadConvertPage.existingAccountSearchInput.selectItemInCombobox(steps.salesFlow.account.getName());
            leadConvertPage.accountInfoApplyButton.click();

            leadConvertPage.contactInfoSection.shouldBe(visible, ofSeconds(30));
            leadConvertPage.contactDetailsInfoSection
                    .shouldHave(exactTextCaseSensitive(NO_CONTACTS_MATCH_THE_CHOSEN_ACCOUNT_MESSAGE));
        });

        step("3. Click 'Edit' button in the Opportunity Info section, populate Close Date field " +
                "and click 'Apply' button", () -> {
            leadConvertPage.opportunityInfoEditButton.click();
            leadConvertPage.closeDateDatepicker.setTomorrowDate();
            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("4. Select 'Influencer' as a Contact Role and click 'Apply' button in Contact Role section",
                leadConvertPage::selectDefaultOpportunityRole
        );

        step("5. Press 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("6. Check converted Lead and created Contact ", () -> {
            var convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ConvertedAccountId, ConvertedOpportunityId, ConvertedContactId " +
                            "FROM Lead " +
                            "WHERE Id = '" + steps.leadConvert.salesLead.getId() + "'",
                    Lead.class);

            step("Check that Lead is converted for Account and new Contact created", () -> {
                assertThat(convertedLead.getConvertedAccountId())
                        .as("ConvertedLead.ConvertedAccountId")
                        .isEqualTo(steps.salesFlow.account.getId());

                assertThat(convertedLead.getConvertedContactId())
                        .as("ConvertedLead.ConvertedContactId value")
                        .isNotNull()
                        .isNotEqualTo(steps.salesFlow.contact.getId());

                assertThat(convertedLead.getConvertedOpportunityId())
                        .as("ConvertedLead.ConvertedOpportunityId value")
                        .isNotNull();
            });
        });
    }
}
