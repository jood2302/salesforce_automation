package ngbs.quotingwizard.existingbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static base.Pages.packagePage;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("NGBS")
public class SaveButtonCartTabUpgradeTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final SaveButtonCartTabExistingBusinessSteps saveButtonCartTabExistingBusinessSteps;

    //  Test data
    private final String mvpPremiumPackageFolderName;
    private final Package mvpPremiumPackage;
    private final Product digitalLinePremium;

    public SaveButtonCartTabUpgradeTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_163074013.json",
                Dataset.class);

        steps = new Steps(data);
        saveButtonCartTabExistingBusinessSteps = new SaveButtonCartTabExistingBusinessSteps(data);

        mvpPremiumPackageFolderName = data.packageFoldersUpgrade[0].name;
        mvpPremiumPackage = data.packageFoldersUpgrade[0].packages[0];
        digitalLinePremium = data.getProductByDataName("LC_DL-UNL_50", mvpPremiumPackage);
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
    @TmsLink("CRM-20578")
    @DisplayName("CRM-20578 - Save button saves changes in cart. Existing Business (Upgrade)")
    @Description("Verify that changes on Price tab are saved after clicking Save button")
    public void test() {
        step("1. Open the Quote Wizard for the Existing Business Opportunity to add a new Sales quote," +
                "select a package different from the preselected, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());

            packagePage.packageSelector.selectPackage(data.chargeTerm, mvpPremiumPackageFolderName, mvpPremiumPackage);
            packagePage.saveChanges();
        });

        step("2. Open the Add Products tab and add additional Phones", () -> {
            steps.quoteWizard.addProductsOnProductsTab(saveButtonCartTabExistingBusinessSteps.phoneToAdd);
        });

        step("3. Open the Price tab, update phones' quantity, add as many additional DigitalLines as the added Phones, " +
                "assign the phones to the DigitalLine, and save changes", () -> {
            cartPage.openTab();
            saveButtonCartTabExistingBusinessSteps.addDigitalLinesAsMuchAsPhones(digitalLinePremium, steps.quoteWizard.opportunity.getId());
            saveButtonCartTabExistingBusinessSteps.assignDevicesToDigitalLinesAndSave(digitalLinePremium);
        });

        step("4. Check the updated Quote Line Items and Area Code Line Item for the Opportunity's Quote", () -> {
            saveButtonCartTabExistingBusinessSteps.checkUpdatedQliAndAreaCodeLineItem(digitalLinePremium, steps.quoteWizard.opportunity.getId());
        });
    }
}
