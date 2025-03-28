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

import java.util.UUID;

import static base.Pages.kycApprovalPage;
import static com.aquiva.autotests.rc.page.salesforce.approval.KycApprovalPage.GUJARAT_SHORT_BILLING_STATE;
import static com.aquiva.autotests.rc.page.salesforce.approval.KycApprovalPage.INCORRECT_INDIA_STATE_ERROR;
import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ContentVersionFactory.createAttachmentForSObject;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.LocalSubscribedAddressFactory.createIndiaLocalSubscribedAddressRecord;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.addSignedOffParticipationAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LocalSubscribedAddressHelper.REGISTERED_ADDRESS_RECORD_TYPE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("P1")
@Tag("IndiaMVP")
public class BillingAddressOnKycApprovalTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Approval__c kycApproval;

    //  Test data
    private final String incorrectStateValue;
    private final String fullStateValue;

    private final String indiaBillingStreetNew;
    private final String indiaBillingCityNew;
    private final String indiaBillingStateNew;
    private final String indiaBillingPostalCodeNew;

    private final String signedOffParticipationAgreementFileName;

    public BillingAddressOnKycApprovalTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/RC_India_Mumbai_MVP_Monthly_Contract_QOP.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        incorrectStateValue = UUID.randomUUID().toString();
        fullStateValue = steps.quoteWizard.indiaAreaCode.state;

        indiaBillingStreetNew = "Lotus Temple Rd, Bahapur, Shambhu Dayal Bagh, Kalkaji";
        indiaBillingCityNew = "New Delhi";
        indiaBillingStateNew = "Delhi";
        indiaBillingPostalCodeNew = "110019";

        signedOffParticipationAgreementFileName = "test4.jpg";
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
    @TmsLink("CRM-24176")
    @DisplayName("CRM-24176 - Populate billing address on the KYC")
    @Description("Verify that billing address can be populated on the KYC and will be copied to the account when KYC is approved")
    public void test() {
        step("1. Open KYC Approval page", () -> {
            kycApprovalPage.openPage(kycApproval.getId());
        });

        step("2. Switch to the 'KYC Details' block, expand 'Billing Address' section, " +
                "and check that there are the same values as on the related Account", () -> {
            kycApprovalPage.billingAddressSection.click();

            kycApprovalPage.billingCityInput
                    .shouldBe(enabled, ofSeconds(20))
                    .shouldHave(exactValue(steps.salesFlow.account.getBillingCity()));
            kycApprovalPage.billingCountryInput.shouldHave(exactValue(steps.salesFlow.account.getBillingCountry()));
            kycApprovalPage.billingStateInput.shouldHave(exactValue(steps.salesFlow.account.getBillingState()));
            kycApprovalPage.billingStreetInput.shouldHave(exactValue(steps.salesFlow.account.getBillingStreet()));
            kycApprovalPage.billingPostalCodeInput.shouldHave(exactValue(steps.salesFlow.account.getBillingPostalCode()));
        });

        step("3. Clear 'State' field, save changes and check that there are no errors after that", () ->
                stepSetNewBillingStateAndCheckAbsenceOfErrorNotifications(EMPTY_STRING)
        );

        step("4. Update State field with incorrect value, click 'Save' button and check error notification", () -> {
            kycApprovalPage.billingStateInput.setValue(incorrectStateValue);
            kycApprovalPage.kycDetailsSaveButton.click();

            kycApprovalPage.notification.shouldHave(exactTextCaseSensitive(INCORRECT_INDIA_STATE_ERROR), ofSeconds(10));
            kycApprovalPage.notificationCloseButton.click();
        });

        step("5. Set 'State' = '" + GUJARAT_SHORT_BILLING_STATE + "', " +
                "save changes and check that there are no errors after that", () ->
                stepSetNewBillingStateAndCheckAbsenceOfErrorNotifications(GUJARAT_SHORT_BILLING_STATE)
        );

        step("6. Set 'State' = '" + fullStateValue + "', " +
                "save changes and check that there are no errors after that", () ->
                stepSetNewBillingStateAndCheckAbsenceOfErrorNotifications(fullStateValue)
        );

        step("7. Check if Approval__c.GstStateNameOfCustomer__c value is equal to '" + fullStateValue + "'", () -> {
            var kycApprovalUpdated = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, GstStateNameOfCustomer__c " +
                            "FROM Approval__c " +
                            "WHERE Account__c = '" + steps.salesFlow.account.getId() + "'",
                    Approval__c.class);

            assertThat(kycApprovalUpdated.getGstStateNameOfCustomer__c())
                    .as("Approval__c.GstStateNameOfCustomer__c value")
                    .isEqualTo(fullStateValue);
        });

        step("8. Update Billing Street, City, State, Postal Code to new values in 'KYC Details' " +
                "and press 'Save' there", () -> {
            kycApprovalPage.billingStreetInput.setValue(indiaBillingStreetNew);
            kycApprovalPage.billingCityInput.setValue(indiaBillingCityNew);
            kycApprovalPage.billingStateInput.setValue(indiaBillingStateNew);
            kycApprovalPage.billingPostalCodeInput.setValue(indiaBillingPostalCodeNew);
            kycApprovalPage.kycDetailsSaveChanges();

            kycApprovalPage.notification.shouldNot(exist);
        });

        step("9. Prepare the KYC record for approval", () -> {
            step("Attach a Signed Off Participation Agreement file and populate 'Date of Sign Off' field via API", () -> {
                var signedOffAgreementAttachment = createAttachmentForSObject(
                        kycApproval.getId(), signedOffParticipationAgreementFileName);
                addSignedOffParticipationAgreement(kycApproval, signedOffAgreementAttachment);
            });

            steps.kycApproval.populateKycApprovalFieldsRequiredForApproval(kycApproval);

            step("Create a new Local Subscribed Address record of 'Registered Address of Company' type " +
                    "for KYC Approval via API", () -> {
                createIndiaLocalSubscribedAddressRecord(kycApproval, REGISTERED_ADDRESS_RECORD_TYPE);
            });
        });

        step("10. Approve KYC Approval record via API", () ->
                enterpriseConnectionUtils.approveSingleRecord(kycApproval.getId())
        );

        step("11. Check that Account's Billing Address is updated with the new address from KYC Approval", () -> {
            //  account's address might be updated with a delay, hence the timeout on the assertion
            var accountUpdated = assertWithTimeout(() -> {
                var accountWithBillingStreet = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT BillingStreet, BillingCity, BillingState, BillingPostalCode " +
                                "FROM Account " +
                                "WHERE Id = '" + steps.salesFlow.account.getId() + "'",
                        Account.class);
                assertEquals(indiaBillingStreetNew, accountWithBillingStreet.getBillingStreet(),
                        "Account.BillingStreet value");
                return accountWithBillingStreet;
            }, ofSeconds(20));

            assertThat(accountUpdated.getBillingStreet())
                    .as("Account.BillingStreet value")
                    .isEqualTo(indiaBillingStreetNew);
            assertThat(accountUpdated.getBillingCity())
                    .as("Account.BillingCity value")
                    .isEqualTo(indiaBillingCityNew);
            assertThat(accountUpdated.getBillingState())
                    .as("Account.BillingState value")
                    .isEqualTo(indiaBillingStateNew);
            assertThat(accountUpdated.getBillingPostalCode())
                    .as("Account.BillingPostalCode value")
                    .isEqualTo(indiaBillingPostalCodeNew);
        });
    }

    /**
     * Check if there are no error notifications in Billing Address subsection
     * after 'Save' button clicking in case of empty or correct State field values.
     *
     * @param newStateValue value of State field to populate
     */
    private void stepSetNewBillingStateAndCheckAbsenceOfErrorNotifications(String newStateValue) {
        step("Populate State field with given value, click 'Save' button and check absence of notifications", () -> {
            kycApprovalPage.billingStateInput.setValue(newStateValue);
            kycApprovalPage.kycDetailsSaveChanges();

            kycApprovalPage.notification.shouldNot(exist);
        });
    }
}
