package com.aquiva.autotests.rc.page.salesforce.approval.modal;

import com.aquiva.autotests.rc.page.salesforce.approval.KycApprovalPage;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import java.util.List;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;

/**
 * Modal window in {@link KycApprovalPage}.
 * User should upload file(-s) to any 'KYC Details' section first.
 * This modal appears after user clicks the uploaded files' link in the corresponding section.
 * <br/>
 * It is used to display uploaded file's details, upload more files, or delete the existing ones.
 */
public class KycDetailsAttachmentsModal {

    //  Section names for the modal's header
    public static final String IDENTITY_DOCUMENT_OF_COMPANY_SECTION = "Identity document of company";
    public static final String PHOTO_ID_PROOF_TYPE_SECTION = "Photo ID Proof Type of Authorised Signatory";

    public final SelenideElement dialogContainer = $x("//c-kyc-section-details//section/div");

    public final SelenideElement uploadedFilesModalWindowHeader = dialogContainer.$("h2");
    public final ElementsCollection uploadedFileSections = dialogContainer.$$x(".//div[@class='file-section']");

    public final SelenideElement okButton = dialogContainer.$(byText("OK"));
    public final SelenideElement uploadFileButton = dialogContainer.$x(".//input[@type='file']");

    /**
     * Get a file attachment item on the modal.
     * Corresponds to the single uploaded file to one of the 'KYC Details' sections.
     *
     * @param fileName full file's name including extension (e.g. "test_file.txt")
     * @return single item for the uploaded file on the modal
     */
    public KycDetailsAttachmentsModalItem getFileAttachmentItem(String fileName) {
        var fileAttachmentElement = dialogContainer
                .$x(".//c-kyc-section-detail[.//b[@title='" + fileName + "']]");
        return new KycDetailsAttachmentsModalItem(fileAttachmentElement);
    }

    /**
     * Delete the attachments in the section.
     *
     * @param fileNames list of full names of the uploaded file(-s) to delete (including the extension),
     *                  e.g. "test1.jpg".
     */
    public void removeAttachments(List<String> fileNames) {
        //  without an additional wait the modal window might return the deleted attachments back
        sleep(3_000);

        for (var fileName : fileNames) {
            var attachmentDeleteButton = getFileAttachmentItem(fileName).getDeleteButton();
            attachmentDeleteButton.shouldBe(visible);
            //  Selenide's click(usingJavascript()) gives unstable results, "HTMLElement.click()" works much better
            executeJavaScript("arguments[0].click()", attachmentDeleteButton);
        }

        //  single click might not close the modal
        okButton.doubleClick();
    }
}
