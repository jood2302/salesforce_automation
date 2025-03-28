package ngbs.approvals;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.sforce.soap.enterprise.sobject.Approval__c;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static base.Pages.kycApprovalPage;
import static com.aquiva.autotests.rc.page.salesforce.approval.KycApprovalPage.MULTIPLE_FILES_LABEL_PATTERN;
import static com.aquiva.autotests.rc.page.salesforce.approval.modal.KycDetailsAttachmentsModal.IDENTITY_DOCUMENT_OF_COMPANY_SECTION;
import static com.aquiva.autotests.rc.page.salesforce.approval.modal.KycDetailsAttachmentsModal.PHOTO_ID_PROOF_TYPE_SECTION;
import static com.aquiva.autotests.rc.utilities.FileUtils.getAttachmentResourceFile;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.DownloadOptions.file;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Approval")
@Tag("IndiaMVP")
public class KycApprovalUploadAndDownloadAttachmentsTest extends BaseTest {
    private final Steps steps;

    private User salesUserWithEditKycApprovalPermissionSet;
    private Approval__c kycApproval;
    private File fileToUploadAndDownload;

    //  Test data
    private final String firstFileName;
    private final String secondFileName;
    private final String thirdFileName;
    private final String counterTwoUploadedFiles;
    private final String counterThreeUploadedFiles;

