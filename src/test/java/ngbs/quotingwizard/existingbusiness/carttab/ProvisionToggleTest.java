package ngbs.quotingwizard.existingbusiness.carttab;

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
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("NGBS")
@Tag("LBO")
public class ProvisionToggleTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product dlUnlimited;

    public ProvisionToggleTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_163074013.json",
                Dataset.class);
        steps = new Steps(data);

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
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
    @TmsLink("CRM-21862")
    @TmsLink("CRM-21865")
    @DisplayName("CRM-21862 - Provision Toggle's visibility in the Quote Wizard for the Existing Business Opportunities.\n " +
            "CRM-21865 - LBO Threshold: Opportunity with existing business and 100 DLs")
    @Description("CRM-21862 - Verify that the Provision Toggle is hidden in the Quote Wizard for the Existing Business Opportunities.\n " +
            "CRM-21865 - Verify that when adding 100 or more DLs, Quote.Enabled_LBO__c = 'false', and the device assignment button is visible")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        //  For CRM-21862
        step("2. Open the Quote Details tab," +
                "check that 'Provision' toggle is hidden, and Quote.Enabled_LBO__c = 'false'", () -> {
            quotePage.openTab();
            quotePage.provisionToggle.shouldBe(hidden);

            steps.lbo.checkEnableLboOnQuote(false);
        });

        //  For CRM-21865
        step("3. Open the Price tab, change quantity of DL Unlimited to 100, save changes, " +
                "and check that 'Device Assignment' button on the DigitalLine Unlimited is visible, " +
                "and Quote.Enabled_LBO__c = 'false'", () -> {
            cartPage.openTab();
            cartPage.setNewQuantityForQLItem(dlUnlimited.name, steps.lbo.thresholdQuantity);
            cartPage.saveChanges();

            cartPage.getQliFromCartByDisplayName(dlUnlimited.name)
                    .getDeviceAssignmentButton()
                    .shouldBe(visible);

            steps.lbo.checkEnableLboOnQuote(false);
        });
    }
}
