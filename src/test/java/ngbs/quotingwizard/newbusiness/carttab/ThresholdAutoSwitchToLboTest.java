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
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.PROVISION_TOGGLE_HINT_TEXT;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
@Tag("LBO")
public class ThresholdAutoSwitchToLboTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product digitalLineBasic;
    private final Product globalMvpEMEA;
    private final Product dlUnlimited;

    public ThresholdAutoSwitchToLboTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_PhonesAndDLsAndGlobalMVP.json",
                Dataset.class);
        steps = new Steps(data);

        digitalLineBasic = data.getProductByDataName("LC_DL-BAS_178");
        globalMvpEMEA = data.getProductByDataName("LC_IBO_284");
        data.setNewProductsToAdd(new Product[]{
                data.getProductByDataName("LC_HD_523"),
                data.getProductByDataName("LC_HDR_619")
        });
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
    @TmsLink("CRM-21863")
    @TmsLink("CRM-21864")
    @DisplayName("CRM-21863 - LBO Threshold: relationship between toggle and quantity of DLs \n" +
            "CRM-21864 - LBO Threshold: dependence of all the types of DLs")
    @Description("CRM-21863 - Verify link between Provision toggle and quantity of DLs and verify Hint is shown on Price tab. \n" +
            "CRM-21864 - Verify dependence on all the types of DLs ")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, add some products, and save changes on the Price tab", () ->
                steps.cartTab.prepareCartTabViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        //  CRM-21863
        step("2. Open the Quote Details tab, check that 'Provision' toggle is ON " +
                "and check a text hint for it", () -> {
            quotePage.openTab();
            steps.lbo.checkProvisionToggleOn(true);
            quotePage.provisionToggleInfoIcon.hover();
            quotePage.tooltip.shouldHave(exactTextCaseSensitive(PROVISION_TOGGLE_HINT_TEXT));
        });

        step("3. Set the Main Area Code, and save changes", () -> {
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.saveChanges();
        });

        step("4. Open the Price tab, change quantity of DL Unlimited to 100 and save changes", () -> {
            cartPage.openTab();
            cartPage.setQuantityForQLItem(dlUnlimited.name, steps.lbo.thresholdQuantity);
            cartPage.saveChanges();
        });

        step("5. Open the Quote Details tab and check that 'Provision' toggle is OFF and disabled", () -> {
            quotePage.openTab();
            steps.lbo.checkProvisionToggleOn(false);
            steps.lbo.checkProvisionToggleEnabled(false);
        });

        step("6. Open the Price tab, change quantity of DL Unlimited to 99 and save changes", () -> {
            cartPage.openTab();
            cartPage.setQuantityForQLItem(dlUnlimited.name, steps.lbo.thresholdQuantity - 1);
            cartPage.saveChanges();
        });

        step("7. Open the Quote Details tab and check that 'Provision' toggle is OFF and enabled", () -> {
            quotePage.openTab();
            steps.lbo.checkProvisionToggleOn(false);
            steps.lbo.checkProvisionToggleEnabled(true);
        });

        step("8. Click 'Provision' toggle, check that toggle is ON, and save changes", () -> {
            quotePage.provisionToggle.click();
            steps.lbo.checkProvisionToggleOn(true);
            quotePage.saveChanges();
        });

        //  CRM-21864
        step("9. Open the Add Products tab, add 'Digital Line Basic' and 'Global MVP - EMEA' licenses, " +
                "open the Price tab, change quantity of DL Unlimited to 98, and save changes", () -> {
            steps.quoteWizard.addProductsOnProductsTab(digitalLineBasic, globalMvpEMEA);

            cartPage.openTab();
            //  total number of DLs should be equal to the threshold: 
            //  98 (DL Unlimited) + 1 (DL Basic) + 1 (Global MVP - EMEA) = 100
            cartPage.setQuantityForQLItem(dlUnlimited.name, steps.lbo.thresholdQuantity - 2);
            cartPage.saveChanges();
        });

        step("10. Open the Quote Details tab, check that 'Provision' toggle is OFF and disabled", () -> {
            quotePage.openTab();
            steps.lbo.checkProvisionToggleOn(false);
            steps.lbo.checkProvisionToggleEnabled(false);
        });
    }
}
