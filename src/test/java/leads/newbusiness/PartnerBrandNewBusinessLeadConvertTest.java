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
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToIntToString;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("Avaya")
@Tag("PDV")
public class PartnerBrandNewBusinessLeadConvertTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String avayaBrand;
    private final String avayaBusinessIdentity;
    private final String forecastedUsers;
    private final String officeService;

    public PartnerBrandNewBusinessLeadConvertTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/Avaya_Office_Monthly_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        avayaBrand = data.brandName;
        avayaBusinessIdentity = data.getBusinessIdentityName();
        forecastedUsers = data.forecastedUsers;
        officeService = data.packageFolders[0].name;
    }

    @BeforeEach
    public void setUpTest() {
        var dealDeskUser = steps.salesFlow.getDealDeskUser();
        steps.leadConvert.createPartnerAccountAndLead(dealDeskUser);
        steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
    }

    @Test
    @TmsLink("CRM-12949")
    @DisplayName("CRM-12949 - Lead Conversion flow for Partner Brands")
    @Description("Verify if the Account has RC_Brand__c equal to: \n" +
            "- Avaya Cloud Office \n" +
            "- Unify Office \n\n" +
            "then after the Lead conversion the Created Opportunity, Account and Contact will have the same data as was set on Lead Conversion Page")
    public void test() {
        step("1. Open Lead Convert page for the Partner test lead", () ->
                leadConvertPage.openDirect(steps.leadConvert.partnerLead.getId())
        );

        step("2. Check that the 'Avaya Cloud Office' is selected as Business Identity in the Opportunity Info Section," +
                "click 'Edit' button, populate Close Date field and click 'Apply' button", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityBusinessIdentityNonEditable
                    .shouldHave(exactTextCaseSensitive(avayaBusinessIdentity), ofSeconds(60));

            leadConvertPage.opportunityInfoEditButton.click();
            leadConvertPage.closeDateDatepicker.setTomorrowDate();
            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("3. Press 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("4. Check that Lead is converted", () ->
                steps.leadConvert.checkLeadConversion(steps.leadConvert.partnerLead)
        );

        step("5. Check the converted Account, Opportunity, and Contact records", () -> {
            var convertedLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Company, FirstName, LastName, Lead_Brand_Name__c, Address, Email, Phone, " +
                            "ConvertedAccountId, ConvertedOpportunityId, ConvertedContactId " +
                            "FROM Lead " +
                            "WHERE Id = '" + steps.leadConvert.partnerLead.getId() + "'",
                    Lead.class);

            step("Check the converted Opportunity", () -> {
                var opportunityFromConvertedLead = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, Name, Brand_Name__c, Tier_Name__c, Forecasted_Users__c " +
                                "FROM Opportunity " +
                                "WHERE Id = '" + convertedLead.getConvertedOpportunityId() + "'",
                        Opportunity.class);
                assertThat(opportunityFromConvertedLead.getName())
                        .as("Opportunity.Name value")
                        .isEqualTo(steps.leadConvert.partnerLead.getCompany());

                assertThat(opportunityFromConvertedLead.getBrand_Name__c())
                        .as("Opportunity.Brand_Name__c value")
                        .isEqualTo(avayaBrand);

                assertThat(opportunityFromConvertedLead.getTier_Name__c())
                        .as("Opportunity.Tier_Name__c value")
                        .isEqualTo(officeService);

                assertThat(doubleToIntToString(opportunityFromConvertedLead.getForecasted_Users__c()))
                        .as("Opportunity.Forecasted_Users__c value")
                        .isEqualTo(forecastedUsers);
            });

            step("Check the converted Account", () -> {
                var accountFromLead = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, Name, RC_Brand__c, BillingAddress " +
                                "FROM Account " +
                                "WHERE Id = '" + convertedLead.getConvertedAccountId() + "'",
                        Account.class);
                assertThat(accountFromLead.getName())
                        .as("Account.Name value")
                        .isEqualTo(convertedLead.getCompany());

                assertThat(accountFromLead.getRC_Brand__c())
                        .as("Account.RC_Brand__c value")
                        .isEqualTo(convertedLead.getLead_Brand_Name__c());

                step("Check the Billing Address on Account", () -> {
                    assertThat(accountFromLead.getBillingAddress().getCountry())
                            .as("Account.BillingAddress.Country value")
                            .isEqualTo(convertedLead.getAddress().getCountry());

                    assertThat(accountFromLead.getBillingAddress().getPostalCode())
                            .as("Account.BillingAddress.PostalCode value")
                            .isEqualTo(convertedLead.getAddress().getPostalCode());

                    assertThat(accountFromLead.getBillingAddress().getState())
                            .as("Account.BillingAddress.State value")
                            .isEqualTo(convertedLead.getAddress().getState());

                    assertThat(accountFromLead.getBillingAddress().getStreet())
                            .as("Account.BillingAddress.Street value")
                            .isEqualTo(convertedLead.getAddress().getStreet());
                });
            });

            step("Check the converted Contact", () -> {
                var contact = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT FirstName, LastName, Phone, Email " +
                                "FROM Contact " +
                                "WHERE Id = '" + convertedLead.getConvertedContactId() + "'",
                        Contact.class);

                assertThat(contact.getFirstName())
                        .as("Contact.FirstName value")
                        .isEqualTo(steps.leadConvert.partnerLead.getFirstName());

                assertThat(contact.getLastName())
                        .as("Contact.LastName value")
                        .isEqualTo(steps.leadConvert.partnerLead.getLastName());

                assertThat(contact.getEmail())
                        .as("Contact.Email value")
                        .isEqualTo(steps.leadConvert.partnerLead.getEmail());

                assertThat(contact.getPhone())
                        .as("Contact.Phone value")
                        .isEqualTo(steps.leadConvert.partnerLead.getPhone());
            });
        });
    }
}
