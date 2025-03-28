package ngbs.quotingwizard.newbusiness;

import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.codeborne.selenide.WebElementCondition;
import com.sforce.soap.enterprise.sobject.CustomAddressAssignments__c;
import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.ws.ConnectionException;
import ngbs.quotingwizard.LboSteps;

import static base.Pages.wizardPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.SALES_QUOTE_RECORD_TYPE;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods related for tests that check Initiate and Cancel ProServ/CC ProServ functionality.
 */
public class InitiateAndCancelProServSteps {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final LboSteps lboSteps;

    /**
     * New instance for the class with test methods
     * related for tests that check Initiate and Cancel ProServ/CC ProServ functionality.
     */
    public InitiateAndCancelProServSteps() {
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        lboSteps = new LboSteps();
    }

    /**
     * <p> - Check that Shipping tab is displayed or not </p>
     * <p> - Check that Provisioning toggle is turned off </p>
     * <p> - Check that Provisioning toggle is enabled or disabled </p>
     * <p> - Check that all Sales Quotes have Enabled_LBO__c = true </p>
     *
     * @param expectedShippingTabCondition expected condition of Shipping tab button
     * @param isProvisioningToggleEnabled  is Provisioning toggle expected to be enabled or disabled
     * @param opportunityId                ID of provided Opportunity
     * @param expectedQuotesQuantity       expected quantity of created MVP Sales Quotes to check (Master and Tech)
     * @throws ConnectionException in case of errors while accessing API
     */
    public void checkShippingTabAndProvisioning(WebElementCondition expectedShippingTabCondition,
                                                boolean isProvisioningToggleEnabled, String opportunityId,
                                                int expectedQuotesQuantity)
            throws ConnectionException {
        wizardPage.shippingTabButton.shouldBe(expectedShippingTabCondition, ofSeconds(20));
        lboSteps.checkProvisionToggleOn(false);
        lboSteps.checkProvisionToggleEnabled(isProvisioningToggleEnabled);

        var mvpSalesQuotes = enterpriseConnectionUtils.query(
                "SELECT Id, Enabled_LBO__c " +
                        "FROM Quote " +
                        "WHERE OpportunityId = '" + opportunityId + "' " +
                        "AND RecordType.Name = '" + SALES_QUOTE_RECORD_TYPE + "' " +
                        "AND ServiceName__c IN ('Office', '')", //  Master Quotes have empty ServiceName__c field
                Quote.class);
        assertThat(mvpSalesQuotes.size())
                .as("Created Quotes' size")
                .isEqualTo(expectedQuotesQuantity);
        for (var quote : mvpSalesQuotes) {
            assertThat(quote.getEnabled_LBO__c())
                    .as("Quote.Enabled_LBO__c value for the Quote with Id = " + quote.getId())
                    .isTrue();
        }
    }

    /**
     * Check if the Quote has related CustomAddressAssignments__c records.
     *
     * @param isCustomAssignmentRecordsArePresent true, if CustomAddressAssignments__c records are expected to be present
     * @throws ConnectionException in case of errors while accessing API
     */
    public void checkIfCustomAssignmentRecordsArePresent(boolean isCustomAssignmentRecordsArePresent) throws ConnectionException {
        var customAddressAssignments = enterpriseConnectionUtils.query(
                "SELECT Id " +
                        "FROM CustomAddressAssignments__c " +
                        "WHERE QuoteLineItem__r.QuoteId = '" + wizardPage.getSelectedQuoteId() + "'",
                CustomAddressAssignments__c.class);
        if (isCustomAssignmentRecordsArePresent) {
            assertThat(customAddressAssignments.size())
                    .as("Quantity of CustomAddressAssignments__c records")
                    .isGreaterThan(0);
        } else {
            assertThat(customAddressAssignments.size())
                    .as("Quantity of CustomAddressAssignments__c records")
                    .isEqualTo(0);
        }
    }
}
