package ngbs.quotingwizard.newbusiness.signup;

import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Case;
import io.qameta.allure.Step;

import java.util.UUID;

import static base.Pages.opportunityPage;
import static com.aquiva.autotests.rc.model.accountgeneration.CreateMultiproductDataInSfdcDTO.RC_CONTACT_CENTER_SERVICE;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomPositiveInteger;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getAccountInNGBS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.CaseHelper.INCONTACT_COMPLETED_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.CaseHelper.INCONTACT_ORDER_RECORD_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.ENGAGE_DIGITAL_STANDALONE_SERVICE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.ENGAGE_VOICE_STANDALONE_SERVICE;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods related to the test cases that execute Sign Up of the Engage Digital, Engage Voice, and/or RingCentral Contact Center services.
 */
public class MultiProductSignUpSteps {
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  RC Contact Center test data
    private final String rcMainNumberValue;
    private final String usGeoRegionOption;
    private final String ringCentralTeam;
    private final String inContactSegmentOption;
    private final String inContactBuId;

    public MultiProductSignUpSteps() {
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        rcMainNumberValue = getRandomPositiveInteger();
        usGeoRegionOption = "US";
        ringCentralTeam = "RingCentral";
        inContactSegmentOption = "1-50 Seats";
        inContactBuId = getRandomPositiveInteger();
    }

