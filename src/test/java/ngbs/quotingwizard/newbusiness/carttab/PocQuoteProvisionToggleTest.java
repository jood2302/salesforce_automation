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
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("NGBS")
@Tag("LBO")
public class PocQuoteProvisionToggleTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final Product dlUnlimited;

    public PocQuoteProvisionToggleTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_Annual_RegularAndPOC.json",
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
    @TmsLink("CRM-21918")
    @DisplayName("CRM-21918 - Provision Toggle should be hidden for new business for POC quote")
    @Description("Verify that Provision Toggle is hidden for new business for POC quote " +
            "and, as a result, actual assignments should be present")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new POC Quote, " +
                        "select a package for it, and save changes", () ->
                steps.quoteWizard.preparePocQuoteViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Add products on the Add Products tab", () ->
                steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd())
        );

        step("3. Open the Price tab, check that Device Assignment button exists, and save changes", () -> {
            cartPage.openTab();
            cartPage.getQliFromCartByDisplayName(dlUnlimited.name)
                    .getDeviceAssignmentButton()
                    .shouldBe(visible);
            cartPage.saveChanges();
        });

        step("4. Open the Quote Details tab, " +
                "check that 'Provision' toggle is hidden, and that POC Quote.Enabled_LBO__c = false, " +
                "populate Main Area Code, and save changes", () -> {
            quotePage.openTab();
            quotePage.provisionToggle.shouldBe(hidden);
            steps.lbo.checkEnableLboOnQuote(false);

            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.saveChanges();
        });

        step("5. Open the Price tab, change quantity of DL Unlimited to 100, save changes, " +
                "and check that Device Assignment button exists", () -> {
            cartPage.openTab();
            cartPage.setQuantityForQLItem(dlUnlimited.name, steps.lbo.thresholdQuantity);
            cartPage.saveChanges();

            cartPage.getQliFromCartByDisplayName(dlUnlimited.name)
                    .getDeviceAssignmentButton()
                    .shouldBe(visible);
        });

        step("6. Check that POC Quote.Enabled_LBO__c = false", () ->
                steps.lbo.checkEnableLboOnQuote(false)
        );
    }
}
