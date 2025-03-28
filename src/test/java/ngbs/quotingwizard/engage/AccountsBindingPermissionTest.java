package ngbs.quotingwizard.engage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.quotePage;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("Engage")
public class AccountsBindingPermissionTest extends BaseTest {
    private final Steps steps;

    public AccountsBindingPermissionTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_EngageDSAndMVP_Monthly_Contract_WithProducts.json",
                Dataset.class);

        steps = new Steps(data);
    }

    @BeforeEach
    public void setUpTest() {
        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-20904")
    @DisplayName("CRM-20904 - Deal Desk Lightning profile has access to Accounts Bindings")
    @Description("Verify that Accounts Bindings is working for the following profile: Deal Desk Lightning")
    public void test() {
        step("1. Open the Quote Wizard for the New Business Engage Opportunity to add a new Sales Quote, " +
                "select a package with Engage Digital Standalone service, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Quote Details tab, " +
                "click 'Manage Account Bindings' button and check that the modal is shown", () -> {
            quotePage.openTab();
            quotePage.manageAccountBindingsButton.click();
            quotePage.manageAccountBindings.accountSearchInput.getSelf().shouldBe(visible, ofSeconds(10));
        });
    }
}
