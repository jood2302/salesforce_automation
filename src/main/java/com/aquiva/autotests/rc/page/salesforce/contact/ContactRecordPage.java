package com.aquiva.autotests.rc.page.salesforce.contact;

import com.aquiva.autotests.rc.page.salesforce.GenericSalesforceModal;
import com.aquiva.autotests.rc.page.salesforce.RecordPage;
import com.codeborne.selenide.SelenideElement;
import com.sforce.soap.enterprise.sobject.Contact;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static java.time.Duration.ofSeconds;

/**
 * The Salesforce page that displays
 * Contact({@link Contact}) record information, such as
 * Account Name, Phone, Email,
 * linked Opportunities, etc.
 */
public class ContactRecordPage extends RecordPage {

    //  Notifications
    public static final String CONTACT_SAVED_SUCCESSFUL_MESSAGE = "Contact \"%s\" was saved.";

    //  Modal windows
    public final DeleteContactModal deleteModal = new DeleteContactModal();
    public final GenericSalesforceModal warningModal = new GenericSalesforceModal();

    //  Contact Info
    public final SelenideElement contactInfoSectionTitle = $(byText("Contact Information"));

    /**
     * Get notification message on Contact Record Page
     * depending on the Contact that was saved.
     *
     * @param contactFullName name of the Contact, which was saved
     * @return notification message about saving the Contact
     * (e.g. <i>"Contact "FirstName Lastname" was saved."</i>)
     */
    public static String getContactSavedSuccessMessage(String contactFullName) {
        return String.format(CONTACT_SAVED_SUCCESSFUL_MESSAGE, contactFullName);
    }

    /**
     * Get the active modal window for editing the current contact.
     *
     * @param currentContactFullName full name of the Contact at the moment of the editing
     */
    public ContactEditModal getContactEditModal(String currentContactFullName) {
        return new ContactEditModal(currentContactFullName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitUntilLoaded() {
        detailsTab.shouldBe(visible, ofSeconds(100));
    }

    /**
     * Press "Edit" button on the Contact Record page.
     * <br/>
     * This method searches "Edit" button among Lightning Experience actions
     * in the upper right corner of the page
     * (even if the button is hidden in the "show more actions" list).
     */
    public void clickEditButton() {
        clickDetailPageButton("Edit");
    }
}
