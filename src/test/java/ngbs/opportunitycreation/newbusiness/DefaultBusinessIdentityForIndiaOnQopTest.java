package ngbs.opportunitycreation.newbusiness;

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

import static base.Pages.opportunityCreationPage;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("Ignite")
public class DefaultBusinessIdentityForIndiaOnQopTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final String indiaCountryName;
    private final String rcIndiaDefaultBI;
    private final List<String> rcIndiaAvailableBis;
    private final List<String> rcIndiaAllBis;

    public DefaultBusinessIdentityForIndiaOnQopTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/opportunitycreation/newbusiness/RC_India_Mumbai_MVP_Monthly_Contract_QOP.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        indiaCountryName = data.getBillingCountry();
        rcIndiaDefaultBI = "RingCentral India Bangalore";
        rcIndiaAvailableBis = List.of("RingCentral India Mumbai", "RingCentral India Maharashtra",
                "RingCentral India Andhra Pradesh / Telangana", "RingCentral India Delhi Metro");

        rcIndiaAllBis = new ArrayList<>(rcIndiaAvailableBis);
        rcIndiaAllBis.add(rcIndiaDefaultBI);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);

        step("Check that 'Default Business Identity Mapping' record of custom metadata type exists " +
                "for '" + rcIndiaDefaultBI + "' and other available India business identities (" + rcIndiaAvailableBis + ")", () -> {
            var rcIndiaDefaultBiMappingCustomMetadataRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Sub_Brand__c = null " +
                            "AND Brand__c = '" + data.brandName + "' " +
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
    @TmsLink("CRM-33687")
    @DisplayName("CRM-33687 - Business Identity mapping for RingCentral India's Business Identity on Quick Opportunity Page")
    @Description("Verify that on the Quick Opportunity Page for Customer Account from India, " +
            "'RingCentral India Bangalore', 'RingCentral India Mumbai', 'RingCentral India Maharashtra', " +
            "'RingCentral India Andhra Pradesh / Telangana' and 'RingCentral India Delhi Metro' " +
            "are always available for selection in the Business Identity picklist")
    public void test() {
        step("1. Open the QOP for the test Customer Account and check Brand field value", () -> {
            opportunityCreationPage.openPage(steps.salesFlow.account.getId());

            opportunityCreationPage.brandOutput.shouldHave(exactTextCaseSensitive(data.brandName));
        });

        step("2. Check that BI picklist is enabled and has '" + rcIndiaDefaultBI + "' preselected value, " +
                "and check that other available options include only RC India business identities", () -> {
            opportunityCreationPage.businessIdentityPicklist
                    .shouldBe(enabled)
                    .getSelectedOption().shouldHave(exactTextCaseSensitive(rcIndiaDefaultBI));
            opportunityCreationPage.businessIdentityPicklist.getOptions().shouldHave(exactTextsCaseSensitiveInAnyOrder(rcIndiaAllBis));
            opportunityCreationPage.businessIdentityPicklist.getOptions().asDynamicIterable()
                    .forEach(businessIdentity -> businessIdentity.shouldBe(enabled));
        });
    }
}
