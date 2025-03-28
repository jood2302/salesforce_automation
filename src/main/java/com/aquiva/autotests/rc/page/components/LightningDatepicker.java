package com.aquiva.autotests.rc.page.components;

import com.codeborne.selenide.SelenideElement;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.codeborne.selenide.Condition.enabled;
import static java.time.Clock.systemUTC;
import static java.time.Duration.ofSeconds;

/**
 * Standard Datepicker element (a text input to capture a date) from Lightning Web Components.
 *
 * @see <a href='https://www.lightningdesignsystem.com/components/datepickers/'>
 * Datepickers documentation</a>
 */
public class LightningDatepicker {
    private DateTimeFormatter dateTimeFormatter;

    private final SelenideElement datepickerElement;

    /**
     * Constructor that defines the datepicker in the DOM.
     *
     * @param datepickerElement web element for the date picker
     *                          (usually, an element with a "lightning-datepicker" tag)
     */
    public LightningDatepicker(SelenideElement datepickerElement) {
        this.datepickerElement = datepickerElement;
    }

    /**
     * Constructor that defines the datepicker in the DOM.
     *
     * @param datepickerElement web element for the date picker
     *                          (usually, an element with a "lightning-datepicker" tag)
     * @param dateTimeFormatter any valid date formatter required by the date picker to set values in
     */
    public LightningDatepicker(SelenideElement datepickerElement, DateTimeFormatter dateTimeFormatter) {
        this.datepickerElement = datepickerElement;
        this.dateTimeFormatter = dateTimeFormatter;
    }

    /**
     * Get input element for entering a date or making an assertion on the existing value.
     *
     * @return SelenideElement that represents input part of the component
     */
    public SelenideElement getInput() {
        return datepickerElement.$x(".//input");
    }

    /**
     * Get the text element for displaying error message.
     *
     * @return SelenideElement that represents a text of the error message for the component
     */
    public SelenideElement getErrorMessage() {
        return datepickerElement.$x("./*[@data-error-message]");
    }

    /**
     * Set tomorrow's date in the datepicker.
     */
    public void setTomorrowDate() {
        var tomorrow = LocalDate.now(systemUTC()).plusDays(1);
        setDate(tomorrow);
    }

    /**
     * Set the given date in the datepicker
     * using the current date formatter.
     *
     * @param date any date in the {@code LocalDate} format
     */
    public void setDate(LocalDate date) {
        var dateFormatted = date.format(getDateTimeFormatter());
        getInput().shouldBe(enabled, ofSeconds(10)).setValue(dateFormatted);
    }

    /**
     * Return the current date formatter for the date picker.
     *
     * @return any previously defined date formatter,
     * or a default formatter if no value was defined for the datepicker
     */
    private DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter != null
                ? dateTimeFormatter
                : DateTimeFormatter.ofPattern("MMM dd, yyyy");
    }
}
