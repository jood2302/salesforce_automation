package ngbs.quotingwizard.newbusiness.quotetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.time.Period;

import static base.Pages.packagePage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.LINK_OFFICE_ACCOUNT_TO_SET_UP_CONTRACT_TERMS_MESSAGE;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.START_AND_END_DATE_INPUT_FORMATTER;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.StringHelper.INR_CURRENCY_ISO_CODE;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getAccountInNGBS;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getContractsInNGBS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewCustomerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory.createOpportunity;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.QuoteFactory.createActiveSalesAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.RC_EU_BRAND_NAME;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.RC_INDIA_MUMBAI_BUSINESS_IDENTITY_ID;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;
import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("IndiaMVP")
public class RcIndiaAccountLinkingTest extends BaseTest {
    private final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final Steps steps;

    private Account officeAccount;
    private Opportunity officeOpportunity;
    private Account rcIndiaAccount;
    private Opportunity rcIndiaOpportunity;

    //  Test data
    private final String officeInitialTerm;
    private final String officeService;
    private final Package officePackage;
    private final Package indiaPackage;
    private final AreaCode indiaAreaCode;

    public RcIndiaAccountLinkingTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_Contract_163077013_RC_India_NB.json",
                Dataset.class);

        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        steps = new Steps(data);

        officeInitialTerm = data.packageFolders[0].packages[0].contractTerms.initialTerm[0];
        officeService = data.packageFolders[0].name;
        officePackage = data.packageFolders[0].packages[0];
        indiaPackage = data.packageFolders[0].packages[1];

        indiaAreaCode = new AreaCode("Local", "India", "Maharashtra", EMPTY_STRING, "22");
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.stepCreateContractInNGBS();
        }

        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.salesFlow.createAccountWithContactAndContactRole(dealDeskUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, dealDeskUser);
        officeAccount = steps.salesFlow.account;
        officeOpportunity = steps.quoteWizard.opportunity;

        step("Set Service_Type__c and RC_Service_name__c = 'Office' on the Office account via API", () -> {
            officeAccount.setService_Type__c(officeService);
            officeAccount.setRC_Service_name__c(officeService);
            enterpriseConnectionUtils.update(officeAccount);
        });

        step("Create a new Billing Account Package object (Package__c) for the Office Account via API", () -> {
            createBillingAccountPackage(officeAccount.getId(), officeAccount.getRC_User_ID__c(), officePackage.id,
                    data.getBrandName(), officeService, CREDIT_CARD_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);
        });

        step("Create an Active Sales Agreement for the Office Opportunity via API", () -> {
            createActiveSalesAgreement(officeOpportunity, officeInitialTerm);
        });

        step("Create New Business RC India Account with Service_Type__c and RC_Service_name__c = 'Office' " +
                "with related Contact and AccountContactRole via API", () -> {
            rcIndiaAccount = createNewCustomerAccountInSFDC(dealDeskUser,
                    new AccountData()
                            .withCurrencyIsoCode(INR_CURRENCY_ISO_CODE)
                            .withBillingCountry(INDIA_BILLING_COUNTRY)
                            .withRcBrand(RC_EU_BRAND_NAME));
            rcIndiaAccount.setService_Type__c(officeService);
            rcIndiaAccount.setRC_Service_name__c(officeService);

            enterpriseConnectionUtils.update(rcIndiaAccount);
        });

        step("Create New Business Indian Opportunity via API", () -> {
            var rcIndiaContact = getPrimaryContactOnAccount(rcIndiaAccount);
            rcIndiaOpportunity = createOpportunity(rcIndiaAccount, rcIndiaContact, true,
                    RC_EU_BRAND_NAME, RC_INDIA_MUMBAI_BUSINESS_IDENTITY_ID, dealDeskUser, INR_CURRENCY_ISO_CODE, officeService);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-23999")
    @DisplayName("CRM-23999 - Linking Indian Account to existing RingCentral Office Account")
    @Description("Verify that Indian Account can be linked to the Existing Business Office Account. \n" +
            "Verify that contract terms are taken from parent Account and cannot be changed (except Start Date)")
    public void test() {
        step("1. Open the Quote Wizard for the Indian Opportunity to add a new Sales quote, " +
                "select a package for it, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(rcIndiaOpportunity.getId());

            packagePage.packageSelector.selectPackage(data.chargeTerm, officeService, indiaPackage);
            packagePage.saveChanges();
        });

        step("2. Open the Quote Details tab and verify that Contract terms are hidden " +
                "and info message is displayed instead", () -> {
            quotePage.openTab();
            quotePage.startDateInput.shouldBe(hidden);
            quotePage.endDateInput.shouldBe(hidden);
            quotePage.initialTermPicklist.shouldBe(hidden);
            quotePage.renewalTermPicklist.shouldBe(hidden);
            quotePage.autoRenewalCheckbox.shouldBe(hidden);
            quotePage.contractTermsInfoPlaceholder
                    .shouldHave(exactTextCaseSensitive(LINK_OFFICE_ACCOUNT_TO_SET_UP_CONTRACT_TERMS_MESSAGE));
        });

        step("3. Set the Main Area Code, save changes, and link Office and India Accounts via Account Bindings Modal", () -> {
            quotePage.setMainAreaCode(indiaAreaCode);
            quotePage.saveChanges();

            quotePage.manageAccountBindingsButton.shouldBe(visible, ofSeconds(60)).click();
            quotePage.manageAccountBindings.infoIcon.shouldBe(hidden);

            quotePage.manageAccountBindings.accountSearchInput.selectItemInCombobox(officeAccount.getName());
            quotePage.manageAccountBindings.quoteSearchInput.getSelf().shouldBe(hidden);
            quotePage.manageAccountBindings.notifications.shouldHave(size(0));

            quotePage.submitAccountBindingChanges();
        });
        step("4. On the Quote Details tab, " +
                "verify that Contract terms are taken from Master account's Active Sales Agreement and NGBS", () -> {
            var expectedStartDate = START_AND_END_DATE_INPUT_FORMATTER.format(now());
            quotePage.startDateInput
                    .shouldBe(enabled)
                    .shouldHave(exactValue(expectedStartDate));

            var contractsOnAccount = getContractsInNGBS(data.billingId, data.packageId);
            var activeContract = contractsOnAccount.stream()
                    .filter(contract -> contract.isContractActive())
                    .findFirst().orElseThrow(() ->
                            new AssertionError("No active contracts found on the NGBS account!"));
            var officeNgbsInitialTerm = activeContract.term;
            var billingStartDate = getAccountInNGBS(data.billingId).getMainPackage().getBillingStartDateAsLocalDate();
            var billingEndDate = billingStartDate.plusMonths(officeNgbsInitialTerm);

            var expectedEndDate = START_AND_END_DATE_INPUT_FORMATTER.format(billingEndDate);
            quotePage.endDateInput
                    .shouldBe(disabled)
                    .shouldHave(exactValue(expectedEndDate));

            //  Initial terms value is displayed on the Quote Details tab: 
            //  it's calculated as a rounded up difference in months between the 'Start Date' and 'End Date' on the Quote
            var periodBetweenNowAndEndDate = Period.between(now(), billingEndDate);
            var expectedInitialTerm = periodBetweenNowAndEndDate.getMonths() + periodBetweenNowAndEndDate.getYears() * 12 +
                    (int) Math.ceil(periodBetweenNowAndEndDate.getDays() / 31.0);

            quotePage.initialTermPicklist.shouldBe(disabled)
                    .getSelectedOption()
                    .shouldHave(exactText(valueOf(expectedInitialTerm)));

            var officeQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Term_months__c, Auto_Renewal__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + officeOpportunity.getId() + "'",
                    Quote.class);

            assertThat(officeQuote.getTerm_months__c())
                    .as("Office Quote.Term_months__c value (Renewal Term)")
                    .isNotNull();
            var expectedRenewalTerm = officeQuote.getTerm_months__c();
            quotePage.renewalTermPicklist.shouldBe(disabled)
                    .getSelectedOption().shouldHave(exactText(expectedRenewalTerm));

            //  Make sure that 'Auto Renewal' checkbox = true (checked) on Office Active Sales Agreement
            assertThat(officeQuote.getAuto_Renewal__c())
                    .as("Office Quote.Auto_Renewal__c value")
                    .isTrue();
            quotePage.autoRenewalCheckbox.shouldBe(disabled, checked);
        });

        step("5. Verify that Master_Account__c on India Account is populated with Office Account Id", () -> {
            var rcIndiaAccountUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Master_Account__c " +
                            "FROM Account " +
                            "WHERE Id = '" + rcIndiaAccount.getId() + "'",
                    Account.class);
            assertThat(rcIndiaAccountUpdated.getMaster_Account__c())
                    .as("RC India Account.Master_Account__c value (should be equal to Office Account Id)")
                    .isEqualTo(officeAccount.getId());
        });

        step("6. Unlink Accounts via Account Manager modal, " +
                "and verify that Contract terms on the Quote Details tab are hidden and info message is displayed instead", () -> {
            quotePage.manageAccountBindingsButton.click();
            quotePage.manageAccountBindings.accountSearchInput.clear();
            quotePage.submitAccountBindingChanges();
            quotePage.waitUntilLoaded();

            quotePage.startDateInput.shouldBe(hidden);
            quotePage.endDateInput.shouldBe(hidden);
            quotePage.initialTermPicklist.shouldBe(hidden);
            quotePage.renewalTermPicklist.shouldBe(hidden);
            quotePage.autoRenewalCheckbox.shouldBe(hidden);
            quotePage.contractTermsInfoPlaceholder
                    .shouldHave(exactTextCaseSensitive(LINK_OFFICE_ACCOUNT_TO_SET_UP_CONTRACT_TERMS_MESSAGE));
        });

        step("7. Verify that Master_Account__c field on the Indian Account is empty/null", () -> {
            var rcIndiaAccountUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Master_Account__c " +
                            "FROM Account " +
                            "WHERE Id = '" + rcIndiaAccount.getId() + "'",
                    Account.class);
            assertThat(rcIndiaAccountUpdated.getMaster_Account__c())
                    .as("RC India Account.Master_Account__c value (should have null value)")
                    .isNull();
        });
    }
}
