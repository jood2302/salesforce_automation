package ngbs.quotingwizard.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.Product2Helper.RECURRING_BILLING_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.hidden;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Multiproduct-Lite")
public class FullMrsCalculationMultiProductTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUserWithPermissionSet;
    private String masterQuoteId;

    //  Test data
    private final Package engageDigitalPackage;
    private final Package rcCcPackage;
    private final String edPackageFolderName;
    private final String rcCcFolderName;

    private final String rcCcServiceName;
    private final String edServiceName;
    private final String mvpServiceName;

    private final int numberOfMonthsInYear;
    private final int rcCcNumberOfFreeMonths;
    private final int edNumberOfFreeMonths;
    private final int mvpNumberOfFreeMonths;

    private final String rcCcSpecialTerms;
    private final String engageDigitalSpecialTerms;
    private final String mvpSpecialTerms;

    public FullMrsCalculationMultiProductTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Annual_Contract_196116013_ED_Standalone_CC_EV_NB.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        engageDigitalPackage = data.packageFolders[1].packages[0];
        rcCcPackage = data.packageFolders[2].packages[0];

        edPackageFolderName = data.packageFolders[1].name;
        rcCcFolderName = data.packageFolders[2].name;

        rcCcServiceName = rcCcFolderName;
        edServiceName = edPackageFolderName;
        mvpServiceName = data.packageFolders[0].name;

        numberOfMonthsInYear = 12;
        rcCcNumberOfFreeMonths = 1;
        edNumberOfFreeMonths = 2;
        mvpNumberOfFreeMonths = 3;

        rcCcSpecialTerms = "1 Free Month of Service";
        engageDigitalSpecialTerms = "2 Free Months of Service";
        mvpSpecialTerms = "3 Free Months of Service";
    }

    @BeforeEach
    public void setUpTest() {
        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.stepCreateContractInNGBS();
        }

        step("Find a user with 'Sales Rep - Lightning' profile " +
                "and 'Allow to user full MRS for FSC Calculation for Existing Customers' permission set", () -> {
            salesRepUserWithPermissionSet = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withPermissionSet(ALLOW_FULL_MRS_FOR_FSC_CALCULATION_FOR_EXISTING_CUSTOMERS_PS)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithPermissionSet);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithPermissionSet);

        step("Login as a user with 'Sales Rep - Lightning' profile " +
                "and 'Allow to user full MRS for FSC Calculation for Existing Customers' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesRepUserWithPermissionSet);
        });
    }

    @Test
    @TmsLink("CRM-30271")
    @DisplayName("CRM-30271 - FSC Full MRS value calculation for one service is based on Total Monthly Recurring Charges " +
            "for that particular service during Change Order")
    @Description("Verify that FSC value calculation for one service is based on Total Monthly Recurring Charges " +
            "for that particular service during Change Order")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select ED and RC CC packages for it, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            packagePage.packageSelector.selectPackageWithoutSeatsSetting(data.chargeTerm, edPackageFolderName, engageDigitalPackage);
            packagePage.packageSelector.selectPackageWithoutSeatsSetting(data.chargeTerm, rcCcFolderName, rcCcPackage);
            packagePage.saveChanges();
        });

        step("2. Open the Price tab, add taxes to the Cart and save changes", () -> {
            cartPage.openTab();
            cartPage.addTaxes();
            cartPage.saveChanges();

            masterQuoteId = wizardPage.getSelectedQuoteId();
        });

        step("3. Open the Billing Details and Terms modal window, switch 'Full MRS' toggle on, " +
                "select different options in Special Terms picklist for all services, click 'Apply' button, " +
                "save changes on the Price tab, and check that Quote.Credit_Amount__c value for the Master Quote " +
                "and for every Technical Quote have expected values", () -> {
            cartPage.footer.billingDetailsAndTermsButton.click();
            cartPage.billingDetailsAndTermsModal.fullMrsToggle.click();

            cartPage.billingDetailsAndTermsModal.specialTermsCcPicklist.selectOption(rcCcSpecialTerms);
            cartPage.billingDetailsAndTermsModal.specialTermsEdPicklist.selectOption(engageDigitalSpecialTerms);
            cartPage.billingDetailsAndTermsModal.specialTermsMvpPicklist.selectOption(mvpSpecialTerms);
            cartPage.billingDetailsAndTermsModal.placeholderLoading.shouldBe(hidden, ofSeconds(10));

            cartPage.applyChangesInBillingDetailsAndTermsModal();
            cartPage.saveChanges();

            var rcCcCreditAmountExpected = checkTechnicalQuoteExpectedCreditAmountValue(rcCcServiceName, rcCcNumberOfFreeMonths);
            var edCreditAmountExpected = checkTechnicalQuoteExpectedCreditAmountValue(edServiceName, edNumberOfFreeMonths);
            var mvpCreditAmountExpected = checkTechnicalQuoteExpectedCreditAmountValue(mvpServiceName, mvpNumberOfFreeMonths);

            step("Check Master Quote.Credit_Amount__c value", () -> {
                var masterQuoteCreditAmountExpected = rcCcCreditAmountExpected + edCreditAmountExpected + mvpCreditAmountExpected;

                var masterQuote = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, Credit_Amount__c " +
                                "FROM Quote " +
                                "WHERE Id = '" + masterQuoteId + "'",
                        Quote.class);
                assertThat(masterQuote.getCredit_Amount__c())
                        .as("Master Quote.Credit_Amount__c value")
                        .isEqualTo(masterQuoteCreditAmountExpected);
            });
        });
    }

    /**
     * Calculate the expected Free Service Credit amount for the given service name and number of free months
     * and check that it's equal to the actual Quote.Credit_Amount__c field value for the provided Technical Quote.
     *
     * @param serviceName        name of the service of the Technical Quote to check FSC value for
     * @param numberOfFreeMonths number of free months for the provided service
     */
    private Double checkTechnicalQuoteExpectedCreditAmountValue(String serviceName, int numberOfFreeMonths) {
        return step("Check Tech Quote.Credit_Amount__c value for " + serviceName, () -> {
            var expectedCreditAmountValue = enterpriseConnectionUtils.query(
                            "SELECT Id, TotalPrice " +
                                    "FROM QuoteLineItem " +
                                    "WHERE QuoteId = '" + masterQuoteId + "' " +
                                    "AND ServiceName__c = '" + serviceName + "' " +
                                    "AND Product2.Billing_Type__c = '" + RECURRING_BILLING_TYPE + "'",
                            QuoteLineItem.class)
                    .stream()
                    .mapToDouble(QuoteLineItem::getTotalPrice)
                    .sum() * numberOfFreeMonths / numberOfMonthsInYear;

            var actualTechnicalQuoteCreditAmountValue = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, Credit_Amount__c " +
                                    "FROM Quote " +
                                    "WHERE MasterQuote__c = '" + masterQuoteId + "' " +
                                    "AND ServiceName__c = '" + serviceName + "'",
                            Quote.class)
                    .getCredit_Amount__c();

            assertThat(actualTechnicalQuoteCreditAmountValue)
                    .as("Technical Quote.Credit_Amount__c value for " + serviceName)
                    .isEqualTo(expectedCreditAmountValue);

            return expectedCreditAmountValue;
        });
    }
}
