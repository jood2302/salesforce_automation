package ngbs.quotingwizard.sync;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.model.ngbs.dto.license.CatalogItem.getItemFromTestData;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.AGREEMENT_STAGE;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getBillingInfoSummaryLicenses;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("LTR-1220")
@Tag("SyncWithNGBS")
public class AutoAndManualDownsellTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final Product digitalLine;
    private final Product dlUnlimited;
    private final Product thetaLakeCallDetailRecords;
    private final Product events100Attendees;

    private final int newLicenseQuantity;

    private final int initialDownsellQuantity;
    private final int completeDownsellQuantity;

    private final List<String> expectedSyncSteps;

    public AutoAndManualDownsellTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/sync/RC_MVP_Monthly_Contract_WithAutoDownsellLicenses_199762013.json",
                Dataset.class);
        steps = new Steps(data);
        steps.ngbs.isGenerateAccountsForSingleTest = true;

        digitalLine = data.getProductByDataName("LC_DL_75");
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        thetaLakeCallDetailRecords = data.getProductByDataName("LC_THTLK_442005");
        events100Attendees = data.getProductByDataName("LC_EVNT_1350005");

        newLicenseQuantity = 7;

        initialDownsellQuantity = 1;
        completeDownsellQuantity = dlUnlimited.existingQuantity - newLicenseQuantity - initialDownsellQuantity;

        expectedSyncSteps = List.of(CONTRACT_SYNC_STEP, DISCOUNT_SYNC_STEP, AUTO_AND_MANUAL_DOWN_SELL_STEP);
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();
        steps.ngbs.purchaseAdditionalLicensesInNGBS(getItemFromTestData(thetaLakeCallDetailRecords.dataName,
                thetaLakeCallDetailRecords.quantity));
        steps.ngbs.purchaseAdditionalLicensesInNGBS(getItemFromTestData(events100Attendees.dataName,
                events100Attendees.quantity));
        steps.ngbs.stepCreateContractInNGBS();

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-36965")
    @DisplayName("CRM-36965 - Blocking downsell step until manual downsell is completed")
    @Description("Verify that there is no possibility of passing the Downsell step " +
            "without removing the licenses listed in 'Products for manual removal and deprovisioning' Case. " +
            "Validation with error message should work on downsell step\n" +
            "Description text for the validation should be: 'Licenses should be removed manually'")
    public void test() {
        step("1. Open the Existing Business Opportunity, switch to the Quote Wizard, " +
                "and click 'Add New' button in Sales Quote section", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();
        });

        step("2. Open the Price tab, reduce the quantity of the DL Unlimited, 'Events 100 attendees' " +
                "and 'Theta Lake Call Detail Records' licenses to " + newLicenseQuantity + " and save changes", () -> {
            cartPage.openTab();
            cartPage.setNewQuantityForQLItem(dlUnlimited.name, newLicenseQuantity);
            cartPage.setNewQuantityForQLItem(thetaLakeCallDetailRecords.name, newLicenseQuantity);
            cartPage.setNewQuantityForQLItem(events100Attendees.name, newLicenseQuantity);
            cartPage.saveChanges();
        });

        step("3. Open the Quote Details tab, select Quote Stage = 'Agreement', set Start Date and save changes", () -> {
            quotePage.openTab();
            quotePage.stagePicklist.selectOption(AGREEMENT_STAGE);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();
        });

        step("4. Set the Quote to Active Agreement via API", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);
        });

        step("5. Close the Opportunity via API", () -> {
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);
        });

        step("6. Press 'Process Order' button on the Opportunity record page, " +
                "and follow the sync process to the 'Down-sell' step", () -> {
            steps.syncWithNgbs.stepStartSyncWithNgbsViaProcessOrder(expectedSyncSteps);

            //  Contract Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(CONTRACT_SYNC_STEP);

            //  Discount Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(DISCOUNT_SYNC_STEP);
        });

        //  Down-sell Step
        step("7. Click 'Next' on the 'Down-sell' step, check that the error message is displayed, " +
                "and the updated quantities of down-sold products in NGBS", () -> {
            //  No down-sell
            steps.syncWithNgbs.clickNextButtonForSync();
            processOrderModal.errorNotifications.shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    format(LICENSES_SHOULD_BE_REMOVED_MANUALLY_ERROR, MVP_SERVICE)), ofSeconds(60));

            var billingInfoLicenses = getBillingInfoSummaryLicenses(data.billingId, data.packageId);

            var expectedProductLicenses = List.of(dlUnlimited, thetaLakeCallDetailRecords, events100Attendees);
            //  Licenses with downsellType__c in ("auto", "autoWithCase") should be down-sold automatically with the new quantity
            thetaLakeCallDetailRecords.quantity = newLicenseQuantity;
            events100Attendees.quantity = newLicenseQuantity;

            for (var product : expectedProductLicenses) {
                step("Check product data for '" + product.name + "'");
                var licenseActual = Arrays.stream(billingInfoLicenses)
                        .filter(license -> license.catalogId.equals(product.dataName))
                        .findFirst();

                assertThat(licenseActual)
                        .as(format("The license from NGBS for the product '%s' (should exist)", product.name))
                        .isPresent();
                assertThat(licenseActual.get().qty)
                        .as(format("The 'quantity' value on the license from NGBS for the product '%s'", product.name))
                        .isEqualTo(product.quantity);
            }
        });

        step("8. Partially downsell DL Unlimited license in NGBS, click 'Next' on the Process Order modal, " +
                "and check that the error message is displayed", () -> {
            //  Partial down-sell
            //  DigitalLine Unlimited can only be removed (downsell) via its parent DigitalLine license
            steps.ngbs.downsellLicensesInNGBS(digitalLine, initialDownsellQuantity);
            steps.syncWithNgbs.clickNextButtonForSync();
            //  There's one more error message because the user didn't finish the down-sell completely
            processOrderModal.errorNotifications.shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    format(LICENSES_SHOULD_BE_REMOVED_MANUALLY_ERROR, MVP_SERVICE),
                    format(LICENSES_SHOULD_BE_REMOVED_MANUALLY_ERROR, MVP_SERVICE)), ofSeconds(60));
        });

        step("9. Downsell all the remaining DL Unlimited licenses in NGBS, click 'Next' on the Process Order modal, " +
                "and check that the 'Down-sell' step is successfully completed, " +
                "and that the sync process is finished successfully", () -> {
            //  Complete down-sell
            steps.ngbs.downsellLicensesInNGBS(digitalLine, completeDownsellQuantity);
            steps.syncWithNgbs.clickNextButtonForSync();
            steps.syncWithNgbs.checkIsSyncStepCompleted(AUTO_AND_MANUAL_DOWN_SELL_STEP);

            steps.syncWithNgbs.checkSyncStepsFinished(expectedSyncSteps);
        });
    }
}
