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
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
public class SaveButtonCartTabUpsellTest extends BaseTest {
    private final Steps steps;
    private final SaveButtonCartTabExistingBusinessSteps saveButtonCartTabExistingBusinessSteps;

    //  Test data
    private final Product digitalLineStandard;

    public SaveButtonCartTabUpsellTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_163074013.json",
                Dataset.class);

        steps = new Steps(data);
        saveButtonCartTabExistingBusinessSteps = new SaveButtonCartTabExistingBusinessSteps(data);

        digitalLineStandard = data.getProductByDataName("LC_DL-UNL_50");
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
    @TmsLink("CRM-20577")
    @DisplayName("CRM-20577 - Save button saves changes in cart. Existing Business (Upsell)")
    @Description("Verify that changes on Price tab are saved after clicking Save button")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select the same package from the NGBS account for it, save changes, and add products on the Add Products tab", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
        });

        step("2. Open the Add Products tab and add additional Phones", () -> {
            steps.quoteWizard.addProductsOnProductsTab(saveButtonCartTabExistingBusinessSteps.phoneToAdd);
        });

        step("3. Open the Price tab, update phones' quantity, add as many additional DigitalLines as the added Phones, " +
                "assign the phones to the DigitalLine, and save changes", () -> {
            cartPage.openTab();
            saveButtonCartTabExistingBusinessSteps.addDigitalLinesAsMuchAsPhones(digitalLineStandard, steps.quoteWizard.opportunity.getId());
            saveButtonCartTabExistingBusinessSteps.assignDevicesToDigitalLinesAndSave(digitalLineStandard);
        });

        step("4. Check the updated Quote Line Items and Area Code Line Item for the Opportunity's Quote", () -> {
            saveButtonCartTabExistingBusinessSteps.checkUpdatedQliAndAreaCodeLineItem(digitalLineStandard, steps.quoteWizard.opportunity.getId());
        });
    }
}
