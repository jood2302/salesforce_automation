package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static base.Pages.wizardPage;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.matchText;
import static io.qameta.allure.Allure.step;
import static java.math.BigDecimal.valueOf;
import static java.math.RoundingMode.FLOOR;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Multiproduct-Lite")
public class FreeServiceCreditCalculationPaymentPlanChangeTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String fscAmountCalculated;

    //  Test data
    private final String monthlyChargeTerm;
    private final String specialTermsInitialValue;
    private final String specialTermsNewValue;
    private final String fscValueRegexp;
    private final int scale;

    public FreeServiceCreditCalculationPaymentPlanChangeTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_NoPhones.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        monthlyChargeTerm = "Monthly";
        specialTermsInitialValue = "3 Free Months of Service";
        specialTermsNewValue = "4 Free Months of Service";

        //  e.g. "$1.99", "$11", "$4.5", etc.
        fscValueRegexp = "^\\$\\d+(\\.\\d{1,2})?$";
        scale = 2;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-29671")
    @DisplayName("CRM-29671 - FSC is calculated correctly if Payment Plan was changed on Billing Details and Terms modal")
    @Description("Verify that FSC calculated correctly if the Payment Plan has been changed " +
            "and saved on Billing Details and Terms modal window")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select a package for it and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Price tab, open the Billing Details and Terms modal window, " +
                "select '3 Free Months of Service' in Number of Months field and click 'Apply' button", () -> {
            cartPage.openTab();
            cartPage.footer.billingDetailsAndTermsButton.click();

            cartPage.billingDetailsAndTermsModal.specialTermsPicklist.selectOption(specialTermsInitialValue);
            cartPage.applyChangesInBillingDetailsAndTermsModal();

            cartPage.footer.freeServiceCreditAmount.should(matchText(fscValueRegexp));
            fscAmountCalculated = cartPage.footer.freeServiceCreditAmount.getText();
        });

        step("3. Open Billing Details and Terms modal window again, change Payment Plan to 'Monthly', save changes " +
                "and check that Free Service Credit value in the Footer is different to value before Payment Plan was changed", () -> {
            cartPage.footer.billingDetailsAndTermsButton.click();
            cartPage.billingDetailsAndTermsModal.selectChargeTerm(monthlyChargeTerm);
            cartPage.applyChangesInBillingDetailsAndTermsModal();

            cartPage.footer.freeServiceCreditAmount.shouldNotHave(exactTextCaseSensitive(fscAmountCalculated));
        });

        step("4. Open Billing Details and Terms modal window, change value in the 'Number of Months' picklist, save changes, " +
                "and check that Free Service Credit is recalculated by annual formula " +
                "(Quote.Free_Service_Credit_Total__c = Quote.Credit_Amount__c + Quote.Free_Service_Taxes__c)", () -> {
            cartPage.footer.billingDetailsAndTermsButton.click();
            cartPage.billingDetailsAndTermsModal.specialTermsPicklist.selectOption(specialTermsNewValue);

            cartPage.applyChangesInBillingDetailsAndTermsModal();
            cartPage.saveChanges();

            var quote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Free_Service_Credit_Total__c, Credit_Amount__c, Free_Service_Taxes__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + wizardPage.getSelectedQuoteId() + "'",
                    Quote.class);
            var fscAnnualExpectedValue = valueOf(quote.getCredit_Amount__c())
                    .add(valueOf(quote.getFree_Service_Taxes__c()))
                    .setScale(scale, FLOOR)
                    .doubleValue();

            assertThat(quote.getFree_Service_Credit_Total__c())
                    .as("Quote.Free_Service_Credit_Total__c value")
                    .isEqualTo(fscAnnualExpectedValue);
        });
    }
}
