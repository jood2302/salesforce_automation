package ngbs.quotingwizard.existingbusiness.upgrade;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.packagePage;
import static base.Pages.quotePage;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("NGBS")
@Tag("QuoteTab")
public class NonContractUpgradeOfficeTest extends BaseTest {
    private final Steps steps;

    public NonContractUpgradeOfficeTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_Contract_163073013.json",
                Dataset.class);

        steps = new Steps(data);
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.stepCreateContractInNGBS();
        }

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-21019")
    @DisplayName("CRM-21019 - Quote Stage can't be switched if none contract is selected.")
    @Description("Verify that Quote Stage cannot be switched if none contract is selected")
    public void test() {
        step("1. Open the Quote Wizard for the Existing Business Opportunity to add a new Sales Quote, " +
                "select the same package without the contract, and save/confirm changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            
            packagePage.packageSelector.setContractSelected(false);
            packagePage.saveChanges();
        });

        step("2. Open the Quote Details tab and check that 'Stage' field is disabled", () -> {
            quotePage.openTab();
            quotePage.stagePicklist
                    .shouldBe(visible, ofSeconds(30))
                    .shouldBe(disabled);
        });
    }
}
