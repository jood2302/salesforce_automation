package com.aquiva.autotests.rc.page.salesforce;

import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.LocalSubscribedAddress__c;

import static com.codeborne.selenide.Selenide.$x;

/**
 * The Standard Salesforce page that displays {@link LocalSubscribedAddress__c} record information,
 * such as Local Subscribed Address Name, related Approval record, Owner,
 * action buttons and many more.
 */
public class LocalSubscribedAddressRecordPage extends RecordPage {

    //  Related list names
    public static final String NOTES_AND_ATTACHMENTS_LIST = "Notes & Attachments";

    //  Information section is the one that contains 'Local Subscribed Address Name' field
    public final SelenideElement informationFieldsSection =
            $x("//span[text()='Local Subscribed Address Name']/ancestor::records-record-layout-section");
}
