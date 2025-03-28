package com.aquiva.autotests.rc.model.prm;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.StringHelper;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static java.time.Clock.systemUTC;
import static java.time.LocalDate.now;
import static java.time.format.DateTimeFormatter.ofPattern;

/**
 * Test data object for PRM Portal tests that work with Deal Registration records.
 * <br/>
 * Normally, test data gets loaded from JSON files via {@link JsonUtils} into the objects of this type.
 * After that, actual test interacts with this object.
 */
public class DealRegistrationData extends DataModel {
    public static final String AUTOTEST_SUFFIX = "AquivaQAAuto";

    //  For 'partnerProgram' field
    public static final String CHANNEL_HARMONY_PARTNER_PROGRAM = "Channel Harmony";
    public static final String IGNITE_PARTNER_PROGRAM = "Ignite";

    //  For 'brand' field
    public static final String RINGCENTRAL_BRAND = "RingCentral";

    //  For 'Installation Service Provider' field
    public static final String RC_PRO_SERVICES_INSTALL_ISP = "RC ProServices Install";

    public String partnerProgram;
    public String firstName;
    public String lastName;
    public String companyName;
    public String emailAddress;
    public String phoneNumber;
    public String country;
    public String state;
    public String city;
    public String address;
    public String postalCode;
    public String tierName;
    public String forecastedUsers;
    public String brand;
    public String industry;
    public String website;
    public String numberOfEmployees;
    @JsonFormat(pattern = "MMM d, yyyy")
    public LocalDate estimatedCloseDate;
    public String howDidYouAcquireThisLead;
    public String description;
    public String existingSolutionProvider;
    public List<String> competitors;
    public List<String> whatsPromptingChange;
    public String isThisAnExistingMitelCustomer;
    public String installationServiceProvider;

    /**
     * Randomize all the existing test data on the object.
     * Useful to create a unique Deal Registration records via tests.
     */
    public void randomizeData() {
        var randomUuidWithDate = "_" + AUTOTEST_SUFFIX + "_" + UUID.randomUUID().toString().substring(0, 6)
                + "_" + (LocalDateTime.now(systemUTC()).format(ofPattern("MM/dd HH:mm")));

        this.firstName += randomUuidWithDate;
        this.lastName += randomUuidWithDate;
        this.companyName += randomUuidWithDate;
        this.emailAddress = StringHelper.getRandomEmail();
        this.phoneNumber = StringHelper.getRandomUSPhone();
        this.estimatedCloseDate = now().plusDays(7);
        this.description += randomUuidWithDate;
    }

    /**
     * Get the 'Estimated Close Date' as it's formatted on the Deal Registration creation page.
     */
    public String getEstimatedCloseDateFormatted() {
        var df = DateTimeFormatter.ofPattern("MMM d, yyyy");
        return df.format(estimatedCloseDate);
    }

    /**
     * Get the full name as a First Name and a Last Name combined together.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
