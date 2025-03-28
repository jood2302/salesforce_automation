package ngbs.quotingwizard.existingbusiness.upgrade;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.CREDIT_CARD_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.POC_RC_ACCOUNT_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.setQuoteToApprovedActiveAgreement;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("POC")
@Tag("POC-to-Paid")
@Tag("SyncWithNGBS")
@Tag("Multiproduct-Lite")
public class PocToPaidOnTheSameOpportunityTest extends BaseTest {
    private final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final Steps steps;

    //  Test data
    private final String officeService;
    private final Package officePocPackage;
    private final String upgradePackageChargeTerm;
    private final String upgradePackageFolderName;
    private final Package upgradePackage;
    private final Product dlUnlimitedUltra;

    private final List<String> expectedSyncSteps;

    public PocToPaidOnTheSameOpportunityTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_POC_163081013_MVP_Annual_Regular_ED_EV_RCCC.json",
                Dataset.class);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        steps = new Steps(data);

        steps.ngbs.isGenerateAccountsForSingleTest = true;

        officeService = data.packageFolders[0].name;
        officePocPackage = data.packageFolders[0].packages[0];
        upgradePackageChargeTerm = data.packageFoldersUpgrade[0].chargeTerm;
        upgradePackageFolderName = data.packageFoldersUpgrade[0].name;
        upgradePackage = data.packageFoldersUpgrade[0].packages[0];
        dlUnlimitedUltra = data.getProductByDataNameFromUpgradeData("LC_DL-UNL_50");

        expectedSyncSteps = List.of(CONTRACT_SYNC_STEP, DISCOUNT_SYNC_STEP, UPGRADE_STEP);
    }

    @BeforeEach
    public void setUpTest() {
        var dealDeskUser = steps.salesFlow.getDealDeskUser();

        //  to create the New Business type of Account and Opportunity (instead of Existing Business) via SOAP API
        data.billingId = EMPTY_STRING;
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);

        step("Open the Quote Wizard for the Opportunity to add a new POC quote, " +
                "select a package for it, and save changes", () -> {
            steps.quoteWizard.preparePocQuoteViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
        });

        steps.ngbs.generateBillingAccount();

        step("Set Account.RC_Account_Status__c = '" + POC_RC_ACCOUNT_STATUS + "' and populate Billing_ID__c and RC_User_ID__c " +
                "with data from generated Billing Account via SFDC API", () -> {
            steps.salesFlow.account.setRC_Account_Status__c(POC_RC_ACCOUNT_STATUS);
            steps.salesFlow.account.setBilling_ID__c(data.billingId);
            steps.salesFlow.account.setRC_User_ID__c(data.rcUserId);
            enterpriseConnectionUtils.update(steps.salesFlow.account);
        });

        step("Create new Billing Account Package objects (Package__c) for the Account " +
                "for the Office PoC package via SFDC API", () -> {
            var officePocBillingAccountPackage = createBillingAccountPackage(steps.salesFlow.account.getId(),
                    data.packageId, officePocPackage.id, data.brandName, officeService, CREDIT_CARD_PAYMENT_METHOD, POC_RC_ACCOUNT_STATUS);
            officePocBillingAccountPackage.setEnabledLBO__c(true);
            enterpriseConnectionUtils.update(officePocBillingAccountPackage);
        });
    }

    @Test
    @TmsLink("CRM-31510")
    @DisplayName("CRM-31510 - POCtoPaid on the same Opportunity")
    @Description("Verify that is possible to make Upgrade from POC to MVP Paid on the same Opportunity")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "and select MVP package with Contract and Annual charge Term, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            packagePage.packageSelector.selectPackage(upgradePackageChargeTerm, upgradePackageFolderName, upgradePackage);
            packagePage.saveChanges();
        });

        step("2. Open the Quote Details tab, set the Start Date, save changes, and set it to Active Agreement via SFDC API", () -> {
            step("Open the Quote Details tab, set the Start Date, and save changes", () -> {
                quotePage.openTab();
                quotePage.setDefaultStartDate();
                quotePage.saveChanges();
            });

            step("Update Quote to Active Agreement via API", () -> {
                var masterQuoteToUpdate = new Quote();
                masterQuoteToUpdate.setId(wizardPage.getSelectedQuoteId());
                setQuoteToApprovedActiveAgreement(masterQuoteToUpdate);
                enterpriseConnectionUtils.update(masterQuoteToUpdate);
            });
        });

        step("3. Close the Opportunity and set ServiceInfo__c.UpgradeStepStatus__c = true via API", () -> {
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);
            steps.syncWithNgbs.stepSkipUpgradeSyncStep(wizardPage.getSelectedQuoteId());
        });

        step("4. Open the Opportunity record page, press 'Process Order' button on the Opportunity record page, " +
                "and check that the MVP service is synced with NGBS successfully", () -> {
            step("Open the Opportunity record page, press 'Process Order' button, " +
                    "check that Quote Selector is hidden and MVP Section is shown", () -> {
                opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
                steps.syncWithNgbs.stepStartSyncWithNgbsViaProcessOrder(expectedSyncSteps);

                opportunityPage.processOrderModal.salesQuoteRadioButtonInput.shouldBe(hidden);
                opportunityPage.processOrderModal.mvpSection.shouldBe(visible);
            });

            step("Check that the Sync process is finished automatically", () -> {
                //  Contract Sync Step (synced automatically)
                steps.syncWithNgbs.checkIsSyncStepCompleted(CONTRACT_SYNC_STEP);

                //  Discount Sync Step (synced automatically)
                steps.syncWithNgbs.checkIsSyncStepCompleted(DISCOUNT_SYNC_STEP);

                //  Upgrade Step (skipped automatically)
                steps.syncWithNgbs.checkIsSyncStepCompleted(UPGRADE_STEP);

                steps.syncWithNgbs.checkSyncStepsFinished(expectedSyncSteps);
            });
        });

        step("5. Check synced contract information in NGBS", () ->
                steps.syncWithNgbs.stepCheckContractInformation(dlUnlimitedUltra)
        );
    }
}
