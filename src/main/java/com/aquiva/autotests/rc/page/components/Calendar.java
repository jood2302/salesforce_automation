package com.aquiva.autotests.rc.page.components;

import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage;
import com.aquiva.autotests.rc.page.opportunity.OpportunityCreationPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selectors.byTitle;
import static com.codeborne.selenide.Selenide.$x;
import static java.time.Duration.ofSeconds;

/**
 * Calendar component used to set some date fields like 'Close date' or 'Default Start Day'
 * <br/>
 * Can be found on {@link OpportunityCreationPage}, {@link QuotePage}
 */
public class Calendar {

    //  Container
    private final SelenideElement datePickerContainer = $x("//ngl-datepicker");

    //  Buttons
    public final SelenideElement previousMonthButton = datePickerContainer.$(byTitle("Previous Month"));
    public final SelenideElement nextMonthButton = datePickerContainer.$(byTitle("Next Month"));
    public final SelenideElement todayButton = datePickerContainer.$(byText("Today"));

    public final SelenideElement monthTableHeader = datePickerContainer.$("h2");
    public final SelenideElement yearPicklist = datePickerContainer.$("ngl-date-year select");
    public final ElementsCollection daysInMonth = datePickerContainer.$$("td:not(.slds-disabled-text) .slds-day");

    /**
     * Populate today's date in the calendar.
     */
    public void setTodayDate() {
        todayButton.shouldBe(visible, ofSeconds(10)).click();
    }

    /**
     * Set the specific date in the calendar (day, month, year).
     *
     * @param day   Day of month to be selected in the calendar (e.g. "26")
     * @param month Month to be selected in the calendar (e.g. "JULY")
     * @param year  Year to be selected in the calendar (e.g. "2022")
     */
    public void setDate(String day, String month, String year) {
        while (!monthTableHeader.getText().equalsIgnoreCase(month)) {
            nextMonthButton.click();
        }
        yearPicklist.selectOptionByValue(year);

        daysInMonth.findBy(text(day)).click();
    }
}
