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
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityContactRoleHelper.SIGNATORY_ROLE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.NEW_BUSINESS_TYPE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("LeadConvert")
@Tag("Verizon")
public class VerizonNewAccountLeadConvertTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesUserWithVerizonPermissionSet;
    private Lead partnerLead;

    //  Test data
    private final String verizonBrandName;
    private final String verizonBusinessIdentityName;
    private final String officeService;

    public VerizonNewAccountLeadConvertTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_Verizon_US_Monthly_NonContract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        verizonBrandName = data.brandName;
        verizonBusinessIdentityName = data.getBusinessIdentityName();
        officeService = data.packageFolders[0].name;
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Sales Rep - Lightning' profile and with 'RingCentral_with_Verizon' permission set", () -> {
            salesUserWithVerizonPermissionSet = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withPermissionSet(RINGCENTRAL_WITH_VERIZON_PS)
                    .execute();
        });

        steps.leadConvert.createPartnerAccountAndLead(salesUserWithVerizonPermissionSet);
        partnerLead = steps.leadConvert.partnerLead;

        step("Login as a user with 'Sales Rep - Lightning' profile and with 'RingCentral_with_Verizon' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesUserWithVerizonPermissionSet);
        });
    }

    @Test
    @TmsLink("CRM-25228")
    @DisplayName("CRM-25228 - Lead Convert for Verizon: New Account")
    @Description("Verify that Lead Conversion works for Verizon Brand with creating new Account:\n" +
            "- Only Office Service on the Lead Conversion page \n" +
            "- Only New Business Type on the converted Opportunity \n" +
            "- Forecasted Office Users field is removed from Lead Conversion page \n" +
            "- Checkbox 'Enable ELA for this Account' is removed from Lead Conversion page")
    public void test() {
        step("1. Open Lead Convert page for the Partner Lead", () -> {
            leadConvertPage.openPage(partnerLead.getId());
        });

        step("2. Switch the toggle into 'Create New Account' position in Account Info section, " +
                "check that 'Enable ELA for this Account' checkbox is not displayed, " +
                "click 'Apply' button, and check that the checkbox is still not displayed", () -> {
            leadConvertPage.newExistingAccountToggle.click();
            leadConvertPage.accountInfoEditButton.click();
            leadConvertPage.elaCheckbox.shouldBe(hidden);

            leadConvertPage.accountInfoApplyButton.click();
            leadConvertPage.elaCheckbox.shouldBe(hidden);
        });

        step("3. Click on 'Edit' in the Opportunity Info Section, and populate Close Date field", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.closeDateDatepicker.setTomorrowDate();
        });

        step("4. Check the Opportunity section's content, and click 'Apply' button", () -> {
            leadConvertPage.businessIdentityPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(verizonBusinessIdentityName));
            leadConvertPage.brandNameOutputField.shouldHave(exactTextCaseSensitive(verizonBrandName));
            leadConvertPage.servicePickList.getOptions().shouldHave(exactTextsCaseSensitiveInAnyOrder(officeService), ofSeconds(20));
            leadConvertPage.forecastedUsersInput.shouldNot(exist);

            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("5. Click 'Convert' button and check that there's a redirect to the Opportunity record page", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("6. Check that the Lead is successfully converted", () -> {
            var convertedLead = steps.leadConvert.checkLeadConversion(partnerLead);

            step("Check the Name of the converted Account", () -> {
                var convertedAccount = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, Name " +
                                "FROM Account " +
                                "WHERE Id = '" + convertedLead.getConvertedAccountId() + "'",
                        Account.class);
                assertThat(convertedAccount.getName())
                        .as("Account.Name value")
                        .isEqualTo(partnerLead.getCompany());
            });

            step("Check FirstName and LastName of the converted Contact", () -> {
                var convertedContact = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, FirstName, LastName " +
                                "FROM Contact " +
                                "WHERE Id = '" + convertedLead.getConvertedContactId() + "'",
                        Contact.class);
                assertThat(convertedContact.getFirstName())
                        .as("Contact.FirstName value")
                        .isEqualTo(partnerLead.getFirstName());

                assertThat(convertedContact.getLastName())
                        .as("Contact.LastName value")
                        .isEqualTo(partnerLead.getLastName());
            });

            step("Check the fields on the converted Opportunity", () -> {
                var opportunityFromConvertedLead = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, Name, Brand_Name__c, Tier_Name__c, Type " +
                                "FROM Opportunity " +
                                "WHERE Id = '" + convertedLead.getConvertedOpportunityId() + "'",
                        Opportunity.class);

                assertThat(opportunityFromConvertedLead.getName())
                        .as("Opportunity.Name value")
                        //  this value should be pre-populated on the Lead Convert page
                        .isEqualTo(partnerLead.getCompany());

                assertThat(opportunityFromConvertedLead.getBrand_Name__c())
                        .as("Opportunity.Brand_Name__c value")
                        .isEqualTo(verizonBrandName);

                assertThat(opportunityFromConvertedLead.getTier_Name__c())
                        .as("Opportunity.Tier_Name__c value")
                        .isEqualTo(officeService);

                assertThat(opportunityFromConvertedLead.getType())
                        .as("Opportunity.Type value")
                        .isEqualTo(NEW_BUSINESS_TYPE);
            });

            step("Check the fields on the OpportunityContactRole record", () -> {
                var opportunityContactRole = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT IsPrimary, Role, ContactId " +
                                "FROM OpportunityContactRole " +
                                "WHERE OpportunityId = '" + convertedLead.getConvertedOpportunityId() + "'",
                        OpportunityContactRole.class);
                assertThat(opportunityContactRole.getIsPrimary())
                        .as("OpportunityContactRole.IsPrimary value")
                        .isTrue();

                assertThat(opportunityContactRole.getRole())
                        .as("OpportunityContactRole.Role value")
                        .isEqualTo(SIGNATORY_ROLE);

                assertThat(opportunityContactRole.getContactId())
                        .as("OpportunityContactRole.ContactId value")
                        .isEqualTo(convertedLead.getConvertedContactId());
            });
        });
    }
}
