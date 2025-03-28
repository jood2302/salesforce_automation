package com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories;

import com.sforce.soap.enterprise.sobject.Opportunity;
import com.sforce.soap.enterprise.sobject.Quote;
import com.sforce.ws.ConnectionException;

import java.util.Calendar;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.setQuoteToApprovedActiveAgreement;

/**
 * Factory class for creating quick instances of {@link Quote} class.
 * <br/>
 * All factory methods also insert created objects into the SF database.
 */
public class QuoteFactory extends SObjectFactory {

    /**
     * Create new Quote object as Active Sales Agreement and insert it into Salesforce via API.
     *
     * @param opportunity Opportunity object for which the quote is created
     *                    (note: should have ID and Name on it!)
     * @param initialTerm Initial term in months (e.g. "12", "24", "36"...)
     * @return Quote object with default parameters and ID from Salesforce
     * @throws ConnectionException in case of DB or network errors.
     */
    public static Quote createActiveSalesAgreement(Opportunity opportunity, String initialTerm)
            throws ConnectionException {
        var quote = new Quote();

        quote.setName(opportunity.getName());
        quote.setOpportunityId(opportunity.getId());

        var from = Calendar.getInstance();
        var to = Calendar.getInstance();
        to.add(Calendar.MONTH, Integer.parseInt(initialTerm));
        quote.setStart_Date__c(from);
        quote.setInitial_Term_months__c(initialTerm);
        quote.setEnd_Date__c(to);
        // renewal term the same as initial term, for simplicity
        quote.setTerm_months__c(initialTerm);

        setQuoteToApprovedActiveAgreement(quote);

        CONNECTION_UTILS.insertAndGetIds(quote);

        return quote;
    }
}