    /**
     * Sign Up 'Engage Voice' service via Process Order modal.
     *
     * @param billingId ID of the main account in NGBS (e.g. "163059013")
     */
    @Step("Sign Up 'Engage Voice' service via Process Order modal")
    public void signUpEngageVoiceServiceStep(String billingId) {
        step("Expand 'Engage Voice' service and click 'Process Engage Voice' button", () -> {
            opportunityPage.processOrderModal.engageVoiceExpandButton.click();

            opportunityPage.processOrderModal.processEngageVoiceButton.click();
            opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));
        });

        step("Select Engage Voice Platform Location, RC Engage Voice Platform and Timezone", () -> {
            opportunityPage.processOrderModal.engageVoicePlatformLocationSelect.selectOption(US1_PLATFORM_LOCATION);
            opportunityPage.processOrderModal.selectRcEngagePlatformFirstOption(ENGAGE_VOICE_SERVICE);
            opportunityPage.processOrderModal.engageVoiceTimezoneSelect.selectOption(ANCHORAGE_TIME_ZONE);
        });

        step("Click 'Sign Up Engage Voice' button and check notification that Engage Voice submitted successfully", () -> {
            signUpFinalStep(ENGAGE_VOICE_SERVICE);
        });

        step("Check that the Engage Voice package is added to the account in NGBS", () ->
                checkAddedPackageAfterSignUp(billingId, ENGAGE_VOICE_STANDALONE_SERVICE)
        );
    }

    /**
     * Sign Up 'Engage Digital' service via Process Order modal.
     *
     * @param billingId ID of the main account in NGBS (e.g. "163059013")
     */
    @Step("Sign Up 'Engage Digital' service via Process Order modal")
    public void signUpEngageDigitalServiceStep(String billingId) {
        step("Expand 'Engage Digital' service and click 'Process Engage Digital' button", () -> {
            opportunityPage.processOrderModal.engageDigitalExpandButton.click();

            opportunityPage.processOrderModal.processEngageDigitalButton.click();
            opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));
        });

        step("Select Engage Digital Platform Location, RC Engage Digital Platform, Language and Timezone " +
                "and populate RC Engage Digital Domain field", () -> {
            opportunityPage.processOrderModal.engageDigitalPlatformLocationSelect.selectOption(US1_PLATFORM_LOCATION);
            opportunityPage.processOrderModal.selectRcEngagePlatformFirstOption(ENGAGE_DIGITAL_SERVICE);
            opportunityPage.processOrderModal.engageDigitalLanguageSelect.selectOption(EN_US_LANGUAGE);
            opportunityPage.processOrderModal.engageDigitalTimezoneSelect.selectOption(ANCHORAGE_TIME_ZONE);

            //  Engage Digital Domain should contain up to 32 symbols
            var randomEngageDomainValue = UUID.randomUUID().toString().substring(0, 31);
            opportunityPage.processOrderModal.engageDigitalDomainInput.setValue(randomEngageDomainValue);
        });

        step("Click 'Sign Up Engage Digital' button and check notification that Engage Digital submitted successfully", () -> {
            signUpFinalStep(ENGAGE_DIGITAL_SERVICE);
        });

        step("Check that the Engage Digital package is added to the account in NGBS", () ->
                checkAddedPackageAfterSignUp(billingId, ENGAGE_DIGITAL_STANDALONE_SERVICE)
        );
    }

    /**
     * Sign Up 'RingCentral Contact Center' service via Process Order modal.
     *
     * @param billingId           ID of the main account in NGBS (e.g. "163059013")
     * @param masterOpportunityId Salesforce ID of the Master Opportunity
     */
    @Step("Sign Up 'RingCentral Contact Center' service via Process Order modal")
    public void signUpRcContactCenterServiceStep(String billingId, String masterOpportunityId) {
        step("Expand RingCentral Contact Center section, populate necessary fields in 'Add General Information' section", () -> {
            opportunityPage.processOrderModal.expandContactCenterSection();
            opportunityPage.processOrderModal.clickProcessContactCenter();

            opportunityPage.processOrderModal.rcCcTimezoneSelect.selectOption(ALASKA_TIME_ZONE);
            opportunityPage.processOrderModal.selectGeoRegionPicklist.selectOption(usGeoRegionOption);
            opportunityPage.processOrderModal.selectImplementationTeamPicklist.selectOption(ringCentralTeam);
            opportunityPage.processOrderModal.selectInContactSegmentPicklist.selectOption(inContactSegmentOption);
            opportunityPage.processOrderModal.ccNumberInput.setValue(rcMainNumberValue);
        });

        step("Click 'Sign Up Contact Center' button " +
                "and check that success notification is shown in the Process Order modal window", () -> {
            signUpFinalStep(CONTACT_CENTER_SERVICE);
        });

        step("Populate 'inContact_BU_ID__c' and 'inContact_Status__c' fields " +
                "of the related Case with Record Type = 'inContact_Order' via API", () -> {
            var inContactOrderCase = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Case " +
                            "WHERE Opportunity_Reference__c = '" + masterOpportunityId + "' " +
                            "AND RecordTypeName__c = '" + INCONTACT_ORDER_RECORD_TYPE + "'",
                    Case.class);
            inContactOrderCase.setInContact_BU_ID__c(Double.valueOf(inContactBuId));
            inContactOrderCase.setInContact_Status__c(INCONTACT_COMPLETED_STATUS);
            enterpriseConnectionUtils.update(inContactOrderCase);
        });

        step("Check that the RingCentral Contact Center package is added to the account in NGBS", () ->
                checkAddedPackageAfterSignUp(billingId, RC_CONTACT_CENTER_SERVICE)
        );
    }

    /**
     * Click 'Sign Up ...' (with service name) button in the Process Order modal, and check that the success notification is shown.
     *
     * @param serviceNameInNotification service name currently being signed-up as it appears on the success notification at the end
     *                                  (e.g. "Contact Center", "Engage Voice")
     */
    public void signUpFinalStep(String serviceNameInNotification) {
        opportunityPage.processOrderModal.signUpButton.shouldBe(enabled, ofSeconds(10)).click();
        opportunityPage.processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(150));
        opportunityPage.processOrderModal.successNotifications
                .shouldHave(exactTexts(format(SERVICE_SUBMITTED_SUCCESSFULLY, serviceNameInNotification)), ofSeconds(60));
    }

    /**
     * Check that the new package is added to the main account in NGBS.
     *
     * @param billingId   ID of the main account in NGBS (e.g. "163059013")
     * @param serviceName service name of the added package (e.g. "RingCentral Contact Center", "Engage Voice Standalone")
     * @return ID of the added package from the NGBS
     */
    public String checkAddedPackageAfterSignUp(String billingId, String serviceName) {
        return step("Check that the new package for the service '" + serviceName + "' is added to the account in NGBS", () -> {
            var accountInfo = getAccountInNGBS(billingId);
            var addedPackageOptional = stream(accountInfo.packages)
                    .filter(pkg -> pkg.product.equals(serviceName))
                    .findAny();
            assertThat(addedPackageOptional)
                    .as("NGBS Account's package with the service name = " + serviceName)
                    .isPresent();

            var addedPackage = addedPackageOptional.get();
            step(addedPackage.description + " package is created on the account in NGBS successfully");
            step("Package's ID = " + addedPackage.id);

            return addedPackage.id;
        });
    }
}
