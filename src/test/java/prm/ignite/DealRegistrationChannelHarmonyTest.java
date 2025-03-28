package prm.ignite;

import base.BaseTest;
import com.aquiva.autotests.rc.model.prm.DealRegistrationData;
import com.aquiva.autotests.rc.model.prm.PortalUserData;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Deal_Registration__c;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;
import prm.PrmSteps;

import static base.Pages.*;
import static com.aquiva.autotests.rc.model.prm.DealRegistrationData.CHANNEL_HARMONY_PARTNER_PROGRAM;
import static com.aquiva.autotests.rc.model.prm.PortalUserData.*;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToIntToString;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.visible;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("DealRegistration")
@Tag("PRM")
public class DealRegistrationChannelHarmonyTest extends BaseTest {
    private final PrmSteps prmSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final PortalUserData portalUserData;
    private final DealRegistrationData dealRegTestData;

    public DealRegistrationChannelHarmonyTest() {
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
    @TmsLink("CRM-35583")
    @DisplayName("CRM-35583 - Deal Registration creation for Channel Harmony")
    @Description("Verify that Deal Registration for 'Channel Harmony' Partner Program can be created in PRM")
    public void test() {
        step("1. Choose 'Deal Registration' option from the Sales tab, and press '+ New' button", () -> {
            portalGlobalNavBar.salesButton.click();
            portalGlobalNavBar.dealRegistrationButton.click();
            dealRegistrationListPage.newButton.shouldBe(visible, ofSeconds(90)).click();
        });

        step("2. Populate all the required fields, and press the 'Submit' button", () -> {
            dealRegistrationCreationPage.submitFormWithPartnerProgram(dealRegTestData);
        });

        step("3. Check the created Deal Registration record on the Deal Registration record page in PRM", () -> {
            dealRegistrationRecordPage.header.shouldBe(visible, ofSeconds(30));

            dealRegistrationRecordPage.firstName.shouldHave(exactTextCaseSensitive(dealRegTestData.firstName));
            dealRegistrationRecordPage.lastName.shouldHave(exactTextCaseSensitive(dealRegTestData.lastName));
            dealRegistrationRecordPage.companyName.shouldHave(exactTextCaseSensitive(dealRegTestData.companyName));
            dealRegistrationRecordPage.emailAddress.shouldHave(exactTextCaseSensitive(dealRegTestData.emailAddress));
            dealRegistrationRecordPage.phoneNumber.shouldHave(exactTextCaseSensitive(dealRegTestData.phoneNumber));

            dealRegistrationRecordPage.address.shouldHave(exactTextCaseSensitive(dealRegTestData.address));
            dealRegistrationRecordPage.city.shouldHave(exactTextCaseSensitive(dealRegTestData.city));
            dealRegistrationRecordPage.state.shouldHave(exactTextCaseSensitive(dealRegTestData.state));
            dealRegistrationRecordPage.postalCode.shouldHave(exactTextCaseSensitive(dealRegTestData.postalCode));
            dealRegistrationRecordPage.country.shouldHave(exactTextCaseSensitive(dealRegTestData.country));

            dealRegistrationRecordPage.forecastedUsers.shouldHave(exactTextCaseSensitive(dealRegTestData.forecastedUsers));
            dealRegistrationRecordPage.industry.shouldHave(exactTextCaseSensitive(dealRegTestData.industry));
            dealRegistrationRecordPage.website.shouldHave(exactTextCaseSensitive(dealRegTestData.website));
            dealRegistrationRecordPage.numberOfEmployees.shouldHave(exactTextCaseSensitive(dealRegTestData.numberOfEmployees));
            dealRegistrationRecordPage.howDidYouAcquireThisLead.shouldHave(exactTextCaseSensitive(dealRegTestData.howDidYouAcquireThisLead));
            dealRegistrationRecordPage.description.shouldHave(exactTextCaseSensitive(dealRegTestData.description));
            dealRegistrationRecordPage.existingSolutionProvider.shouldHave(exactTextCaseSensitive(dealRegTestData.existingSolutionProvider));

            //  e.g. "Avaya;Zoom"
            var competitorsExpected = String.join(";", dealRegTestData.competitors);
            dealRegistrationRecordPage.competitors.shouldHave(exactTextCaseSensitive(competitorsExpected));
            //  e.g. "End of life equipment;Other"
            var whatsPromptingChangeExpected = String.join(";", dealRegTestData.whatsPromptingChange);
            dealRegistrationRecordPage.whatsPromptingChange.shouldHave(exactTextCaseSensitive(whatsPromptingChangeExpected));

            dealRegistrationRecordPage.partnerProgram.shouldHave(exactTextCaseSensitive(dealRegTestData.partnerProgram));
            dealRegistrationRecordPage.isThisAnExistingMitelCustomer.shouldHave(exactTextCaseSensitive(dealRegTestData.isThisAnExistingMitelCustomer));
        });

        step("4. Check the created Deal Registration record in SFDC", () -> {
            var dealRegistration = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, First_Name__c, Last_Name__c, Company_Name__c, Email_Address__c, Phone_Number__c, " +
                            "Country__c, State__c, City__c, Address__c, Postal_Code__c, " +
                            "Forecasted_Users__c, Industry__c, Website__c, Number_of_Employees__c, " +
                            "Estimated_Close_Date__c, How_did_you_acquire_this_Lead__c, " +
                            "Description__c, Existing_Solution_Provider__c, " +
                            "Competitor_s__c, Whats_prompting_change__c, " +
                            "Partner_Program__c, Mitel_Customer_or_Prospect__c, " +
                            "Partner_Contact__r.Name " +
                            "FROM Deal_Registration__c " +
                            "WHERE Last_Name__c = '" + dealRegTestData.lastName + "'",
                    Deal_Registration__c.class);

            assertThat(dealRegistration.getFirst_Name__c())
                    .as("Deal_Registration__c.First_Name__c value")
                    .isEqualTo(dealRegTestData.firstName);

            assertThat(dealRegistration.getLast_Name__c())
                    .as("Deal_Registration__c.Last_Name__c value")
                    .isEqualTo(dealRegTestData.lastName);

            assertThat(dealRegistration.getCompany_Name__c())
                    .as("Deal_Registration__c.Company_Name__c value")
                    .isEqualTo(dealRegTestData.companyName);

            assertThat(dealRegistration.getEmail_Address__c())
                    .as("Deal_Registration__c.Email_Address__c value")
                    .isEqualTo(dealRegTestData.emailAddress);

            assertThat(dealRegistration.getPhone_Number__c())
                    .as("Deal_Registration__c.Phone_Number__c value")
                    .isEqualTo(dealRegTestData.phoneNumber);

            assertThat(dealRegistration.getCountry__c())
                    .as("Deal_Registration__c.Country__c value")
                    .isEqualTo(dealRegTestData.country);

            assertThat(dealRegistration.getState__c())
                    .as("Deal_Registration__c.State__c value")
                    .isEqualTo(dealRegTestData.state);

            assertThat(dealRegistration.getCity__c())
                    .as("Deal_Registration__c.City__c value")
                    .isEqualTo(dealRegTestData.city);

            assertThat(dealRegistration.getAddress__c())
                    .as("Deal_Registration__c.Address__c value")
                    .isEqualTo(dealRegTestData.address);

            assertThat(dealRegistration.getPostal_Code__c())
                    .as("Deal_Registration__c.Postal_Code__c value")
                    .isEqualTo(dealRegTestData.postalCode);

            assertThat(doubleToIntToString(dealRegistration.getForecasted_Users__c()))
                    .as("Deal_Registration__c.Forecasted_Users__c value")
                    .isEqualTo(dealRegTestData.forecastedUsers);

            assertThat(dealRegistration.getIndustry__c())
                    .as("Deal_Registration__c.Industry__c value")
                    .isEqualTo(dealRegTestData.industry);

            assertThat(dealRegistration.getWebsite__c())
                    .as("Deal_Registration__c.Website__c value")
                    .isEqualTo(dealRegTestData.website);

            assertThat(dealRegistration.getNumber_of_Employees__c())
                    .as("Deal_Registration__c.Number_of_Employees__c value")
                    .isEqualTo(dealRegTestData.numberOfEmployees);

            var estimatedCloseDateActual = dealRegistration.getEstimated_Close_Date__c()
                    .toInstant().atZone(UTC).toLocalDate();
            assertThat(estimatedCloseDateActual)
                    .as("Deal_Registration__c.Estimated_Close_Date__c value (as LocalDate)")
                    .isEqualTo(dealRegTestData.estimatedCloseDate);

            assertThat(dealRegistration.getHow_did_you_acquire_this_Lead__c())
                    .as("Deal_Registration__c.How_did_you_acquire_this_Lead__c value")
                    .isEqualTo(dealRegTestData.howDidYouAcquireThisLead);

            assertThat(dealRegistration.getDescription__c())
                    .as("Deal_Registration__c.Description__c value")
                    .isEqualTo(dealRegTestData.description);

            assertThat(dealRegistration.getExisting_Solution_Provider__c())
                    .as("Deal_Registration__c.Existing_Solution_Provider__c value")
                    .isEqualTo(dealRegTestData.existingSolutionProvider);

            assertThat(dealRegistration.getCompetitor_s__c())
                    .as("Deal_Registration__c.Competitor_s__c values")
                    .isEqualTo(String.join(";", dealRegTestData.competitors));

            assertThat(dealRegistration.getWhats_prompting_change__c())
                    .as("Deal_Registration__c.Whats_prompting_change__c values")
                    .isEqualTo(String.join(";", dealRegTestData.whatsPromptingChange));

            assertThat(dealRegistration.getPartner_Program__c())
                    .as("Deal_Registration__c.Partner_Program__c value")
                    .isEqualTo(dealRegTestData.partnerProgram);

            assertThat(dealRegistration.getMitel_Customer_or_Prospect__c())
                    .as("Deal_Registration__c.Mitel_Customer_or_Prospect__c value")
                    .isEqualTo(dealRegTestData.isThisAnExistingMitelCustomer);

            assertThat(dealRegistration.getPartner_Contact__r().getName())
                    .as("Deal_Registration__c.Partner_Contact__r.Name value")
                    .isEqualTo(portalUserData.contactName);
        });
    }
}
