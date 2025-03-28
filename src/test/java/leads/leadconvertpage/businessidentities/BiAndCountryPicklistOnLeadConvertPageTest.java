package leads.leadconvertpage.businessidentities;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Default_Business_Identity_Mapping__mdt;
import com.sforce.soap.enterprise.sobject.Lead;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.*;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("P1")
@Tag("Avaya")
@Tag("LeadConvert")
@Tag("Ignite")
public class BiAndCountryPicklistOnLeadConvertPageTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private String salesLeadId;
    private String leadCountry;

    //  Test data
    private final String avayaBrandName;
    private final List<String> avayaAvailableBusinessIdentities;

    public BiAndCountryPicklistOnLeadConvertPageTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/Avaya_Office_Monthly_Contract.json",
                Dataset.class);

        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        avayaBrandName = data.brandName;
        avayaAvailableBusinessIdentities = List.of(AVAYA_US_BUSINESS_IDENTITY_NAME, AVAYA_CA_BUSINESS_IDENTITY_NAME);
    }

    @BeforeEach
    public void setUpTest() {
        var dealDeskUser = steps.salesFlow.getDealDeskUser();

        steps.leadConvert.createPartnerAccountAndLead(dealDeskUser);

        steps.leadConvert.createSalesLead(dealDeskUser);
        salesLeadId = steps.leadConvert.salesLead.getId();

        step("Check that 'Default Business Identity Mapping' record of custom metadata type exists " +
                "for 'Avaya Cloud Office' (US) business identity", () -> {
            var avayaUsDefaultBiMappingCustomMetadataRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Sub_Brand__c = null " +
                            "AND Brand__c = '" + avayaBrandName + "' " +
                            "AND Country__c = '" + US_BILLING_COUNTRY + "' " +
                            "AND Default_Business_Identity__c = '" + AVAYA_US_BUSINESS_IDENTITY_NAME + "'",
                    Default_Business_Identity_Mapping__mdt.class);
            assertThat(avayaUsDefaultBiMappingCustomMetadataRecords.size())
                    .as("Quantity of Default_Business_Identity_Mapping__mdt records " +
                            "for 'Avaya Cloud Office' (US) business identity")
                    .isEqualTo(1);
        });

        step("Login as a user with 'Deal Desk Lightning' profile and with 'Change Business Identity' Permission Set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(dealDeskUser);
        });
    }

    @Test
    @TmsLink("CRM-22463")
    @TmsLink("CRM-35220")
    @DisplayName("CRM-22463 - Business Identities for Partner Brands. Avaya. \n" +
            "CRM-35220 - 'Country' picklist displayed on LCP for Sales and Partner Lead's.")
    @Description("CRM-22463 - Verify if the Partner Lead's Partner Account has RC_Brand__c equal to: \n" +
            "- Avaya Cloud Office \n" +
            "- Unify Office \n\n" +
            "then on the LC page Business Identity picklist only contains values corresponding to its Brand. \n\n" +
            "CRM-35220 - Verify that \n" +
            "- 'Country' picklist is displayed on LCP for the Sales Lead \n" +
            "- 'Country' picklist is not displayed on LCP for the Partner Lead")
    public void test() {
        //  CRM-35220
        step("1. Open Lead Convert page for the PARTNER test lead, and check that the Country field is NOT displayed " +
                "in the Opportunity Info Section in non-edit mode", () -> {
            leadConvertPage.openDirect(steps.leadConvert.partnerLead.getId());

            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityCountryNonEditable.shouldBe(hidden);
        });

        //  CRM-22463
        step("2. Click 'Edit' in Opportunity Section, and verify that Business Identity picklist only contains '" +
                avayaBrandName + "'-related Business Identities", () -> {
            leadConvertPage.opportunityInfoEditButton.shouldBe(visible, ofSeconds(60)).click();
            leadConvertPage.businessIdentityPicklist.shouldBe(enabled);
            leadConvertPage.businessIdentityPicklist.getOptions()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(avayaAvailableBusinessIdentities));
        });

        //  CRM-35220
        step("3. Check that Country picklist is NOT displayed", () -> {
            leadConvertPage.countryPicklist.shouldBe(hidden);
        });

        step("4. Open Lead Convert page for the SALES test lead, and switch the toggle into 'Create New Account' position " +
                "in the Account Info section", () -> {
            leadConvertPage.openPage(salesLeadId);
            leadConvertPage.newExistingAccountToggle.click();
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
        });

        //  CRM-35220
        step("5. Check that the Country field is displayed in the Opportunity Info Section in non-edit mode " +
                "with Lead.Country__c value from the Sales Lead", () -> {
            var salesLead = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, Country__c " +
                            "FROM Lead " +
                            "WHERE Id = '" + salesLeadId + "'",
                    Lead.class);
            leadCountry = salesLead.getCountry__c();

            leadConvertPage.opportunityCountryNonEditable.shouldHave(exactTextCaseSensitive(leadCountry));
        });

        //  CRM-35220
        step("6. Click 'Edit' in Opportunity Section, " +
                "and verify that Country picklist is displayed with preselected '" + leadCountry + "' value", () -> {
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.countryPicklist.shouldBe(enabled);
            leadConvertPage.countryPicklist.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(leadCountry));
        });
    }
}
