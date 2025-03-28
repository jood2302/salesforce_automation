package ngbs.quotingwizard.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("NGBS")
@Tag("QOP")
public class NgbsExistingBusinessAccountUpgradeTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final String upgradePackageFolderName;
    private final Package upgradePackage;

    public NgbsExistingBusinessAccountUpgradeTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_163074013.json",
                Dataset.class);
        steps = new Steps(data);

        upgradePackageFolderName = data.packageFoldersUpgrade[0].name;
        upgradePackage = data.packageFoldersUpgrade[0].packages[0];
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-21253")
    @DisplayName("CRM-21253 - Existing Business | No buttons are enabled after Upgrade Quote Creation")
    @Description("Verify that Save/Discard/Cancel Upgrade buttons behave correctly. Cancel Upgrade button is enabled after Upgrade")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote " +
                "and check that 'Save and Continue' button is enabled", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());

            packagePage.saveAndContinueButton.shouldBe(enabled);
        });

        step("2. Select a different package and save changes", () -> {
            packagePage.packageSelector.selectPackage(data.chargeTerm, upgradePackageFolderName, upgradePackage);
            packagePage.saveChanges();
        });

        step("3. Switch between the Price and Quote Details tabs, " +
                "verify that switching performs without any confirmation windows " +
                "and 'Save Changes' button is disabled", () -> {
            cartPage.openTab();
            cartPage.saveButton.shouldBe(disabled);

            quotePage.openTab();
            quotePage.saveButton.shouldBe(disabled);
        });
    }
}
