package leads.newbusiness;

import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Lead;
import com.sforce.soap.enterprise.sobject.Opportunity;
import leads.LeadConvertSteps;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Clock.systemUTC;
import static java.time.Duration.ofSeconds;
import static java.time.LocalDate.now;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test steps for the Close Date field validations on the Lead Convert page
 * for the different types of Leads (Sales, Partner).
 */
public class CloseDateValidationsLeadConvertPageSteps {
    public final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final LeadConvertSteps leadConvertSteps;

    //  Test data
    private final LocalDate closeDateInPast;
    private final LocalDate closeDateInFuture;
    private final DateTimeFormatter invalidCloseDateInputFormat;

    /**
     * New instance for the class with the test methods/steps related to
     * the checks Close Date field validations on the Lead Convert page.
     */
    public CloseDateValidationsLeadConvertPageSteps() {
        data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);

        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        leadConvertSteps = new LeadConvertSteps(data);

        closeDateInPast = now(systemUTC()).minusDays(2);
        closeDateInFuture = now(systemUTC()).plusDays(1);
        invalidCloseDateInputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    /**
     * <p> - Open the Lead Convert page for the test Lead. </>
     * <p> - Check all the different validations for the Close Date field on the Lead Convert page (negative checks). </p>
     * <p> - Convert the Lead with the correct Close Date value, and check Opportunity's CloseDate field value. </p>
     *
     * @param lead a Lead record to check and convert
     */
    public void checkCloseDateValidationsOnLeadConvertPageTestSteps(Lead lead) {
        step("1. Open the Lead Convert page for the test Lead", () -> {
            leadConvertPage.openPage(lead.getId());
        });

        step("2. Select 'Create a new account' toggle option, click 'Convert' button " +
                "and check that the error message is shown in the Opportunity section", () -> {
            leadConvertPage.newExistingAccountToggle.click();
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.convertButton.click();

            leadConvertPage.opportunityInfoErrorMessages.shouldHave(itemWithText(SELECT_CLOSE_DATE_IS_REQUIRED_ERROR));
        });

        step("3. Click 'Edit' in the Opportunity section, click 'Apply' button, check that Close Date field is required " +
                "and that 'Close Date is required' error message is shown below the Close Date field", () -> {
            leadConvertPage.opportunityInfoEditButton.click();
            leadConvertPage.closeDateDatepicker.getInput().shouldBe(enabled, ofSeconds(10));

            leadConvertPage.opportunityInfoApplyButton.click();
            //  it's the same as checking the standard HTML5 validation message "Please fill in this field"
            leadConvertPage.closeDateDatepicker.getInput()
                    .shouldHave(attribute("required"));
            leadConvertPage.closeDateDatepicker.getErrorMessage()
                    .shouldHave(textCaseSensitive(CLOSE_DATE_IS_REQUIRED_ERROR));
        });

        step("4. Populate the Close Date field with alphabetical only text, click 'Apply' button " +
                "and check that the error message is shown below the Close Date field", () -> {
            leadConvertPage.closeDateDatepicker.getInput().setValue(TEST_STRING);
            leadConvertPage.opportunityInfoApplyButton.click();

            //  TODO Known Issue PBC-25078 (There's an error message about empty Close Date field, but should be about the invalid format)
            leadConvertPage.closeDateDatepicker.getErrorMessage()
                    .shouldHave(textCaseSensitive(YOUR_ENTRY_DOESNT_MATCH_ALLOWED_FORMAT_ERROR));
        });

        step("5. Populate the Close Date field with a valid date in the future in the incorrect format yyyy-MM-dd, " +
                "click 'Apply' button and check that the error message is shown below the Close Date field", () -> {
            leadConvertPage.closeDateDatepicker.getInput().setValue(closeDateInFuture.format(invalidCloseDateInputFormat));
            leadConvertPage.opportunityInfoApplyButton.click();

            leadConvertPage.closeDateDatepicker.getErrorMessage()
                    .shouldHave(textCaseSensitive(YOUR_ENTRY_DOESNT_MATCH_ALLOWED_FORMAT_ERROR));
        });

        step("6. Click on Close Date field, select any date in the past, click 'Apply' button " +
                "and check that the error message is shown below the Close Date field", () -> {
            leadConvertPage.closeDateDatepicker.setDate(closeDateInPast);
            leadConvertPage.opportunityInfoApplyButton.click();

            leadConvertPage.closeDateDatepicker.getErrorMessage()
                    .shouldHave(textCaseSensitive(CLOSE_DATE_CANNOT_BE_IN_THE_PAST_ERROR));
        });

        step("7. Clear the Close Date field and check that the error message is shown below the Close Date field", () -> {
            leadConvertPage.closeDateDatepicker.getInput().clear();

            leadConvertPage.closeDateDatepicker.getErrorMessage()
                    .shouldHave(textCaseSensitive(CLOSE_DATE_IS_REQUIRED_ERROR));
        });

        step("8. Populate the Close Date with a correct date in the future, check that there are no errors, " +
                "click Apply button in the Opportunity section, and click Convert button", () -> {
            leadConvertPage.closeDateDatepicker.setDate(closeDateInFuture);
            //  to un-focus the datepicker and trigger its validation
            leadConvertPage.forecastedUsersInput.click();
            leadConvertPage.closeDateDatepicker.getErrorMessage().shouldBe(hidden);

            leadConvertPage.opportunityInfoApplyButton.click();
            leadConvertPage.opportunityInfoErrorMessages.shouldHave(size(0));

            leadConvertSteps.pressConvertButton();
        });

        step("9. Check that the Lead is converted, and Opportunity's CloseDate field is populated with the correct value", () -> {
            var convertedLead = leadConvertSteps.checkLeadConversion(lead);

            var opportunityFromLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT CloseDate " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + convertedLead.getConvertedOpportunityId() + "'",
                    Opportunity.class);
            var opportunityCloseDateActual = opportunityFromLead.getCloseDate()
                    .toInstant().atZone(UTC).toLocalDate();
            assertThat(opportunityCloseDateActual)
                    .as("Opportunity.CloseDate value (as LocalDate)")
                    .isEqualTo(closeDateInFuture);
        });
    }
}
