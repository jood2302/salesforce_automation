package com.aquiva.autotests.rc.utilities;

import com.sforce.soap.enterprise.sobject.SObject;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;

/**
 * Utility class that provides tests (and other callers) with useful methods
 * for string transformations/formatting.
 * <p></p>
 * Additionally, class contains useful string constants.
 */
public class StringHelper {

    /**
     * Just an empty string value.
     * Explicit declaration is better than implicit one.
     */
    public static final String EMPTY_STRING = "";

    /**
     * 'None' value that usually used in picklists/selects/dropdowns
     * when no value is specified.
     */
    public static final String NONE = "None";

    /**
     * Test string value.
     */
    public static final String TEST_STRING = "Test String value";

    /**
     * String value for percent symbol.
     */
    public static final String PERCENT = "%";

    /**
     * String value for US currency ISO code.
     */
    public static final String USD_CURRENCY_ISO_CODE = "USD";

    /**
     * String value for Canada currency ISO code.
     */
    public static final String CAD_CURRENCY_ISO_CODE = "CAD";

    /**
     * String value for India currency ISO code.
     */
    public static final String INR_CURRENCY_ISO_CODE = "INR";

    /**
     * String value for Switzerland currency ISO code.
     */
    public static final String CHF_CURRENCY_ISO_CODE = "CHF";

    /**
     * String value for US currency sign.
     */
    public static final String USD_CURRENCY_SIGN = "$";

    /**
     * String value for zero price in "x.xx" format.
     */
    public static final String ZERO_PRICE = "0.00";

    /**
     * Format provided string value for currency using default digits group separator - <b> "," (comma). </b>
     * <p> Examples:
     * <p> - if value = "11298844.97", then result = "11,298,844.97". </p>
     * <p> - if value = "11 298 844.97", then result = "11,298,844.97". </p>
     * <p>
     * <p> - If value is less than 1000.00, it's returned unchanged (on "999.99" will return "999.99")
     * </p>
     * <p> - If value lacks decimal part, it's added as "00" by default (on "8344" will return "8,344.00") </p>
     * <p></p>
     * <b> Note! Only values with "." (dot) decimal separator are supported
     * (YES: "1933.98", NO: "1933,98"). Values with more than one "." separator can behave unexpectedly! </b>
     *
     * @param value string representation for a currency value (e.g. "928448.98")
     * @return formatted string with digits group separators (e.g. "11,298,844.97")
     */
    public static String getCurrencyFormatGrouped(String value) {
        return getCurrencyFormatGrouped(value, ",");
    }

    /**
     * Format provided string value for currency using digits group separator.
     * <p> Examples:
     * <p> * if value = "11298844.97" and separator = "," (comma), then result = "11,298,844.97". </p>
     * <p> * if value = "11298844.97" and separator = " " (space), then result = "11 298 844.97". </p>
     * <p> * if value = "11 298 844.97" and separator = "," (space), then result = "11,298,844.97". </p>
     * <p>
     * <p> - If value is less than 1000.00, it's returned unchanged (on "999.99" will return "999.99")
     * </p>
     * <p> - If value lacks decimal part, it's added as "00" by default (on "8344" will return "8,344.00") </p>
     * <p></p>
     * <b> Note! Only values with "." (dot) decimal separator are supported
     * (YES: "1933.98", NO: "1933,98"). Values with more than one "." separator can behave unexpectedly! </b>
     *
     * @param value          string representation for a currency value (e.g. "928448.98")
     * @param groupSeparator separator for groups of digits (e.g. "," (comma))
     * @return formatted string with digits group separators (e.g. "11,298,844.97")
     */
    public static String getCurrencyFormatGrouped(String value, String groupSeparator) {
        //  Splitting value on integer (index 0) and decimal (index 1) parts using "."
        String[] valueSplit = value.replaceAll("[^0-9.]", EMPTY_STRING).split("\\.");
        String integerPart = valueSplit[0].replaceAll("[^0-9]", EMPTY_STRING);

        //  Return unchanged value for currencies <= 999
        if (integerPart.length() < 4) {
            return value;
        }

        //  Inserting group separator in between groups of 3 digits
        StringBuilder sb = new StringBuilder(integerPart);
        for (int i = integerPart.length() - 3; i > 0; i -= 3) {
            sb.insert(i, groupSeparator);
        }

        //  Adding back the decimal part at the end
        sb.append(".");
        if (valueSplit.length == 1) {
            sb.append("00");
        } else {
            sb.append(valueSplit[1]);
        }

        return sb.toString();
    }

