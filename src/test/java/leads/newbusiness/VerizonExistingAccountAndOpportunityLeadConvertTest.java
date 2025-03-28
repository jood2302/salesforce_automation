package leads.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Contact;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContactHelper.WORKING_OPP_CONTACT_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.ContactHelper.getFullName;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.selected;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("P1")
@Tag("LeadConvert")
@Tag("Verizon")
public class VerizonExistingAccountAndOpportunityLeadConvertTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesUserWithVerizonPermissionSet;

    //  Test data
    private final String verizonBrandName;

    public VerizonExistingAccountAndOpportunityLeadConvertTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_Verizon_US_Monthly_NonContract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        verizonBrandName = data.brandName;
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Sales Rep - Lightning' profile and with 'RingCentral_with_Verizon' permission set", () -> {
            salesUserWithVerizonPermissionSet = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withPermissionSet(RINGCENTRAL_WITH_VERIZON_PS)
                    .execute();
        });

        steps.leadConvert.createSalesLead(salesUserWithVerizonPermissionSet);
        steps.salesFlow.createAccountWithContactAndContactRole(salesUserWithVerizonPermissionSet);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesUserWithVerizonPermissionSet);

        step("Set Lead.Lead_Brand_Name__c = '" + verizonBrandName + "' via API", () -> {
            steps.leadConvert.salesLead.setLead_Brand_Name__c(verizonBrandName);
            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        //  to be able to select the existing account from the 'Matched Accounts' list instead of flaky Account Lookup
        //  and the existing contact from the 'Matched Contacts' list
        step("Set the same email on the Sales Lead and the test Account's Contact via API", () -> {
            //  workaround to bypass the 'Automated Process' updating the Contact's Email back to the original one
            step("Wait until Contact.Contact_Status__c is updated by the Automated Process", () -> {
                assertWithTimeout(() -> {
                    var contactUpdated = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, Contact_Status__c " +
                                    "FROM Contact " +
                                    "WHERE Id = '" + steps.salesFlow.contact.getId() + "'",
                            Contact.class);
                    assertEquals(WORKING_OPP_CONTACT_STATUS, contactUpdated.getContact_Status__c(),
                            "Contact.Contact_Status__c value");
                }, ofSeconds(10));
            });

            var testEmail = UUID.randomUUID() + "@example.com";

            steps.leadConvert.salesLead.setEmail(testEmail);
            steps.salesFlow.contact.setEmail(testEmail);
            enterpriseConnectionUtils.update(steps.leadConvert.salesLead, steps.salesFlow.contact);
        });

        step("Login as a user with 'Sales Rep - Lightning' profile and with 'RingCentral_with_Verizon' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesUserWithVerizonPermissionSet);
        });
    }

    @Test
    @TmsLink("CRM-25242")
    @DisplayName("CRM-25242 - Lead Convert for Verizon: Existing New Business Account, Existing Opportunity")
    @Description("Verify that Lead Conversion works for Verizon Brand with choosing existing Account and existing Opportunity")
    public void test() {
        step("1. Open Lead Convert page for the Sales Lead", () -> {
            leadConvertPage.openPage(steps.leadConvert.salesLead.getId());
        });

        step("2. Select New Business Account (from the 'Matched Accounts') and click on 'Apply' button", () ->
                leadConvertPage.selectMatchedAccount(steps.salesFlow.account.getId())
        );

        step("3. Check that 'Select existing Opportunity' option is preselected in the 'Opportunity' section " +
                "with the existing Opportunity in the list, " +
                "and that the Account's Contact is preselected in the 'Contact' section", () -> {
            leadConvertPage.opportunitySelectExistingOppOption.click();
            leadConvertPage.getMatchedOpportunity(steps.quoteWizard.opportunity.getId())
                    .getSelectButtonInput()
                    .shouldBe(selected);

            leadConvertPage.getMatchedContact(steps.salesFlow.contact.getId())
                    .getSelectButtonInput()
                    .shouldBe(selected);
            leadConvertPage.contactInfoSelectedContactName
                    .shouldHave(exactTextCaseSensitive(getFullName(steps.salesFlow.contact)));
        });

        step("4. Click 'Convert' button and check that there's a redirect to the Opportunity record page", () ->
                steps.leadConvert.pressConvertButton()
        );
    }
}
