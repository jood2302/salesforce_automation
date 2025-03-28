package ngbs.approvals;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.sforce.soap.enterprise.sobject.Approval__c;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static base.Pages.kycApprovalPage;
import static com.aquiva.autotests.rc.page.salesforce.approval.KycApprovalPage.MISSING_SIGNED_OFF_DATE_ERROR;
import static com.aquiva.autotests.rc.page.salesforce.approval.KycApprovalPage.MULTIPLE_FILES_LABEL_PATTERN;
import static com.aquiva.autotests.rc.utilities.FileUtils.getAttachmentResourceFile;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("IndiaMVP")
public class SignedOffParticipationAgreementKycApprovalTest extends BaseTest {
    private final Steps steps;

    private File signedOffParticipationAgreementFileToUpload;
    private Approval__c kycApproval;

    //  Test data
    private final String additionalFileName;

    public SignedOffParticipationAgreementKycApprovalTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_IndiaAndUS_MVP_Monthly_Contract.json",
                Dataset.class);

        steps = new Steps(data);

        additionalFileName = "test1.jpg";
    }

    @BeforeEach
    public void setUpTest() {
        var testUser = steps.kycApproval.getSalesUserWithKycApprovalPermissionSet();

        steps.salesFlow.createAccountWithContactAndContactRole(testUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, testUser);

        kycApproval = steps.kycApproval.changeOwnerOfDefaultKycApproval(testUser.getId(), steps.salesFlow.account.getId());

        step("Login as a user with 'Sales Rep - Lightning' profile with 'KYC_Approval_Edit' permission set", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(testUser)
        );
    }

    @Test
    @TmsLink("CRM-24178")
    @DisplayName("CRM-24178 - Populating Signed off Participation Agreement section")
    @Description("Verify that files can be uploaded to the 'Signed off Participation Agreement' section and section " +
            "contains required date field 'Date of Sign off'")
    public void test() {
        step("1. Open India Account's KYC Approval page", () -> {
            kycApprovalPage.openPage(kycApproval.getId());
        });

        step("2. Expand 'Signed off Participation Agreement' section " +
                "and check that 'Date of Sign off' field is empty and disabled", () -> {
            kycApprovalPage.signedOffParticipationAgreementSection.click();
            kycApprovalPage.signedOffDatePicker.getInput().shouldBe(empty, disabled);
        });

        step("3. Upload file to 'Signed off Participation Agreement' section, " +
                "check that file is uploaded and check that 'Date of Sign off' field became enabled", () -> {
            signedOffParticipationAgreementFileToUpload =
                    getAttachmentResourceFile(steps.kycApproval.signedOffParticipationAgreementFileName);
            kycApprovalPage.signedOffParticipationAgreementUploadInput
                    .shouldBe(visible, ofSeconds(10))
                    .uploadFile(signedOffParticipationAgreementFileToUpload);
            kycApprovalPage.signedOffParticipationAgreementUploadedFilesLink
                    .shouldHave(exactText(steps.kycApproval.signedOffParticipationAgreementFileName));
            kycApprovalPage.signedOffDatePicker.getInput().shouldBe(enabled);
        });

        step("4. Upload one more file to 'Signed off Participation Agreement' section, " +
                "check that both files are attached", () -> {
            //  workaround for Selenoid issue with multiple files upload
            kycApprovalPage.signedOffParticipationAgreementUploadInput.setValue(EMPTY_STRING);
            var additionalFileToUpload = getAttachmentResourceFile(additionalFileName);
            kycApprovalPage.signedOffParticipationAgreementUploadInput.uploadFile(additionalFileToUpload);

            var expectedAttachmentLinkForTwoFiles = String.format(MULTIPLE_FILES_LABEL_PATTERN, 2);
            kycApprovalPage.signedOffParticipationAgreementUploadedFilesLink
                    .shouldHave(exactTextCaseSensitive(expectedAttachmentLinkForTwoFiles));
        });

        step("5. Click 'Save' button in KYC Details section and check error on 'Date of Sign off' field", () -> {
            kycApprovalPage.kycDetailsSaveButton.click();
            kycApprovalPage.signedOffDatePicker.getErrorMessage().shouldHave(textCaseSensitive(MISSING_SIGNED_OFF_DATE_ERROR));
        });

        step("6. Populate 'Billing Address' section fields, set 'Date of Sign Off', " +
                "and save changes in 'KYC Details' block", () -> {
            steps.kycApproval.populateBillingAddressSectionWithIndiaAddress();
            kycApprovalPage.signedOffDatePicker.setTomorrowDate();
            kycApprovalPage.kycDetailsSaveChanges();
        });

        step("7. Delete all files from 'Signed off Participation Agreement' section via API, " +
                "check that 'Date of Sign off' field is cleared and disabled, and save changes in the KYC section", () -> {
            kycApprovalPage.signedOffParticipationAgreementUploadedFilesLink.click();
            var attachmentFileNamesList = List.of(steps.kycApproval.signedOffParticipationAgreementFileName, additionalFileName);
            kycApprovalPage.kycDetailsAttachmentsModal.removeAttachments(attachmentFileNamesList);

            kycApprovalPage.signedOffDatePicker.getInput().shouldBe(empty, disabled);
            kycApprovalPage.kycDetailsSaveChanges();

            kycApprovalPage.notification.shouldNot(exist);
        });
    }
}
