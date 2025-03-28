package ngbs.quotingwizard;

import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.ws.ConnectionException;

import java.util.Set;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.getTechQuote;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test steps for test cases that check Quote's QuoteType__c and Status fields for Multi-Product Unified Billing accounts.
 */
public class MultiProductQuoteTypeAndStatusSteps {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    /**
     * New instance for the class with the steps/methods 
     * that check Quote's QuoteType__c and Status fields for Multi-Product Unified Billing accounts.
     */
    public MultiProductQuoteTypeAndStatusSteps() {
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    /**
     * Check the QuoteType__c and Status field values for the Master and all the Technical Quotes.
     *
     * @param expectedQuoteType   the expected value of QuoteType__c field for the Master and Technical Quotes
     * @param expectedQuoteStatus the expected value of Status field for the Master and Technical Quotes
     * @throws ConnectionException in case of errors while accessing API
     */
    public void checkQuoteTypeAndStatus(String expectedQuoteType, String expectedQuoteStatus,
                                        String masterQuoteId, Set<String> allSelectedServices) throws ConnectionException {
        var masterQuote = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, QuoteType__c, Status " +
                        "FROM Quote " +
                        "WHERE Id = '" + masterQuoteId + "'",
                Quote.class);
        assertThat(masterQuote.getQuoteType__c())
                .as("Master Quote.QuoteType__c value")
                .isEqualTo(expectedQuoteType);
        assertThat(masterQuote.getStatus())
                .as("Master Quote.Status value")
                .isEqualTo(expectedQuoteStatus);

        for (var service : allSelectedServices) {
            var techQuote = getTechQuote(masterQuoteId, service);
            assertThat(techQuote.getQuoteType__c())
                    .as("Tech " + techQuote.getServiceName__c() + " Quote.QuoteType__c value")
                    .isEqualTo(expectedQuoteType);
            assertThat(techQuote.getStatus())
                    .as("Tech " + techQuote.getServiceName__c() + " Quote.Status value")
                    .isEqualTo(expectedQuoteStatus);
        }
    }
}
