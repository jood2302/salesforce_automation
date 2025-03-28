package ngbs.quotingwizard.sync;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Case;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.CaseHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.AGREEMENT_QUOTE_TYPE;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("PDFGeneration")
@Tag("Phoenix")
@Tag("SyncWithNGBS")
public class UpsellPhoenixTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String meetingsService;
    private final String standardPackageFullName;
    private final Product videoPro;

    private final List<String> expectedPdfTemplates;

    private final List<String> expectedSyncSteps;

    public UpsellPhoenixTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/sync/Unify_Office_Phoenix_Monthly_Contract_129413013.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        meetingsService = data.packageFolders[0].name;
        standardPackageFullName = data.packageFolders[0].packages[0].getFullName();
        videoPro = data.getProductByDataName("LC_SM_405");

        expectedPdfTemplates = List.of("Incremental Order Form", "Change Order Form");

        expectedSyncSteps = List.of(CONTRACT_SYNC_STEP, DISCOUNT_SYNC_STEP, UP_SELL_STEP);
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
        }
        steps.ngbs.stepCreateContractInNGBS(
                data.billingId, data.packageId,
                data.packageFolders[0].packages[0].contractExtId,
                data.packageFolders[0].packages[0].productsFromBilling[1]
        );
        steps.syncWithNgbs.stepDeleteDiscountsOnAccount();

        var dealDeskUser = steps.salesFlow.getDealDeskUser();

        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        steps.syncWithNgbs.stepCreateAdditionalActiveSalesAgreement(steps.salesFlow.account, steps.salesFlow.contact,
                dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);

    }

    @Test
    @TmsLink("CRM-21893")
    @DisplayName("CRM-21893 - Phoenix: SyncWithNGBS for Upsell")
    @Description("Verify that a validation error appears when syncing upsell Phoenix opportunity " +
            "with NGBS on 'Up-sell (in external system)' step + verify a list of available PDF templates")
    public void test() {
        step("1. Open the Existing Business Opportunity, switch to the Quote Wizard, " +
                "add a new Sales Quote, check the selected package, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardOnOpportunityRecordPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addNewSalesQuote();

            packagePage.packageSelector.getSelectedPackage().getName()
                    .shouldHave(exactTextCaseSensitive(standardPackageFullName));
            packagePage.packageSelector.packageFilter.contractSelectInput.shouldBe(checked);

            packagePage.saveChanges();
        });

        step("2. Open the Price tab, set quantity for Video Pro+ = " + videoPro.quantity + " and save the changes", () -> {
            cartPage.openTab();
            cartPage.setNewQuantityForQLItem(videoPro.name, videoPro.quantity);
            cartPage.saveChanges();
        });

        step("3. Open the Quote Details tab, set 'Quote Stage' = 'Agreement', and save changes", () -> {
            quotePage.openTab();
            quotePage.stagePicklist.shouldBe(enabled).selectOption(AGREEMENT_QUOTE_TYPE);
            quotePage.saveChanges();
        });

        step("4. Click 'Generate PDF' and check the list of PDF Templates, " +
                "and press 'Cancel' in the 'PDF Generate' modal", () -> {
            quotePage.generatePdfButton.click();
            quotePage.pdfGenerateModal.pdfAllTemplateNames
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(expectedPdfTemplates), ofSeconds(60));

            quotePage.pdfGenerateModal.cancelButton.click();
        });

        step("5. Close the opportunity via API", () ->
                steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity)
        );

        step("6. Press 'Process Order' button on the Opportunity page and follow the sync process to the end", () -> {
            steps.syncWithNgbs.stepStartSyncWithNgbsViaProcessOrder(expectedSyncSteps, meetingsService);

            //  Contract Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(CONTRACT_SYNC_STEP);

            //  Discount Sync Step (synced automatically)
            steps.syncWithNgbs.checkIsSyncStepCompleted(DISCOUNT_SYNC_STEP);

            //  Up-sell Step
            processOrderModal.nextButton.click();
            processOrderModal.expandNotifications();

            processOrderModal.errorNotifications
                    .shouldHave(exactTexts(format(LICENSES_SHOULD_BE_ADDED_MANUALLY_IN_SW_ERROR, MEETINGS_SERVICE)), ofSeconds(60));

            processOrderModal.closeButton.click();
        });

        step("7. Check that the Case for manual up-sell was created " +
                "and that Case's Description contains links to the Account and Opportunity " +
                "and the info that this Quote requires Manual Order", () -> {
            var dealDeskCase = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Subject, Description " +
                            "FROM Case " +
                            "WHERE AccountId = '" + steps.salesFlow.account.getId() + "'" +
                            "AND Opportunity_Reference__c = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Case.class);

            assertThat(dealDeskCase.getSubject())
                    .as("Case.Subject value(should contain info about manual order)")
                    .contains(MANUAL_ORDER_REQUIRED_SUBJECT);
            assertThat(dealDeskCase.getDescription())
                    .as("Case.Description value (should contain link to the Account)")
                    .contains(formatAccountLinkInCaseDescription(steps.salesFlow.account.getId()));
            assertThat(dealDeskCase.getDescription())
                    .as("Case.Description value (should contain link to the Opportunity)")
                    .contains(formatOpportunityLinkInCaseDescription(steps.quoteWizard.opportunity.getId()));
            assertThat(dealDeskCase.getDescription())
                    .as("Case.Description value (should contain the info that this Quote requires Manual Order)")
                    .contains(PHOENIX_QUOTE_CONTAINS_UPSELL_DESCRIPTION);
        });
    }
}
