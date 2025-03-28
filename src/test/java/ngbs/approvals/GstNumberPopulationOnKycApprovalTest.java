package ngbs.approvals;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.Approval__c;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.kycApprovalPage;
import static com.aquiva.autotests.rc.page.salesforce.approval.KycApprovalPage.INCORRECT_GST_NUMBER_ERROR;
import static com.aquiva.autotests.rc.page.salesforce.approval.KycApprovalPage.getGstNumberDoesNotMatchBillingStateError;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContentVersionFactory.createAttachmentForSObject;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.addGstNumber;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("IndiaMVP")
public class GstNumberPopulationOnKycApprovalTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Approval__c kycApproval;

    //  Test data
    private final String indiaBillingStateMaharashtra;
    private final String indiaBillingStateSikkim;
    private final String indiaBillingCity;
    private final String indiaBillingStreet;
    private final String indiaBillingPostalCode;

    private final String incorrectDigitalGstNumberValue;
    private final String nonMatchingGstNumberValue;
    private final String indiaBillingStateMaharashtraGstNumberValue;
    private final String indiaBillingStateSikkimGstNumberValue;

    private final String gstNumberFileName;

    public GstNumberPopulationOnKycApprovalTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_IndiaAndUS_MVP_Monthly_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        indiaBillingStateMaharashtra = "MH";
        indiaBillingStateSikkim = "SK";
        indiaBillingCity = "Mumbai";
        indiaBillingStreet = "A/85, Deewan Shopping Cent";
        indiaBillingPostalCode = "400102";

        incorrectDigitalGstNumberValue = "1";
        nonMatchingGstNumberValue = "89AAAAA01";
        indiaBillingStateMaharashtraGstNumberValue = "27AAAAAA001";
        indiaBillingStateSikkimGstNumberValue = "11AAAAA001";

        gstNumberFileName = "rc.png";
    }

    @BeforeEach
    public void setUpTest() {
        var testUser = steps.kycApproval.getSalesUserWithKycApprovalPermissionSet();

        steps.salesFlow.createAccountWithContactAndContactRole(testUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, testUser);

        kycApproval = steps.kycApproval.changeOwnerOfDefaultKycApproval(testUser.getId(), steps.salesFlow.account.getId());

        step("Clear Billing Address on the Account via API", () -> {
            var accountToUpdate = new Account();
            accountToUpdate.setId(steps.salesFlow.account.getId());
            accountToUpdate.setFieldsToNull(new String[]{
                    "BillingCountry", "BillingCity", "BillingStreet", "BillingState", "BillingPostalCode"
            });

            enterpriseConnectionUtils.update(accountToUpdate);
        });

        step("Login as a user with 'Sales Rep - Lightning' profile with 'Edit KYC Approval' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(testUser);
        });
    }

    @Test
    @TmsLink("CRM-24050")
    @DisplayName("CRM-24050 - Populating GST Number")
    @Description("Verify that GST Number is validated upon save on KYC and incorrect GST Number doesn't pass validation")
    public void test() {
        step("1. Open India Account's KYC Approval page", () -> {
            kycApprovalPage.openPage(kycApproval.getId());
        });

        step("2. Populate fields in 'Billing Address' section in 'KYC Details' block (with 'MH' billing state) " +
                "and click 'Save' button", () -> {
            steps.kycApproval.populateBillingAddressSectionWithIndiaAddress(
                    indiaBillingStreet, indiaBillingCity, indiaBillingStateMaharashtra, indiaBillingPostalCode);
            kycApprovalPage.kycDetailsSaveChanges();
        });

        step("3. Upload file to the 'GST No. & Certificate' section via API", () -> {
            var gstNumberAttachment = createAttachmentForSObject(
                    kycApproval.getId(), gstNumberFileName);
            addGstNumber(kycApproval, EMPTY_STRING, gstNumberAttachment);
            enterpriseConnectionUtils.update(kycApproval);
        });

        step("4. Refresh the KYC Approval page, switch to the 'KYC Details' block, " +
                "populate 'GST No' field with value that doesn't match any billing state, " +
                "click 'Save' button and check error notification message", () -> {
            refresh();
            kycApprovalPage.gstNumberUploadedFilesLink.shouldHave(exactTextCaseSensitive(gstNumberFileName), ofSeconds(20));
            kycApprovalPage.gstNumberSection.click();
            stepCheckErrorNotificationMessage(nonMatchingGstNumberValue, INCORRECT_GST_NUMBER_ERROR);
        });

        step("5. Populate 'GST No' field with '" + TEST_STRING + "' value, click 'Save' button " +
                "and check error notification message", () ->
                stepCheckErrorNotificationMessage(TEST_STRING, INCORRECT_GST_NUMBER_ERROR)
        );

        step("6. Populate 'GST No' field with '" + incorrectDigitalGstNumberValue + "' value, click 'Save' button " +
                "and check error notification message", () ->
                stepCheckErrorNotificationMessage(incorrectDigitalGstNumberValue, INCORRECT_GST_NUMBER_ERROR)
        );

        step("7. Populate 'GST No' field with value that doesn't match '" + indiaBillingStateMaharashtra + "' state " +
                "but matches any other valid billing state, click 'Save' button and check error notification message", () -> {
            var errorMessage = getGstNumberDoesNotMatchBillingStateError(indiaBillingStateSikkim);
            stepCheckErrorNotificationMessage(indiaBillingStateSikkimGstNumberValue, errorMessage);
        });

        step("8. Populate 'GST No' field with value which matches the value in the 'Billing State' field ('MH'), " +
                "click 'Save' button and check absence of error notifications", () -> {
            kycApprovalPage.gstNumberInput.setValue(indiaBillingStateMaharashtraGstNumberValue);
            kycApprovalPage.kycDetailsSaveChanges();
            kycApprovalPage.notification.shouldNot(exist);
        });
    }

    /**
     * Check that expected error notification appears after populating 'Gst No' field with incorrect value
     * and attempting to save changes.
     *
     * @param gstNumberTestValue   any invalid GST Number that causes an error while saving
     * @param expectedErrorMessage error notification message that's expected to appear after the attempt to save changes
     */
    private void stepCheckErrorNotificationMessage(String gstNumberTestValue, String expectedErrorMessage) {
        kycApprovalPage.gstNumberInput
                .shouldBe(enabled, ofSeconds(10))
                .setValue(gstNumberTestValue);
        kycApprovalPage.kycDetailsSaveButton.click();
        kycApprovalPage.notification.shouldHave(exactTextCaseSensitive(expectedErrorMessage), ofSeconds(60));
        kycApprovalPage.notificationCloseButton.click();
    }
}
