package ngbs.quotingwizard.sync.contractcancelation;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.QUOTE_HAS_DOWNSELL_OR_CONTRACT_EXIT_TOOLTIP;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
@Tag("SyncWithNGBS")
public class UpgradeAndDownsellContractCancelTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product digitalLine;
    private final Product dlUnlimited;

    private final String standardPackageFullName;
    private final Package upgradePackage;
    private final String upgradePackageFolderName;

    private final List<String> expectedSyncSteps;

    public UpgradeAndDownsellContractCancelTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/sync/RC_MVP_Monthly_Contract_186622013.json",
                Dataset.class);
        steps = new Steps(data);

        dlUnlimited = data.getProductByDataNameFromUpgradeData("LC_DL-UNL_50");
        digitalLine = data.getProductByDataNameFromUpgradeData("LC_DL_75");

        standardPackageFullName = data.packageFolders[0].packages[0].getFullName();
        upgradePackage = data.packageFoldersUpgrade[0].packages[0];
        upgradePackageFolderName = data.packageFoldersUpgrade[0].name;

        expectedSyncSteps = List.of(CONTRACT_CANCEL_STEP, DISCOUNT_SYNC_STEP, MANUAL_DOWN_SELL_STEP, UPGRADE_STEP);
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
        } else {
            steps.syncWithNgbs.stepResetLicensesStateAfterDownsell(dlUnlimited);
        }
        steps.syncWithNgbs.stepDeleteDiscountsOnAccount();
        steps.ngbs.stepCreateContractInNGBS();

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.syncWithNgbs.stepCreateAdditionalActiveSalesAgreement(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @Tag("KnownIssue")
    @Issue("PBC-26288")
    @TmsLink("CRM-19604")
    @TmsLink("CRM-37468")
    @DisplayName("CRM-19604 - Contract cancellation with Upgrade + Downsell with optional Discounts. \n" +
            "CRM-37468 - Text In Notification of a Generate PDF Button On inContract Downsell Quote")
    @Description("CRM-19604 - User deselect Contract with adding some new Lines but doesn't cross the Rater. \n" +
            "CRM-37468 - Verify that tooltip on Generate PDF Button is shown: " +
            "'The quote has downsell or contract exit. PDF cannot be generated until the quote is approved.'")
    public void test() {
        //  CRM-19604
        step("1. Open the Opportunity, switch to the Quote Wizard, click 'Add New' in the Sales Quote section, " +
                "and check the preselected package", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();

            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(standardPackageFullName));
            packagePage.packageSelector.packageFilter.contractSelectInput.shouldBe(checked);
        });

        step("2. Select another package on the Select Package tab", () ->
                steps.syncWithNgbs.stepUpgradeWithContract(upgradePackageFolderName, upgradePackage, upgradePackage.contract)
        );

        //  CRM-37468
        step("3. Open the Price tab, decrease quantity for DL Unlimited, add discount for it, save changes, " +
                "hover over the Generate PDF button, and check its tooltip", () -> {
            steps.syncWithNgbs.stepSetupQuantitiesAndDiscounts(dlUnlimited, steps.quoteWizard.localAreaCode);
            cartPage.saveChanges();

            cartPage.generatePdfButton.hover();
            cartPage.tooltip.shouldHave(exactTextCaseSensitive(QUOTE_HAS_DOWNSELL_OR_CONTRACT_EXIT_TOOLTIP));
        });

        step("4. Approve the current quote, close the Opportunity " +
                "and set ServiceInfo__c.UpgradeStepStatus__c = true (all via API)", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedStatus(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);
            steps.syncWithNgbs.stepSkipUpgradeSyncStep(wizardPage.getSelectedQuoteId());
        });

        //  CRM-19604
        step("5. Press 'Process Order' on the Opportunity record page and follow the sync process to the end", () -> {
            //  TODO Known Issue PBC-26288 (The steps are correct, but the their order is not)
            steps.syncWithNgbs.stepStartSyncWithNgbsViaProcessOrder(expectedSyncSteps);

            //  Contract Cancellation Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(CONTRACT_CANCEL_STEP);

            //  Discount Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(DISCOUNT_SYNC_STEP);

            //  DigitalLine Unlimited can only be removed (downsell) via its parent DigitalLine license
            steps.ngbs.downsellLicensesInNGBS(digitalLine);
            //  Down-sell Step (no notifications)
            steps.syncWithNgbs.clickNextButtonForSync();
            steps.syncWithNgbs.checkIsSyncStepCompleted(MANUAL_DOWN_SELL_STEP);

            //  Upgrade Step (skipped automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(UPGRADE_STEP);

            steps.syncWithNgbs.checkSyncStepsFinished(expectedSyncSteps);
        });

        //  CRM-19604
        step("6. Check synced discount information in NGBS", () ->
                steps.syncWithNgbs.stepCheckDiscountsInformation(dlUnlimited, upgradePackage)
        );

        //  CRM-19604
        step("7. Check that the contract is terminated in NGBS", () ->
                steps.syncWithNgbs.stepCheckContractTerminated()
        );
    }
}
