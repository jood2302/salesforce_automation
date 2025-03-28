package ngbs.approvals;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.KycApprovalCreationModal.KYC_APPROVAL_DATES_FORMATTER;
import static com.aquiva.autotests.rc.page.salesforce.approval.KycApprovalPage.KYC_DETAILS_DATES_FORMATTER;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContentVersionFactory.createAttachmentForSObject;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.LocalSubscribedAddressFactory.createIndiaLocalSubscribedAddressRecord;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LocalSubscribedAddressHelper.LOCAL_SUBSCRIBED_ADDRESS_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LocalSubscribedAddressHelper.REGISTERED_ADDRESS_RECORD_TYPE;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("IndiaMVP")
public class CopyDataFromKycApprovalTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Approval__c kycApproval;
    private Approval__c kycApprovalUpdated;
    private Approval__c newKycApproval;
    private String identityDocumentOfCompanyDateOfIssue;
    private String photoIdProofTypeDateOfIssue;
    private String authorizationLetterDateOfIssue;
    private User salesUserWithEditKycApprovalPermissionSet;

    //  Test data
    private final boolean isGstExemptionSezCustomer;

    public CopyDataFromKycApprovalTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_IndiaAndUS_MVP_Monthly_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        isGstExemptionSezCustomer = true;
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
    @TmsLink("CRM-23971")
    @DisplayName("CRM-23971 - Copying data during manual KYC Approval request creation (all data)")
    @Description("Verify that subsequent KYC Approval requests are created with some data " +
            "inherited from previous KYC Approval request")
    public void test() {
        step("1. Populate non-required fields on the India Account's KYC Approval via API", () -> {
            kycApproval.setRcPointOfPresence__c(UUID.randomUUID().toString());
            kycApproval.setTypeOfCustomer__c(B2B_CUSTOMER_TYPE);
            kycApproval.setGstExemptionSezCustomer__c(isGstExemptionSezCustomer);
            kycApproval.setGst__c(GST_REGULAR_TYPE);
            kycApproval.setOther__c(UUID.randomUUID().toString());
            kycApproval.setCoDoSoWoHo__c(UUID.randomUUID().toString());
            kycApproval.setLandmark__c(UUID.randomUUID().toString());

            enterpriseConnectionUtils.update(kycApproval);
        });

        step("2. Upload attachments and populate all fields in the 'KYC Details' block, " +
                "and prepare the KYC record for approval", () -> {
            step("Populate required fields and add attachments in " +
                    "'Signed off Participation Agreement', 'GST No. & Certificate', " +
                    "'SEZ Exemption Certificate & Number' sections of 'KYC Details' block via API", () -> {
                var signOffAgreementAttachment = createAttachmentForSObject(
                        kycApproval.getId(), steps.kycApproval.signedOffParticipationAgreementFileName);
                addSignedOffParticipationAgreement(kycApproval, signOffAgreementAttachment);

                var gstNumberAttachment = createAttachmentForSObject(
                        kycApproval.getId(), steps.kycApproval.gstNumberFileName);
                addGstNumber(kycApproval, steps.kycApproval.gstNumber, gstNumberAttachment);
                //  by uploading files via API and already having a valid India address on KYC Details
                //  disabled 'GST State Code of Customer' field might not be populated automatically (but we need it later)
                kycApproval.setGstStateCodeOfCustomer__c(steps.kycApproval.gstStateCodeOfCustomer);
                kycApproval.setGstStateNameOfCustomer__c(steps.kycApproval.gstStateNameOfCustomer);

                var sezCertificateAttachment = createAttachmentForSObject(
                        kycApproval.getId(), steps.kycApproval.sezCertificateExemptionFileName);
                addSezCertificate(kycApproval, sezCertificateAttachment);

                enterpriseConnectionUtils.update(kycApproval);
            });

            kycApprovalPage.openPage(kycApproval.getId());
            steps.kycApproval.populateKycApprovalFieldsRequiredForApproval(kycApproval);

            //  Get values from date fields to check them later on the new KYC Approval
            identityDocumentOfCompanyDateOfIssue = kycApprovalPage.identityDocumentOfCompanyDateOfIssueInput.getInput().getValue();
            photoIdProofTypeDateOfIssue = kycApprovalPage.photoIdProofTypeDateOfIssueInput.getInput().getValue();
            authorizationLetterDateOfIssue = kycApprovalPage.authorisationLetterDateOfIssueInput.getInput().getValue();
        });

        step("3. Add Local Subscribed Address records of 'Registered Address of Company' " +
                "and 'Local Subscribed Address of Company' types via API", () -> {
            createIndiaLocalSubscribedAddressRecord(kycApproval, REGISTERED_ADDRESS_RECORD_TYPE);
            createIndiaLocalSubscribedAddressRecord(kycApproval, LOCAL_SUBSCRIBED_ADDRESS_RECORD_TYPE);
        });

        step("4. Approve KYC Approval via API", () ->
                enterpriseConnectionUtils.approveSingleRecord(kycApproval.getId())
        );

        step("5. Open Account record page, open new Approval creation modal window " +
                "and select 'KYC Approval Request' approval type", () -> {
            accountRecordPage.openPage(steps.salesFlow.account.getId());
            accountRecordPage.openCreateNewApprovalModal();
            accountRecordPage.newApprovalRecordTypeSelectionModal.selectApprovalType(KYC_APPROVAL_RECORD_TYPE);
            accountRecordPage.newApprovalRecordTypeSelectionModal.nextButton.click();
        });

        step("6. Check that KYC Approval creation modal window fields are populated " +
                "with values from initial KYC approval", () -> {
            kycApprovalUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, RcPointOfPresence__c, PanGirNo__c, GstNo__c, TypeOfCustomer__c, " +
                            "NameOfTheAuthorisedSignatory__c, GenderOfAuthorisedSignatory__c, DateOfSignOff__c, " +
                            "DateOfBirthOfAuthorisedSignatory__c, GstExemptionSezCustomer__c, KycFiles__c, Other__c, " +
                            "NameOfFatherOrHusband__c, NationalityOfAuthorisedSignatory__c, CoDoSoWoHo__c, HouseNo__c, " +
                            "StreetAddress__c, Landmark__c, Area__c, City__c, District__c, State__c, PinCode__c, " +
                            "Billing_City__c, Billing_State__c, Billing_Street_Address__c, Billing_Zip_Code__c, " +
                            "GstStateCodeOfCustomer__c, GstStateNameOfCustomer__c, Gst__c " +
                            "FROM Approval__c " +
                            "WHERE Account__c = '" + steps.salesFlow.account.getId() + "'",
                    Approval__c.class);

            kycApprovalCreationModal.rcPointOfPresenceInput
                    .shouldHave(exactValue(kycApprovalUpdated.getRcPointOfPresence__c()), ofSeconds(10));
            kycApprovalCreationModal.panGirNumberInput.shouldHave(exactValue(kycApprovalUpdated.getPanGirNo__c()));
            kycApprovalCreationModal.gstExemptionSezCustomerInput.shouldBe(checked);
            kycApprovalCreationModal.gstTypeInput.getInput()
                    .shouldHave(exactTextCaseSensitive(kycApprovalUpdated.getGst__c()));
            kycApprovalCreationModal.gstNumberOutput.shouldHave(exactValue(kycApprovalUpdated.getGstNo__c()));
            kycApprovalCreationModal.gstStateCodeOutput
                    .shouldHave(exactValue(kycApprovalUpdated.getGstStateCodeOfCustomer__c()));
            kycApprovalCreationModal.gstStateNameOutput
                    .shouldHave(exactValue(kycApprovalUpdated.getGstStateNameOfCustomer__c()));
            kycApprovalCreationModal.typeOfCustomerInput.getInput()
                    .shouldHave(exactTextCaseSensitive(kycApprovalUpdated.getTypeOfCustomer__c()));
            kycApprovalCreationModal.chosenPurposesForProcuringServicesList
                    .shouldHave(texts(steps.kycApproval.purposeForProcuringTheServiceTestValues));
            kycApprovalCreationModal.otherTextarea.shouldHave(exactValue(kycApprovalUpdated.getOther__c()));
            kycApprovalCreationModal.nameOfFatherOrHusbandInput
                    .shouldHave(exactValue(kycApprovalUpdated.getNameOfFatherOrHusband__c()));
            kycApprovalCreationModal.nameOfTheAuthorisedSignatoryInput
                    .shouldHave(exactValue(kycApprovalUpdated.getNameOfTheAuthorisedSignatory__c()));
            kycApprovalCreationModal.genderOfAuthorisedSignatoryInput.getInput()
                    .shouldHave(exactTextCaseSensitive(kycApprovalUpdated.getGenderOfAuthorisedSignatory__c()));

            var expectedDateOfBirthOfAuthorisedSignatory = KYC_APPROVAL_DATES_FORMATTER
                    .format(kycApprovalUpdated.getDateOfBirthOfAuthorisedSignatory__c().toInstant().atZone(UTC));
            kycApprovalCreationModal.dateOfBirthOfAuthorisedSignatoryInput
                    .shouldHave(exactValue(expectedDateOfBirthOfAuthorisedSignatory));

            kycApprovalCreationModal.nationalityOfAuthorisedSignatoryOutput
                    .shouldHave(exactTextCaseSensitive(kycApprovalUpdated.getNationalityOfAuthorisedSignatory__c()));
            kycApprovalCreationModal.coDoSoWoHoInput.shouldHave(exactValue(kycApprovalUpdated.getCoDoSoWoHo__c()));
            kycApprovalCreationModal.houseNoInput.shouldHave(exactValue(kycApprovalUpdated.getHouseNo__c()));
            kycApprovalCreationModal.streetAddressInput.shouldHave(exactValue(kycApprovalUpdated.getStreetAddress__c()));
            kycApprovalCreationModal.landmarkInput.shouldHave(exactValue(kycApprovalUpdated.getLandmark__c()));
            kycApprovalCreationModal.areaSectorLocalityInput.shouldHave(exactValue(kycApprovalUpdated.getArea__c()));
            kycApprovalCreationModal.villageTownCityInput.shouldHave(exactValue(kycApprovalUpdated.getCity__c()));
            kycApprovalCreationModal.districtInput.shouldHave(exactValue(kycApprovalUpdated.getDistrict__c()));
            kycApprovalCreationModal.stateUtInput.getInput()
                    .shouldHave(exactTextCaseSensitive(kycApprovalUpdated.getState__c()));
            kycApprovalCreationModal.pinCodeInput.shouldHave(exactValue(kycApprovalUpdated.getPinCode__c()));
        });

        step("7. Populate 'Approval Name' field and click 'Save' button", () -> {
            var uniqueName = UUID.randomUUID().toString();
            kycApprovalCreationModal.approvalNameInput.setValue(uniqueName);

            kycApprovalCreationModal.saveChanges();
            kycApprovalPage.waitUntilLoaded();
        });

        step("8. Check that fields in 'KYC Details' on all sections are populated with the same values " +
                "as on initial KYC Approval", () -> {
            kycApprovalPage.identityDocumentOfCompanySection.click();
            kycApprovalPage.identityDocumentOfCompanyDocumentNoInput
                    .shouldBe(enabled, ofSeconds(20))
                    .shouldHave(exactValue(steps.kycApproval.identityDocumentOfCompanyDocumentNumberValue));
            kycApprovalPage.identityDocumentOfCompanyPlaceOfIssueInput
                    .shouldHave(exactValue(steps.kycApproval.identityDocumentOfCompanyPlaceOfIssueValue));
            kycApprovalPage.identityDocumentOfCompanyIssuingAuthorityInput
                    .shouldHave(exactValue(steps.kycApproval.identityDocumentOfCompanyIssuingAuthorityValue));
            kycApprovalPage.identityDocumentOfCompanyDateOfIssueInput.getInput()
                    .shouldHave(exactValue(identityDocumentOfCompanyDateOfIssue));

            kycApprovalPage.photoIdProofTypeSection.click();
            kycApprovalPage.photoIdProofTypeDocumentNoInput
                    .shouldHave(exactValue(steps.kycApproval.photoIdProofTypeDocumentNumberValue));
            kycApprovalPage.photoIdProofTypePlaceOfIssueInput
                    .shouldHave(exactValue(steps.kycApproval.photoIdProofTypePlaceOfIssueValue));
            kycApprovalPage.photoIdProofTypeIssuingAuthorityInput
                    .shouldHave(exactValue(steps.kycApproval.photoIdProofTypeIssuingAuthorityValue));
            kycApprovalPage.photoIdProofTypeDateOfIssueInput.getInput()
                    .shouldHave(exactValue(photoIdProofTypeDateOfIssue));

            kycApprovalPage.authorisationLetterSection.click();
            kycApprovalPage.authorisationLetterDocumentNoInput
                    .shouldHave(exactValue(steps.kycApproval.authorizationLetterDocumentNumberValue));
            kycApprovalPage.authorisationLetterPlaceOfIssueInput
                    .shouldHave(exactValue(steps.kycApproval.authorizationLetterPlaceOfIssueValue));
            kycApprovalPage.authorisationLetterIssuingAuthorityInput
                    .shouldHave(exactValue(steps.kycApproval.authorizationLetterIssuingAuthorityValue));
            kycApprovalPage.authorisationLetterDateOfIssueInput.getInput()
                    .shouldHave(exactValue(authorizationLetterDateOfIssue));

            kycApprovalPage.signedOffParticipationAgreementSection.click();
            var expectedSignOffDate = KYC_DETAILS_DATES_FORMATTER
                    .format(kycApprovalUpdated.getDateOfSignOff__c().toInstant().atZone(UTC));
            kycApprovalPage.signedOffDatePicker.getInput().shouldHave(exactValue(expectedSignOffDate));

            kycApprovalPage.gstNumberSection.click();
            kycApprovalPage.gstNumberInput.shouldHave(exactValue(kycApprovalUpdated.getGstNo__c()));
            kycApprovalPage.gstStateCodeOfCustomerInput
                    .shouldHave(exactValue(kycApprovalUpdated.getGstStateCodeOfCustomer__c()));

            kycApprovalPage.billingAddressSection.click();
            kycApprovalPage.billingStreetInput.shouldHave(exactValue(kycApprovalUpdated.getBilling_Street_Address__c()));
            kycApprovalPage.billingPostalCodeInput.shouldHave(exactValue(kycApprovalUpdated.getBilling_Zip_Code__c()));
            kycApprovalPage.billingCityInput.shouldHave(exactValue(kycApprovalUpdated.getBilling_City__c()));
            kycApprovalPage.billingStateInput.shouldHave(exactValue(kycApprovalUpdated.getBilling_State__c()));
        });

        step("9. Check that other KYC Approval fields are populated correctly from the initial KYC Approval", () -> {
            newKycApproval = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, RcPointOfPresence__c, PanGirNo__c, GstNo__c, TypeOfCustomer__c, " +
                            "NameOfTheAuthorisedSignatory__c, GenderOfAuthorisedSignatory__c, DateOfSignOff__c, " +
                            "DateOfBirthOfAuthorisedSignatory__c, GstExemptionSezCustomer__c, KycFiles__c, Other__c, " +
                            "NameOfFatherOrHusband__c, NationalityOfAuthorisedSignatory__c, CoDoSoWoHo__c, HouseNo__c, " +
                            "StreetAddress__c, Landmark__c, Area__c, City__c, District__c, State__c, PinCode__c, " +
                            "Billing_City__c, Billing_State__c, Billing_Street_Address__c, Billing_Zip_Code__c, " +
                            "GstStateCodeOfCustomer__c, GstStateNameOfCustomer__c, Gst__c " +
                            "FROM Approval__c " +
                            "WHERE Account__c = '" + steps.salesFlow.account.getId() + "'" +
                            "ORDER BY CreatedDate " +
                            "LIMIT 1",
                    Approval__c.class);

            assertThat(newKycApproval.getRcPointOfPresence__c())
                    .as("New KYC Approval__c.RcPointOfPresence__c value")
                    .isEqualTo(kycApprovalUpdated.getRcPointOfPresence__c());
            assertThat(newKycApproval.getPanGirNo__c())
                    .as("New KYC Approval__c.PanGirNo__c value")
                    .isEqualTo(kycApprovalUpdated.getPanGirNo__c());
            assertThat(newKycApproval.getGstNo__c())
                    .as("New KYC Approval__c.GstNo__c value")
                    .isEqualTo(kycApprovalUpdated.getGstNo__c());
            assertThat(newKycApproval.getTypeOfCustomer__c())
                    .as("New KYC Approval__c.TypeOfCustomer__c value")
                    .isEqualTo(kycApprovalUpdated.getTypeOfCustomer__c());
            assertThat(newKycApproval.getNameOfTheAuthorisedSignatory__c())
                    .as("New KYC Approval__c.NameOfTheAuthorisedSignatory__c value")
                    .isEqualTo(kycApprovalUpdated.getNameOfTheAuthorisedSignatory__c());
            assertThat(newKycApproval.getGenderOfAuthorisedSignatory__c())
                    .as("New KYC Approval__c.GenderOfAuthorisedSignatory__c value")
                    .isEqualTo(kycApprovalUpdated.getGenderOfAuthorisedSignatory__c());
            assertThat(newKycApproval.getDateOfSignOff__c())
                    .as("New KYC Approval__c.DateOfSignOff__c value")
                    .isEqualTo(kycApprovalUpdated.getDateOfSignOff__c());
            assertThat(newKycApproval.getDateOfBirthOfAuthorisedSignatory__c())
                    .as("New KYC Approval__c.DateOfBirthOfAuthorisedSignatory__c value")
                    .isEqualTo(kycApprovalUpdated.getDateOfBirthOfAuthorisedSignatory__c());
            assertThat(newKycApproval.getGstExemptionSezCustomer__c())
                    .as("New KYC Approval__c.GstExemptionSezCustomer__c value")
                    .isEqualTo(kycApprovalUpdated.getGstExemptionSezCustomer__c());
            assertThat(newKycApproval.getKycFiles__c())
                    .as("New KYC Approval__c.KycFiles__c value")
                    .isEqualTo(kycApprovalUpdated.getKycFiles__c());
            assertThat(newKycApproval.getOther__c())
                    .as("New KYC Approval__c.Other__c value")
                    .isEqualTo(kycApprovalUpdated.getOther__c());
            assertThat(newKycApproval.getNameOfFatherOrHusband__c())
                    .as("New KYC Approval__c.NameOfFatherOrHusband__c value")
                    .isEqualTo(kycApprovalUpdated.getNameOfFatherOrHusband__c());
            assertThat(newKycApproval.getNationalityOfAuthorisedSignatory__c())
                    .as("New KYC Approval__c.NationalityOfAuthorisedSignatory__c value")
                    .isEqualTo(kycApprovalUpdated.getNationalityOfAuthorisedSignatory__c());
            assertThat(newKycApproval.getCoDoSoWoHo__c())
                    .as("New KYC Approval__c.CoDoSoWoHo__c value")
                    .isEqualTo(kycApprovalUpdated.getCoDoSoWoHo__c());
            assertThat(newKycApproval.getHouseNo__c())
                    .as("New KYC Approval__c.HouseNo__c value")
                    .isEqualTo(kycApprovalUpdated.getHouseNo__c());
            assertThat(newKycApproval.getStreetAddress__c())
                    .as("New KYC Approval__c.StreetAddress__c value")
                    .isEqualTo(kycApprovalUpdated.getStreetAddress__c());
            assertThat(newKycApproval.getLandmark__c())
                    .as("New KYC Approval__c.Landmark__c value")
                    .isEqualTo(kycApprovalUpdated.getLandmark__c());
            assertThat(newKycApproval.getArea__c())
                    .as("New KYC Approval__c.Area__c value")
                    .isEqualTo(kycApprovalUpdated.getArea__c());
            assertThat(newKycApproval.getCity__c())
                    .as("New KYC Approval__c.City__c value")
                    .isEqualTo(kycApprovalUpdated.getCity__c());
            assertThat(newKycApproval.getDistrict__c())
                    .as("New KYC Approval__c.District__c value")
                    .isEqualTo(kycApprovalUpdated.getDistrict__c());
            assertThat(newKycApproval.getState__c())
                    .as("New KYC Approval__c.State__c value")
                    .isEqualTo(kycApprovalUpdated.getState__c());
            assertThat(newKycApproval.getPinCode__c())
                    .as("New KYC Approval__c.PinCode__c value")
                    .isEqualTo(kycApprovalUpdated.getPinCode__c());
            assertThat(newKycApproval.getBilling_City__c())
                    .as("New KYC Approval__c.Billing_City__c value")
                    .isEqualTo(kycApprovalUpdated.getBilling_City__c());
            assertThat(newKycApproval.getBilling_State__c())
                    .as("New KYC Approval__c.Billing_State__c value")
                    .isEqualTo(kycApprovalUpdated.getBilling_State__c());
            assertThat(newKycApproval.getBilling_Street_Address__c())
                    .as("New KYC Approval__c.Billing_Street_Address__c value")
                    .isEqualTo(kycApprovalUpdated.getBilling_Street_Address__c());
            assertThat(newKycApproval.getBilling_Zip_Code__c())
                    .as("New KYC Approval__c.Billing_Zip_Code__c value")
                    .isEqualTo(kycApprovalUpdated.getBilling_Zip_Code__c());
            assertThat(newKycApproval.getGstStateCodeOfCustomer__c())
                    .as("New KYC Approval__c.GstStateCodeOfCustomer__c value")
                    .isEqualTo(kycApprovalUpdated.getGstStateCodeOfCustomer__c());
            assertThat(newKycApproval.getGstStateNameOfCustomer__c())
                    .as("New KYC Approval__c.GstStateNameOfCustomer__c value")
                    .isEqualTo(kycApprovalUpdated.getGstStateNameOfCustomer__c());
            assertThat(newKycApproval.getGst__c())
                    .as("New KYC Approval__c.Gst__c value")
                    .isEqualTo(kycApprovalUpdated.getGst__c());
        });

        step("10. Check that all attachments on a new KYC Approval are copied from initial KYC Approval", () -> {
            var initialKycFileIds = List.of(kycApprovalUpdated.getKycFiles__c()
                    .replace(",", EMPTY_STRING)
                    .split(KYC_FILES_DELIMITER));
            var newKycFileIds = List.of(newKycApproval.getKycFiles__c()
                    .replace(",", EMPTY_STRING)
                    .split(KYC_FILES_DELIMITER));
            var initialKycFileIdList = initialKycFileIds.stream()
                    .collect(joining("','", "('", "')"));

            assertThat(newKycFileIds.size())
                    .as("Number of attachments on a new KYC Approval")
                    .isEqualTo(initialKycFileIds.size());

            for (var newKycFileId : newKycFileIds) {
                step("Check the attachment on the new KYC Approval with ContentDocumentId = '" +
                        newKycFileId + "'", () -> {
                    var newKycContentVersionRecord = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, Title, VersionData " +
                                    "FROM ContentVersion " +
                                    "WHERE ContentDocumentId = '" + newKycFileId + "'",
                            ContentVersion.class);
                    var initialKycContentVersionRecord = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, VersionData " +
                                    "FROM ContentVersion " +
                                    "WHERE Title = '" + newKycContentVersionRecord.getTitle() + "' " +
                                    "AND ContentDocumentId IN " + initialKycFileIdList,
                            //  without ContentDocumentId or ID no records will be returned
                            ContentVersion.class);
                    assertThat(newKycContentVersionRecord.getVersionData())
                            .as("ContentVersion.VersionData of a new KYC Approval attachment (binary data) " +
                                    "with title = '%s'", newKycContentVersionRecord.getTitle())
                            .isEqualTo(initialKycContentVersionRecord.getVersionData());
                });
            }
        });

        step("11. Check that Local Subscribed Address records on a new KYC Approval " +
                "are copied from initial KYC approval and their fields are populated with the same values", () -> {
            var initialLocalSubscribedAddressRecords = enterpriseConnectionUtils.query(
                    "SELECT Id, RecordTypeId, City__c, District__c, PinCode__c, State__c, StreetAddress__c " +
                            "FROM LocalSubscribedAddress__c " +
                            "WHERE Approval__c = '" + kycApproval.getId() + "' " +
                            "ORDER BY RecordTypeId DESC",
                    LocalSubscribedAddress__c.class);

            var newLocalSubscribedAddressRecords = enterpriseConnectionUtils.query(
                    "SELECT Id, RecordTypeId, City__c, District__c, PinCode__c, State__c, StreetAddress__c " +
                            "FROM LocalSubscribedAddress__c " +
                            "WHERE Approval__c = '" + newKycApproval.getId() + "' " +
                            "ORDER BY RecordTypeId DESC",
                    LocalSubscribedAddress__c.class);

            assertThat(newLocalSubscribedAddressRecords.size())
                    .as("Number of LocalSubscribedAddress__c records on the new KYC Approval")
                    .isEqualTo(initialLocalSubscribedAddressRecords.size());

            for (int i = 0; i < newLocalSubscribedAddressRecords.size(); i++) {
                step("Check LocalSubscribedAddress__c record with Id = "
                        + newLocalSubscribedAddressRecords.get(i).getId());

                assertThat(newLocalSubscribedAddressRecords.get(i).getRecordTypeId())
                        .as("LocalSubscribedAddress__c.RecordTypeId value for the new KYC Approval")
                        .isEqualTo(initialLocalSubscribedAddressRecords.get(i).getRecordTypeId());

                assertThat(newLocalSubscribedAddressRecords.get(i).getDistrict__c())
                        .as("LocalSubscribedAddress__c.District__c value for the new KYC Approval")
                        .isEqualTo(initialLocalSubscribedAddressRecords.get(i).getDistrict__c());

                assertThat(newLocalSubscribedAddressRecords.get(i).getState__c())
                        .as("LocalSubscribedAddress__c.State__c value for the new KYC Approval")
                        .isEqualTo(initialLocalSubscribedAddressRecords.get(i).getState__c());

                assertThat(newLocalSubscribedAddressRecords.get(i).getCity__c())
                        .as("LocalSubscribedAddress__c.City__c value for the new KYC Approval")
                        .isEqualTo(initialLocalSubscribedAddressRecords.get(i).getCity__c());

                assertThat(newLocalSubscribedAddressRecords.get(i).getStreetAddress__c())
                        .as("LocalSubscribedAddress__c.StreetAddress__c value for the new KYC Approval")
                        .isEqualTo(initialLocalSubscribedAddressRecords.get(i).getStreetAddress__c());

                assertThat(newLocalSubscribedAddressRecords.get(i).getPinCode__c())
                        .as("LocalSubscribedAddress__c.PinCode__c value for the new KYC Approval")
                        .isEqualTo(initialLocalSubscribedAddressRecords.get(i).getPinCode__c());
            }
        });
    }
}
