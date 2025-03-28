package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.MultiProductQuoteTypeAndStatusSteps;
import org.junit.jupiter.api.*;

import java.util.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("LTR-569")
@Tag("Quote")
@Tag("MultiProduct-UB")
public class MultiProductQuoteTypeAndStatusChangedTest extends BaseTest {
    private final Steps steps;
    private final MultiProductQuoteTypeAndStatusSteps multiProductQuoteTypeAndStatusSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUserWithEnabledMpUbFT;
    private String opportunityId;
    private String masterQuoteId;

    //  Test data
    private final Map<String, Package> packageFolderNameToPackageMap;
    private final Set<String> allSelectedServices;

    private final Product callRecordingStorage;

    public MultiProductQuoteTypeAndStatusChangedTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_Annual_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        multiProductQuoteTypeAndStatusSteps = new MultiProductQuoteTypeAndStatusSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[1].name, data.packageFolders[1].packages[0],
                data.packageFolders[3].name, data.packageFolders[3].packages[0]
        );

        allSelectedServices = packageFolderNameToPackageMap.keySet();
        callRecordingStorage = data.getProductByDataName("SA_CRS30_24", data.packageFolders[1].packages[0]);
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Deal Desk Lightning' profile and 'Edit_Status_on_Quote' permission set " +
                "and with 'Enable Multi-Product Unified Billing' Feature Toggle", () -> {
            dealDeskUserWithEnabledMpUbFT = getUser()
                    .withProfile(DEAL_DESK_LIGHTNING_PROFILE)
                    .withFeatureToggles(List.of(ENABLE_MULTIPRODUCT_UNIFIED_BILLING_FT))
                    .withPermissionSet(EDIT_STATUS_ON_QUOTE_PS)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUserWithEnabledMpUbFT);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUserWithEnabledMpUbFT);
        opportunityId = steps.quoteWizard.opportunity.getId();

        step("Log in as a user with 'Deal Desk Lightning' profile, and 'Edit_Status_on_Quote' permission set, " +
                "and 'Enable Multi-Product Unified Billing' Feature Toggle", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUserWithEnabledMpUbFT)
        );
    }

    @Test
    @TmsLink("CRM-36665")
    @DisplayName("CRM-36665 - UB. QuoteType__c and Status fields are changed for the Technical Quotes (New Business)")
    @Description("Verify that for Technical Quotes values of the following fields are changed so that Master " +
            "and Technical quotes would have the same status and stage: Quote.QuoteType__c, Quote.Status")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select MVP, Engage Voice and RC Contact Center packages for it, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityForMultiProduct(opportunityId, packageFolderNameToPackageMap);

            masterQuoteId = wizardPage.getSelectedQuoteId();
        });

        step("2. Check that the Master and all Technical Quotes have QuoteType__c = 'Quote' and Status = 'Draft'", () ->
                multiProductQuoteTypeAndStatusSteps.checkQuoteTypeAndStatus(QUOTE_QUOTE_TYPE, DRAFT_QUOTE_STATUS,
                        masterQuoteId, allSelectedServices)
        );

        step("3. Open the Add Products tab and add necessary products " +
                "and open the Quote Details tab, populate necessary fields, and save changes", () -> {
            //  to remove error message 'Choose exactly 1 license from Call recording storage'
            steps.quoteWizard.addProductsOnProductsTab(callRecordingStorage);

            quotePage.openTab();
            quotePage.setDefaultStartDate();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.discountJustificationTextArea.setValue(TEST_STRING);
            quotePage.saveChanges();
        });

        //  to remove error message 'CC ProServ quote is required for Contact Center service'
        step("4. Click 'Initiate CC ProServ' button, click 'Submit' button in popup window, " +
                "check that CC ProServ is initiated, and set СС ProServ Quote.ProServ_Status__c = 'Sold' via API" +
                "and reload the Quote Wizard", () -> {
            quotePage.initiateCcProServ();
            quotePage.waitUntilLoaded();

            var ccProServQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ProServ_Status__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + opportunityId + "' " +
                            "AND RecordType.Name = '" + CC_PROSERV_QUOTE_RECORD_TYPE + "'",
                    Quote.class);
            assertThat(ccProServQuote.getProServ_Status__c())
                    .as("СС ProServ Quote.ProServ_Status__c value")
                    .isEqualTo(CREATED_PROSERV_STATUS);

            ccProServQuote.setProServ_Status__c(SOLD_PROSERV_STATUS);
            enterpriseConnectionUtils.update(ccProServQuote);

            refresh();
            wizardPage.waitUntilLoaded();
            packagePage.packageSelector.getSelectedPackage().getSelf().shouldBe(visible, ofSeconds(10));
        });

        step("5. Open the Quote Details tab, set Quote Type to 'Agreement' and Status to 'Active' and save changes", () -> {
            quotePage.openTab();
            quotePage.stagePicklist.selectOption(AGREEMENT_QUOTE_TYPE);
            quotePage.agreementStatusPicklist.selectOption(ACTIVE_QUOTE_STATUS);

            quotePage.saveChanges();
        });

        step("6. Check that the Master and all Technical Quotes have QuoteType__c = 'Agreement' and Status = 'Active'", () ->
                multiProductQuoteTypeAndStatusSteps.checkQuoteTypeAndStatus(AGREEMENT_QUOTE_TYPE, ACTIVE_QUOTE_STATUS,
                        masterQuoteId, allSelectedServices)
        );
    }
}