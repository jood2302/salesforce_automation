package ngbs.quotingwizard.sync.contractcancelation;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.dto.account.FreeServiceCreditUpdateDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.cartPage;
import static base.Pages.packagePage;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.CONTRACT_CANCEL_STEP;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.DISCOUNT_SYNC_STEP;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.updateFreeServiceCredit;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("PDV")
@Tag("NGBS")
@Tag("SyncWithNGBS")
public class FreeServiceCreditContractCancelTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final Product dlUnlimited;

    private final String standardPackageFullName;
    private final Package upgradePackage;

    private final double serviceCreditAmountBeforeSync;
    private final double serviceCreditAmountAfterSync;

    private final List<String> expectedSyncSteps;

    public FreeServiceCreditContractCancelTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/sync/RC_MVP_Monthly_Contract_186621013.json",
                Dataset.class);
        steps = new Steps(data);

        dlUnlimited = data.getProductByDataNameFromUpgradeData("LC_DL-UNL_50");

        standardPackageFullName = data.packageFolders[0].packages[0].getFullName();
        upgradePackage = data.packageFoldersUpgrade[0].packages[0];

        serviceCreditAmountBeforeSync = 100;
        serviceCreditAmountAfterSync = 0;

        expectedSyncSteps = List.of(CONTRACT_CANCEL_STEP, DISCOUNT_SYNC_STEP);
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
        }
        steps.ngbs.stepCreateContractInNGBS();
        steps.syncWithNgbs.stepDeleteDiscountsOnAccount();

        var serviceCredit = new FreeServiceCreditUpdateDTO(serviceCreditAmountBeforeSync);
        updateFreeServiceCredit(data.billingId, serviceCredit);

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.syncWithNgbs.stepCreateAdditionalActiveSalesAgreement(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-19613")
    @TmsLink("CRM-19607")
    @TmsLink("CRM-13338")
    @DisplayName("CRM-19613 - With Contract cancellation 'Free Service Credit' is overwritten with 0. \n" +
            "CRM-19607 - Contract cancellation with Adding new Discounts for current Package. \n" +
            "CRM-13338 - Sync with NGBS remove FSC in NGBS. Deselecting Contract")
    @Description("CRM-19613 - User deselect Contract. \n" +
            "CRM-19607 - User deselect Contract with adding some new Lines but doesn't cross the Rater. \n" +
            "CRM-13338 - To check that FSC is synced to NGBS after user clicks Process Order button")
    public void test() {
        step("1. Open the Opportunity, switch to the Quote Wizard, " +
                "click 'Add New' in the Sales Quote section, and check the preselected package", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();

            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(standardPackageFullName));
            packagePage.packageSelector.packageFilter.contractSelectInput.shouldBe(checked);
        });

        step("2. Uncheck contract checkbox", () -> {
            packagePage.packageSelector.setContractSelected(false);
        });

        step("3. Open the Price tab, set discount for DL Unlimited and save changes", () -> {
            steps.syncWithNgbs.stepSetupQuantitiesAndDiscounts(dlUnlimited, steps.quoteWizard.localAreaCode);
            cartPage.saveChanges();
        });

        step("4. Approve the current quote and close the Opportunity (all via API)", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedStatus(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);
        });

        //  CRM-19607, CRM-19613
        step("5. Press 'Process Order' on the Opportunity record page and follow the sync process to the end", () -> {
            steps.syncWithNgbs.stepStartSyncWithNgbsViaProcessOrder(expectedSyncSteps);

            //  Contract Cancellation Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(CONTRACT_CANCEL_STEP);

            //  Discount Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(DISCOUNT_SYNC_STEP);

            steps.syncWithNgbs.checkSyncStepsFinished(expectedSyncSteps);
        });

        //  CRM-19607
        step("6. Check synced discount information in NGBS", () ->
                steps.syncWithNgbs.stepCheckDiscountsInformation(dlUnlimited, upgradePackage)
        );

        //  CRM-19607, CRM-19613
        step("7. Check that the contract is terminated in NGBS", () ->
                steps.syncWithNgbs.stepCheckContractTerminated()
        );

        //  CRM-19613, CRM-13338
        step("8. Check the amount of Free Service Credit on the account in NGBS", () ->
                steps.syncWithNgbs.stepCheckFreeServiceCreditAmount(serviceCreditAmountAfterSync)
        );
    }
}
