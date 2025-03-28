package ngbs.opportunitycreation.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.AccountData;
import com.sforce.soap.enterprise.sobject.*;
import com.sforce.ws.ConnectionException;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.salesforce.approval.TaxExemptionManagerPage.*;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountFactory.createNewPartnerAccountInSFDC;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.ApprovalFactory.createTeaApproval;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.SubBrandsMappingFactory.createNewSubBrandsMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.*;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.refresh;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("P1")
@Tag("Approval")
@Tag("Ignite")
@Tag("TaxExemption")
public class SalesTaxExemptionAmazonOpportunityCreationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUser;
    private Account partnerAccount;
    private Approval__c teaApproval;

    //  Test data
    private final String serviceName;
    private final String defaultAmazonBusinessIdentity;
    private final String amazonUsCountry;
    private final String amazonUsSubBrandName;
    private final List<String> allTaxExemptionStatuses;

    public SalesTaxExemptionAmazonOpportunityCreationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/RC_MVP_Annual_Contract_QOP.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        serviceName = data.packageFolders[0].name;
        defaultAmazonBusinessIdentity = "Amazon US";
        amazonUsCountry = "United States";
        amazonUsSubBrandName = "1210.AMZN";
        allTaxExemptionStatuses = List.of(REQUESTED_EXEMPTION_STATUS, APPROVED_EXEMPTION_STATUS, CURRENT_EXEMPTION_STATUS,
                REJECTED_EXEMPTION_STATUS);
    }

    @BeforeEach
    public void setUpTest() {
        salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);

        step("Create a new test Partner Account " +
                "with RC_Brand__c = '" + data.brandName + "', Service_Type__c = '" + serviceName + "', " +
                "and Sub_Brands_Permitted_To_Sell__c = '" + amazonUsSubBrandName + "' via API", () -> {
            partnerAccount = createNewPartnerAccountInSFDC(salesRepUser, new AccountData(data));
            partnerAccount.setRC_Brand__c(data.brandName);
            partnerAccount.setService_Type__c(serviceName);
            partnerAccount.setSub_Brands_Permitted_To_Sell__c(amazonUsSubBrandName);

            enterpriseConnectionUtils.update(partnerAccount);
        });

        step("Create a new SubBrandsMapping__c Custom Setting record for the Partner Account via API", () -> {
            var testSubBrandsMapping = createNewSubBrandsMapping(partnerAccount);

            testSubBrandsMapping.setSub_Brand__c(amazonUsSubBrandName);
            enterpriseConnectionUtils.update(testSubBrandsMapping);
        });

        step("Check that 'Default Business Identity Mapping' record of custom metadata type exists " +
                "for 'Amazon US' Default Business Identity", () -> {
            var amazonUsDefaultBiMappingCustomMetadataRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Sub_Brand__c = '" + amazonUsSubBrandName + "' " +
                            "AND Brand__c = '" + data.brandName + "' " +
                            "AND Country__c = '" + amazonUsCountry + "' " +
                            "AND Default_Business_Identity__c = '" + defaultAmazonBusinessIdentity + "'",
                    Default_Business_Identity_Mapping__mdt.class);
            assertThat(amazonUsDefaultBiMappingCustomMetadataRecords.size())
                    .as("Quantity of Default_Business_Identity_Mapping__mdt records for 'Amazon US'")
                    .isEqualTo(1);
        });

        step("Set the test Customer Account's fields with the values of Partner Account's fields via API", () -> {
            steps.salesFlow.account.setParent_Partner_Account__c(partnerAccount.getId());
            steps.salesFlow.account.setPartner_Account__c(partnerAccount.getId());
            steps.salesFlow.account.setRC_Brand__c(partnerAccount.getRC_Brand__c());
            steps.salesFlow.account.setPartner_ID__c(partnerAccount.getPartner_ID__c());

            enterpriseConnectionUtils.update(steps.salesFlow.account);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-35193")
    @TmsLink("CRM-33663")
    @DisplayName("CRM-35193 - QOP. Tax Exempt Approval for Sales Taxes is auto-created and auto-approved " +
            "after Opportunity creation for Amazon US Business Identity. \n" +
            "CRM-33663 - 'Sales' Tax is preselected and disabled on manually created Tax Exempt Approval for Amazon US Business Identity")
    @Description("CRM-35193 - Verify that Tax Exempt Approval for Sales Taxes is auto-created and auto-approved " +
            "for Amazon US Business Identity after Opportunity Creation via QOP. \n" +
            "CRM-33663 - Verify that 'Sales' Tax Type is preselected as 'Approved' and check-boxes for this Tax Type are disabled " +
            "on manually created Tax Exempt Approval for Amazon US Business Identity")
    public void test() {
        step("1. Open the QOP for the test Customer Account and populate Close Date field", () -> {
            opportunityCreationPage.openPage(steps.salesFlow.account.getId());
            opportunityCreationPage.populateCloseDate();
        });

        //  CRM-35193
        step("2. Click 'Continue to Opportunity' button and check that Tax Exempt Approval is created " +
                "with correct values of Status__c, SalesTaxExemption__c and RecordType.Name fields", () -> {
            steps.opportunityCreation.pressContinueToOpp();

            var autoCreatedTaxExemptApproval = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, RecordType.Name, Status__c, SalesTaxExemption__c " +
                            "FROM Approval__c " +
                            "WHERE Account__c = '" + steps.salesFlow.account.getId() + "'",
                    Approval__c.class);
            assertThat(autoCreatedTaxExemptApproval.getStatus__c())
                    .as("Tax Exempt Approval__c.Status__c value")
                    .isEqualTo(APPROVAL_STATUS_APPROVED);
            assertThat(autoCreatedTaxExemptApproval.getSalesTaxExemption__c())
                    .as("Tax Exempt Approval__c.SalesTaxExemption__c value")
                    .isEqualTo(TAX_EXEMPTION_APPROVED);
            assertThat(autoCreatedTaxExemptApproval.getRecordType().getName())
                    .as("Tax Exempt Approval__c.RecordType.Name value")
                    .isEqualTo(TEA_APPROVAL_RECORD_TYPE);
        });

        //  CRM-33663
        step("3. Create a new Tax Exempt Approval for the test Opportunity via API", () -> {
            teaApproval = createTeaApproval(steps.salesFlow.account.getId(), opportunityPage.getCurrentRecordId(),
                    steps.salesFlow.contact.getId(), salesRepUser.getId());
        });

        //  CRM-33663
        step("4. Open the Tax Exemption Manager page for the newly created test Tax Exempt Approval, " +
                "check that the 'Sales tax' type is preselected as 'Approved', and all checkboxes for this type are disabled", () -> {
            taxExemptionManagerPage.openPage(teaApproval.getId());

            checkSalesTypeTaxes();
        });

        //  CRM-33663
        step("5. Select 'Local tax' as 'Requested' with a checkbox, save changes, " +
                "check that the 'Sales tax' type is preselected as 'Approved' and all checkboxes for this type are disabled, " +
                "and check that Approval's Status__c, SalesTaxExemption__c and LocalTaxExemption__c fields have expected values", () -> {
            taxExemptionManagerPage.setExemptionStatus(LOCAL_TAX_TYPE, REQUESTED_EXEMPTION_STATUS);
            taxExemptionManagerPage.saveChanges();

            checkSalesTypeTaxes();
            checkUpdatedTaxExemptApproval(APPROVAL_STATUS_NEW);
        });

        //  CRM-33663
        step("6. Click 'Submit for Approval' button, " +
                "check that 'Sales tax' type is preselected as 'Approved', and all check-boxes for this type are disabled, " +
                "and check that Approval's Status__c, SalesTaxExemption__c and LocalTaxExemption__c fields have expected values", () -> {
            taxExemptionManagerPage.clickSubmitForApproval();

            checkSalesTypeTaxes();
            checkUpdatedTaxExemptApproval(APPROVAL_STATUS_PENDING);
        });

        //  CRM-33663
        step("7. Approve the test Tax Exempt approval via API, re-open the Tax Exemption Manager page, " +
                "check that 'Sales tax' type is preselected as 'Approved', and all check-boxes for this type are disabled, " +
                "and check that Approval's Status__c, SalesTaxExemption__c and LocalTaxExemption__c fields have expected values", () -> {
            enterpriseConnectionUtils.approveSingleRecord(teaApproval.getId());

            refresh();
            taxExemptionManagerPage.waitUntilLoaded();
            checkSalesTypeTaxes();

            assertWithTimeout(() -> {
                var teaApprovalUpdated = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, Status__c, SalesTaxExemption__c, LocalTaxExemption__c " +
                                "FROM Approval__c " +
                                "WHERE Id = '" + teaApproval.getId() + "'",
                        Approval__c.class);
                assertEquals(APPROVAL_STATUS_APPROVED, teaApprovalUpdated.getStatus__c(),
                        "Tax Exempt Approval__c.Status__c value");
                assertEquals(TAX_EXEMPTION_APPROVED, teaApprovalUpdated.getSalesTaxExemption__c(),
                        "Tax Exempt Approval__c.SalesTaxExemption__c value");
                assertEquals(TAX_EXEMPTION_APPROVED, teaApprovalUpdated.getLocalTaxExemption__c(),
                        "Tax Exempt Approval__c.LocalTaxExemption__c value");
            }, ofSeconds(20));
        });
    }

    /**
     * Check that checkbox with 'Sales tax' type and 'Approved' tax exemption status is selected
     * and each checkbox with 'Sales tax' type is disabled.
     */
    private void checkSalesTypeTaxes() {
        taxExemptionManagerPage.getExemptionCheckbox(SALES_TAX_TYPE, APPROVED_EXEMPTION_STATUS)
                .$("input")
                .shouldBe(visible, ofSeconds(10))
                .shouldBe(checked);
        for (var taxExemptionStatus : allTaxExemptionStatuses) {
            taxExemptionManagerPage.getExemptionCheckbox(SALES_TAX_TYPE, taxExemptionStatus).$("input").shouldBe(disabled);
        }
    }

    /**
     * Check that Tax Exempt Approval's Status__c, SalesTaxExemption__c
     * and LocalTaxExemption__c fields have expected values.
     *
     * @param expectedStatusValue an expected value of Approval__c.Status__c field
     * @throws ConnectionException in case of errors while accessing API
     */
    private void checkUpdatedTaxExemptApproval(String expectedStatusValue)
            throws ConnectionException {
        var teaApprovalUpdated = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, Status__c, SalesTaxExemption__c, LocalTaxExemption__c " +
                        "FROM Approval__c " +
                        "WHERE Id = '" + teaApproval.getId() + "'",
                Approval__c.class);
        assertThat(teaApprovalUpdated.getStatus__c())
                .as("Tax Exempt Approval__c.Status__c value")
                .isEqualTo(expectedStatusValue);
        assertThat(teaApprovalUpdated.getSalesTaxExemption__c())
                .as("Tax Exempt Approval__c.SalesTaxExemption__c value")
                .isEqualTo(TAX_EXEMPTION_APPROVED);
        assertThat(teaApprovalUpdated.getLocalTaxExemption__c())
                .as("Tax Exempt Approval__c.LocalTaxExemption__c value")
                .isEqualTo(TAX_EXEMPTION_REQUESTED);
    }
}