    /**
     * Remove one of the elements from the list of similar elements separated by 'separator'
     * and return the new list without removed element.
     * <p></p>
     * <i> Examples:
     * <p> list = "18,19,20", elementToRemove = "19", separator = "," </p>
     * <p> => result = <b>"18,20"</b> </p>
     * <p> list = "18", elementToRemove = "18", separator = "," </p>
     * <p> => result = <b>""</b> </p>
     * <p> list = "18 19 20", elementToRemove = "19", separator = "-" </p>
     * <p> => result = <b>"18 19 20"</b> </p>
     * </i>
     *
     * @param listString      list of similar elements separated by some separator, like comma (e.g. "18,19,20")
     * @param elementToRemove one of the elements to be removed from the list
     * @param separator       any separator for elements (e.g. comma (","))
     * @return new string list without replaced element.
     * Returns the same list if separator/elementToReplace are not found in the list.
     */
    public static String removeElementFromList(String listString, String elementToRemove, String separator) {
        if (!listString.contains(elementToRemove)) {
            return listString;
        }

        if (listString.contains(separator)) {
            List<String> charList = new ArrayList<>(Arrays.asList(listString.split(Pattern.quote(separator))));
            while (charList.contains(elementToRemove)) {
                charList.remove(elementToRemove);
            }

            return String.join(separator, charList);
        } else {
            return listString;
        }
    }

    /**
     * Get randomly generated email address with a common domain.
     *
     * @return random email with a common domain "example.com"
     * (e.g. "ae13320a-db2c-477d-997a-f6dbc8c1c93d@example.com")
     */
    public static String getRandomEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    /**
     * Get randomly generated US-based 10-digit phone number
     * using a pattern "(617) NXX-XXXX".
     * <br/>
     * More info on valid values via this link:
     * <a href="https://en.wikipedia.org/wiki/North_American_Numbering_Plan#Modern_plan">LINK</a>.
     *
     * @return 10-digit random phone number with the US area code
     * (e.g. "(617) 248-3864", "(617) 365-0448", etc...)
     */
    public static String getRandomUSPhone() {
        var centralOfficeCode = new Random().nextInt(800) + 200;
        if ((centralOfficeCode - 11) % 100 == 0) {
            centralOfficeCode += 1;
        }

        var lineNumber = new Random().nextInt(10000);

        return String.format("(617) %3d-%04d", centralOfficeCode, lineNumber);
    }

    /**
     * Get randomly generated Canada-based 10-digit phone number
     * using a pattern "(416) NXX-XXXX".
     * <br/>
     * More info on valid values via this link:
     * <a href="https://en.wikipedia.org/wiki/North_American_Numbering_Plan#Modern_plan">LINK</a>.
     *
     * @return 10-digit random phone number with the US area code
     * (e.g. "(416) 248-3864", "(416) 365-0448", etc...)
     */
    public static String getRandomCanadaPhone() {
        return getRandomUSPhone().replaceAll("\\(\\D+\\)", "(416)");
    }

    /**
     * Get random positive integer number.
     * Anything from 0 to 2<sup>16</sup>.
     *
     * @return positive random integer number as a string (e.g. "4623457", "574")
     */
    public static String getRandomPositiveInteger() {
        return String.valueOf(Math.abs(new Random().nextInt()));
    }

    /**
     * Get formatted string which contains quantity and price.
     *
     * @param quantity       The quantity digital line(s) or device(s) on 'Cart' tab in SalesForce
     * @param price          The price for one digital line or device on 'Cart' tab in SalesForce
     * @param currencySymbol The symbol of currency(e.g. '$' for USD)
     * @return String with quantity, price and currency symbol for digital line or device
     * (e.g "1 x $49.99")
     */
    public static String formatPriceAndQuantityForFunnelSummaryPage(Integer quantity, String price, String currencySymbol) {
        return String.format("%d x %s%s", quantity, currencySymbol, price);
    }

    /**
     * Get SObject IDs as a list for filtering SOQL queries.
     *
     * @param sObjects any collection of SObjects like Accounts, Contacts, Opportunities, etc.
     * @return string list of IDs in parentheses and enclosed in single quotes.
     * E.g. ('Id1','Id2','Id3') or ('') if there are no records
     */
    public static String getSObjectIdsListAsString(Collection<? extends SObject> sObjects) {
        return sObjects.stream()
                .map(SObject::getId)
                .collect(joining("','", "('", "')"));
    }

    /**
     * Get String literals as a list for filtering SOQL queries.
     *
     * @param listStrings any collection of string literals
     * @return string list of string literals in parentheses and enclosed in single quotes.
     * E.g. ('Office','Meetings','Engage Digital Standalone')
     */
    public static String getStringListAsString(Collection<String> listStrings) {
        return listStrings.stream().collect(joining("','", "('", "')"));
    }
}
