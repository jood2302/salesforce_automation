package leads.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.LEGACY_BILLING_SYSTEM;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.PROFESSIONAL_SERVICES_LIGHTNING_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.hidden;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("LeadConvert")
public class LegacyBiPartnerLeadConvertFlowTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    private User proServUser;

    public LegacyBiPartnerLeadConvertFlowTest() {
        data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/Telus_Office_Legacy.json",
                Dataset.class);
        steps = new Steps(data);
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Professional Services Lightning' profile", () -> {
            proServUser = getUser().withProfile(PROFESSIONAL_SERVICES_LIGHTNING_PROFILE).execute();
        });

        steps.leadConvert.createPartnerAccountAndLead(proServUser);

        step("Login as a user with 'Professional Services Lightning' profile", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(proServUser);
        });
    }

    @Test
    @TmsLink("CRM-25580")
    @DisplayName("CRM-25580 - Convert Lead for Legacy brand. Partner Lead")
    @Description("Verify that: \n" +
            "- the Opportunity section on the Lead Convert page for the legacy partner brand displays correct data \n" +
            "- the Lead with legacy partner brand can be converted into the Opportunity")
    public void test() {
        step("1. Open the Lead Convert page for the test Partner lead", () ->
                leadConvertPage.openPage(steps.leadConvert.partnerLead.getId())
        );

        step("2. Switch the toggle into 'Create new Account' position", () ->
                leadConvertPage.newExistingAccountToggle.click()
        );

        step("3. Click 'Edit' in Opportunity section, " +
                "check that Business Identity = '" + data.getBusinessIdentityName() + "', " +
                "check Billing System and Brand fields, populate Close Date field, and click 'Apply' button", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();
            leadConvertPage.opportunityLoadingBar.shouldBe(hidden, ofSeconds(60));

            leadConvertPage.businessIdentityPicklist.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(data.getBusinessIdentityName()));
            leadConvertPage.billingSystemOutputField.shouldHave(exactTextCaseSensitive(LEGACY_BILLING_SYSTEM));
            leadConvertPage.brandNameOutputField.shouldHave(exactTextCaseSensitive(data.getBusinessIdentityName()));

            leadConvertPage.closeDateDatepicker.setTomorrowDate();

            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("4. Click 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("5. Check that the Lead is successfully converted", () ->
                steps.leadConvert.checkLeadConversion(steps.leadConvert.partnerLead)
        );
    }
}
