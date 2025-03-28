package com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper;

import com.sforce.soap.enterprise.sobject.QuoteLineItem;

import java.util.Calendar;
import java.util.Date;

/**
 * Helper class to facilitate operations on {@link QuoteLineItem} objects.
 */
public class QuoteLineItemHelper extends SObjectHelper {
    //  For 'Discount_type__c' field
    public final static String DISCOUNT_TYPE_CURRENCY = "Currency";
    public final static String DISCOUNT_TYPE_PERCENTAGE = "Percentage";

    //  For 'ChargeTerm__c' field
    public final static String ONE_TIME_CHARGE_TERM = "One - Time";
    public final static String ANNUAL_CHARGE_TERM = "Annual";
    public final static String MONTHLY_CHARGE_TERM = "Monthly";

    //  For 'Billing_Cycle_Duration__c' field
    public final static String ONE_TIME_BCD = "0";
    public final static String MONTHLY_BCD = "1";
    public final static String ANNUAL_BCD = "12";

    /**
     * Set 'SalesEndDate__c' field for QuoteLineItem object with date in the past (current date/time - 15 days)
     *
     * @param quoteLineItem QuoteLineItem object to set up with SalesEndDate__c
     */
    public static void setSalesEndDateInThePast(QuoteLineItem quoteLineItem) {
        var salesEndDatePast = Calendar.getInstance();
        salesEndDatePast.setTime(new Date());
        salesEndDatePast.add(Calendar.DATE, -15);

        quoteLineItem.setSalesEndDate__c(salesEndDatePast);
    }

    /**
     * Set 'SalesEndDate__c' field for QuoteLineItem object with date in the future (current date/time + 15 days)
     *
     * @param quoteLineItem QuoteLineItem object to set up with SalesEndDate__c
     */
    public static void setSalesEndDateInTheFuture(QuoteLineItem quoteLineItem) {
        var salesEndDateFuture = Calendar.getInstance();
        salesEndDateFuture.setTime(new Date());
        salesEndDateFuture.add(Calendar.DATE, 15);

        quoteLineItem.setSalesEndDate__c(salesEndDateFuture);
    }
}
