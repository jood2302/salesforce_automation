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

import static base.Pages.cartPage;
import static base.Pages.packagePage;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.DISCOUNT_SYNC_STEP;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
@Tag("SyncWithNGBS")
public class NewDiscountsSyncNonContractTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product dlUnlimited;

    private final Package standardPackage;

    private final List<String> expectedSyncSteps;

    public NewDiscountsSyncNonContractTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/sync/RC_MVP_Monthly_NonContract_163118013.json",
                Dataset.class);
        steps = new Steps(data);

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");

        standardPackage = data.packageFolders[0].packages[0];

        expectedSyncSteps = List.of(DISCOUNT_SYNC_STEP);
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
        }
        steps.syncWithNgbs.stepDeleteDiscountsOnAccount();

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-12738")
    @DisplayName("CRM-12738 - Adding new Discounts for current Package.")
    @Description("User adds some new Lines but doesn't cross the Rater.")
    public void test() {
        step("1. Open the Opportunity, switch to the Quote Wizard, click 'Add New' in the Sales Quote section, " +
                "check the preselected package, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();

            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(standardPackage.getFullName()));
            packagePage.packageSelector.packageFilter.contractSelectInput.shouldNotBe(checked);

            packagePage.saveChanges();
        });

        step("2. Open the Price tab, add a discount for DL Unlimited and save changes", () -> {
            steps.syncWithNgbs.stepSetupQuantitiesAndDiscounts(dlUnlimited, steps.quoteWizard.localAreaCode);
            cartPage.saveChanges();
        });

        step("3. Approve the current quote and close the Opportunity via API", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedStatus(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);
        });

        step("4. Press 'Process Order' on the Opportunity record page and follow the sync process to the end", () -> {
            steps.syncWithNgbs.stepStartSyncWithNgbsViaProcessOrder(expectedSyncSteps);

            //  Discount Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(DISCOUNT_SYNC_STEP);

            steps.syncWithNgbs.checkSyncStepsFinished(expectedSyncSteps);
        });

        step("5. Check synced discount information in NGBS", () ->
                steps.syncWithNgbs.stepCheckDiscountsInformation(dlUnlimited, standardPackage)
        );
    }
}
