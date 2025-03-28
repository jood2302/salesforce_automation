package com.aquiva.autotests.rc.page.prm;

import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Deal_Registration__c;

import static com.codeborne.selenide.Selenide.$x;
import static java.lang.String.format;

/**
 * The page that displays Deal Registration ({@link Deal_Registration__c}) record information,
 * such as its Lead details (First Name, Last Name, Phone number...),
 * Deal Details (Partner Program, Competitors...), and other related information.
 */
public class DealRegistrationRecordPage {
    public static final String FIELD_XPATH_FORMAT = "//div[./span=\"%s:\"]//following-sibling::*[1]/*";

    public final SelenideElement header = $x("//h2[./*='Deal Registration Details']");

    public final SelenideElement firstName = $x(format(FIELD_XPATH_FORMAT, "First Name"));
    public final SelenideElement lastName = $x(format(FIELD_XPATH_FORMAT, "Last Name"));
    public final SelenideElement companyName = $x(format(FIELD_XPATH_FORMAT, "Company Name"));
    public final SelenideElement emailAddress = $x(format(FIELD_XPATH_FORMAT, "Email Address"));
    public final SelenideElement phoneNumber = $x(format(FIELD_XPATH_FORMAT, "Phone Number"));
    public final SelenideElement address = $x(format(FIELD_XPATH_FORMAT, "Address"));
    public final SelenideElement city = $x(format(FIELD_XPATH_FORMAT, "City"));
    public final SelenideElement state = $x(format(FIELD_XPATH_FORMAT, "State/Province"));
    public final SelenideElement postalCode = $x(format(FIELD_XPATH_FORMAT, "Postal Code"));
    public final SelenideElement country = $x(format(FIELD_XPATH_FORMAT, "Country"));
    public final SelenideElement forecastedUsers = $x(format(FIELD_XPATH_FORMAT, "Forecasted Users"));
    public final SelenideElement industry = $x(format(FIELD_XPATH_FORMAT, "Industry"));
    public final SelenideElement website = $x(format(FIELD_XPATH_FORMAT, "Website"));
    public final SelenideElement numberOfEmployees = $x(format(FIELD_XPATH_FORMAT, "Number of Employees"));
    public final SelenideElement howDidYouAcquireThisLead = $x(format(FIELD_XPATH_FORMAT, "How did you acquire this Lead?"));
    public final SelenideElement description = $x(format(FIELD_XPATH_FORMAT, "Description"));
    public final SelenideElement existingSolutionProvider = $x(format(FIELD_XPATH_FORMAT, "Existing Solution Provider"));
    public final SelenideElement competitors = $x(format(FIELD_XPATH_FORMAT, "Competitors"));
    public final SelenideElement whatsPromptingChange = $x(format(FIELD_XPATH_FORMAT, "What's prompting change?"));
    public final SelenideElement partnerProgram = $x(format(FIELD_XPATH_FORMAT, "Partner Program"));
    public final SelenideElement isThisAnExistingMitelCustomer = $x(format(FIELD_XPATH_FORMAT, "Is this an existing Mitel Customer or Prospect"));
}
