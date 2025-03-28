package com.aquiva.autotests.rc.page.salesforce.contact;

import com.aquiva.autotests.rc.page.salesforce.RecordCreationModal;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Contact;

/**
 * Standard record editing modal for the {@link Contact} records.
 * <br/>
 * Contains some contact-related fields, like 'First Name', 'Last Name', etc.
 * <p>
 * <b> Note: Might be opened by clicking 'Edit' lightning button from {@link ContactRecordPage} page.</b>
 */
public class ContactEditModal extends RecordCreationModal {

    //  Error messages
    public static final String CONTACT_CANNOT_BE_MODIFIED_ERROR = "Contact cannot be modified. Please contact Deal Desk for any questions.";

    public final SelenideElement firstNameInput = dialogContainer.$x(".//input[@name='firstName']");

    /**
     * Constructor for Contact record's editing modal window with initialization
     * of its dialog container's locator using its header's title.
     *
     * @param currentContactFullName full name of the Contact at the moment of the editing
     */
    public ContactEditModal(String currentContactFullName) {
        super("Edit " + currentContactFullName);
    }
}