    public KycApprovalUploadAndDownloadAttachmentsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_IndiaAndUS_MVP_Monthly_Contract.json",
                Dataset.class);

        steps = new Steps(data);

        firstFileName = "test1.jpg";
        secondFileName = "test2.jpg";
        thirdFileName = "test3.jpg";

        counterTwoUploadedFiles = String.format(MULTIPLE_FILES_LABEL_PATTERN, 2);
        counterThreeUploadedFiles = String.format(MULTIPLE_FILES_LABEL_PATTERN, 3);
    }

    @BeforeEach
    public void setUpTest() {
        salesUserWithEditKycApprovalPermissionSet = steps.kycApproval.getSalesUserWithKycApprovalPermissionSet();

        steps.salesFlow.createAccountWithContactAndContactRole(salesUserWithEditKycApprovalPermissionSet);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact,
                salesUserWithEditKycApprovalPermissionSet);

        kycApproval = steps.kycApproval.changeOwnerOfDefaultKycApproval(
                salesUserWithEditKycApprovalPermissionSet.getId(), steps.salesFlow.account.getId());

        step("Login as a user with 'Sales Rep - Lightning' profile with 'KYC_Approval_Edit' permission set", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(salesUserWithEditKycApprovalPermissionSet)
        );
    }

    @Test
    @TmsLink("CRM-24006")
    @TmsLink("CRM-24009")
    @DisplayName("CRM-24006 - KYC Approval Custom attachment upload files. \n" +
            "CRM-24009 - KYC Approval Custom attachment download files")
    @Description("CRM-24006 - Verify that you can upload files for Custom attachment. \n" +
            "CRM-24009 - Verify that you can download files from Custom attachment")
    public void test() {
        step("1. Open KYC Approval page", () -> {
            kycApprovalPage.openPage(kycApproval.getId());
        });

        step("2. Upload a file to 'Identity document of company' section", () -> {
            fileToUploadAndDownload = getAttachmentResourceFile(firstFileName);
            kycApprovalPage.identityDocumentOfCompanyUploadInput.shouldBe(visible, ofSeconds(30))
                    .uploadFile(fileToUploadAndDownload);
            kycApprovalPage.identityDocumentOfCompanyUploadedFilesLink.shouldHave(exactTextCaseSensitive(firstFileName));
        });

        step("3. Click the document name in 'Identity document of company' section, " +
                "check header name of the opened modal window and that document is displayed without preview", () -> {
            kycApprovalPage.identityDocumentOfCompanyUploadedFilesLink.click();

            kycApprovalPage.kycDetailsAttachmentsModal.uploadedFilesModalWindowHeader
                    .shouldHave(exactTextCaseSensitive(IDENTITY_DOCUMENT_OF_COMPANY_SECTION));
            var fileAttachmentItem = kycApprovalPage.kycDetailsAttachmentsModal.getFileAttachmentItem(firstFileName);
            fileAttachmentItem.getFilePreview().shouldNot(exist);
            fileAttachmentItem.getUnsavedFileIcon().shouldBe(visible);

            kycApprovalPage.kycDetailsAttachmentsModal.okButton.click();
        });

        step("4. Upload more than one file to 'Photo ID Proof Type of Authorised Signatory' section", () -> {
            var firstFile = getAttachmentResourceFile(firstFileName);
            var secondFile = getAttachmentResourceFile(secondFileName);

            kycApprovalPage.photoIdProofTypeUploadInput.uploadFile(firstFile);
            //  workaround for Selenoid issue with multiple files upload
            kycApprovalPage.photoIdProofTypeUploadInput.setValue(EMPTY_STRING);
            kycApprovalPage.photoIdProofTypeUploadInput.uploadFile(secondFile);

            kycApprovalPage.photoIdProofTypeUploadedFilesLink
                    .shouldHave(exactTextCaseSensitive(counterTwoUploadedFiles));
        });

        step("5. Click on 'View all' link in 'Photo ID Proof Type of Authorised Signatory' section " +
                "and check header name of the modal window and that documents are displayed without preview", () -> {
            kycApprovalPage.photoIdProofTypeUploadedFilesLink.click();

            kycApprovalPage.kycDetailsAttachmentsModal.uploadedFilesModalWindowHeader
                    .shouldHave(exactTextCaseSensitive(PHOTO_ID_PROOF_TYPE_SECTION));

            var expectedFileNames = List.of(firstFileName, secondFileName);
            for (var fileName : expectedFileNames) {
                var fileAttachmentItem = kycApprovalPage.kycDetailsAttachmentsModal.getFileAttachmentItem(fileName);
                fileAttachmentItem.getUnsavedFileIcon().shouldBe(visible);
                fileAttachmentItem.getFilePreview().shouldNot(exist);
            }
        });

        step("6. Click Upload button in modal window, select some document, click 'OK' button, " +
                "check that counter is updated", () -> {
            var thirdFile = getAttachmentResourceFile(thirdFileName);
            kycApprovalPage.kycDetailsAttachmentsModal.uploadFileButton.uploadFile(thirdFile);
            kycApprovalPage.kycDetailsAttachmentsModal.okButton.click();

            kycApprovalPage.photoIdProofTypeUploadedFilesLink
                    .shouldHave(exactTextCaseSensitive(counterThreeUploadedFiles));
        });

        step("7. Populate all fields within sections - " +
                "'Identity documents of company' and 'Photo ID Proof Type of Authorised Signatory' - " +
                "and save changes", () -> {
            kycApprovalPage.identityDocumentOfCompanySection.click();
            kycApprovalPage.identityDocumentOfCompanyDocumentNoInput
                    .setValue(steps.kycApproval.identityDocumentOfCompanyDocumentNumberValue);
            kycApprovalPage.identityDocumentOfCompanyPlaceOfIssueInput
                    .setValue(steps.kycApproval.identityDocumentOfCompanyPlaceOfIssueValue);
            kycApprovalPage.identityDocumentOfCompanyIssuingAuthorityInput
                    .setValue(steps.kycApproval.identityDocumentOfCompanyIssuingAuthorityValue);
            kycApprovalPage.identityDocumentOfCompanyDateOfIssueInput.setTomorrowDate();

            kycApprovalPage.photoIdProofTypeSection.click();
            kycApprovalPage.photoIdProofTypeDocumentNoInput.setValue(steps.kycApproval.photoIdProofTypeDocumentNumberValue);
            kycApprovalPage.photoIdProofTypePlaceOfIssueInput.setValue(steps.kycApproval.photoIdProofTypePlaceOfIssueValue);
            kycApprovalPage.photoIdProofTypeIssuingAuthorityInput.setValue(steps.kycApproval.photoIdProofTypeIssuingAuthorityValue);
            kycApprovalPage.photoIdProofTypeDateOfIssueInput.setTomorrowDate();

            kycApprovalPage.kycDetailsSaveChanges();
        });

        step("8. Click on 'View all' link in 'Photo ID Proof Type of Authorised Signatory' section " +
                "and check that documents are displayed with preview", () -> {
            kycApprovalPage.photoIdProofTypeUploadedFilesLink.click();

            var expectedFileNames = List.of(firstFileName, secondFileName, thirdFileName);
            for (var fileName : expectedFileNames) {
                var fileAttachmentItem = kycApprovalPage.kycDetailsAttachmentsModal.getFileAttachmentItem(fileName);
                fileAttachmentItem.getFilePreview().shouldBe(visible, ofSeconds(10));
                fileAttachmentItem.getUnsavedFileIcon().shouldNot(exist);
            }

            //  single click might not close the modal
            kycApprovalPage.kycDetailsAttachmentsModal.okButton.doubleClick();
        });

        step("9. Click on document link in 'Identity document of company' section, upload file in opened modal window, " +
                "click 'OK' button and check that document link is changed", () -> {
            kycApprovalPage.identityDocumentOfCompanyUploadedFilesLink.scrollIntoView("{block: \"center\"}").click();

            var secondFile = getAttachmentResourceFile(secondFileName);
            //  workaround for Selenoid issue with multiple files upload
            kycApprovalPage.kycDetailsAttachmentsModal.uploadFileButton.setValue(EMPTY_STRING);
            kycApprovalPage.kycDetailsAttachmentsModal.uploadFileButton.uploadFile(secondFile);
            kycApprovalPage.kycDetailsAttachmentsModal.okButton.click();

            kycApprovalPage.identityDocumentOfCompanyUploadedFilesLink
                    .shouldHave(exactTextCaseSensitive(counterTwoUploadedFiles));
        });

        step("10. Click 'Cancel' button in 'KYC Details' section " +
                "and check that document link is returned to previous value", () -> {
            kycApprovalPage.kycDetailsCancelButton.click();
            kycApprovalPage.identityDocumentOfCompanyUploadedFilesLink
                    .shouldHave(exactTextCaseSensitive(firstFileName));
        });

        step("11. Click on document link in 'Identity document of company' section " +
                "and check that file from previous step isn't uploaded", () -> {
            kycApprovalPage.identityDocumentOfCompanyUploadedFilesLink.click();
            kycApprovalPage.kycDetailsAttachmentsModal.uploadedFileSections.shouldHave(size(1));
            kycApprovalPage.kycDetailsAttachmentsModal.getFileAttachmentItem(firstFileName)
                    .getFileName()
                    .shouldHave(exactTextCaseSensitive(firstFileName));
        });

        //  CRM-24009
        step("12. Click 'Download' button for a document in uploaded files modal window " +
                "and check that document is downloaded properly", () -> {
            var downloadedFile = kycApprovalPage.kycDetailsAttachmentsModal.getFileAttachmentItem(firstFileName)
                    .getDownloadFileButton()
                    .download(file().withName(firstFileName).withTimeout(ofSeconds(20)));

            assertThat(downloadedFile)
                    .as("Downloaded attachment")
                    .hasName(firstFileName)
                    .hasBinaryContent(readFileToByteArray(fileToUploadAndDownload));
        });
    }
}
