package ngbs.quotingwizard.sync.noncontract;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("NGBS")
@Tag("SyncWithNGBS")
public class UpgradeUpsellAndDownsellNonContractTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product dlUnlimitedAdvanced;
    private final Product roomsLicense;
    private final Product digitalLine;

    private final String standardPackageFullName;
    private final Package upgradePackage;
    private final String upgradePackageFolderName;

    private final List<String> expectedSyncSteps;

    public UpgradeUpsellAndDownsellNonContractTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/sync/RC_MVP_Monthly_NonContract_163112013.json",
                Dataset.class);
        steps = new Steps(data);

        dlUnlimitedAdvanced = data.getProductByDataNameFromUpgradeData("LC_DL-UNL_50");
        roomsLicense = data.getProductByDataNameFromUpgradeData("LC_RCRM_77");
        digitalLine = data.getProductByDataNameFromUpgradeData("LC_DL_75");

        standardPackageFullName = data.packageFolders[0].packages[0].getFullName();
        upgradePackage = data.packageFoldersUpgrade[0].packages[0];
        upgradePackageFolderName = data.packageFoldersUpgrade[0].name;

        expectedSyncSteps = List.of(DISCOUNT_SYNC_STEP, MANUAL_DOWN_SELL_STEP, UPGRADE_STEP, ORDER_SYNC);

        steps.ngbs.isGenerateAccountsForSingleTest = true;
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();
        steps.syncWithNgbs.stepDeleteDiscountsOnAccount();

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-20474")
    @DisplayName("CRM-20474 - Upgrade + Upsell + Downsell with optional Discounts")
    @Description("User adds some new Lines but doesn't cross the Rater")
    public void test() {
        step("1. Open the Opportunity, switch to the Quote Wizard, click 'Add New' in the Sales Quote section, " +
                "and check the preselected package", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();

            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(standardPackageFullName));
            packagePage.packageSelector.packageFilter.contractSelectInput.shouldNotBe(checked);
        });

        step("2. Choose another package on the Select Package tab", () ->
                steps.syncWithNgbs.stepUpgradeWithContract(upgradePackageFolderName, upgradePackage, upgradePackage.contract)
        );

        step("3. Add a new license on the Add Products tab", () ->
                steps.quoteWizard.addProductsOnProductsTab(roomsLicense)
        );

        step("4. Open the Price tab, decrease quantity for DL Unlimited, add discount for it and save changes", () -> {
            steps.syncWithNgbs.stepSetupQuantitiesAndDiscounts(dlUnlimitedAdvanced, steps.quoteWizard.localAreaCode);
            cartPage.saveChanges();
        });

        step("5. Approve the current quote, close the Opportunity " +
                "and set ServiceInfo__c.UpgradeStepStatus__c = true (all via API)", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedStatus(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);
            steps.syncWithNgbs.stepSkipUpgradeSyncStep(wizardPage.getSelectedQuoteId());
        });

        step("6. Press 'Process Order' button on the Opportunity record page and follow the sync process to the end", () -> {
            steps.syncWithNgbs.stepStartSyncWithNgbsViaProcessOrder(expectedSyncSteps);

            //  Discount Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(DISCOUNT_SYNC_STEP);

            //  DigitalLine Unlimited can only be removed (downsell) via its parent DigitalLine license
            steps.ngbs.downsellLicensesInNGBS(digitalLine);
            //  Down-sell Step (no notifications)
            steps.syncWithNgbs.clickNextButtonForSync();
            steps.syncWithNgbs.checkIsSyncStepCompleted(MANUAL_DOWN_SELL_STEP);

            //  Upgrade Step (skipped automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(UPGRADE_STEP);

            //  Order Sync Step
            steps.syncWithNgbs.clickNextButtonForSync();
            steps.syncWithNgbs.checkIsSyncStepCompleted(ORDER_SYNC);

            steps.syncWithNgbs.checkSyncStepsFinished(expectedSyncSteps);
        });

        step("7. Check synced discount information in NGBS", () ->
                steps.syncWithNgbs.stepCheckDiscountsInformation(dlUnlimitedAdvanced, upgradePackage)
        );
    }
}
