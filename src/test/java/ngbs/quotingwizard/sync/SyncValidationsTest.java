package ngbs.quotingwizard.sync;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityShareFactory;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.SALES_ENGINEER_LIGHTNING_PROFILE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.getUser;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.closeWindow;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("NGBS")
@Tag("SyncWithNGBS")
public class SyncValidationsTest extends BaseTest {
    private final Steps steps;

    private User dealDeskUser;

    //  Test data
    private final Product dlUnlimited;
    private final Product phoneToAdd;

    private final String standardPackageFullName;

    private final List<String> expectedSyncSteps;

    public SyncValidationsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/sync/RC_MVP_Monthly_Contract_177156013.json",
                Dataset.class);
        steps = new Steps(data);

        phoneToAdd = data.getProductByDataName("LC_HD_687");
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");

        standardPackageFullName = data.packageFolders[0].packages[0].getFullName();

        expectedSyncSteps = List.of(CONTRACT_SYNC_STEP, DISCOUNT_SYNC_STEP, REPRICE_STEP, ORDER_SYNC);

        steps.ngbs.isGenerateAccountsForSingleTest = true;
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();
        steps.ngbs.stepCreateContractInNGBS();
        steps.syncWithNgbs.stepResetContractState(dlUnlimited);

        dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.syncWithNgbs.stepCreateAdditionalActiveSalesAgreement(steps.salesFlow.account, steps.salesFlow.contact,
                dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-12839")
    @TmsLink("CRM-13086")
    @TmsLink("CRM-12760")
    @DisplayName("CRM-12839 - Only DL Unlimited should be sent as contractual License to BAP. \n" +
            "CRM-13086 - MVP Process Order functionality is not available for Profiles w/o Permission. \n" +
            "CRM-12760 - Process Order Button. Not Closed Opportunity.")
    @Description("CRM-12839 - Check that Only DL Unlimited is sent to Billing after Process Order button is pressed. \n" +
            "CRM-13086 - To check that Process Order functionality is not available for profiles that don't have assigned Custom Permission. \n" +
            "CRM-12760 - Check if User clicks On Process Order for not Closed Opportunity the Notification Window will appear and nothing will be synced.")
    public void test() {
        step("1. Open the Existing Business Opportunity, switch to the Quote Wizard, " +
                "click 'Add New' button in Sales Quote section, check the selected package and save changes", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();

            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(standardPackageFullName));
            packagePage.packageSelector.packageFilter.contractSelectInput.shouldBe(checked);

            packagePage.saveChanges();
        });

        step("2. Open the Add Products tab and add a new phone to the cart", () ->
                steps.quoteWizard.addProductsOnProductsTab(phoneToAdd)
        );

        step("3. Open the Price tab, set quantity for DL Unlimited = " + dlUnlimited.quantity + ", " +
                "set quantity for the new phone = " + phoneToAdd.quantity + ", " +
                "add discount for DL Unlimited, assign all new devices to DLs and save changes", () -> {
            steps.syncWithNgbs.stepSetupQuantitiesAndDiscounts(dlUnlimited, steps.quoteWizard.localAreaCode, phoneToAdd);
            cartPage.saveChanges();

            closeWindow();
        });

        //  For CRM-12760
        step("4. Click 'Process Order' button on the Opportunity record page, " +
                "check the error message on the Process Order modal, and close the modal window", () -> {
            opportunityPage.clickProcessOrderButton();
            processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60));
            processOrderModal.errorNotifications
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(format(OPPORTUNITY_SHOULD_BE_IN_CLOSED_WON_STAGE_ERROR, MVP_SERVICE)));
            processOrderModal.closeButton.click();
        });

        step("5. Close the opportunity via API", () ->
                steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity)
        );

        //  For CRM-13086
        step("6. Manually share the Opportunity with the user without Custom Permission = 'Can_Sync_With_NGBS' via API, " +
                "re-login as this user, open the Opportunity, press 'Process Order' button, and check the error message", () -> {
            //  Profile 'Sales Engineer Lightning' does NOT have custom permission = 'Can_Sync_With_NGBS'
            var userWithoutSyncPermission = getUser().withProfile(SALES_ENGINEER_LIGHTNING_PROFILE).execute();
            OpportunityShareFactory.shareOpportunity(steps.quoteWizard.opportunity.getId(), userWithoutSyncPermission.getId());

            steps.sfdc.reLoginAsUser(userWithoutSyncPermission);
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());

            opportunityPage.clickProcessOrderButton();

            processOrderModal.alertNotificationBlock.shouldBe(visible, ofSeconds(60));
            processOrderModal.errorNotifications.shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    format(YOU_DONT_HAVE_PERMISSION_TO_USE_PROCESS_ORDER_ERROR, MVP_SERVICE)), ofSeconds(1));
        });

        step("7. Re-login as a user with 'Deal Desk Lightning' profile and open the Opportunity record page", () -> {
            steps.sfdc.reLoginAsUserWithSessionReset(dealDeskUser);
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
        });

        step("8. Press 'Process Order' button on the Opportunity record page and follow the sync process to the end", () -> {
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

        //  For CRM-12839
        step("9. Check synced contract information in NGBS", () ->
                steps.syncWithNgbs.stepCheckContractInformation(dlUnlimited)
        );
    }
}
