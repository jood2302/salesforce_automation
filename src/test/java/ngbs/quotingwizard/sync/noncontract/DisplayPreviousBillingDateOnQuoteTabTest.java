package ngbs.quotingwizard.sync.noncontract;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.*;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getAccountInNGBS;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getContractsInNGBS;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.SetValueOptions.withDate;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("NGBS")
@Tag("SyncWithNGBS")
@Tag("ContractTerm")
public class DisplayPreviousBillingDateOnQuoteTabTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    private LocalDate previousBillingDate;

    //  Test data
    private final String upgradePackageFolderName;
    private final Package upgradePackage;

    private final Product phoneToAdd;
    private final Product dlUnlimited;

    private final List<String> expectedSyncSteps;

    public DisplayPreviousBillingDateOnQuoteTabTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/sync/RC_MVP_Monthly_NonContract_163114013.json",
                Dataset.class);
        steps = new Steps(data);

        steps.ngbs.isGenerateAccountsForSingleTest = true;

        upgradePackageFolderName = data.packageFoldersUpgrade[0].name;
        upgradePackage = data.packageFoldersUpgrade[0].packages[0];

        phoneToAdd = data.getProductByDataNameFromUpgradeData("LC_HD_611");
        dlUnlimited = data.getProductByDataNameFromUpgradeData("LC_DL-UNL_50");

        expectedSyncSteps = List.of(CONTRACT_SYNC_STEP, DISCOUNT_SYNC_STEP, REPRICE_STEP, ORDER_SYNC);
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
        }

        step("Get the previous billing date from NGBS account via NGBS API", () -> {
            previousBillingDate = getAccountInNGBS(data.billingId).getMainPackage().getPreviousBillingDateAsLocalDate();
        });

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-25888")
    @DisplayName("CRM-25888 - Display previous billing date on the Quote Details tab (monthly to monthly)")
    @Description("Verify that the previous billing date on the Quote Details tab is displayed")
    public void test() {
        step("1. Open the Opportunity, switch to the Quote Wizard, and click 'Add New' in the Sales Quote section", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
        });

        step("2. Select another package and save changes", () -> {
            steps.syncWithNgbs.stepUpgradeWithContract(upgradePackageFolderName, upgradePackage, upgradePackage.contract);
            packagePage.saveChanges();
        });

        step("3. Add a phone on the Add Products tab, open the Price tab, " +
                "set up quantities, and assign the phone to the DLs", () -> {
            steps.quoteWizard.addProductsOnProductsTab(phoneToAdd);
            steps.syncWithNgbs.stepSetupQuantitiesAndDiscounts(dlUnlimited, steps.quoteWizard.localAreaCode, phoneToAdd);
        });

        step("4. Open the Quote Details tab, and check the message with billing date under the 'Start Date' field", () -> {
            quotePage.openTab();

            var expectedMessage = getBillingDateMessage(previousBillingDate);
            quotePage.initialTermMessage.shouldHave(exactTextCaseSensitive(expectedMessage));
        });

        step("5. Set the 'Start Date' less than the previous billing date, save changes, " +
                "change Quote stage to 'Agreement', and check the error message", () -> {
            var dateBeforeBillingDate = previousBillingDate.minusDays(1);
            quotePage.startDateInput.setValue(withDate(dateBeforeBillingDate));
            quotePage.saveChanges();

            quotePage.stagePicklist.selectOption(AGREEMENT_STAGE);
            quotePage.startDateSection.shouldHave(exactTextCaseSensitive(START_DATE_SHOULD_BE_IN_RANGE_ERROR));
        });

        step("6. Change Quote stage to 'Quote', " +
                "set the 'Start Date' that exceeds the previous billing date by more than 30 days, save changes, " +
                "change Quote stage back to 'Agreement', and check the error message", () -> {
            quotePage.stagePicklist.selectOption(QUOTE_STAGE);

            var dateAfterBillingDate = previousBillingDate.plusDays(31);
            quotePage.startDateInput.setValue(withDate(dateAfterBillingDate));
            quotePage.saveChanges();

            quotePage.stagePicklist.selectOption(AGREEMENT_STAGE);
            quotePage.startDateSection.shouldHave(exactTextCaseSensitive(START_DATE_SHOULD_BE_IN_RANGE_ERROR));
        });

        step("7. Change Quote stage to 'Quote', " +
                "set the same 'Start Date' as previous billing date, save changes, " +
                "change Quote stage back to 'Agreement', and check that there is no error message", () -> {
            quotePage.stagePicklist.selectOption(QUOTE_STAGE);

            quotePage.startDateInput.setValue(withDate(previousBillingDate));
            quotePage.saveChanges();

            quotePage.stagePicklist.selectOption(AGREEMENT_STAGE);
            quotePage.startDateSection.shouldNot(exist);
        });

        step("8. Change Quote stage to 'Quote', " +
                "set the 'Start Date' in range from +1 to +30 days than previous billing date, save changes, " +
                "change Quote stage back to 'Agreement', check that there is no error message, and save changes", () -> {
            quotePage.stagePicklist.selectOption(QUOTE_STAGE);

            var newBillingDate = previousBillingDate.plusDays(5);
            quotePage.startDateInput.setValue(withDate(newBillingDate));
            quotePage.saveChanges();

            quotePage.stagePicklist.selectOption(AGREEMENT_STAGE);
            quotePage.startDateSection.shouldNot(exist);

            quotePage.saveChanges();
        });

        step("9. Update the Quote to Approved Active Agreement, close the Opportunity, " +
                "and set ServiceInfo__c.UpgradeStepStatus__c = true (all via API)", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);
            steps.syncWithNgbs.stepSkipUpgradeSyncStep(wizardPage.getSelectedQuoteId());
        });

        step("10. Press 'Process Order' button on the Opportunity record page and follow the sync process to the end", () -> {
            steps.syncWithNgbs.stepStartSyncWithNgbsViaProcessOrder(expectedSyncSteps);

            //  Contract Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(CONTRACT_SYNC_STEP);

            //  Discount Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(DISCOUNT_SYNC_STEP);

            //  Reprice Step
            steps.syncWithNgbs.checkRepriceStep();

            //  Order sync Step
            steps.syncWithNgbs.clickNextButtonForSync();
            steps.syncWithNgbs.checkIsSyncStepCompleted(ORDER_SYNC);

            steps.syncWithNgbs.checkSyncStepsFinished(expectedSyncSteps);
        });

        step("11. Check that the Contract is created for the Account in NGBS", () -> {
            var contractsOnAccount = getContractsInNGBS(data.billingId, data.packageId);
            var activeContract = contractsOnAccount.stream()
                    .filter(contract -> contract.isContractActive())
                    .findFirst();
            assertThat(activeContract)
                    .as("Active Contract on the Account in NGBS")
                    .isPresent();
        });
    }
}
