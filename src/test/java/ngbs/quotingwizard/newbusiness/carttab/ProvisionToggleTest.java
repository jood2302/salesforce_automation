package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static base.Pages.quotePage;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("P1")
@Tag("PDV")
@Tag("NGBS")
@Tag("LBO")
public class ProvisionToggleTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product dlUnlimited;

    public ProvisionToggleTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_NoPhones.json",
                Dataset.class);
        steps = new Steps(data);

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-21859")
    @TmsLink("CRM-20903")
    @DisplayName("CRM-21859 - Provision Toggle and Provision Type on Quoting Tool 2.0 (New business). \n" +
            "CRM-20903 - Provision toggle should be shown only for LBO services")
    @Description("CRM-21859 - To verify Quoting Tool to display info area 'Provision type', " +
            "Provision and assignments if LBO Quote Available Feature Toggle is on. \n" +
            "CRM-20903 - To verify that LBO functionality is not shown for non-LBO products")
    public void test() {
        step("1. Open the Quote Wizard for a New Business Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        //  CRM-21859
        step("2. Check that Quote.Enabled_LBO__c = false", () ->
                steps.lbo.checkEnableLboOnQuote(false)
        );

        //  CRM-21859
        step("3. Open the Price tab and verify that Assignment Configurator icon is present for the DL Unlimited", () -> {
            cartPage.openTab();
            cartPage.getQliFromCartByDisplayName(dlUnlimited.name).getDeviceAssignmentButton().shouldBe(visible);
        });

        step("4. Open the Quote Details tab, check that 'Provision' toggle is displayed and turned ON " +
                "and that Self-Provisioned checkbox is present and unchecked", () -> {
            quotePage.openTab();

            //  CRM-20903
            quotePage.provisionToggle.shouldBe(visible);

            //  CRM-21859
            steps.lbo.checkProvisionToggleOn(true);

            //  CRM-21859
            quotePage.selfProvisionedCheckbox.shouldBe(visible).shouldNotBe(checked);
        });

        //  CRM-21859
        step("5. Switch the 'Provision' toggle, check that Self-Provisioned checkbox is disappeared, " +
                "populate Main Area Code, and save changes", () -> {
            quotePage.provisionToggle.click();
            steps.lbo.checkProvisionToggleOn(false);
            quotePage.selfProvisionedCheckbox.shouldNot(exist);
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.saveChanges();
        });

        //  CRM-21859
        step("6. Check that Quote.Enabled_LBO__c = true", () ->
                steps.lbo.checkEnableLboOnQuote(true)
        );

        //  CRM-21859
        step("7. Open the Price tab and verify that Assignment Configurator icons is absent for the DL Unlimited", () -> {
            cartPage.openTab();
            cartPage.getQliFromCartByDisplayName(dlUnlimited.name).getDeviceAssignmentButton().shouldBe(hidden);
        });

        //  CRM-21859
        step("8. Open the Quote Details tab, switch the 'Provision' toggle back to ON, " +
                "check that Self-Provisioned checkbox is present and unchecked and save changes", () -> {
            quotePage.openTab();
            quotePage.provisionToggle.click();
            steps.lbo.checkProvisionToggleOn(true);
            quotePage.selfProvisionedCheckbox.shouldBe(visible).shouldNotBe(checked);
            quotePage.saveChanges();
        });

        //  CRM-21859
        step("9. Check that Quote.Enabled_LBO__c = false", () ->
                steps.lbo.checkEnableLboOnQuote(false)
        );
    }
}
