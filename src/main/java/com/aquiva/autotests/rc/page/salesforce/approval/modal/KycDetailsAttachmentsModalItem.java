package com.aquiva.autotests.rc.page.salesforce.approval.modal;

import com.aquiva.autotests.rc.page.salesforce.approval.KycApprovalPage;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

/**
 * Represents a single item for the uploaded file (attachment)
 * for the different sections of "KYC Details".
 * Can be found on {@link KycApprovalPage}).
 * <br/>
 * User can see the preview of the uploaded files, download or delete them.
 */
public class KycDetailsAttachmentsModalItem {
    private final SelenideElement attachmentElement;

    private final By unsavedFileIcon = By.xpath(".//*[contains(@class,'slds-icon-doctype-attachment')]");
    private final By filePreviewImage = By.xpath(".//img[@data-id='file']");
    private final By fileName = By.xpath(".//b[@title]");
    private final By downloadFileButton = By.xpath(".//button[@title='download']");
    private final By deleteButton = By.xpath(".//button[@title='delete']");

    /**
     * Constructor with a web element as a parameter.
     *
     * @param attachmentElement SelenideElement that is used to locate the attachment file element in the DOM
     */
    public KycDetailsAttachmentsModalItem(SelenideElement attachmentElement) {
        this.attachmentElement = attachmentElement;
    }

    /**
     * Get preview image of the uploaded file.
     * Doesn't exist if the file isn't uploaded to database yet.
     */
    public SelenideElement getFilePreview() {
        return attachmentElement.$(filePreviewImage);
    }

    /**
     * Get the name of the uploaded file (including the extension).
     * E.g. "test1.txt".
     */
    public SelenideElement getFileName() {
        return attachmentElement.$(fileName);
    }

    /**
     * Get icon for the uploaded, but unsaved file.
     * It displays when file is submitted for upload, but not uploaded to the database yet.
     */
    public SelenideElement getUnsavedFileIcon() {
        return attachmentElement.$(unsavedFileIcon);
    }

    /**
     * Get the download button for the uploaded file.
     */
    public SelenideElement getDownloadFileButton() {
        return attachmentElement.$(downloadFileButton);
    }

    /**
     * Get the delete button for the uploaded file.
     */
    public SelenideElement getDeleteButton() {
        return attachmentElement.$(deleteButton);
    }
}
