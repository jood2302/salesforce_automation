package leads.leadconvertpage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Default_Business_Identity_Mapping__mdt;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static base.Pages.leadConvertPage;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Ignite")
public class DefaultBusinessIdentityForIndiaLeadConvertPageTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String indiaCountryName;
    private final String rcIndiaBrandName;
    private final String rcIndiaDefaultBI;
    private final List<String> rcIndiaAvailableBis;
    private final List<String> rcIndiaAllBis;

    public DefaultBusinessIdentityForIndiaLeadConvertPageTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_India_Mumbai_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        indiaCountryName = data.getBillingCountry();
        rcIndiaBrandName = data.brandName;
        rcIndiaDefaultBI = "RingCentral India Bangalore";
        rcIndiaAvailableBis = List.of("RingCentral India Mumbai", "RingCentral India Maharashtra",
                "RingCentral India Andhra Pradesh / Telangana", "RingCentral India Delhi Metro");

        rcIndiaAllBis = new ArrayList<>(rcIndiaAvailableBis);
        rcIndiaAllBis.add(rcIndiaDefaultBI);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.leadConvert.createSalesLead(salesRepUser);

        step("Set the test Sales Lead's Country__c and Country fields to '" + indiaCountryName + "' " +
                "and Lead_Brand_Name__c = '" + rcIndiaBrandName + "' via API", () -> {
            steps.leadConvert.salesLead.setCountry__c(indiaCountryName);
            steps.leadConvert.salesLead.setCountry(indiaCountryName);
            steps.leadConvert.salesLead.setLead_Brand_Name__c(rcIndiaBrandName);

            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        step("Check that 'Default Business Identity Mapping' record of custom metadata type exists " +
                "for '" + rcIndiaDefaultBI + "' and other available India business identities (" + rcIndiaAvailableBis + ")", () -> {
            var rcIndiaDefaultBiMappingCustomMetadataRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Sub_Brand__c = null " +
                            "AND Brand__c = '" + rcIndiaBrandName + "' " +
                            "AND Country__c = '" + indiaCountryName + "' " +
                            "AND Default_Business_Identity__c = '" + rcIndiaDefaultBI + "' " +
                            "AND Available_Business_Identities__c = '" + String.join(";", rcIndiaAvailableBis) + "'",
                    Default_Business_Identity_Mapping__mdt.class);
            assertThat(rcIndiaDefaultBiMappingCustomMetadataRecords.size())
                    .as("Quantity of Default_Business_Identity_Mapping__mdt records " +
                            "for '" + rcIndiaDefaultBI + "' and other RC India business identities (" + rcIndiaAvailableBis + ")")
                    .isEqualTo(1);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-33686")
    @DisplayName("CRM-33686 - Business Identity mapping for RingCentral India's Business Identity on Lead Conversion Page")
    @Description("Verify that on Sales Lead Conversion Page " +
            "only 'RingCentral India Bangalore', 'RingCentral India Mumbai', 'RingCentral India Maharashtra', " +
            "'RingCentral India Andhra Pradesh / Telangana' and 'RingCentral India Delhi Metro' business identities " +
            "are available to select")
    public void test() {
        step("1. Open the Lead Convert page for the test Sales Lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Switch the toggle into 'Create New Account' position", () ->
                leadConvertPage.newExistingAccountToggle.click()
        );

        step("3. Click 'Edit' in Opportunity section and check that Brand = '" + rcIndiaBrandName + "' " +
                "and Country picklist has preselected value = '" + indiaCountryName + "'", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.brandNameOutputField.shouldHave(exactTextCaseSensitive(rcIndiaBrandName));
            leadConvertPage.countryPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(indiaCountryName));
        });

        step("4. Check that BI picklist is enabled and has preselected value = '" + rcIndiaDefaultBI + "' " +
                "and check that other available options include only RC India business identities", () -> {
            leadConvertPage.businessIdentityPicklist
                    .shouldBe(enabled)
                    .getSelectedOption().shouldHave(exactTextCaseSensitive(rcIndiaDefaultBI));
            leadConvertPage.businessIdentityPicklist.getOptions().shouldHave(exactTextsCaseSensitiveInAnyOrder(rcIndiaAllBis));
            leadConvertPage.businessIdentityPicklist.getOptions().asDynamicIterable()
                    .forEach(businessIdentity -> businessIdentity.shouldBe(enabled));
        });
    }
}
