package ngbs.quotingwizard.sync;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.packagePage;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("PDV")
@Tag("NGBS")
@Tag("SyncWithNGBS")
public class UpsellTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final Product dlUnlimited;
    private final Product phoneToAdd;

    private final String upgradePackageFolderName;
    private final Package upgradePackage;

    private final List<String> expectedSyncSteps;

    public UpsellTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/sync/RC_MVP_Monthly_NonContract_186675013.json",
                Dataset.class);
        steps = new Steps(data);

        dlUnlimited = data.getProductByDataNameFromUpgradeData("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataNameFromUpgradeData("LC_HD_687");

        upgradePackageFolderName = data.packageFoldersUpgrade[0].name;
        upgradePackage = data.packageFoldersUpgrade[0].packages[0];

        expectedSyncSteps = List.of(CONTRACT_SYNC_STEP, DISCOUNT_SYNC_STEP, REPRICE_STEP, ORDER_SYNC);

        steps.ngbs.isGenerateAccountsForSingleTest = true;
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        steps.syncWithNgbs.stepDeleteDiscountsOnAccount();
        steps.syncWithNgbs.stepTerminateActiveContractOnAccount();

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-12752")
    @DisplayName("CRM-12752 - Upsell with optional Discounts || Applying new Contract + Upsell.")
    @Description("User adds some new Lines but doesn't cross the Rater")
    public void test() {
        step("1. Open the Existing Business Opportunity, switch to the Quote Wizard, add a new Sales Quote, " +
                "select the same package with a contract, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            packagePage.packageSelector.selectPackage(data.chargeTerm, upgradePackageFolderName, upgradePackage);
            packagePage.saveChanges();
        });

        step("2. Add a new phone on the Add Products tab", () ->
                steps.quoteWizard.addProductsOnProductsTab(phoneToAdd)
        );

        step("3. Open the Price tab, set quantity for DL Unlimited = " + dlUnlimited.quantity + ", " +
                "set quantity for the new phone = " + phoneToAdd.quantity + ", " +
                "add discount for DL Unlimited, and assign all new devices", () ->
                steps.syncWithNgbs.stepSetupQuantitiesAndDiscounts(dlUnlimited, steps.quoteWizard.localAreaCode, phoneToAdd)
        );

        step("4. Populate Initial Term and Start Date on the Quote Details tab and save changes", () ->
                steps.syncWithNgbs.stepPopulateRequiredContractedInformationOnQuoteDetailsTab()
        );

        step("5. Set current Quote to Active Agreement and close the opportunity via API", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);
        });

        step("6. Press 'Process Order' button on the Opportunity record page and follow the sync process to the end", () -> {
            steps.syncWithNgbs.stepStartSyncWithNgbsViaProcessOrder(expectedSyncSteps);

            //  Contract Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(CONTRACT_SYNC_STEP);

            //  Discount Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(DISCOUNT_SYNC_STEP);

            //  Reprice Step
            steps.syncWithNgbs.checkRepriceStep();

            //  Order Sync Step
            steps.syncWithNgbs.clickNextButtonForSync();
            steps.syncWithNgbs.checkIsSyncStepCompleted(ORDER_SYNC);

            steps.syncWithNgbs.checkSyncStepsFinished(expectedSyncSteps);
        });

        step("7. Check synced contract information in NGBS", () ->
                steps.syncWithNgbs.stepCheckContractInformation(dlUnlimited)
        );

        step("8. Check synced discount information in NGBS", () ->
                steps.syncWithNgbs.stepCheckDiscountsInformation(dlUnlimited, upgradePackage)
        );
    }
}
