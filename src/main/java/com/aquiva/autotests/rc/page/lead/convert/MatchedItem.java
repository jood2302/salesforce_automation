package com.aquiva.autotests.rc.page.lead.convert;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

/**
 * One of items in the "matched items" table on {@link LeadConvertPage}.
 * <br/>
 * It is used for items in "Matched Accounts", "Matched Opportunities", "Matched Contacts" tables.
 */
public class MatchedItem {

    //  'Matched Accounts' table column numbers
    public static final int MATCHED_ACCOUNTS_RC_USER_ID_COLUMN = 2;
    public static final int MATCHED_ACCOUNTS_RC_ACCOUNT_NUMBER_COLUMN = 3;
    public static final int MATCHED_ACCOUNTS_RC_ACCOUNT_STATUS_COLUMN = 4;
    public static final int MATCHED_ACCOUNTS_TYPE_COLUMN = 5;
    public static final int MATCHED_ACCOUNTS_CURRENT_OWNER_COLUMN = 6;

    private final SelenideElement element;

    private final By nameElement = By.xpath(".//a");
    private final By selectButton = By.xpath(".//lightning-input");

    /**
     * Constructor.
     *
     * @param element web element for the main container element of the Matched Item
     */
    public MatchedItem(SelenideElement element) {
        this.element = element;
    }

    /**
     * Return the name element of the current Matched Item.
     * This value is in the table's column "{SObject} Name" (e.g. "Account Name").
     */
    public SelenideElement getName() {
        return element.$(nameElement);
    }

    /**
     * Return select element for the current Matched Item.
     * Use this element to select the item in the table
     * via standard Selenium {@code element.click()} method.
     */
    public SelenideElement getSelectButton() {
        return element.$(selectButton);
    }

    /**
     * Return 'input' element for select radio button of the current Matched Item.
     * Use this element to check that the radio button (and the element) is selected/enabled
     * via standard Selenium API ({@code element.isSelected()} / {@code element.isEnabled()}).
     */
    public SelenideElement getSelectButtonInput() {
        return element.$(selectButton).$x(".//input");
    }

    /**
     * Get the value in the item's column by its number.
     * <br/>
     * Note: this method is useful for any unique columns that only exist in the specific table
     * (e.g. only Matched Account items has "RC User ID" column with number = 2
     * while Matched Opportunity items has "Stage" column at the same number).
     *
     * @param columnNumber 1...N number of the column
     * @return web element with an item's value at the specific column
     */
    public SelenideElement getElementInColumn(int columnNumber) {
        return element.$x("./td[" + (columnNumber + 1) + "]/*");
    }
}
