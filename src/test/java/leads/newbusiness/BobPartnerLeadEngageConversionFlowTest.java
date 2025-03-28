package leads.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper;
import com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LeadHelper;
import com.sforce.soap.enterprise.sobject.Contact;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.createPartnerInNGBS;
import static com.aquiva.autotests.rc.utilities.ngbs.PartnerNgbsFactory.createBillOnBehalfPartner;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.AccountContactRoleFactory.createAccountContactRole;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountContactRoleHelper.ACCOUNTS_PAYABLE_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.ACTIVE_PARTNER_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.getPrimaryContactOnAccount;
import static com.codeborne.selenide.Condition.enabled;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("LeadConvert")
@Tag("Bill-on-Behalf")
@Tag("Engage")
public class BobPartnerLeadEngageConversionFlowTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Contact partnerAccountContact;
    private Double ngbsPartnerId;

    //  Test data
    private final String businessIdentityName;
    private final String engageDigitalStandaloneService;

    public BobPartnerLeadEngageConversionFlowTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_EngageDS_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        businessIdentityName = data.businessIdentity.name;
        engageDigitalStandaloneService = data.packageFolders[0].name;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createPartnerAccountAndLead(salesRepUser);

        step("Create Bill on Behalf partner in NGBS via API", () -> {
            var ngbsPartnerObject = createBillOnBehalfPartner();
            var ngbsPartner = createPartnerInNGBS(ngbsPartnerObject);
            ngbsPartnerId = Double.valueOf(ngbsPartner.id);
        });

        step("Make partner Account 'Bill-on-Behalf' using SFDC API", () -> {
            step("Populate Partner Account's Partner_Type__c, BusinessIdentity__c, " +
                    "PartnerStatus__c and NGBS_Partner_ID__c fields via API", () -> {
                steps.leadConvert.partnerAccount.setPartner_Type__c(AccountHelper.BILL_ON_BEHALF_PARTNER_TYPE);
                steps.leadConvert.partnerAccount.setBusinessIdentity__c(businessIdentityName);
                steps.leadConvert.partnerAccount.setPartnerStatus__c(ACTIVE_PARTNER_STATUS);
                steps.leadConvert.partnerAccount.setNGBS_Partner_ID__c(ngbsPartnerId);
                enterpriseConnectionUtils.update(steps.leadConvert.partnerAccount);
            });

            step("Create 'Accounts Payable' contact role and link it with partner Account via API", () -> {
                partnerAccountContact = getPrimaryContactOnAccount(steps.leadConvert.partnerAccount);
                createAccountContactRole(steps.leadConvert.partnerAccount, partnerAccountContact, ACCOUNTS_PAYABLE_ROLE, false);
            });
        });

        step("Prepare partner Lead and make it 'Bill-on-Behalf' using SFDC API", () -> {
            steps.leadConvert.partnerLead.setFirstName(partnerAccountContact.getFirstName());
            steps.leadConvert.partnerLead.setLastName(partnerAccountContact.getLastName());
            steps.leadConvert.partnerLead.setPhone(partnerAccountContact.getPhone());
            steps.leadConvert.partnerLead.setEmail(partnerAccountContact.getEmail());
            steps.leadConvert.partnerLead.setBusinessIdentity__c(businessIdentityName);
            steps.leadConvert.partnerLead.setPartner_Type__c(LeadHelper.BILL_ON_BEHALF_PARTNER_TYPE);

            enterpriseConnectionUtils.update(steps.leadConvert.partnerLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-25591")
    @DisplayName("CRM-25591 - Convert Engage Digital Standalone BoB partner lead")
    @Description("Verify that BoB partner lead can be converted into Engage Digital Standalone opportunity")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.partnerLead.getId())
        );

        step("2. Switch the toggle into 'Create New Account' position in Account Info section", () ->
                leadConvertPage.newExistingAccountToggle
                        .shouldBe(enabled, ofSeconds(60))
                        .click()
        );

        step("3. Click 'Edit' in Opportunity Section, select Service = '" + engageDigitalStandaloneService + "', " +
                "populate Close Date field, and click 'Apply' button", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.selectService(engageDigitalStandaloneService);
            leadConvertPage.closeDateDatepicker.setTomorrowDate();
            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("4. Press 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("5. Check that Lead is converted into Account, Opportunity and Contact", () ->
                steps.leadConvert.checkLeadConversion(steps.leadConvert.partnerLead)
        );
    }
}
