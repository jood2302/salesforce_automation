package leads.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.SubBrandsMappingFactory.createNewSubBrandsMapping;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ApprovalHelper.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("P1")
@Tag("Approval")
@Tag("TaxExemption")
@Tag("Ignite")
public class SalesTaxExemptionPartnerLeadConvertTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String defaultAmazonBusinessIdentity;
    private final String amazonUsCountry;
    private final String brandName;
    private final String tierName;
    private final String amazonUsSubBrandName;

    public SalesTaxExemptionPartnerLeadConvertTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        defaultAmazonBusinessIdentity = "Amazon US";
        amazonUsCountry = "United States";
        brandName = data.brandName;
        tierName = data.packageFolders[0].name;
        amazonUsSubBrandName = "1210.AMZN";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.leadConvert.createPartnerAccountAndLead(salesRepUser);

        step("Set Partner Account's RC_Brand__c = '" + brandName + "', Service_Type__c = '" + tierName + "', " +
                "Sub_Brands_Permitted_To_Sell__c = '" + amazonUsSubBrandName + "' via API", () -> {
            steps.leadConvert.partnerAccount.setRC_Brand__c(brandName);
            steps.leadConvert.partnerAccount.setService_Type__c(tierName);
            steps.leadConvert.partnerAccount.setSub_Brands_Permitted_To_Sell__c(amazonUsSubBrandName);

            enterpriseConnectionUtils.update(steps.leadConvert.partnerAccount);
        });

        step("Create a new SubBrandsMapping__c Custom Setting record for the Partner Account via API", () -> {
            var testSubBrandsMapping = createNewSubBrandsMapping(steps.leadConvert.partnerAccount);

            testSubBrandsMapping.setSub_Brand__c(amazonUsSubBrandName);
            enterpriseConnectionUtils.update(testSubBrandsMapping);
        });

        step("Check that 'Default Business Identity Mapping' record of custom metadata type exists " +
                "for 'Amazon US' Default Business Identity", () -> {
            var amazonUsDefaultBiMappingCustomMetadataRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Sub_Brand__c = '" + amazonUsSubBrandName + "' " +
                            "AND Brand__c = '" + brandName + "' " +
                            "AND Country__c = '" + amazonUsCountry + "' " +
                            "AND Default_Business_Identity__c = '" + defaultAmazonBusinessIdentity + "'",
                    Default_Business_Identity_Mapping__mdt.class);
            assertThat(amazonUsDefaultBiMappingCustomMetadataRecords.size())
                    .as("Quantity of Default_Business_Identity_Mapping__mdt records for 'Amazon US'")
                    .isEqualTo(1);
        });

        step("Populate the test Partner Lead.Country__c, Lead_Brand_Name__c and LeadPartnerID__c fields " +
                "with the test Partner Account's field values via API", () -> {
            steps.leadConvert.partnerLead.setCountry__c(steps.leadConvert.partnerAccount.getBillingCountry());
            steps.leadConvert.partnerLead.setLead_Brand_Name__c(steps.leadConvert.partnerAccount.getRC_Brand__c());
            steps.leadConvert.partnerLead.setLeadPartnerID__c(steps.leadConvert.partnerAccount.getPartner_ID__c());

            enterpriseConnectionUtils.update(steps.leadConvert.partnerLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33460")
    @DisplayName("CRM-33460 - LCP. Tax Exempt Approval for Sales Taxes is auto-created and auto-approved for Amazon US Business Identity")
    @Description("Verify that Tax Exempt Approval for Sales Taxes is auto-created and auto-approved for Amazon US Business Identity " +
            "after Opportunity Creation via Lead Convert")
    public void test() {
        step("1. Open Lead Convert page for the test Partner Lead", () -> {
            leadConvertPage.openPage(steps.leadConvert.partnerLead.getId());
        });

        step("2. Switch the toggle into 'Create New Account' position", () ->
                leadConvertPage.newExistingAccountToggle.click()
        );

        step("3. Click 'Edit' in the Opportunity Section, select Business Identity '" + defaultAmazonBusinessIdentity + "', " +
                "populate Close Date field and click 'Apply' button", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.businessIdentityPicklist.selectOption(defaultAmazonBusinessIdentity);
            leadConvertPage.closeDateDatepicker.setTomorrowDate();
            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("4. Click 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("5. Check that the Lead is converted", () ->
                steps.leadConvert.checkLeadConversion(steps.leadConvert.partnerLead)
        );

        step("6. Check that the new Account is converted from the lead with the correct Tax_Exempt__c " +
                "and TaxExemptionApprovals__c values, and that the new Tax Exempt Approval is created for the converted Account " +
                "with the correct RecordType.Name, Status__c, SalesTaxExemption__c values", () -> {
            var convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ConvertedAccountId " +
                            "FROM Lead " +
                            "WHERE Id = '" + steps.leadConvert.partnerLead.getId() + "'",
                    Lead.class);

            var createdTeaApproval = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, RecordType.Name, Status__c, SalesTaxExemption__c " +
                            "FROM Approval__c " +
                            "WHERE Account__r.Id = '" + convertedLead.getConvertedAccountId() + "' " +
                            "OR Account__r.Master_Account__c = '" + convertedLead.getConvertedAccountId() + "'",
                    Approval__c.class);
            assertThat(createdTeaApproval.getStatus__c())
                    .as("Approval.Status__c value")
                    .isEqualTo(APPROVAL_STATUS_APPROVED);
            assertThat(createdTeaApproval.getSalesTaxExemption__c())
                    .as("Approval.SalesTaxExemption__c value")
                    .isEqualTo(TAX_EXEMPTION_APPROVED);
            assertThat(createdTeaApproval.getRecordType().getName())
                    .as("Approval.RecordType.Name value")
                    .isEqualTo(TEA_APPROVAL_RECORD_TYPE);

            step("Check that the Converted Account's Tax_Exempt__c and TaxExemptionApprovals__c values = true", () -> {
                assertWithTimeout(() -> {
                    var convertedAccount = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, Tax_Exempt__c, TaxExemptionApprovals__c " +
                                    "FROM Account " +
                                    "WHERE Id = '" + convertedLead.getConvertedAccountId() + "'",
                            Account.class);
                    assertTrue(convertedAccount.getTax_Exempt__c(), "Converted Account.Tax_Exempt__c value");
                    assertTrue(convertedAccount.getTaxExemptionApprovals__c(), "Converted Account.TaxExemptionApprovals__c value");
                }, ofSeconds(30));
            });
        });
    }
}
