package leads.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.LeadHelper.*;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P0")
@Tag("FVT")
@Tag("TaiwanMVP")
public class RcTaiwanLeadConvertFlowTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public RcTaiwanLeadConvertFlowTest() {
        data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_Taiwan_MVP_Monthly_NB.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);

        step("Update Sales Lead's Lead_Brand_Name__c, CurrencyIsoCode, Country__c and Billing Address fields via API", () -> {
            steps.leadConvert.salesLead.setLead_Brand_Name__c(data.brandName);
            steps.leadConvert.salesLead.setCurrencyIsoCode(data.currencyISOCode);
            steps.leadConvert.salesLead.setStreet(TAIWAN_BILLING_STREET);
            steps.leadConvert.salesLead.setPostalCode(TAIWAN_BILLING_POSTAL_CODE);
            steps.leadConvert.salesLead.setCountry(TAIWAN_BILLING_COUNTRY);
            steps.leadConvert.salesLead.setCountry__c(TAIWAN_BILLING_COUNTRY);
            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-27438")
    @DisplayName("CRM-27438 - Validate pre-populated values for BI = RC Taiwan on lead conversion page after Lead edit")
    @Description("To Validate pre-populated values for BI = RC Taiwan on lead conversion page after Lead edit")
    public void test() {
        step("1. Open Lead Convert page for the test lead", () ->
                leadConvertPage.openPage(steps.leadConvert.salesLead.getId())
        );

        step("2. Select 'Create a new account' toggle option", () ->
                leadConvertPage.newExistingAccountToggle.click()
        );

        step("3. Click 'Edit' button in the Opportunity Info Section, " +
                "check Business identity, Currency and Brand Name fields populated, " +
                "populate Close Date field and click 'Apply' button", () -> {
            leadConvertPage.waitUntilOpportunitySectionIsLoaded();
            leadConvertPage.opportunityInfoEditButton.click();

            leadConvertPage.businessIdentityPicklist.getSelectedOption().shouldHave(exactTextCaseSensitive(data.getBusinessIdentityName()));
            leadConvertPage.currencyPicklist
                    .shouldBe(disabled, ofSeconds(10))
                    .getSelectedOption().shouldHave(exactText(data.currencyISOCode));
            leadConvertPage.brandNameOutputField.shouldHave(exactTextCaseSensitive(data.brandName));

            leadConvertPage.closeDateDatepicker.setTomorrowDate();

            leadConvertPage.opportunityInfoApplyButton.scrollIntoView(true).click();
        });

        step("4. Click 'Convert' button", () ->
                steps.leadConvert.pressConvertButton()
        );

        step("5. Check that Lead is converted", () -> {
            steps.leadConvert.checkLeadConversion(steps.leadConvert.salesLead);
        });
    }
}
