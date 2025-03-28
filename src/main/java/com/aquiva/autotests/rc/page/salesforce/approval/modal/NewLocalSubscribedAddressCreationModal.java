package com.aquiva.autotests.rc.page.salesforce.approval.modal;

import com.aquiva.autotests.rc.page.salesforce.RecordCreationModal;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.LocalSubscribedAddress__c;

import static com.codeborne.selenide.Selenide.$x;

/**
 * Modal window for a new {@link LocalSubscribedAddress__c} record creation.
 */
public class NewLocalSubscribedAddressCreationModal extends RecordCreationModal {
    //  Section names
    public static final String INFORMATION_SECTION = "Information";
    public static final String PHYSICAL_INSPECTION_PRIOR_TO_ACTIVATION_SECTION =
            "Physical Inspection prior to activation of Services";
    public static final String PHYSICAL_INSPECTION_EVERY_SIX_MONTHS_SECTION =
            "Physical Inspection every 6 months";

    //  Error messages
    public static final String ONLY_ONE_REGISTERED_ADDRESS_CAN_BE_CREATED_ERROR =
            "Only one Registered Address of Company can be created per KYC";

    //  Elements
    public final SelenideElement localSubscribedAddressNameInput =
            $x("//label[text()='Local Subscribed Address Name']/following-sibling::div/input");

    /**
     * Constructor for {@link LocalSubscribedAddress__c} record creation modal window
     * located via its header's title that includes the active record type.
     *
     * @param localSubscribedAddressRecordType Local Subscribed Address record's record type
     *                                         (e.g. "Local Subscribed Address of Company")
     */
    public NewLocalSubscribedAddressCreationModal(String localSubscribedAddressRecordType) {
        super("New Local Subscribed Address: " + localSubscribedAddressRecordType);
    }
}
