package ngbs.quotingwizard.sync;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.dto.account.FreeServiceCreditUpdateDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.packagePage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.updateFreeServiceCredit;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("NGBS")
@Tag("SyncWithNGBS")
public class SendFreeServiceCreditTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product dlUnlimited;
    private final Product phoneToAdd;

    private final String standardPackageFullName;

    private final String specialTerms;
    private final double serviceCreditAmountBeforeSync;

    private final List<String> expectedSyncSteps;

    public SendFreeServiceCreditTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/sync/RC_MVP_Monthly_NonContract_186576013.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        dlUnlimited = data.getProductByDataNameFromUpgradeData("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataNameFromUpgradeData("LC_HD_687");

        standardPackageFullName = data.packageFolders[0].packages[0].getFullName();

        specialTerms = "2 Free Months of Service";
        serviceCreditAmountBeforeSync = 0;

        expectedSyncSteps = List.of(CONTRACT_SYNC_STEP, DISCOUNT_SYNC_STEP, REPRICE_STEP, ORDER_SYNC);

        steps.ngbs.isGenerateAccountsForSingleTest = true;
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        var serviceCredit = new FreeServiceCreditUpdateDTO(serviceCreditAmountBeforeSync);
        updateFreeServiceCredit(data.billingId, serviceCredit);

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-13320")
    @DisplayName("CRM-13320 - Sync with NGBS send FSC to NGBS. Going under contract.")
    @Description("To check that FSC is synced to NGBS after user clicks 'Process Order' button.")
    public void test() {
        step("1. Open the Existing Business Opportunity, switch to the Quote Wizard, " +
                "click 'Add New' button in Sales Quote section, and check the selected package", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();

            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(standardPackageFullName));
            packagePage.packageSelector.packageFilter.contractSelectInput.shouldNotBe(checked);
        });

        step("2. Select a contract for the selected package and save changes", () -> {
            packagePage.packageSelector.setContractSelected(true);
            packagePage.saveChanges();
        });

        step("3. Open the Add Products tab and add a new phone to the cart", () ->
                steps.quoteWizard.addProductsOnProductsTab(phoneToAdd)
        );

        step("4. Open the Price tab, set discounts for a new phone and DL Unlimited", () ->
                steps.syncWithNgbs.stepSetupQuantitiesAndDiscounts(dlUnlimited, steps.quoteWizard.localAreaCode,
                        phoneToAdd)
        );

        step("5. Open the Quote Details tab, " +
                "populate Initial Term and Start Date, " +
                "set 'Number of Months' in Free Service Credit = '" + specialTerms + "' on the Billing Details and Terms modal, " +
                "and save changes", () -> {
            quotePage.openTab();
            quotePage.initialTermPicklist.selectOption(data.getInitialTerm());
            quotePage.setDefaultStartDate();
            quotePage.applyNewSpecialTerms(specialTerms);
        });

        step("6. Set the current quote to the Active Agreement and close the Opportunity via API", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);
        });

        step("7. Press 'Process Order' on the Opportunity record page and follow the sync process to the end", () -> {
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

        step("8. Check the amount of Free Service Credit on the account in NGBS and SFDC", () -> {
            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Free_Service_Credit_Total__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);
            steps.syncWithNgbs.stepCheckFreeServiceCreditAmount(quote.getFree_Service_Credit_Total__c());
        });
    }
}
