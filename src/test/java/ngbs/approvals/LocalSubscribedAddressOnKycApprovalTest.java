package ngbs.approvals;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Approval__c;
import com.sforce.soap.enterprise.sobject.LocalSubscribedAddress__c;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.salesforce.LocalSubscribedAddressRecordPage.NOTES_AND_ATTACHMENTS_LIST;
import static com.aquiva.autotests.rc.page.salesforce.RecordPage.NEW_BUTTON_LABEL;
import static com.aquiva.autotests.rc.page.salesforce.approval.KycApprovalPage.LOCAL_SUBSCRIBED_ADDRESSES_RELATED_LIST;
import static com.aquiva.autotests.rc.page.salesforce.approval.modal.NewLocalSubscribedAddressCreationModal.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LocalSubscribedAddressHelper.*;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("IndiaMVP")
public class LocalSubscribedAddressOnKycApprovalTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Approval__c kycApproval;

    //  Test data
    private final Integer expectedQuantityOfLocalSubscribedAddressRecords;
    private final String localSubscribedAddressName;

    public LocalSubscribedAddressOnKycApprovalTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_IndiaAndUS_MVP_Monthly_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        expectedQuantityOfLocalSubscribedAddressRecords = 2;
        localSubscribedAddressName = "TestLocalSubscribedAddress";
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
    @TmsLink("CRM-23962")
    @DisplayName("CRM-23962 - Adding 'Local Subscribed Address' records to KYC approval")
    @Description("Verify that 'Local Subscribed Address' can be added to KYC Approval. \n" +
            "Verify that both 'Local Subscribed Address of Company' and 'Registered Address of Company' record types are available. \n" +
            "Verify that correct page layout is assigned to each record type. " +
            "Verify that KYC cannot have more than one related 'Registered Address of Company' record.")
    public void test() {
        step("1. Open India Account's KYC Approval page", () -> {
            kycApprovalPage.openPage(kycApproval.getId());
        });

        step("2. Click 'New' button in 'Local Subscribed Address' related list " +
                "and check available record types modal window content", () -> {
            kycApprovalPage.clickHiddenListButtonOnRelatedList(LOCAL_SUBSCRIBED_ADDRESSES_RELATED_LIST, NEW_BUTTON_LABEL);
            selectRecordTypeModal.recordTypesList.shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    LOCAL_SUBSCRIBED_ADDRESS_RECORD_TYPE, REGISTERED_ADDRESS_RECORD_TYPE), ofSeconds(10));
        });

        step("3. Select 'Local Subscribed Address of Company', click 'Submit' button " +
                "and check that Local Subscribed Address record creation modal window contains three sections", () -> {
            selectRecordTypeModal.selectLocalSubscribedAddressType(LOCAL_SUBSCRIBED_ADDRESS_RECORD_TYPE);
            selectRecordTypeModal.submitButton.click();

            localSubscribedAddressOfCompanyCreationModal.getSectionHeaders()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(
                            INFORMATION_SECTION,
                            PHYSICAL_INSPECTION_PRIOR_TO_ACTIVATION_SECTION,
                            PHYSICAL_INSPECTION_EVERY_SIX_MONTHS_SECTION), ofSeconds(10));
        });

        step("4. Populate Local Subscribed Address Name and click 'Save' button in modal window", () -> {
            localSubscribedAddressOfCompanyCreationModal.localSubscribedAddressNameInput.setValue(localSubscribedAddressName);

            localSubscribedAddressOfCompanyCreationModal.saveChanges();
        });

        step("5. Check that there are Information section fields and sections " +
                "'Physical Inspection prior to activation Services', 'Physical Inspection every 6 months' " +
                "are displayed on the created record ", () -> {
            localSubscribedAddressRecordPage.informationFieldsSection.shouldBe(visible, ofSeconds(20));

            localSubscribedAddressRecordPage.recordSections.shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    PHYSICAL_INSPECTION_PRIOR_TO_ACTIVATION_SECTION,
                    PHYSICAL_INSPECTION_EVERY_SIX_MONTHS_SECTION));
        });

        step("6. Check that there is 'Notes & Attachments' related list on created Local Subscribed Address record", () -> {
            localSubscribedAddressRecordPage.relatedTab.click();
            localSubscribedAddressRecordPage.getRelatedListByName(NOTES_AND_ATTACHMENTS_LIST)
                    .shouldBe(visible, ofSeconds(10));
        });

        step("6. Open KYC Approval page and click 'New' button in 'Local Subscribed Address' related list", () -> {
            kycApprovalPage.openPage(kycApproval.getId());
            kycApprovalPage.clickHiddenListButtonOnRelatedList(LOCAL_SUBSCRIBED_ADDRESSES_RELATED_LIST, NEW_BUTTON_LABEL);
        });

        step("7. Select 'Registered Address of Company' record type, click 'Submit' button " +
                "and check that record creation modal window contains only one 'Information' section", () -> {
            selectRecordTypeModal.selectLocalSubscribedAddressType(REGISTERED_ADDRESS_RECORD_TYPE);
            selectRecordTypeModal.submitButton.click();

            registeredAddressOfCompanyCreationModal.getSectionHeaders()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(INFORMATION_SECTION), ofSeconds(10));
        });

        step("8. Populate Local Subscribed Address Name and click 'Save' button in modal window", () -> {
            registeredAddressOfCompanyCreationModal.localSubscribedAddressNameInput
                    .setValue(localSubscribedAddressName);

            registeredAddressOfCompanyCreationModal.saveChanges();
        });

        step("9. Check that there's only 'Information' section fields are displayed on created record page " +
                "and 'Notes & Attachments' related list is displayed", () -> {
            localSubscribedAddressRecordPage.informationFieldsSection.shouldBe(visible, ofSeconds(20));

            localSubscribedAddressRecordPage.relatedTab.click();
            localSubscribedAddressRecordPage.getRelatedListByName(NOTES_AND_ATTACHMENTS_LIST)
                    .shouldBe(visible, ofSeconds(10));
        });

        step("10. Open KYC Approval page and click 'New' button in 'Local Subscribed Addresses' related list", () -> {
            kycApprovalPage.openPage(kycApproval.getId());
            kycApprovalPage.clickHiddenListButtonOnRelatedList(LOCAL_SUBSCRIBED_ADDRESSES_RELATED_LIST, NEW_BUTTON_LABEL);
        });

        step("11. Select 'Registered Address of Company' record type and click 'Submit' button", () -> {
            selectRecordTypeModal.selectLocalSubscribedAddressType(REGISTERED_ADDRESS_RECORD_TYPE);
            selectRecordTypeModal.submitButton.click();
        });

        step("12. Populate Local Subscribed Address Name, click 'Save' button in modal window, " +
                "check appeared error message, and close modal window", () -> {
            registeredAddressOfCompanyCreationModal.localSubscribedAddressNameInput
                    .setValue(localSubscribedAddressName);

            registeredAddressOfCompanyCreationModal.getSaveButton().click();
            registeredAddressOfCompanyCreationModal.errorsPopUpModal.getErrorsList()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(ONLY_ONE_REGISTERED_ADDRESS_CAN_BE_CREATED_ERROR), ofSeconds(10));
            registeredAddressOfCompanyCreationModal.closeWindow();
        });

        step("13. Select 'Local Subscribed Address of Company' and click 'Submit' button", () -> {
            selectRecordTypeModal.selectLocalSubscribedAddressType(LOCAL_SUBSCRIBED_ADDRESS_RECORD_TYPE);
            selectRecordTypeModal.submitButton.click();
        });

        step("14. Populate Local Subscribed Address Name and click 'Save' button in modal window", () -> {
            localSubscribedAddressOfCompanyCreationModal.localSubscribedAddressNameInput
                    .setValue(localSubscribedAddressName);

            localSubscribedAddressOfCompanyCreationModal.saveChanges();
        });

        step("15. Check that a new 'Local Subscribed Address of Company' record is created " +
                "and now there are two 'Local Subscribed Address of Company' records related to the KYC Approval", () -> {
            localSubscribedAddressRecordPage.informationFieldsSection.shouldBe(visible, ofSeconds(20));

            localSubscribedAddressRecordPage.recordSections.shouldHave(exactTextsCaseSensitiveInAnyOrder(
                    PHYSICAL_INSPECTION_PRIOR_TO_ACTIVATION_SECTION,
                    PHYSICAL_INSPECTION_EVERY_SIX_MONTHS_SECTION));

            var localSubscribedAddressOfCompanyRecordTypeId = enterpriseConnectionUtils
                    .getRecordTypeId(LOCAL_SUBSCRIBED_ADDRESS_SOBJECT_API_NAME,
                            LOCAL_SUBSCRIBED_ADDRESS_RECORD_TYPE);

            var localSubscribedAddressRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM LocalSubscribedAddress__c " +
                            "WHERE Approval__c = '" + kycApproval.getId() + "' " +
                            "AND RecordTypeId = '" + localSubscribedAddressOfCompanyRecordTypeId + "'",
                    LocalSubscribedAddress__c.class);

            assertThat(localSubscribedAddressRecords.size())
                    .as("Quantity of related LocalSubscribedAddress__c records with " +
                            "'Local Subscribed Address of Company' record type")
                    .isEqualTo(expectedQuantityOfLocalSubscribedAddressRecords);
        });
    }
}
