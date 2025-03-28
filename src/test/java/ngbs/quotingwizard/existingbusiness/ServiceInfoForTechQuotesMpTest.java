package ngbs.quotingwizard.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.components.packageselector.PackageSelector.PACKAGE_FROM_ACCOUNT_BADGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.CREDIT_CARD_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.PAID_RC_ACCOUNT_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.getTechQuote;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Selenide.$$;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("Account")
@Tag("Opportunity")
@Tag("Quote")
@Tag("MultiProduct-UB")
@Tag("LTR-569")
public class ServiceInfoForTechQuotesMpTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUser;
    private String opportunityId;
    private String masterQuoteId;

    //  Test data
    private final String officeServiceName;
    private final Package officePackage;
    private final String rcCcServiceName;
    private final Package rcCcPackage;
    private final String engageDigitalServiceName;
    private final Package engageDigitalPackage;

    private final List<String> allSelectedServicesBeforeUpgrade;
    private final List<String> allSelectedServicesOnUpgrade;

    private final Product polycomPhone;
    private final Product dlUnlimited;
    private final Product rcCcProduct;
    private final Product engageDigitalProduct;

    public ServiceInfoForTechQuotesMpTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_RCCC_Monthly_Contract_1192790005_ED_Standalone.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        steps.ngbs.isGenerateAccountsForSingleTest = true;

        officeServiceName = data.packageFolders[0].name;
        officePackage = data.packageFolders[0].packages[0];
        rcCcServiceName = data.packageFolders[1].name;
        rcCcPackage = data.packageFolders[1].packages[0];
        engageDigitalServiceName = data.packageFoldersUpgrade[0].name;
        engageDigitalPackage = data.packageFoldersUpgrade[0].packages[0];

        allSelectedServicesBeforeUpgrade = List.of(officeServiceName, rcCcServiceName);
        allSelectedServicesOnUpgrade = List.of(officeServiceName, rcCcServiceName, engageDigitalServiceName);

        polycomPhone = data.getProductByDataName("LC_HD_687");
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        rcCcProduct = data.getProductByDataName("CC_RCCCGD_576", rcCcPackage);
        engageDigitalProduct = data.getProductByDataName("SA_LINECRWHATSUP_11", engageDigitalPackage);
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateMultiProductUnifiedBillingAccount();
        }

        step("Find a user with 'Sales Rep - Lightning' profile and 'Enable Multi-Product Unified Billing' Feature Toggle", () -> {
            salesRepUser = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withFeatureToggles(List.of(ENABLE_MULTIPRODUCT_UNIFIED_BILLING_FT))
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        opportunityId = steps.quoteWizard.opportunity.getId();

        step("Create new Billing Account Package objects (Package__c) for the Account " +
                "for the Office and RC CC NGBS packages via SFDC API", () -> {
            createBillingAccountPackage(steps.salesFlow.account.getId(), data.packageId, officePackage.id,
                    data.brandName, officeServiceName, CREDIT_CARD_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);

            createBillingAccountPackage(steps.salesFlow.account.getId(), rcCcPackage.ngbsPackageId, rcCcPackage.id,
                    data.brandName, rcCcServiceName, CREDIT_CARD_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);
        });

        step("Log in as a user with 'Sales Rep - Lightning' profile and 'Enable Multi-Product Unified Billing' Feature Toggle", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
        });
    }

    @Test
    @TmsLink("CRM-36668")
    @TmsLink("CRM-36666")
    @TmsLink("CRM-37235")
    @TmsLink("CRM-36664")
    @DisplayName("CRM-36668 - UB. Records on 'Service Info' object are created for all Technical Quotes " +
            "while creating MP Quote by user with enabled FT 'Enable Multi-Product Unified Billing'. Existing Business.\n" +
            "CRM-36666 - UB. Technical Accounts and Opportunities are not created while creating Quote " +
            "by user with enabled FT 'Enable Multi-Product Unified Billing'. Existing Business.\n" +
            "CRM-37235 - All MP UB Quotes (Master and Technical) are created under same Opportunity. (Existing Business).\n" +
            "CRM-36664 - Only Master Quote is Primary for UB MP Quotes. Existing Business")
    @Description("CRM-36668 - Verify that Records on 'Service Info' object are created for all Technical Quotes " +
            "while creating Quote by user with enabled FT 'Enable Multi-Product Unified Billing'. Existing Business.\n" +
            "CRM-36666 - Verify that technical Accounts and Opportunities are not created " +
            "while creating Multi-Product Quote For Existing Business Account by user with enabled FT 'Enable Multi-Product Unified Billing'.\n" +
            "CRM-37235 - Verify that all MP UB Quotes (Master and Technical) are created under same Opportunity.\n" +
            "CRM-36664 - Verify that only Master Quote has isPrimary__c = 'true' and all Technical Quotes " +
            "have isPrimary__c = 'false' for UB MP Quotes")
    public void test() {
        //  CRM-36666
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "and check that packages from the Account are selected", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(opportunityId);

            var packagesFromAccountNames = List.of(officePackage.getFullName(), rcCcPackage.getFullName());

            var actualPackageNames = packagePage.packageSelector.getAllSelectedPackages()
                    .stream()
                    .map(pkg -> pkg.getName())
                    .toList();
            $$(actualPackageNames).shouldHave(exactTextsCaseSensitiveInAnyOrder(packagesFromAccountNames));

            packagePage.packageSelector.getAllSelectedPackages().forEach(selectedPackage ->
                    selectedPackage.getBadge().shouldHave(exactTextCaseSensitive(PACKAGE_FROM_ACCOUNT_BADGE))
            );
        });

        step("2. Open the Add Products tab and add some products to the Cart", () ->
                steps.quoteWizard.addProductsOnProductsTab(polycomPhone, rcCcProduct)
        );

        step("3. Open the Price tab, increase quantity of the DL Unlimited, " +
                "set quantity of the added Phone, assign it to the DL and save changes", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(dlUnlimited, polycomPhone);
            steps.cartTab.assignDevicesToDL(polycomPhone.name, dlUnlimited.name, steps.quoteWizard.localAreaCode,
                    polycomPhone.quantity);

            cartPage.saveChanges();
            masterQuoteId = wizardPage.getSelectedQuoteId();
        });

        //  CRM-36666
        step("4. Check that there are no Technical Accounts and Opportunities are created " +
                "and Master Quote and 2 Technical Quotes are created", () -> {
            var techAccounts = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Account " +
                            "WHERE Master_Account__c = '" + steps.salesFlow.account.getId() + "'",
                    Account.class);
            assertThat(techAccounts.size())
                    .as("Number of Technical Accounts")
                    .isEqualTo(0);

            var techOpportunities = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Opportunity " +
                            "WHERE MasterOpportunity__c = '" + opportunityId + "'",
                    Opportunity.class);
            assertThat(techOpportunities.size())
                    .as("Number of Technical Opportunities")
                    .isEqualTo(0);

            var techQuotes = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Quote " +
                            "WHERE MasterQuote__c = '" + masterQuoteId + "'",
                    Quote.class);
            assertThat(techQuotes.size())
                    .as("Number of Technical Quotes")
                    .isEqualTo(allSelectedServicesBeforeUpgrade.size());
        });

        //  CRM-37235
        step("5. Check that the Master Quote, Office and RC CC Technical Quotes are linked to the same test Opportunity", () -> {
            var masterQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, OpportunityId " +
                            "FROM Quote " +
                            "WHERE Id = '" + masterQuoteId + "'",
                    Quote.class);
            assertThat(masterQuote.getOpportunityId())
                    .as("Master Quote.OpportunityId value")
                    .isEqualTo(opportunityId);

            var officeTechQuote = getTechQuote(masterQuote.getId(), officeServiceName);
            assertThat(officeTechQuote.getOpportunityId())
                    .as("Tech office Quote.OpportunityId value")
                    .isEqualTo(opportunityId);

            var rcCcTechQuote = getTechQuote(masterQuote.getId(), rcCcServiceName);
            assertThat(rcCcTechQuote.getOpportunityId())
                    .as("Tech RC CC Quote.OpportunityId value")
                    .isEqualTo(opportunityId);
        });

        step("6. Open the Select Package tab, select Engage Digital package, " +
                "open the Add Products tab, add Engage Digital license to the Cart, open the Price tab and save changes", () -> {
            packagePage.openTab();
            packagePage.packageSelector.selectPackageWithoutSeatsSetting(data.chargeTerm, engageDigitalServiceName, engageDigitalPackage);

            steps.quoteWizard.addProductsOnProductsTab(engageDigitalProduct);
            cartPage.openTab();
            cartPage.saveChanges();
        });

        //  CRM-36668
        step("7. Check that ServiceInfo__c record is not created for Master Quote and records are created only for Technical Quotes, " +
                "and that Quote__r.IsMultiProductTechnicalQuote__c = true for all ServiceInfo__c records", () -> {
            step("Check the there's no ServiceInfo__c record for the Master Quote", () -> {
                var serviceInfoForMasterQuote = enterpriseConnectionUtils.query(
                        "SELECT Id " +
                                "FROM ServiceInfo__c " +
                                "WHERE Quote__r.Id = '" + masterQuoteId + "'",
                        ServiceInfo__c.class);
                assertThat(serviceInfoForMasterQuote.size())
                        .as("Number of ServiceInfo__c records for the Master Quote")
                        .isEqualTo(0);
            });

            step("Check that ServiceInfo__c record is created for the each Tech Quote, " +
                    "and all corresponding Tech Quote.IsMultiProductTechnicalQuote__c = true", () -> {
                var serviceInfoForTechQuotes = enterpriseConnectionUtils.query(
                        "SELECT Id, Quote__r.IsMultiProductTechnicalQuote__c, Quote__r.ServiceName__c " +
                                "FROM ServiceInfo__c " +
                                "WHERE Quote__r.MasterQuote__c = '" + masterQuoteId + "'",
                        ServiceInfo__c.class);
                assertThat(serviceInfoForTechQuotes.size())
                        .as("Number of ServiceInfo__c records for the Tech Quotes")
                        .isEqualTo(allSelectedServicesOnUpgrade.size());

                //  this way we can check that the ServiceInfo__c records are created for all Tech Quotes
                var serviceNamesOnTechQuotes = serviceInfoForTechQuotes.stream()
                        .map(serviceInfo -> serviceInfo.getQuote__r().getServiceName__c())
                        .toList();
                assertThat(serviceNamesOnTechQuotes)
                        .as("ServiceInfo__c.Quote__r.ServiceName__c values (should contain all selected services)")
                        .containsExactlyInAnyOrderElementsOf(allSelectedServicesOnUpgrade);

                var isMultiProductTechnicalQuoteValuesOnTechQuotes = serviceInfoForTechQuotes.stream()
                        .map(serviceInfo -> serviceInfo.getQuote__r().getIsMultiProductTechnicalQuote__c())
                        .toList();
                assertThat(isMultiProductTechnicalQuoteValuesOnTechQuotes)
                        .as("ServiceInfo__c.Quote__r.IsMultiProductTechnicalQuote__c values (should be true for all records)")
                        .containsOnly(true);
            });
        });

        //  CRM-36666
        step("8. Check that there are no Technical Accounts and Opportunities are created", () -> {
            var techAccounts = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Account " +
                            "WHERE Master_Account__c = '" + steps.salesFlow.account.getId() + "'",
                    Account.class);
            assertThat(techAccounts.size())
                    .as("Number of Technical Accounts")
                    .isEqualTo(0);

            var techOpportunities = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Opportunity " +
                            "WHERE MasterOpportunity__c = '" + opportunityId + "'",
                    Opportunity.class);
            assertThat(techOpportunities.size())
                    .as("Number of Technical Opportunities")
                    .isEqualTo(0);
        });

        step("9. Check the OpportunityId and isPrimary__c values " +
                "for the Master Quote and the Office, ED, and RC CC Technical Quotes", () -> {
            var masterQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, OpportunityId, isPrimary__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + masterQuoteId + "'",
                    Quote.class);
            var officeTechQuote = getTechQuote(masterQuoteId, officeServiceName);
            var rcCcTechQuote = getTechQuote(masterQuoteId, rcCcServiceName);
            var edTechQuote = getTechQuote(masterQuoteId, engageDigitalServiceName);

            //  CRM-37235
            step("Check that the Master Quote and the Office, ED, and RC CC Technical Quotes " +
                    "are linked to the same test Opportunity", () -> {
                assertThat(masterQuote.getOpportunityId())
                        .as("Master Quote.OpportunityId value")
                        .isEqualTo(opportunityId);
                assertThat(officeTechQuote.getOpportunityId())
                        .as("Tech office Quote.OpportunityId value")
                        .isEqualTo(opportunityId);
                assertThat(rcCcTechQuote.getOpportunityId())
                        .as("Tech RC CC Quote.OpportunityId value")
                        .isEqualTo(opportunityId);
                assertThat(edTechQuote.getOpportunityId())
                        .as("Tech ED Quote.OpportunityId value")
                        .isEqualTo(opportunityId);
            });

            //  CRM-36664
            step("Check that Master Quote has isPrimary__c = 'true' " +
                    "and the Office, ED, and RC CC Technical Quotes have isPrimary__c = 'false'", () -> {
                assertThat(masterQuote.getIsPrimary__c())
                        .as("Master Quote.isPrimary__c value")
                        .isTrue();
                assertThat(officeTechQuote.getIsPrimary__c())
                        .as("Tech office Quote.isPrimary__c value")
                        .isFalse();
                assertThat(rcCcTechQuote.getIsPrimary__c())
                        .as("Tech RC CC Quote.isPrimary__c value")
                        .isFalse();
                assertThat(edTechQuote.getIsPrimary__c())
                        .as("Tech ED Quote.isPrimary__c value")
                        .isFalse();
            });
        });
    }
}
