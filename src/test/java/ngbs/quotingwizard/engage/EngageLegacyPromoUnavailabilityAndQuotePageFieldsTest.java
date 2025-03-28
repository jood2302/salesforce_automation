package ngbs.quotingwizard.engage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.cartPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("P1")
@Tag("PDV")
@Tag("Engage")
@Tag("Promos")
@Tag("QuoteTab")
public class EngageLegacyPromoUnavailabilityAndQuotePageFieldsTest extends BaseTest {
    private final Steps steps;

    private User dealDeskUserWithProcessEngageLegacyPermissionSet;

    public EngageLegacyPromoUnavailabilityAndQuotePageFieldsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_EngageVoiceLegacy_Monthly_Contract_1TypeOfContractedSeats.json",
                Dataset.class);

        steps = new Steps(data);
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with a 'Deal Desk Lightning' profile, 'Process_Engage_Legacy' Permission Set, " +
                "and Enable_Promotions__c feature toggle", () -> {
            dealDeskUserWithProcessEngageLegacyPermissionSet = getUser()
                    .withProfile(DEAL_DESK_LIGHTNING_PROFILE)
                    .withPermissionSet(PROCESS_ENGAGE_LEGACY_PS)
                    .withFeatureToggles(List.of(ENABLE_PROMOTIONS_FT))
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUserWithProcessEngageLegacyPermissionSet);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact,
                dealDeskUserWithProcessEngageLegacyPermissionSet);

        step("Login as a user with a 'Deal Desk Lightning' profile, 'Process_Engage_Legacy' Permission Set, " +
                "and Enable_Promotions__c feature toggle", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUserWithProcessEngageLegacyPermissionSet)
        );
    }

    @Test
    @TmsLink("CRM-20811")
    @TmsLink("CRM-22509")
    @DisplayName("CRM-20811 - Contract terms are available for Engage Legacy w/o Bindings. \n" +
            "CRM-22509 - Invisibility of Promos button for Engage Legacy opportunities")
    @Description("CRM-20811 - Verify that Contract terms (initial terms, renewal terms, start/end dates etc) " +
            "are available for Engage Legacy w/o Bindings. \n" +
            "CRM-22509 - To verify case when Promos button should not be displayed on a Quote of Engage Legacy Opportunity")
    public void test() {
        step("1. Open the Quote Wizard for the Engage Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        //  For CRM-22509
        step("2. Open the Price tab, and verify that 'Promos' button isn't displayed for Engage Legacy package", () -> {
            cartPage.openTab();
            cartPage.moreActionsButton.hover();
            cartPage.addPromosButton.shouldBe(hidden);
        });

        //  For CRM-20811
        step("3. Open the Quote Details tab and check that Contract Term fields are visible", () -> {
            quotePage.openTab();

            quotePage.initialTermPicklist.shouldBe(visible);
            quotePage.renewalTermPicklist.shouldBe(visible);
            quotePage.autoRenewalCheckbox.shouldBe(visible);
            quotePage.startDateInput.shouldBe(visible);
            quotePage.endDateInput.shouldBe(visible);
        });
    }
}
