package ngbs.quotingwizard;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Step;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.SALES_QUOTE_RECORD_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods for test cases related to {@link QuotePage} of {@link NGBSQuotingWizardPage}.
 */
public class QuoteTabSteps {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    public final boolean autoRenewalWithContract;
    public final boolean autoRenewalNoContract;

    /**
     * New instance of the test methods for test cases related to {@link QuotePage}.
     */
    public QuoteTabSteps() {
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        autoRenewalWithContract = true;
        autoRenewalNoContract = false;
    }

    /**
     * Get Quote.Auto_Renewal__c field value and compare it with provided value.
     *
     * @param opportunityId            ID of the Opportunity related to the target Quote
     * @param expectedAutoRenewalValue expected value of Quote's 'Auto_Renewal__c' field
     * @throws Exception in case of malformed DB queries or network errors.
     */
    @Step("Compare Quote.Auto_Renewal__c field value with a provided value")
    public void stepCheckQuoteAutoRenewal(String opportunityId, boolean expectedAutoRenewalValue) throws Exception {
        var quote = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, Auto_Renewal__c " +
                        "FROM Quote " +
                        "WHERE OpportunityId = '" + opportunityId + "' " +
                        "AND RecordType.Name = '" + SALES_QUOTE_RECORD_TYPE + "'",
                Quote.class);
        assertThat(quote.getAuto_Renewal__c())
                .as("Quote.Auto_Renewal__c value")
                .isEqualTo(expectedAutoRenewalValue);
    }
}
