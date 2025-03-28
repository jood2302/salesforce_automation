package ngbs.quotingwizard.sync.contractcancelation;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.cartPage;
import static base.Pages.packagePage;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("NGBS")
@Tag("SyncWithNGBS")
public class UpsellContractCancelTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product dlUnlimited;
    private final Product phoneToAdd;

    private final String standardPackageFullName;
    private final Package upgradePackage;

    private final List<String> expectedSyncSteps;

    public UpsellContractCancelTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/sync/RC_MVP_Monthly_Contract_186649013.json",
                Dataset.class);
        steps = new Steps(data);

        dlUnlimited = data.getProductByDataNameFromUpgradeData("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataNameFromUpgradeData("LC_HD_687");

        standardPackageFullName = data.packageFolders[0].packages[0].getFullName();
        upgradePackage = data.packageFoldersUpgrade[0].packages[0];

        expectedSyncSteps = List.of(CONTRACT_CANCEL_STEP, DISCOUNT_SYNC_STEP, ORDER_SYNC);

        steps.ngbs.isGenerateAccountsForSingleTest = true;
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();
        steps.ngbs.stepCreateContractInNGBS();

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.syncWithNgbs.stepCreateAdditionalActiveSalesAgreement(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-19606")
    @DisplayName("CRM-19606 - Contract cancellation with Upsell with optional Discounts.")
    @Description("User deselect Contract with adding some new Lines but doesn't cross the Rater.")
    public void test() {
        step("1. Open the Opportunity, switch to the Quote Wizard, click 'Add New' in the Sales Quote section, " +
                "and check the preselected package", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();

            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(standardPackageFullName));
            packagePage.packageSelector.packageFilter.contractSelectInput.shouldBe(checked);
        });

        step("2. Uncheck contract and save changes", () -> {
            packagePage.packageSelector.setContractSelected(false);
            packagePage.saveChanges();
        });

        step("3. Open the Add Products tab and add a new phone to Cart", () ->
                steps.quoteWizard.addProductsOnProductsTab(phoneToAdd)
        );

        step("4. Open the Price tab, set quantity for DL Unlimited = " + dlUnlimited.quantity + ", " +
                "set quantity for the new phone = " + phoneToAdd.quantity + ", " +
                "add discount for DL Unlimited, assign all new devices and save changes", () -> {
            steps.syncWithNgbs.stepSetupQuantitiesAndDiscounts(dlUnlimited, steps.quoteWizard.localAreaCode, phoneToAdd);
            cartPage.saveChanges();
        });

        step("5. Approve the current quote and close the Opportunity (all via API)", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedStatus(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);
        });

        step("6. Press 'Process Order' button on the Opportunity record page and follow the sync process to the end", () -> {
            steps.syncWithNgbs.stepStartSyncWithNgbsViaProcessOrder(expectedSyncSteps);

            //  Contract Cancellation Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(CONTRACT_CANCEL_STEP);

            //  Discount Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(DISCOUNT_SYNC_STEP);

            //  Order sync Step
            steps.syncWithNgbs.clickNextButtonForSync();
            steps.syncWithNgbs.checkIsSyncStepCompleted(ORDER_SYNC);

            steps.syncWithNgbs.checkSyncStepsFinished(expectedSyncSteps);
        });

        step("7. Check synced discount information in NGBS", () ->
                steps.syncWithNgbs.stepCheckDiscountsInformation(dlUnlimited, upgradePackage)
        );

        step("8. Check that the contract is terminated in NGBS", () ->
                steps.syncWithNgbs.stepCheckContractTerminated()
        );
    }
}
