package ngbs.quotingwizard.newbusiness.quotetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.AGREEMENT_QUOTE_TYPE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("QuoteTab")
public class QuoteIntendedPaymentMethodTest extends BaseTest {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final Steps steps;

    public QuoteIntendedPaymentMethodTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_1PhoneAnd1DL_RegularAndPOC.json",
                Dataset.class);

        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        steps = new Steps(data);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-22983")
    @TmsLink("CRM-22984")
    @TmsLink("CRM-22985")
    @DisplayName("CRM-22983 - PaymentMethod__c field is displayed on the Quote Details tab. \n" +
            "CRM-22984 - PaymentMethod__c field is filled correctly. New Business. Quote Stage. \n" +
            "CRM-22985 - PaymentMethod__c field is filled correctly. New Business. Agreement Stage")
    @Description("CRM-22983 - Verify that PaymentMethod__c field is displayed on the Quote Details Tab " +
            "if 'Select Packages by Business Identity' feature toggle set as true . \n" +
            "CRM-22984 - Verify that PaymentMethod__c field is filled correctly if Quote Stage = 'Quote'. \n" +
            "CRM-22985 - Verify that PaymentMethod__c field is filled correctly if Quote Stage = 'Agreement'")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select package for it, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
        });

        //  CRM-22983
        step("2. Open the Quote Details tab, set Main Area Code and Start Date, save changes, " +
                "and check that Payment Method picklist is displayed and has preselected value 'None'", () -> {
            quotePage.openTab();

            //  Set Main Area Code here to change Stage to 'Agreement' later
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();

            quotePage.paymentMethodPicklist
                    .shouldBe(visible)
                    .getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(QuotePage.NONE_PAYMENT_METHOD));
        });

        //  CRM-22983
        step("3. Check that Quote.PaymentMethod__c = null", () ->
                stepCheckPaymentMethodOnQuote(null)
        );

        //  CRM-22984
        step("4. Select 'Credit Card' payment method, save changes, " +
                "and check that Quote.PaymentMethod__c = 'CreditCard'", () -> {
            quotePage.selectPaymentMethod(QuotePage.CREDIT_CARD_PAYMENT_METHOD);
            quotePage.saveChanges();

            stepCheckPaymentMethodOnQuote(QuoteHelper.CREDITCARD_PAYMENT_METHOD);
        });

        //  CRM-22984
        step("5. Change Payment Method to 'Invoice', save changes, " +
                "and check that Quote.PaymentMethod__c = 'Invoice'", () -> {
            quotePage.selectPaymentMethod(QuotePage.INVOICE_PAYMENT_METHOD);
            quotePage.saveChanges();

            stepCheckPaymentMethodOnQuote(QuoteHelper.INVOICE_PAYMENT_METHOD);
        });

        //  CRM-22985
        step("6. Change Stage to 'Agreement', " +
                "check that Payment Method picklist is visible and disabled and still has value = 'Invoice', " +
                "save changes, and check that Quote.PaymentMethod__c = 'Invoice'", () -> {
            quotePage.stagePicklist.selectOption(AGREEMENT_QUOTE_TYPE);

            quotePage.paymentMethodPicklist
                    .shouldBe(visible, disabled)
                    .getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(QuotePage.INVOICE_PAYMENT_METHOD));

            quotePage.saveChanges();

            stepCheckPaymentMethodOnQuote(QuoteHelper.INVOICE_PAYMENT_METHOD);
        });
    }

    /**
     * Check that Quote.PaymentMethod__c field has the expected value for the current quote.
     *
     * @param expectedPaymentMethod expected value of Quote.PaymentMethod__c field
     */
    private void stepCheckPaymentMethodOnQuote(String expectedPaymentMethod) {
        step("Check that Quote.PaymentMethod__c = " + expectedPaymentMethod, () -> {
            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, PaymentMethod__c " +
                            "FROM Quote " +
                            "WHERE OpportunityId = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Quote.class);

            assertThat(quote.getPaymentMethod__c())
                    .as("Quote.PaymentMethod__c value")
                    .isEqualTo(expectedPaymentMethod);
        });
    }
}
