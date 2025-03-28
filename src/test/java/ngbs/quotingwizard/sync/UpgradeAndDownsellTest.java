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

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("NGBS")
@Tag("SyncWithNGBS")
public class UpgradeAndDownsellTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product dlUnlimitedCore;
    private final Product digitalLine;

    private final int initialDownsellQuantity;
    private final int remainingDownsellQuantity;

    private final Package standardPackage;
    private final Package upgradePackage;
    private final String upgradePackageFolderName;

    private final List<String> expectedSyncSteps;

    public UpgradeAndDownsellTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/sync/RC_MVP_Monthly_Contract_163102013.json",
                Dataset.class);
        steps = new Steps(data);

        dlUnlimitedCore = data.getProductByDataNameFromUpgradeData("LC_DL-UNL_50");
        digitalLine = data.getProductByDataNameFromUpgradeData("LC_DL_75");

        initialDownsellQuantity = 2;
        //  e.g. 30 - 25 - 2 = 3;
        remainingDownsellQuantity = digitalLine.existingQuantity - digitalLine.quantity - initialDownsellQuantity;

        standardPackage = data.packageFolders[0].packages[0];
        upgradePackage = data.packageFoldersUpgrade[0].packages[0];
        upgradePackageFolderName = data.packageFoldersUpgrade[0].name;

        expectedSyncSteps = List.of(CONTRACT_SYNC_STEP, DISCOUNT_SYNC_STEP, MANUAL_DOWN_SELL_STEP, UPGRADE_STEP);
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
        } else {
            steps.syncWithNgbs.stepResetLicensesStateAfterDownsell(dlUnlimitedCore);
        }

        steps.syncWithNgbs.stepDeleteDiscountsOnAccount();
        steps.ngbs.stepCreateContractInNGBS();
        steps.syncWithNgbs.stepResetContractState(dlUnlimitedCore);

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.syncWithNgbs.stepCreateAdditionalActiveSalesAgreement(steps.salesFlow.account, steps.salesFlow.contact,
                dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-12753")
    @DisplayName("CRM-12753 - Upgrade + Downsell with optional Discounts for Existing || New Contract")
    @Description("User adds some new Lines but doesn't cross the Rater")
    public void test() {
        step("1. Open the Existing Business Opportunity, switch to the Quote Wizard, " +
                "click 'Add New' button in Sales Quote section and check the selected package", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());

            steps.quoteWizard.addNewSalesQuote();
            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(standardPackage.getFullName()));
            packagePage.packageSelector.packageFilter.contractSelectInput.shouldBe(checked);
        });

        step("2. Select another package with a contract on the Select Package tab", () ->
                steps.syncWithNgbs.stepUpgradeWithContract(upgradePackageFolderName, upgradePackage, upgradePackage.contract)
        );

        step("3. Open the Price tab, decrease quantity for DL Unlimited, add discount for it and save changes", () -> {
            steps.syncWithNgbs.stepSetupQuantitiesAndDiscounts(dlUnlimitedCore, steps.quoteWizard.localAreaCode);
            cartPage.saveChanges();
        });

        step("4. Close the Opportunity and set ServiceInfo__c.UpgradeStepStatus__c = true via API", () -> {
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);
            steps.syncWithNgbs.stepSkipUpgradeSyncStep(wizardPage.getSelectedQuoteId());
        });

        step("5. Press 'Process Order' button on the Opportunity record page and follow the sync process to the end", () -> {
            steps.syncWithNgbs.stepStartSyncWithNgbsViaProcessOrder(expectedSyncSteps);

            //  Contract Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(CONTRACT_SYNC_STEP);

            //  Discount Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(DISCOUNT_SYNC_STEP);

            //  Down-sell Step
            step("Check that the error message is displayed if the user did not finish the Downsell in NGBS completely, " +
                    "and that the step is successfully completed otherwise", () -> {
                //  No down-sell
                steps.syncWithNgbs.clickNextButtonForSync();
                processOrderModal.errorNotifications.shouldHave(exactTextsCaseSensitiveInAnyOrder(
                        format(LICENSES_SHOULD_BE_REMOVED_MANUALLY_ERROR, MVP_SERVICE)), ofSeconds(60));

                //  Partial down-sell
                //  DigitalLine Unlimited can only be removed (downsell) via its parent DigitalLine license
                steps.ngbs.downsellLicensesInNGBS(digitalLine, initialDownsellQuantity);
                steps.syncWithNgbs.clickNextButtonForSync();
                //  There's one more error message because the user didn't finish the down-sell completely
                processOrderModal.errorNotifications.shouldHave(exactTextsCaseSensitiveInAnyOrder(
                        format(LICENSES_SHOULD_BE_REMOVED_MANUALLY_ERROR, MVP_SERVICE),
                        format(LICENSES_SHOULD_BE_REMOVED_MANUALLY_ERROR, MVP_SERVICE)), ofSeconds(60));

                //  Complete down-sell
                steps.ngbs.downsellLicensesInNGBS(digitalLine, remainingDownsellQuantity);
                steps.syncWithNgbs.clickNextButtonForSync();

                steps.syncWithNgbs.checkIsSyncStepCompleted(MANUAL_DOWN_SELL_STEP);
            });

            //  Upgrade Step (skipped automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(UPGRADE_STEP);

            steps.syncWithNgbs.checkSyncStepsFinished(expectedSyncSteps);
        });

        step("6. Check synced contract information in NGBS", () ->
                steps.syncWithNgbs.stepCheckContractInformation(dlUnlimitedCore)
        );

        step("7. Check synced discount information in NGBS", () ->
                steps.syncWithNgbs.stepCheckDiscountsInformation(dlUnlimitedCore, upgradePackage)
        );
    }
}
