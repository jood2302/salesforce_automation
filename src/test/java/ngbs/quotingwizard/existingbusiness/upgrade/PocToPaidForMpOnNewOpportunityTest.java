package ngbs.quotingwizard.existingbusiness.upgrade;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.newbusiness.signup.MultiProductSignUpSteps;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createInvoiceApprovalApproved;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.CREDIT_CARD_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.POC_RC_ACCOUNT_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.*;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("POC")
@Tag("POC-to-Paid")
@Tag("SyncWithNGBS")
@Tag("Multiproduct-Lite")
public class PocToPaidForMpOnNewOpportunityTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final MultiProductSignUpSteps multiProductSignUpSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUser;
    private Account account;
    private Opportunity opportunity;

    //  Test data
    private final String upgradePackageChargeTerm;
    private final Package officePocPackage;
    private final String officeServiceName;
    private final Package officePackage;
    private final String engageVoiceServiceName;
    private final Package evPackage;
    private final String rcCcServiceName;
    private final Package rcCcPackage;
    private final Product engageVoiceProduct;
    private final Product dlUnlimitedUltra;

    private final List<String> expectedSyncSteps;

    public PocToPaidForMpOnNewOpportunityTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_POC_163081013_MVP_Annual_Regular_ED_EV_RCCC.json",
                Dataset.class);
        steps = new Steps(data);
        multiProductSignUpSteps = new MultiProductSignUpSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        upgradePackageChargeTerm = data.packageFoldersUpgrade[0].chargeTerm;

        steps.ngbs.isGenerateAccountsForSingleTest = true;

        officePocPackage = data.packageFolders[0].packages[0];
        officeServiceName = data.packageFoldersUpgrade[0].name;
        officePackage = data.packageFoldersUpgrade[0].packages[0];
        engageVoiceServiceName = data.packageFolders[2].name;
        evPackage = data.packageFolders[2].packages[0];
        rcCcServiceName = data.packageFolders[3].name;
        rcCcPackage = data.packageFolders[3].packages[0];
        engageVoiceProduct = data.getProductByDataName("SA_CRS30_24", evPackage);
        dlUnlimitedUltra = data.getProductByDataNameFromUpgradeData("LC_DL-UNL_50");

        expectedSyncSteps = List.of(CONTRACT_SYNC_STEP, DISCOUNT_SYNC_STEP, UPGRADE_STEP);
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        account = steps.salesFlow.account;
        steps.quoteWizard.createOpportunity(account, steps.salesFlow.contact, dealDeskUser);
        opportunity = steps.quoteWizard.opportunity;

        step("Set Account's RC_User_ID__c = '" + data.rcUserId + "' " +
                "and RC_Account_Status__c = '" + POC_RC_ACCOUNT_STATUS + "' via SFDC API", () -> {
            account.setRC_User_ID__c(data.rcUserId);
            account.setRC_Account_Status__c(POC_RC_ACCOUNT_STATUS);
            enterpriseConnectionUtils.update(account);
        });

        step("Create new Billing Account Package objects (Package__c) for the Account " +
                "for the Office PoC package via SFDC API", () -> {
            var officePocBillingAccountPackage = createBillingAccountPackage(account.getId(),
                    data.packageId, officePocPackage.id, data.brandName, officeServiceName, CREDIT_CARD_PAYMENT_METHOD, POC_RC_ACCOUNT_STATUS);
            officePocBillingAccountPackage.setEnabledLBO__c(true);
            enterpriseConnectionUtils.update(officePocBillingAccountPackage);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-31221")
    @DisplayName("CRM-31221 - POC-to-Paid for MP (MVP + EV + RC CC) on new Opportunity")
    @Description("Verify that is possible to make Upgrade from POC to MP (MVP + EV + RC CC) on new Opportunity")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "and select MVP, EV and RC CC packages with Annual charge Term", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(opportunity.getId());
            packagePage.packageSelector.selectPackage(upgradePackageChargeTerm, officeServiceName, officePackage);
            packagePage.packageSelector.selectPackage(upgradePackageChargeTerm, engageVoiceServiceName, evPackage);
            packagePage.packageSelector.selectPackage(upgradePackageChargeTerm, rcCcServiceName, rcCcPackage);
        });

        step("2. Add necessary products on the Add Products tab, open the Price tab, and save changes", () -> {
            steps.quoteWizard.addProductsOnProductsTab(engageVoiceProduct);
            cartPage.openTab();
            cartPage.saveChanges();
        });

        step("3. Click 'Initiate CC ProServ' button, click 'Submit' button in popup window " +
                "and check that 'Initiate CC ProServ' button is hidden and 'CC ProServ Created' button is visible and disabled " +
                "and set CC ProServ Quote.ProServ_Status__c = 'Sold' via API", () -> {
            cartPage.initiateCcProServ();
            cartPage.waitUntilLoaded();

            step("Set Quote.ProServ_Status__c = 'Sold' for CC ProServ Quote via API", () -> {
                var ccProServQuote = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id " +
                                "FROM Quote " +
                                "WHERE OpportunityId = '" + opportunity.getId() + "' " +
                                "AND RecordType.Name = '" + CC_PROSERV_QUOTE_RECORD_TYPE + "'",
                        Quote.class);
                ccProServQuote.setProServ_Status__c(SOLD_PROSERV_STATUS);
                enterpriseConnectionUtils.update(ccProServQuote);
            });
        });

        step("4. Create approved Invoicing Approval Request for the test Account via API", () -> {
            createInvoiceApprovalApproved(opportunity, account, steps.salesFlow.contact, dealDeskUser.getId(), true);
        });

        step("5. Open the Quote Details tab, set the Start Date, save changes, and set it to Active Agreement via SFDC API", () -> {
            step("Open the Quote Details tab, set Start Date and save changes", () -> {
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

        step("6. Close the Opportunity and set ServiceInfo__c.UpgradeStepStatus__c = true (for 'Office' service) via API", () -> {
            steps.quoteWizard.stepCloseOpportunity(opportunity);
            steps.syncWithNgbs.stepSkipUpgradeSyncStepForMultiproductQuote(wizardPage.getSelectedQuoteId(), List.of(officeServiceName));
        });

        step("7. Open the Opportunity record page, open the Process Order modal window, " +
                "and sync POC to Paid for MVP service", () -> {
            step("Open the Opportunity record page, click 'Process Order' button, " +
                    "check that Quote Selector is hidden and all added services' sections are shown", () -> {
                opportunityPage.openPage(opportunity.getId());
                steps.syncWithNgbs.stepStartSyncWithNgbsViaProcessOrder(expectedSyncSteps);

                opportunityPage.processOrderModal.salesQuoteRadioButtonInput.shouldBe(hidden);
                opportunityPage.processOrderModal.mvpSection.shouldBe(visible);
                opportunityPage.processOrderModal.engageVoiceSection.shouldBe(visible);
                opportunityPage.processOrderModal.contactCenterSection.shouldBe(visible);
            });

            step("Check that the Opportunity is synced for MVP service", () -> {
                //  Contract Sync Step (synced automatically)
                steps.syncWithNgbs.checkIsSyncStepCompleted(CONTRACT_SYNC_STEP);

                //  Discount Sync Step (synced automatically)
                steps.syncWithNgbs.checkIsSyncStepCompleted(DISCOUNT_SYNC_STEP);

                //  Upgrade Step (skipped automatically)
                steps.syncWithNgbs.checkIsSyncStepCompleted(UPGRADE_STEP);

                steps.syncWithNgbs.checkSyncStepsFinished(expectedSyncSteps);
            });
        });

        step("8. Sign Up Engage Voice, and RingCentral Contact Center services in the Process Order modal", () -> {
            multiProductSignUpSteps.signUpEngageVoiceServiceStep(data.billingId);
            multiProductSignUpSteps.signUpRcContactCenterServiceStep(data.billingId, opportunity.getId());
        });

        step("9. Check synced contract information in NGBS", () ->
                steps.syncWithNgbs.stepCheckContractInformation(dlUnlimitedUltra)
        );
    }
}
