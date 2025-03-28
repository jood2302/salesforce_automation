package ngbs.approvals;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Approval__c;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createKycApproval;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createTeaApproval;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContentVersionFactory.createAttachmentForSObject;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.LocalSubscribedAddressFactory.createIndiaLocalSubscribedAddressRecord;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LocalSubscribedAddressHelper.REGISTERED_ADDRESS_RECORD_TYPE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("Approval")
@Tag("IndiaMVP")
@Tag("TaxExemption")
public class CreateTaxExemptApprovalForIndiaTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Approval__c defaultKycApproval;
    private Approval__c newKycApproval;
    private Approval__c teaApproval;
    private User salesUserWithEditKycApprovalPermissionSet;

    //  Test data
    private final String newGstNumber;
    private final String newGstNumberFileName;
    private final String newSezCertificateFileName;

    public CreateTaxExemptApprovalForIndiaTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/RC_MVP_India_Monthly_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        newGstNumber = "29ADLFS9935J1Z9";
        newGstNumberFileName = "rc.png";
        newSezCertificateFileName = "sezCertificateNew.jpg";
    }

    @BeforeEach
    public void setUpTest() {
        salesUserWithEditKycApprovalPermissionSet = steps.kycApproval.getSalesUserWithKycApprovalPermissionSet();

        steps.salesFlow.createAccountWithContactAndContactRole(salesUserWithEditKycApprovalPermissionSet);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact,
                salesUserWithEditKycApprovalPermissionSet);

        defaultKycApproval = steps.kycApproval.changeOwnerOfDefaultKycApproval(
                salesUserWithEditKycApprovalPermissionSet.getId(), steps.salesFlow.account.getId());

        step("Login as a user with 'Sales Rep - Lightning' profile with 'KYC_Approval_Edit' permission set", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(salesUserWithEditKycApprovalPermissionSet)
        );
    }

    @Test
    @TmsLink("CRM-25080")
    @DisplayName("CRM-25080 - India Tax Exemption Approval (TEA): The data for TEA received from the last KYC approval")
    @Description("Verify receiving GST Number and SEZ certificate from new KYC approval" +
            "(the new KYC approvals data differ from previous KYC approval)")
    public void test() {
        step("1. Add Signed Off Participation Agreement, GST Number and SEZ Certificate " +
                "to the KYC Approval (that was created automatically) via API", () -> {
            var signOffAgreementAttachment = createAttachmentForSObject(
                    defaultKycApproval.getId(), steps.kycApproval.signedOffParticipationAgreementFileName);
            addSignedOffParticipationAgreement(defaultKycApproval, signOffAgreementAttachment);

            var gstNumberAttachment = createAttachmentForSObject(
                    defaultKycApproval.getId(), steps.kycApproval.gstNumberFileName);
            addGstNumber(defaultKycApproval, steps.kycApproval.gstNumber, gstNumberAttachment);

            var sezCertificateAttachment = createAttachmentForSObject(
                    defaultKycApproval.getId(), steps.kycApproval.sezCertificateExemptionFileName);
            addSezCertificate(defaultKycApproval, sezCertificateAttachment);

            enterpriseConnectionUtils.update(defaultKycApproval);
        });

        step("2. Open KYC Approval page, add attachments to the remaining sections in 'KYC Details' block, " +
                "set other required fields via API to prepare the KYC record for approval", () -> {
            kycApprovalPage.openPage(defaultKycApproval.getId());
            steps.kycApproval.populateKycApprovalFieldsRequiredForApproval(defaultKycApproval);
        });

        step("3. Create a new Local Subscribed Address record of 'Registered Address of Company' type " +
                "for KYC Approval via API", () -> {
            createIndiaLocalSubscribedAddressRecord(defaultKycApproval, REGISTERED_ADDRESS_RECORD_TYPE);
        });

        step("4. Approve the KYC Approval via API", () ->
                enterpriseConnectionUtils.approveSingleRecord(defaultKycApproval.getId())
        );

        step("5. Create a second KYC Approval with Signed Off Participation Agreement, " +
                "add GST number and SEZ Certificate to it via API", () -> {
            newKycApproval = createKycApproval(steps.quoteWizard.opportunity, steps.salesFlow.account,
                    steps.kycApproval.signedOffParticipationAgreementFileName,
                    salesUserWithEditKycApprovalPermissionSet.getId());

            var newGstNumberAttachment = createAttachmentForSObject(
                    newKycApproval.getId(), newGstNumberFileName);
            addGstNumber(newKycApproval, newGstNumber, newGstNumberAttachment);

            var newSezCertificateAttachment = createAttachmentForSObject(
                    newKycApproval.getId(), newSezCertificateFileName);
            addSezCertificate(newKycApproval, newSezCertificateAttachment);

            enterpriseConnectionUtils.update(newKycApproval);
        });

        step("6. Open KYC Approval page and add all the necessary data to prepare KYC record for approval ", () -> {
            //  need to populate separately as KYC has default billing address (US)
            step("Open the KYC Approval page and populate all fields in 'Billing Address' section in 'KYC Details' block", () -> {
                kycApprovalPage.openPage(newKycApproval.getId());
                steps.kycApproval.populateBillingAddressSectionWithIndiaAddress();
            });

            steps.kycApproval.populateKycApprovalFieldsRequiredForApproval(newKycApproval);
        });

        step("7. Approve the new KYC Approval via API", () ->
                enterpriseConnectionUtils.approveSingleRecord(newKycApproval.getId())
        );

        step("8. Create Tax Exempt Approval via API and check that it's approved automatically", () -> {
            teaApproval = createTeaApproval(steps.salesFlow.account.getId(), steps.quoteWizard.opportunity.getId(),
                    steps.salesFlow.contact.getId(),
                    salesUserWithEditKycApprovalPermissionSet.getId());

            var teaApprovalUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Status__c " +
                            "FROM Approval__c " +
                            "WHERE Id = '" + teaApproval.getId() + "'",
                    Approval__c.class);

            assertThat(teaApprovalUpdated.getStatus__c())
                    .as("TEA Approval.Status__c value")
                    .isEqualTo(APPROVAL_STATUS_APPROVED);
        });

        step("9. Open Tax Exempt Approval page, switch to the Tax Exemption Manager, " +
                "and verify that GST Number and SEZ certificate are received from the second (latest) KYC approval", () -> {
            teaApprovalPage.openPage(teaApproval.getId());
            teaApprovalPage.switchToTaxExemptionIframe();

            taxExemptionManagerPage.gstNumberInput
                    .shouldHave(exactValue(newGstNumber), ofSeconds(30))
                    .shouldBe(disabled);
            taxExemptionManagerPage.sezCertificateLink.shouldHave(exactTextCaseSensitive(newSezCertificateFileName));
        });
    }
}
