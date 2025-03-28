package ngbs.quotingwizard.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.MultiProductQuoteTypeAndStatusSteps;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Set;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.CREDIT_CARD_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.PAID_RC_ACCOUNT_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("LTR-569")
@Tag("Quote")
@Tag("MultiProduct-UB")
public class MultiProductQuoteTypeAndStatusChangedTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final MultiProductQuoteTypeAndStatusSteps multiProductQuoteTypeAndStatusSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User dealDeskUserWithEnabledMpUbFT;
    private Quote additionalActiveSalesAgreement;
    private String masterQuoteId;

    //  Test data
    private final String officeServiceName;
    private final Package officePackage;
    private final String rcCcServiceName;
    private final Package rcCcPackage;
    private final String engageDigitalServiceName;
    private final Package engageDigitalPackage;
    private final Set<String> allSelectedServices;

    public MultiProductQuoteTypeAndStatusChangedTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_RCCC_Monthly_Contract_1192790005_ED_Standalone.json",
                Dataset.class);

        steps = new Steps(data);
        multiProductQuoteTypeAndStatusSteps = new MultiProductQuoteTypeAndStatusSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        steps.ngbs.isGenerateAccountsForSingleTest = true;

        officeServiceName = data.packageFolders[0].name;
        officePackage = data.packageFolders[0].packages[0];
        rcCcServiceName = data.packageFolders[1].name;
        rcCcPackage = data.packageFolders[1].packages[0];
        engageDigitalServiceName = data.packageFoldersUpgrade[0].name;
        engageDigitalPackage = data.packageFoldersUpgrade[0].packages[0];
        allSelectedServices = Set.of(officeServiceName, rcCcServiceName, engageDigitalServiceName);
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateMultiProductUnifiedBillingAccount();
        }

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

        additionalActiveSalesAgreement = steps.syncWithNgbs.stepCreateAdditionalActiveSalesAgreement(steps.salesFlow.account,
                steps.salesFlow.contact, dealDeskUserWithEnabledMpUbFT);

        step("Create new Billing Account Package objects (Package__c) for the Account " +
                "for the Office and RC CC NGBS packages via SFDC API", () -> {
            createBillingAccountPackage(steps.salesFlow.account.getId(), data.packageId, officePackage.id,
                    data.brandName, officeServiceName, CREDIT_CARD_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);

            createBillingAccountPackage(steps.salesFlow.account.getId(), rcCcPackage.ngbsPackageId, rcCcPackage.id,
                    data.brandName, rcCcServiceName, CREDIT_CARD_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);
        });

        step("Log in as a user with 'Deal Desk Lightning' profile, and 'Edit_Status_on_Quote' permission set, " +
                "and 'Enable Multi-Product Unified Billing' Feature Toggle", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUserWithEnabledMpUbFT)
        );
    }

    @Test
    @TmsLink("CRM-37250")
    @DisplayName("CRM-37250 - UB. QuoteType__c and Status fields are changed for the Technical Quotes (Existing Business)")
    @Description("Verify that for Technical Quotes values of the following fields are changed so that Master and Technical quotes " +
            "would have same status and stage: Quote.QuoteType__c, Quote.Status__c")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, select Engage Digital package, " +
                "and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            packagePage.packageSelector.selectPackageWithoutSeatsSetting(data.chargeTerm, engageDigitalServiceName, engageDigitalPackage);

            packagePage.saveChanges();
            masterQuoteId = wizardPage.getSelectedQuoteId();
        });

        step("2. Check that the Master and all Technical Quotes have QuoteType__c = 'Quote' and Status = 'Draft'", () ->
                multiProductQuoteTypeAndStatusSteps.checkQuoteTypeAndStatus(QUOTE_QUOTE_TYPE, DRAFT_QUOTE_STATUS,
                        masterQuoteId, allSelectedServices)
        );

        step("3. Set Status = 'Executed' for the existing Active Sales Agreement via API", () -> {
            additionalActiveSalesAgreement.setStatus(EXECUTED_QUOTE_STATUS);
            enterpriseConnectionUtils.update(additionalActiveSalesAgreement);
        });

        step("4. Open the Quote Details tab, set Quote Type to 'Agreement' and Status to 'Active' and save changes", () -> {
            quotePage.openTab();
            quotePage.stagePicklist.selectOption(AGREEMENT_QUOTE_TYPE);
            quotePage.agreementStatusPicklist.selectOption(ACTIVE_QUOTE_STATUS);

            quotePage.saveChanges();
        });

        step("5. Check that the Master and all Technical Quotes have QuoteType__c = 'Agreement' and Status = 'Active'", () ->
                multiProductQuoteTypeAndStatusSteps.checkQuoteTypeAndStatus(AGREEMENT_QUOTE_TYPE, ACTIVE_QUOTE_STATUS,
                        masterQuoteId, allSelectedServices)
        );
    }
}
