package ngbs.quotingwizard.sync;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.enabled;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;

@Tag("P1")
@Tag("NGBS")
@Tag("SyncWithNGBS")
public class NothingToSyncMessageTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product dlUnlimited;
    private final Product phoneToAdd;
    private final List<String> expectedSyncSteps;

    public NothingToSyncMessageTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/sync/RC_MVP_Annual_Contract_165611013.json",
                Dataset.class);
        steps = new Steps(data);

        dlUnlimited = data.getProductsFromBilling()[5];
        phoneToAdd = data.getNewProductsToAdd()[0];

        expectedSyncSteps = List.of(CONTRACT_SYNC_STEP, DISCOUNT_SYNC_STEP, REPRICE_STEP, ORDER_SYNC);
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
        }
        steps.ngbs.stepCreateContractInNGBS();

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.syncWithNgbs.stepCreateAdditionalActiveSalesAgreement(steps.salesFlow.account, steps.salesFlow.contact,
                dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-12919")
    @DisplayName("CRM-12919 - Nothing to Sync message allows user to continue with steps. Annual Contract")
    @Description("To check that Only DL Unlimited is sent to Billing after 'Process Order' button is pressed")
    public void test() {
        step("1. Open the Existing Business Opportunity, switch to the Quote Wizard, " +
                "click 'Add New' button in Sales Quote section and select the same package from the NGBS account for it", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        step("2. Open the Add Products tab and add a new phone", () ->
                steps.quoteWizard.addProductsOnProductsTab(phoneToAdd)
        );

        step("3. Open the Price tab, increase the quantity of DLs, assign all the Phones to the DLs, and save changes", () -> {
            steps.syncWithNgbs.stepSetupQuantitiesAndDiscounts(dlUnlimited, steps.quoteWizard.localAreaCode, phoneToAdd);
            cartPage.saveChanges();
        });

        step("4. Close the Opportunity and set ServiceInfo__c.UpgradeStepStatus__c = true via API", () -> {
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);
            steps.syncWithNgbs.stepSkipUpgradeSyncStep(wizardPage.getSelectedQuoteId());
        });

        step("5. Press 'Process Order' button on the Opportunity record page " +
                "and check all the available sync steps in the 'Process Order' modal window", () -> {
            steps.syncWithNgbs.stepStartSyncWithNgbsViaProcessOrder(expectedSyncSteps);
        });

        step("6. Press 'Next' and check that 'Nothing to sync' message appears for 'Contract sync' and 'Discount sync' steps", () -> {
            //  Contract Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(CONTRACT_SYNC_STEP);

            //  Discount Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(DISCOUNT_SYNC_STEP);

            //  Reprice Step
            processOrderModal.expandNotifications();
            processOrderModal.infoNotifications
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(format(NOTHING_TO_SYNC_MESSAGE, MVP_SERVICE)));
        });

        step("7. Check that the next available step ('Reprice') is available for user", () -> {
            processOrderModal.mvpCompletedSyncStepNames.shouldHave(size(2));
            processOrderModal.nextButton.shouldBe(enabled);
        });
    }
}
