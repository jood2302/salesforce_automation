package ngbs.quotingwizard.sync.noncontract;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;
import service.performance.PerformanceTest;

import java.util.List;

import static base.Pages.packagePage;
import static base.Pages.wizardPage;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("NGBS")
@Tag("SyncWithNGBS")
@PerformanceTest
public class UpgradeAndUpsellWithDiscountsNonContractTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product dlUnlimitedAdvanced;
    private final Product phoneToAdd;

    private final String standardPackageFullName;
    private final Package upgradePackage;
    private final String upgradePackageFolderName;

    private final List<String> expectedSyncSteps;

    public UpgradeAndUpsellWithDiscountsNonContractTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/sync/RC_MVP_Monthly_NonContract_186645013.json",
                Dataset.class);

        steps = new Steps(data);

        dlUnlimitedAdvanced = data.getProductByDataNameFromUpgradeData("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataNameFromUpgradeData("LC_HD_687");

        standardPackageFullName = data.packageFolders[0].packages[0].getFullName();
        upgradePackage = data.packageFoldersUpgrade[0].packages[0];
        upgradePackageFolderName = data.packageFoldersUpgrade[0].name;

        expectedSyncSteps = List.of(CONTRACT_SYNC_STEP, DISCOUNT_SYNC_STEP, UPGRADE_STEP, ORDER_SYNC);

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
    @TmsLink("CRM-12754")
    @DisplayName("CRM-12754 - Upgrade + Upsell with optional Discounts for the new contract")
    @Description("Upgrade with optional Upsell (changing package and adding new lines)")
    public void test() {
        step("1. Open the Opportunity, switch to the Quote Wizard, click 'Add New' in the Sales Quote section, " +
                "and check the preselected package", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();

            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(standardPackageFullName));
            packagePage.packageSelector.packageFilter.contractSelectInput.shouldNotBe(checked);
        });

        step("2. Select another package with Office Contract and save changes", () -> {
            steps.syncWithNgbs.stepUpgradeWithContract(upgradePackageFolderName, upgradePackage, upgradePackage.contract);
            packagePage.saveChanges();
        });

        step("3. Add a new phone on the Add Products tab", () ->
                steps.quoteWizard.addProductsOnProductsTab(phoneToAdd)
        );

        step("4. Open the Price tab, set quantity for DL Unlimited = " + dlUnlimitedAdvanced.quantity + ", " +
                "set quantity for the new phone = " + phoneToAdd.quantity + ", " +
                "add discount for DL Unlimited, assign all new devices", () ->
                steps.syncWithNgbs.stepSetupQuantitiesAndDiscounts(dlUnlimitedAdvanced, steps.quoteWizard.localAreaCode, phoneToAdd)
        );

        step("5. Open the Quote Details tab, populate Initial Term, Start Date, and save changes", () ->
                steps.syncWithNgbs.stepPopulateRequiredContractedInformationOnQuoteDetailsTab()
        );

        step("6. Close the Opportunity and set ServiceInfo__c.UpgradeStepStatus__c = true via API", () -> {
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);
            steps.syncWithNgbs.stepSkipUpgradeSyncStep(wizardPage.getSelectedQuoteId());
        });

        step("7. Press 'Process Order' button on the Opportunity record page and follow the sync process to the end", () -> {
            steps.syncWithNgbs.stepStartSyncWithNgbsViaProcessOrder(expectedSyncSteps);

            //  Contract Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(CONTRACT_SYNC_STEP);

            //  Discount Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(DISCOUNT_SYNC_STEP);

            //  Upgrade Step (skipped automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(UPGRADE_STEP);

            //  Order Sync Step
            steps.syncWithNgbs.clickNextButtonForSync();
            steps.syncWithNgbs.checkIsSyncStepCompleted(ORDER_SYNC);

            steps.syncWithNgbs.checkSyncStepsFinished(expectedSyncSteps);
        });

        step("8. Check synced discount information in NGBS", () ->
                steps.syncWithNgbs.stepCheckDiscountsInformation(dlUnlimitedAdvanced, upgradePackage)
        );

        step("9. Check synced contract information in NGBS", () ->
                steps.syncWithNgbs.stepCheckContractInformation(dlUnlimitedAdvanced)
        );
    }
}
