package com.aquiva.autotests.rc.page.components;

import com.aquiva.autotests.rc.page.opportunity.legacyquotingwizard.BaseLegacyQuotingWizardPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static java.time.Duration.ofSeconds;

/**
 * Datepicker component used to set some date fields like 'Estimated Completion Date' or 'ProServ Forecasted Close Date'.
 * <p></p>
 * Can be found on {@link BaseLegacyQuotingWizardPage}.
 */
public class LegacyDatePicker {

    //  Container
    private final SelenideElement datePickerContainer = $(".uiDatePicker");

    //  Buttons
    public final SelenideElement previousMonthButton = datePickerContainer.$(".prevMonth");
    public final SelenideElement nextMonthButton = datePickerContainer.$(".nextMonth");
    public final SelenideElement todayButton = datePickerContainer.$("button.today");

    public final SelenideElement monthTableHeader = datePickerContainer.$("h2.monthYear");
    public final SelenideElement yearPicklist = datePickerContainer.$("select");
    public final ElementsCollection daysInMonth = datePickerContainer.$$(".slds-day:not(.prevMonth):not(.nextMonth)");

    /**
     * Populate today's date in legacy datepicker.
     */
    public void setTodayDate() {
        todayButton.shouldBe(visible, ofSeconds(10)).click();
    }

    /**
     * Set the specific date in the datepicker (day, month, year).
     *
     * @param day   Day of month to be selected in calendar
     * @param month Month to be selected in calendar
     * @param year  Year to be selected in calendar
     */
    public void setDate(String day, String month, String year) {
        while (!monthTableHeader.getText().equalsIgnoreCase(month)) {
            nextMonthButton.click();
        }
        yearPicklist.selectOptionByValue(year);

        daysInMonth.findBy(text(day)).click();
    }
}
