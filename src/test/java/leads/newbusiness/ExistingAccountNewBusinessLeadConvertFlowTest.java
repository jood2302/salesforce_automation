package leads.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Lead;
import com.sforce.soap.enterprise.sobject.Quote;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.BI_FORMAT;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.NEW_BUSINESS_TYPE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("PDV")
@Tag("LeadConvert")
@Tag("PackageTab")
@Tag("NGBS")
public class ExistingAccountNewBusinessLeadConvertFlowTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String chargeTerm;
    private final String currencyIsoCode;
    private final String ringCentralBiId;
    private final String ringCentralBiName;
    private final String serviceName;
    private final Package officePackage;

    public ExistingAccountNewBusinessLeadConvertFlowTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        chargeTerm = data.chargeTerm;
        currencyIsoCode = data.currencyISOCode;
        ringCentralBiId = data.businessIdentity.id;
        ringCentralBiName = data.getBusinessIdentityName();
        serviceName = data.packageFolders[0].name;
        officePackage = data.packageFolders[0].packages[0];
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);

        step("Set Sales Lead's Email and Phone from Account's Contact via API", () -> {
            steps.leadConvert.salesLead.setEmail(steps.salesFlow.contact.getEmail());
            steps.leadConvert.salesLead.setPhone(steps.salesFlow.contact.getPhone());

            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-10771")
    @TmsLink("CRM-21378")
    @TmsLink("CRM-22734")
    @DisplayName("CRM-10771 - New Business. Existing Account. Lead Conversion. \n" +
            "CRM-21378 - New Business | Lead Convert | Save and Continue button is available. \n" +
            "CRM-22734 - Business Identity field is filled after Opportunity was created from Lead Convert")
    @Description("CRM-10771 - Verify that Lead Conversion is functional with Opportunity. \n" +
            "CRM-21378 - Verify that 'Save and Continue' button behaves correctly. \n" +
            "CRM-22734 - Verify that Business Identity field is filled after Opportunity was created from Lead Convert.")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select New Business Account (from the 'Matched Accounts' table), " +
                "and click 'Apply' button in Account Info section", () ->
                leadConvertPage.selectMatchedAccount(steps.salesFlow.account.getId())
        );

        step("3. Click 'Edit' in Opportunity Section, " +
                "check that Business Identity = 'RingCentral Inc.', select Service = 'Office', " +
                "populate Close Date field and click 'Apply' button", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.businessIdentityPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(ringCentralBiName));
            leadConvertPage.selectService(serviceName);
            leadConvertPage.closeDateDatepicker.setTomorrowDate();

            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("4. Select Contact Role and click 'Apply' button in Contact Role section",
                leadConvertPage::selectDefaultOpportunityRole
        );

        step("5. Press 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        //  CRM-10771
        step("6. Check converted Lead + created Account, Contact and Opportunity", () -> {
            var convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ConvertedAccountId, ConvertedOpportunityId, ConvertedContactId, " +
                            "ConvertedOpportunity.Type " +
                            "FROM Lead " +
                            "WHERE Id = '" + steps.leadConvert.salesLead.getId() + "'",
                    Lead.class);

            step("Check that Lead is converted for Account and Contact", () -> {
                assertThat(convertedLead.getConvertedAccountId())
                        .as("ConvertedLead.ConvertedAccountId value ( = selected New Business Account.Id)")
                        .isEqualTo(steps.salesFlow.account.getId());

                assertThat(convertedLead.getConvertedContactId())
                        .as("ConvertedLead.ConvertedContactId value ( = selected New Business Account's Contact.Id)")
                        .isEqualTo(steps.salesFlow.contact.getId());
            });

            step("Check Converted Opportunity", () -> {
                assertThat(convertedLead.getConvertedOpportunityId())
                        .as("ConvertedLead.ConvertedOpportunityId value")
                        .isNotNull();

                assertThat(convertedLead.getConvertedOpportunity().getType())
                        .as("ConvertedLead.ConvertedOpportunity.Type value")
                        .isEqualTo(NEW_BUSINESS_TYPE);
            });
        });

        //  CRM-21378
        step("7. Switch to the Quote Wizard on the Opportunity record page, " +
                "add a new Sales Quote, check the 'Save and Continue' button, " +
                "select a package, and save changes", () -> {
            opportunityPage.switchToNGBSQW();
            steps.quoteWizard.addNewSalesQuote();
            packagePage.saveAndContinueButton.shouldBe(visible, enabled);

            packagePage.packageSelector.selectPackage(chargeTerm, serviceName, officePackage);
            packagePage.saveChanges();
        });

        //  CRM-22734
        step("8. Check the 'BusinessIdentity__c' fields on the created Opportunity and its Quote", () -> {
            var expectedBusinessIdentityValue = String.format(BI_FORMAT, currencyIsoCode, ringCentralBiId);

            var createdQuote = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT BusinessIdentity__c, Opportunity.BusinessIdentity__c " +
                            "FROM Quote " +
                            "WHERE Id = '" + wizardPage.getSelectedQuoteId() + "'",
                    Quote.class);

            assertThat(createdQuote.getOpportunity().getBusinessIdentity__c())
                    .as("Quote.Opportunity.BusinessIdentity__c value")
                    .isEqualTo(expectedBusinessIdentityValue);
            assertThat(createdQuote.getBusinessIdentity__c())
                    .as("Quote.BusinessIdentity__c value")
                    .isEqualTo(expectedBusinessIdentityValue);
        });
    }
}
