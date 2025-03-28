package ngbs.quotingwizard.newbusiness.quotetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.START_AND_END_DATE_INPUT_FORMATTER;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.Long.parseLong;

@Tag("P1")
@Tag("NGBS")
@Tag("QuoteTab")
@Tag("POC")
public class PocQuoteFieldsTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final String[] initialTermOptions;
    private final String initialTermDefaultValue;

    public PocQuoteFieldsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_POC.json",
                Dataset.class);

        steps = new Steps(data);

        initialTermOptions = data.packageFolders[0].packages[0].contractTerms.initialTerm;
        initialTermDefaultValue = initialTermOptions[3];
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-11590")
    @TmsLink("CRM-11732")
    @TmsLink("CRM-20324")
    @DisplayName("CRM-11590 - POC Initial Term Picklist values. \n" +
            "CRM-11732 - Renewal and Auto-renewal fields are hidden from the POC Quotes. \n" +
            "CRM-20324 - End Date field is populated on POC Quote.")
    @Description("CRM-11590 - Verify that Initial Term field contains following values: " +
            "5 days, 15 days, 30 days (selected by default), 60 days. \n" +
            "CRM-11732 - Check that user can't see the Renewal and Auto-renewal fields on the POC Quotes. \n" +
            "CRM-20324 - To check when user populates Start Date field on the POC Quote, the End Date field automatically populates too.")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new POC quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.preparePocQuoteViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        //  CRM-11590
        step("2. Open the Quote Details tab and check 'Initial Term' picklist", () -> {
            quotePage.openTab();
            quotePage.initialTermPicklist.getOptions().shouldHave(exactTexts(initialTermOptions));
            quotePage.initialTermPicklist.getSelectedOption().shouldHave(exactText(initialTermDefaultValue));
        });

        //  CRM-11732
        step("3. Check that 'Renewal' and 'Auto-renewal' fields are not displayed on the Quote Details tab", () -> {
            quotePage.autoRenewalCheckbox.shouldBe(hidden);
            quotePage.renewalTermPicklist.shouldBe(hidden);
        });

        //  CRM-20324
        step("4. Populate the 'Start Date' field and check the auto-population of 'End Date' field", () -> {
            quotePage.setDefaultStartDate();
            quotePage.startDateInput.shouldNotHave(exactValue(EMPTY_STRING));

            var startDateParsed = LocalDate.parse(quotePage.startDateInput.val(), START_AND_END_DATE_INPUT_FORMATTER);
            //  End date value is auto-calculated as 'Start Date + Initial Term (in days)'
            var startDatePlusInitialTerm = startDateParsed.plusDays(parseLong(initialTermDefaultValue));
            var expectedEndDate = START_AND_END_DATE_INPUT_FORMATTER.format(startDatePlusInitialTerm);
            quotePage.endDateInput.shouldHave(exactValue(expectedEndDate));
        });
    }
}
