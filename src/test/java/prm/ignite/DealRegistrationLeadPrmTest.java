package prm.ignite;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.prm.DealRegistrationData;
import com.aquiva.autotests.rc.model.prm.PortalUserData;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;
import prm.PrmSteps;

import static base.Pages.*;
import static com.aquiva.autotests.rc.model.prm.DealRegistrationData.CHANNEL_HARMONY_PARTNER_PROGRAM;
import static com.aquiva.autotests.rc.model.prm.PortalUserData.*;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.DqDealQualificationHelper.APPROVED_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("P1")
@Tag("DealRegistration")
@Tag("PRM")
@Tag("Lead")
@Tag("Ignite")
@Tag("LeadConvert")
public class DealRegistrationLeadPrmTest extends BaseTest {
    private final Steps steps;
    private final PrmSteps prmSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private Deal_Registration__c dealRegistration;
    private Lead lead;

    //  Test data
    private final PortalUserData portalUserData;
    private final DealRegistrationData dealRegTestData;

    public DealRegistrationLeadPrmTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        prmSteps = new PrmSteps();
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        portalUserData = prmSteps.getPortalUserData(IGNITE_PORTAL, PARTNER_HIERARCHY_LEVEL, PARTNER_FULL_ACCESS_PERSONA);
        dealRegTestData = prmSteps.getDealRegDataForIgnitePortal(CHANNEL_HARMONY_PARTNER_PROGRAM);
    }

    @BeforeEach
    public void setUpTest() {
        prmSteps.initLoginToIgnitePrmPortal(portalUserData.getUsernameSandbox(), portalUserData.getPassword());
    }

    @Test
    @TmsLink("CRM-35582")
    @TmsLink("CRM-35709")
    @TmsLink("CRM-27170")
    @DisplayName("CRM-35582 - Lead Creation from Deal Registration. \n" +
            "CRM-35709 - Lead Conversion for Channel Harmony. \n" +
            "CRM-27170 - Partner contact field on the Lead detail page")
    @Description("CRM-35582 - Test Case describes how to create a Lead from Deal Registration " +
            "with Partner_Program__c = 'Channel Harmony'. \n" +
            "CRM-35709 - Test Case describes how to create and convert a Lead from Deal Registration " +
            "with Partner_Program__c = 'Channel Harmony'. \n" +
            "CRM-27170 - Verify that Partner Contact field is displayed on the Lead detail page")
    public void test() {
        step("1. Choose 'Deal Registration' option from the Sales tab, and press '+ New' button", () -> {
            portalGlobalNavBar.salesButton.click();
            portalGlobalNavBar.dealRegistrationButton.click();
            dealRegistrationListPage.newButton.shouldBe(visible, ofSeconds(90)).click();
        });

        step("2. Populate all the required fields, and press the 'Submit' button", () -> {
            dealRegistrationCreationPage.submitFormWithPartnerProgram(dealRegTestData);

            dealRegistrationRecordPage.header.shouldBe(visible, ofSeconds(30));
        });

        step("3. Set the related Deal_Registration__c.Status__c = 'Approved' via API", () -> {
            dealRegistration = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM Deal_Registration__c " +
                            "WHERE Last_Name__c = '" + dealRegTestData.lastName + "'",
                    Deal_Registration__c.class);
            dealRegistration.setStatus__c(APPROVED_STATUS);
            enterpriseConnectionUtils.update(dealRegistration);
        });

        //  CRM-35582
        step("4. Find the created Lead record and check all common fields on it", () -> {
            lead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name, Company, Email, Phone, Lead_Brand_Name__c, Lead_Tier_Name__c, " +
                            "Number_of_Users__c, Street, City, State, PostalCode, Country, Industry, " +
                            "Estimated_Close_Date__c, Website, How_did_you_acquire_this_Lead__c, Description, " +
                            "Existing_Solution_Provider__c, Competitors__c, Prompting_Change__c, " +
                            "Partner_Program__c, Partner_Account__r.Name, Partner_Contact__r.Name, ConvertedOpportunityId " +
                            "FROM Lead " +
                            "WHERE Company = '" + dealRegTestData.companyName + "'",
                    Lead.class);

            assertThat(lead.getName())
                    .as("Lead.Name value")
                    .isEqualTo(dealRegTestData.firstName + " " + dealRegTestData.lastName);

            assertThat(lead.getCompany())
                    .as("Lead.Company value")
                    .isEqualTo(dealRegTestData.companyName);

            assertThat(lead.getEmail())
                    .as("Lead.Email value")
                    .isEqualTo(dealRegTestData.emailAddress);

            assertThat(lead.getPhone())
                    .as("Lead.Phone value")
                    .isEqualTo(dealRegTestData.phoneNumber);

            assertThat(lead.getLead_Brand_Name__c())
                    .as("Lead.Lead_Brand_Name__c value")
                    .isEqualTo(dealRegTestData.brand);

            assertThat(lead.getLead_Tier_Name__c())
                    .as("Lead.Lead_Tier_Name__c value")
                    .isEqualTo(dealRegTestData.tierName);

            assertThat(lead.getNumber_of_Users__c())
                    .as("Lead.Number_of_Users__c value")
                    .isEqualTo(Double.valueOf(dealRegTestData.forecastedUsers));

            assertThat(lead.getStreet())
                    .as("Lead.Street value")
                    .isEqualTo(dealRegTestData.address);

            assertThat(lead.getCity())
                    .as("Lead.City value")
                    .isEqualTo(dealRegTestData.city);

            assertThat(lead.getState())
                    .as("Lead.State value")
                    .isEqualTo(dealRegTestData.state);

            assertThat(lead.getPostalCode())
                    .as("Lead.PostalCode value")
                    .isEqualTo(dealRegTestData.postalCode);

            assertThat(lead.getCountry())
                    .as("Lead.Country value")
                    .isEqualTo(dealRegTestData.country);

            assertThat(lead.getIndustry())
                    .as("Lead.Industry value")
                    .isEqualTo(dealRegTestData.industry);

            var estimatedCloseDateActual = lead.getEstimated_Close_Date__c()
                    .toInstant().atZone(UTC).toLocalDate();
            assertThat(estimatedCloseDateActual)
                    .as("Lead.Estimated_Close_Date__c value")
                    .isEqualTo(dealRegTestData.estimatedCloseDate);

            assertThat(lead.getWebsite())
                    .as("Lead.Website value")
                    .isEqualTo(dealRegTestData.website);

            assertThat(lead.getHow_did_you_acquire_this_Lead__c())
                    .as("Lead.How_did_you_acquire_this_Lead__c value")
                    .isEqualTo(dealRegTestData.howDidYouAcquireThisLead);

            assertThat(lead.getDescription())
                    .as("Lead.Description value")
                    .isEqualTo(dealRegTestData.description);

            assertThat(lead.getExisting_Solution_Provider__c())
                    .as("Lead.Existing_Solution_Provider__c value")
                    .isEqualTo(dealRegTestData.existingSolutionProvider);

            assertThat(asList(lead.getCompetitors__c().split(";")))
                    .as("Lead.Competitors__c value")
                    .containsExactlyInAnyOrderElementsOf(dealRegTestData.competitors);

            assertThat(asList(lead.getPrompting_Change__c().split(";")))
                    .as("Lead.Prompting_Change__c value")
                    .containsExactlyInAnyOrderElementsOf(dealRegTestData.whatsPromptingChange);

            assertThat(lead.getPartner_Account__r().getName())
                    .as("Lead.Partner_Account__r.Name value")
                    .isEqualTo(portalUserData.accountName);

            assertThat(lead.getPartner_Program__c())
                    .as("Lead.Partner_Program__c value")
                    .isEqualTo(dealRegTestData.partnerProgram);

            //  CRM-27170
            assertThat(lead.getPartner_Contact__r().getName())
                    .as("Lead.Partner_Contact__r.Name value")
                    .isEqualTo(portalUserData.contactName);
        });

        //  CRM-35582
        step("5. Open the Edit Leads page in PRM for the created Lead, and check all common fields on it", () -> {
            portalEditLeadPage.openPage(lead.getId());

            portalEditLeadPage.firstName.shouldHave(exactTextCaseSensitive(dealRegTestData.firstName));
            portalEditLeadPage.lastName.shouldHave(exactTextCaseSensitive(dealRegTestData.lastName));
            portalEditLeadPage.emailAddress.shouldHave(exactTextCaseSensitive(dealRegTestData.emailAddress));
            portalEditLeadPage.phoneNumber.shouldHave(exactTextCaseSensitive(dealRegTestData.phoneNumber));
            portalEditLeadPage.street.shouldHave(exactTextCaseSensitive(dealRegTestData.address));
            portalEditLeadPage.city.shouldHave(exactTextCaseSensitive(dealRegTestData.city));
            portalEditLeadPage.stateOrProvince.shouldHave(exactTextCaseSensitive(dealRegTestData.state));
            portalEditLeadPage.postalCode.shouldHave(exactTextCaseSensitive(dealRegTestData.postalCode));
            portalEditLeadPage.billingCountry.shouldHave(exactTextCaseSensitive(dealRegTestData.country));

            portalEditLeadPage.brandName.shouldHave(exactTextCaseSensitive(dealRegTestData.brand));
            portalEditLeadPage.numberOfUsers.shouldHave(exactTextCaseSensitive(dealRegTestData.forecastedUsers));
            portalEditLeadPage.numberOfEmployees.shouldHave(exactTextCaseSensitive(dealRegTestData.numberOfEmployees));
            portalEditLeadPage.industry.shouldHave(exactTextCaseSensitive(dealRegTestData.industry));

            //  e.g. "5/1/2023"
            var estimatedCloseDateExpected = dealRegTestData.estimatedCloseDate.format(ofPattern("M/d/yyyy"));
            portalEditLeadPage.estimatedCloseDate.shouldHave(exactTextCaseSensitive(estimatedCloseDateExpected));

            portalEditLeadPage.howDidYouAcquireThisLead.shouldHave(exactTextCaseSensitive(dealRegTestData.howDidYouAcquireThisLead));
            portalEditLeadPage.description.shouldHave(exactTextCaseSensitive(dealRegTestData.description));
            portalEditLeadPage.website.shouldHave(exactTextCaseSensitive(dealRegTestData.website));

            //  e.g. "End of life equipment;Other"
            var whatsPromptingChangeExpected = String.join(";", dealRegTestData.whatsPromptingChange.stream().sorted().toList());
            portalEditLeadPage.whatIsPromptingChange.shouldHave(exactTextCaseSensitive(whatsPromptingChangeExpected));
            //  e.g. "Avaya;Zoom"
            var competitorsExpected = String.join(";", dealRegTestData.competitors.stream().sorted().toList());
            portalEditLeadPage.competitors.shouldHave(exactTextCaseSensitive(competitorsExpected));

            portalEditLeadPage.existingSolutionProvider.shouldHave(exactTextCaseSensitive(dealRegTestData.existingSolutionProvider));
            portalEditLeadPage.partnerAccount.shouldHave(exactTextCaseSensitive(portalUserData.accountName));
            portalEditLeadPage.partnerProgram.shouldHave(exactTextCaseSensitive(dealRegTestData.partnerProgram));

            //  CRM-27170
            portalEditLeadPage.partnerContact.shouldHave(exactTextCaseSensitive(portalUserData.contactName));
        });

        step("6. Transfer the ownership of the Lead to the CRM/SFDC user with 'Deal Desk Lightning' profile via API, " +
                "and log in as this user to CRM/SFDC", () -> {
            var crmUser = getUser()
                    .withProfile(DEAL_DESK_LIGHTNING_PROFILE)
                    //  to avoid issues with records sharing during the Lead Conversion (access to the Lead's Partner Account/Contact)
                    .withGroupMembership(NON_GSP_GROUP)
                    .execute();

            var leadToUpdate = new Lead();
            leadToUpdate.setId(lead.getId());
            leadToUpdate.setOwnerId(crmUser.getId());
            enterpriseConnectionUtils.update(leadToUpdate);

            steps.sfdc.initLoginToSfdcAsTestUser(crmUser);
        });

        step("7. Open the Lead Convert page for the test lead, " +
                "switch the toggle into 'Create New Account' position, " +
                "click 'Edit' button in the Opportunity Info section, populate the Close Date, and click 'Apply' button", () -> {
            leadConvertPage.openPage(lead.getId());
            leadConvertPage.newExistingAccountToggle.click();

            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();
            leadConvertPage.brandNameOutputField.shouldHave(exactTextCaseSensitive(dealRegTestData.brand));

            leadConvertPage.closeDateDatepicker.setDate(dealRegTestData.estimatedCloseDate);
            leadConvertPage.opportunityInfoApplyButton.click();
        });

        step("8. Press 'Convert' button",
                steps.leadConvert::pressConvertButton
        );

        //  CRM-35709
        step("9. Check that Lead is converted", () ->
                steps.leadConvert.checkLeadConversion(lead)
        );

        //  CRM-35709
        step("10. Check fields on the Opportunity converted from Lead", () -> {
            lead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, ConvertedOpportunityId, ConvertedAccountId, ConvertedContactId, " +
                            "LeadPartnerID__c " +
                            "FROM Lead " +
                            "WHERE Id = '" + lead.getId() + "'",
                    Lead.class);

            var convertedOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name, Brand_Name__c, Tier_Name__c, Forecasted_Users__c, Partner_ID__c, " +
                            "Website__c, Prompting_Change__c, Existing_Solution_Provider__c, " +
                            "CloseDate, Partner_Account__r.Name, Partner_Contact__r.Name, " +
                            "Description " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + lead.getConvertedOpportunityId() + "'",
                    Opportunity.class);

            assertThat(convertedOpportunity.getName())
                    .as("Opportunity.Name value")
                    .isEqualTo(dealRegTestData.companyName);

            assertThat(convertedOpportunity.getTier_Name__c())
                    .as("Opportunity.Tier_Name__c value")
                    .isEqualTo(dealRegTestData.tierName);

            assertThat(convertedOpportunity.getBrand_Name__c())
                    .as("Opportunity.Brand_Name__c value")
                    .isEqualTo(dealRegTestData.brand);

            assertThat(convertedOpportunity.getForecasted_Users__c())
                    .as("Opportunity.Forecasted_Users__c value")
                    .isEqualTo(Double.valueOf(dealRegTestData.forecastedUsers));

            assertThat(convertedOpportunity.getPartner_ID__c())
                    .as("Opportunity.Partner_ID__c value")
                    .isEqualTo(lead.getLeadPartnerID__c());

            assertThat(convertedOpportunity.getWebsite__c())
                    .as("Opportunity.Website__c value")
                    .isEqualTo(dealRegTestData.website);

            assertThat(asList(convertedOpportunity.getPrompting_Change__c().split(";")))
                    .as("Opportunity.Prompting_Change__c value")
                    .containsExactlyInAnyOrderElementsOf(dealRegTestData.whatsPromptingChange);

            assertThat(convertedOpportunity.getExisting_Solution_Provider__c())
                    .as("Opportunity.Existing_Solution_Provider__c value")
                    .isEqualTo(dealRegTestData.existingSolutionProvider);

            var estimatedCloseDateActual = convertedOpportunity.getCloseDate()
                    .toInstant().atZone(UTC).toLocalDate();
            assertThat(estimatedCloseDateActual)
                    .as("Opportunity.CloseDate value")
                    .isEqualTo(dealRegTestData.estimatedCloseDate);

            assertThat(convertedOpportunity.getPartner_Account__r().getName())
                    .as("Opportunity.Partner_Account__r.Name value")
                    .isEqualTo(portalUserData.accountName);

            assertThat(convertedOpportunity.getPartner_Contact__r().getName())
                    .as("Opportunity.Partner_Contact__r.Name value")
                    .isEqualTo(portalUserData.contactName);

            assertThat(convertedOpportunity.getDescription())
                    .as("Opportunity.Description value")
                    .isEqualTo(dealRegTestData.description);
        });

        //  CRM-35709
        step("11. Check fields on the Account record from converted Lead", () -> {
            var convertedAccount = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Name, RC_Brand__c, BillingAddress, Partner_Contact__r.Name, " +
                            "Partner_Account__r.Name, Partner_ID__c, Website " +
                            "FROM Account " +
                            "WHERE Id = '" + lead.getConvertedAccountId() + "'",
                    Account.class);

            assertThat(convertedAccount.getName())
                    .as("Account.Name value")
                    .isEqualTo(dealRegTestData.companyName);

            assertThat(convertedAccount.getRC_Brand__c())
                    .as("Account.RC_Brand__c value")
                    .isEqualTo(dealRegTestData.brand);

            assertThat(convertedAccount.getBillingAddress().getCountry())
                    .as("Account.BillingAddress.Country value")
                    .isEqualTo(dealRegTestData.country);

            assertThat(convertedAccount.getBillingAddress().getPostalCode())
                    .as("Account.BillingAddress.PostalCode value")
                    .isEqualTo(dealRegTestData.postalCode);

            assertThat(convertedAccount.getBillingAddress().getState())
                    .as("Account.BillingAddress.State value")
                    .isEqualTo(dealRegTestData.state);

            assertThat(convertedAccount.getBillingAddress().getStreet())
                    .as("Account.BillingAddress.Street value")
                    .isEqualTo(dealRegTestData.address);

            assertThat(convertedAccount.getPartner_Contact__r().getName())
                    .as("Account.Partner_Contact__r.Name value")
                    .isEqualTo(portalUserData.contactName);

            assertThat(convertedAccount.getPartner_Account__r().getName())
                    .as("Account.Partner_Account__r.Name value")
                    .isEqualTo(portalUserData.accountName);

            assertThat(convertedAccount.getWebsite())
                    .as("Account.Website value")
                    .isEqualTo(dealRegTestData.website);
        });

        //  CRM-35709
        step("12. Check fields on the Contact record from converted Lead", () -> {
            var convertedContact = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, FirstName, LastName, Email, Phone " +
                            "FROM Contact " +
                            "WHERE Id = '" + lead.getConvertedContactId() + "'",
                    Contact.class);
            assertThat(convertedContact.getFirstName())
                    .as("Contact.FirstName value")
                    .isEqualTo(dealRegTestData.firstName);

            assertThat(convertedContact.getLastName())
                    .as("Contact.LastName value")
                    .isEqualTo(dealRegTestData.lastName);

            assertThat(convertedContact.getEmail())
                    .as("Contact.Email value")
                    .isEqualTo(dealRegTestData.emailAddress);

            assertThat(convertedContact.getPhone())
                    .as("Contact.Phone value")
                    .isEqualTo(dealRegTestData.phoneNumber);
        });

        step("13. Transfer the ownership of the converted Account and Opportunity " +
                "to the PRM portal user with username = '" + portalUserData.username + "' via API", () -> {
            var portalUser = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM User " +
                            "WHERE Username = '" + portalUserData.getUsernameSandbox() + "'",
                    User.class);

            var accountToUpdate = new Account();
            accountToUpdate.setId(lead.getConvertedAccountId());
            accountToUpdate.setOwnerId(portalUser.getId());

            var opportunityToUpdate = new Opportunity();
            opportunityToUpdate.setId(lead.getConvertedOpportunityId());
            opportunityToUpdate.setOwnerId(portalUser.getId());

            enterpriseConnectionUtils.update(accountToUpdate, opportunityToUpdate);
        });

        //  CRM-35709
        step("14. Open the Customer Details Page for the converted Account in the PRM Portal and check the fields", () -> {
            portalCustomerDetailsPage.openPage(lead.getConvertedAccountId());

            portalCustomerDetailsPage.accountName.shouldHave(exactTextCaseSensitive(dealRegTestData.companyName));
            portalCustomerDetailsPage.billingStreet.shouldHave(exactTextCaseSensitive(dealRegTestData.address));
            portalCustomerDetailsPage.billingCity.shouldHave(exactTextCaseSensitive(dealRegTestData.city));
            portalCustomerDetailsPage.billingCountry.shouldHave(exactTextCaseSensitive(dealRegTestData.country));
            portalCustomerDetailsPage.billingPostalCode.shouldHave(exactTextCaseSensitive(dealRegTestData.postalCode));
            portalCustomerDetailsPage.billingState.shouldHave(exactTextCaseSensitive(dealRegTestData.state));
            portalCustomerDetailsPage.partnerContact.shouldHave(exactTextCaseSensitive(portalUserData.contactName));
        });

        //  CRM-35709
        step("15. Open the Opportunity Details Page for the converted Opportunity in the PRM Portal and check the fields", () -> {
            step("Wait until Opportunity.Number_of_Employees__c gets the value via async process", () -> {
                assertWithTimeout(() -> {
                    var updatedOpportunity = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, Number_of_Employees__c " +
                                    "FROM Opportunity " +
                                    "WHERE Id = '" + lead.getConvertedOpportunityId() + "'",
                            Opportunity.class);
                    assertNotNull(updatedOpportunity.getNumber_of_Employees__c(), "Opportunity.Number_of_Employees__c value");
                    return null;
                }, ofSeconds(180), ofSeconds(5));
            });

            portalOpportunityDetailsPage.openPage(lead.getConvertedOpportunityId());

            portalOpportunityDetailsPage.name.shouldHave(exactTextCaseSensitive(dealRegTestData.companyName));
            portalOpportunityDetailsPage.brandName.shouldHave(exactTextCaseSensitive(dealRegTestData.brand));
            portalOpportunityDetailsPage.partnerContact.shouldHave(exactTextCaseSensitive(portalUserData.contactName));

            //  e.g. "5/1/2023"
            var estimatedCloseDateExpected = dealRegTestData.estimatedCloseDate.format(ofPattern("M/d/yyyy"));
            portalOpportunityDetailsPage.closeDate.shouldHave(exactTextCaseSensitive(estimatedCloseDateExpected));
            //  e.g. "End of life equipment;Other"
            var whatsPromptingChangeExpected = String.join(";", dealRegTestData.whatsPromptingChange.stream().sorted().toList());
            portalOpportunityDetailsPage.whatIsPromptingChange.shouldHave(exactTextCaseSensitive(whatsPromptingChangeExpected));

            portalOpportunityDetailsPage.existingSolutionProvider.shouldHave(exactTextCaseSensitive(dealRegTestData.existingSolutionProvider));
            portalOpportunityDetailsPage.partnerId.shouldHave(exactTextCaseSensitive(lead.getLeadPartnerID__c()));
            portalOpportunityDetailsPage.tierName.shouldHave(exactTextCaseSensitive(dealRegTestData.tierName));
            portalOpportunityDetailsPage.forecastedUsers.shouldHave(exactTextCaseSensitive(dealRegTestData.forecastedUsers));
            portalOpportunityDetailsPage.description.shouldHave(exactTextCaseSensitive(dealRegTestData.description));
            portalOpportunityDetailsPage.numberOfEmployees.shouldHave(exactTextCaseSensitive(dealRegTestData.numberOfEmployees));
        });
    }
}
