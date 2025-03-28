package leads.leadconvertpage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.page.lead.convert.MatchedItem;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Opportunity;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.MATCHED_OPPORTUNITIES_HEADERS;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.MATCHED_OPPORTUNITIES_TABLE_LABEL;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory.createOpportunity;
import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$$;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

@Tag("P1")
@Tag("LeadConvert")
public class ExistingAccountSelectExistingOppLeadConvertPageTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Opportunity firstOpportunity;
    private Opportunity secondOpportunity;

    //  Test data
    private final String tierName;

    public ExistingAccountSelectExistingOppLeadConvertPageTest() {
        data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        tierName = data.packageFolders[0].name;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);

        step("Create two Opportunities with unique names for the Account " +
                "using its primary Contact as a Contact Role via API", () -> {
            firstOpportunity = createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, true,
                    data.brandName, data.businessIdentity.id, salesRepUser, data.currencyISOCode, tierName);
            secondOpportunity = createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, true,
                    data.brandName, data.businessIdentity.id, salesRepUser, data.currencyISOCode, tierName);

            //  update opportunities' names to distinguish them during the test flow
            firstOpportunity.setName(firstOpportunity.getName() + "_1");
            secondOpportunity.setName(secondOpportunity.getName() + "_2");
            enterpriseConnectionUtils.update(firstOpportunity);
            enterpriseConnectionUtils.update(secondOpportunity);
        });

        step("Set the same Email and Phone for the Lead and the Account's Contact via API", () -> {
            steps.leadConvert.salesLead.setEmail(steps.salesFlow.contact.getEmail());
            steps.leadConvert.salesLead.setPhone(steps.salesFlow.contact.getPhone());
            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20777")
    @DisplayName("CRM-20777 - Lead Conversion - Sales Lead - Select Existing Account - Opportunity section - Select existing Opportunity")
    @Description("Verify that Opportunity section functions properly when selecting an opportunity for a lead that is being converted")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select New Business Account (from the 'Matched Accounts' table) " +
                "and press 'Apply' button", () -> {
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

        step("4. Check that 'Matched Opportunities' table contains required columns", () -> {
            leadConvertPage.matchedOpportunitiesTableHeaders.shouldHave(exactTexts(MATCHED_OPPORTUNITIES_HEADERS));
        });

        step("5. Check that 'Matched Opportunities' table includes records related to Account", () -> {
            leadConvertPage.matchedOpportunitiesTableItems.shouldHave(sizeGreaterThan(0));
            var actualMatchedOpportunitiesNames = leadConvertPage.getMatchedOpportunitiesList()
                    .stream()
                    .map(MatchedItem::getName)
                    .collect(toList());

            $$(actualMatchedOpportunitiesNames).shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    firstOpportunity.getName(), secondOpportunity.getName()
            ), ofSeconds(10));
        });

        step("6. Select Account's first opportunity in the table and check that it's selected", () -> {
            leadConvertPage.getMatchedOpportunity(firstOpportunity.getId())
                    .getSelectButton()
                    .click();

            leadConvertPage.getMatchedOpportunity(firstOpportunity.getId())
                    .getSelectButtonInput()
                    .shouldBe(selected);
        });

        step("7. Select Account's second opportunity in the table and check that it's selected " +
                "and the first one is no longer selected", () -> {
            leadConvertPage.getMatchedOpportunity(secondOpportunity.getId())
                    .getSelectButton()
                    .click();

            leadConvertPage.getMatchedOpportunity(secondOpportunity.getId())
                    .getSelectButtonInput()
                    .shouldBe(selected);
            leadConvertPage.getMatchedOpportunity(firstOpportunity.getId())
                    .getSelectButtonInput()
                    .shouldNotBe(selected);
        });

        step("8. Click 'Apply' button in the Opportunity section " +
                "and check that second opportunity is still selected, " +
                "and that all options in Opportunity section are disabled", () -> {
            leadConvertPage.opportunityInfoApplyButton.click();

            leadConvertPage.getMatchedOpportunity(secondOpportunity.getId())
                    .getSelectButtonInput()
                    .shouldBe(selected);

            leadConvertPage.opportunityDoNotCreateOppOptionInput.shouldBe(disabled);
            leadConvertPage.opportunityCreateNewOppOptionInput.shouldBe(disabled);
            leadConvertPage.opportunitySelectExistingOppOptionInput.shouldBe(disabled);
        });

        step("9. Check that 'Contact' and 'Opportunity Role' sections are visible", () -> {
            leadConvertPage.contactInfoSection.shouldBe(visible);
            leadConvertPage.contactRoleSection.shouldBe(visible);
        });
    }
}
