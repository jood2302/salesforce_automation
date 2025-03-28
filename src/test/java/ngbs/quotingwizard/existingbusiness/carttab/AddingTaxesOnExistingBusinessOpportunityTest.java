package ngbs.quotingwizard.existingbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.Product2Helper.FAMILY_TAXES;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Tag("P1")
@Tag("Taxes")
public class AddingTaxesOnExistingBusinessOpportunityTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String monthlyChargeTerm;
    private final String annualChargeTerm;

    public AddingTaxesOnExistingBusinessOpportunityTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_NonContract_163074013.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        monthlyChargeTerm = "Annual";
        annualChargeTerm = "Monthly";
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateBillingAccount();

        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-20877")
    @DisplayName("CRM-20877 - Adding Taxes on Change Order Opp")
    @Description("Verify that when user clicks 'Add Taxes' on the Price Tab of the Quote Wizard, " +
            "the Taxes are added as the products on the tab and as a QLIs with the specified charge term")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select '" + annualChargeTerm + "' charge term on the Select Package tab, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            packagePage.packageSelector.packageFilter.selectChargeTerm(annualChargeTerm);
            packagePage.saveChanges();
        });

        step("2. Open the Price Tab, click 'Add Taxes' button, and save changes", () -> {
            cartPage.openTab();
            cartPage.addTaxes();
            cartPage.taxCartItems.shouldHave(sizeGreaterThan(0));
            cartPage.saveChanges();
        });

        step("3. Check that QuoteLineItem.Charge_Term__c is '" + annualChargeTerm + "' for Taxes QLIs", () -> {
            checkChargeTermForTaxes(annualChargeTerm);
        });

        step("4. Open the Billing Details and Terms modal window, change charge term to '" + monthlyChargeTerm + "', click 'Apply' button, " +
                "click 'Add Taxes' button, and save changes", () -> {
            cartPage.footer.billingDetailsAndTermsButton.click();
            cartPage.billingDetailsAndTermsModal.selectChargeTerm(monthlyChargeTerm);
            cartPage.applyChangesInBillingDetailsAndTermsModal();

            cartPage.addTaxes();
            cartPage.taxCartItems.shouldHave(sizeGreaterThan(0));

            cartPage.saveChanges();
        });

        step("5. Check that QuoteLineItem.Charge_Term__c is '" + monthlyChargeTerm + "' for Taxes QLIs", () -> {
            checkChargeTermForTaxes(monthlyChargeTerm);
        });
    }

    /**
     * Check that the Taxes QLIs exist,
     * and that QuoteLineItem.Charge_Term__c for them has the specified value.
     *
     * @param chargeTerm expected charge term value
     * @throws ConnectionException in case of malformed DB queries or network failures
     */
    private void checkChargeTermForTaxes(String chargeTerm) throws ConnectionException {
        var taxQuoteLineItems = enterpriseConnectionUtils.query(
                "SELECT Charge_Term__c, Display_Name__c " +
                        "FROM QuoteLineItem " +
                        "WHERE QuoteId = '" + wizardPage.getSelectedQuoteId() + "' " +
                        "AND Product2.Family = '" + FAMILY_TAXES + "'",
                QuoteLineItem.class);
        assertThat(taxQuoteLineItems.size())
                .as("Number of Taxes QLIs")
                .isGreaterThan(0);

        for (var taxQuoteLineItem : taxQuoteLineItems) {
            assertThat(taxQuoteLineItem.getCharge_Term__c())
                    .as("QuoteLineItem.Charge_Term__c value for " + taxQuoteLineItem.getDisplay_Name__c())
                    .isEqualTo(chargeTerm);
        }
    }
}
