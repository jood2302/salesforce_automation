package leads.leadconvertpage;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.Default_Business_Identity_Mapping__mdt;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static base.Pages.leadConvertPage;
import static com.aquiva.autotests.rc.page.lead.convert.LeadConvertPage.NO_MATCHING_BUSINESS_IDENTITY_WAS_FOUND;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.US_BILLING_COUNTRY;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("LeadConvert")
@Tag("QTC-944")
public class ConvertButtonCheckForEmptyBrandOrBillingCountryLeadConvertPageTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public ConvertButtonCheckForEmptyBrandOrBillingCountryLeadConvertPageTest() {
        data = JsonUtils.readConfigurationResource(
                "data/leadConvert/newbusiness/RC_MVP_Monthly_Contract_NoProducts.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);

        //  to be able to select the existing account from the 'Matched Accounts' list instead of flaky Account Lookup
        step("Set the same email on the Sales Lead and the test Account's Contact via API", () -> {
            steps.leadConvert.salesLead.setEmail(steps.salesFlow.contact.getEmail());
            enterpriseConnectionUtils.update(steps.leadConvert.salesLead);
        });

        step("Set RC_Brand__c = '" + data.getBrandName() + "' and BillingCountry = null on the Account via API", () -> {
            var accountToUpdate = new Account();
            accountToUpdate.setId(steps.salesFlow.account.getId());
            accountToUpdate.setFieldsToNull(new String[]{"BillingCountry"});
            accountToUpdate.setRC_Brand__c(data.getBrandName());
            enterpriseConnectionUtils.update(accountToUpdate);
        });

        step("Check that 'Default Business Identity Mapping' record of custom metadata type exists " +
                "for 'United States' country and 'RingCentral' brand", () -> {
            var rcDefaultBiMappingCustomMetadataRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM Default_Business_Identity_Mapping__mdt " +
                            "WHERE Brand__c = '" + data.getBrandName() + "' " +
                            "AND Country__c = '" + US_BILLING_COUNTRY + "' " +
                            "AND Is_Core_Brand__c = true",
                    Default_Business_Identity_Mapping__mdt.class);
            assertThat(rcDefaultBiMappingCustomMetadataRecords.size())
                    .as("Quantity of Default_Business_Identity_Mapping__mdt record for 'United States'/'RingCentral'")
                    .isEqualTo(1);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-35767")
    @TmsLink("CRM-35768")
    @DisplayName("CRM-35767 - 'Convert' button is disabled on Sales Lead Convert to Account with blank Billing Country on it. \n" +
            "CRM-35768 - 'Convert' button is disabled on Sales Lead Convert to Account with blank Brand on it")
    @Description("CRM-35767 - Verify that 'Convert' button is disabled on Sales Lead Convert to Account with blank BillingCountry on it. \n" +
            "CRM-35768 - Verify that 'Convert' button is disabled on Sales Lead Convert to Account with blank RC_Brand__c on it")
    public void test() {
        step("1. Open Lead Convert page for the test Sales Lead", () -> {
            leadConvertPage.openPage(steps.leadConvert.salesLead.getId());
        });

        step("2. Select New Business Account (from the 'Matched Accounts' table) and click on 'Apply' button", () -> {
            leadConvertPage.selectMatchedAccount(steps.salesFlow.account.getId());
        });

        //  CRM-35767
        step("3. Check that the 'Convert' button is disabled, hover over it, and check the displayed tooltip for it", () -> {
            checkConvertButtonDisabledAndTooltipExist();
        });

        step("4. Set RC_Brand__c = null and BillingCountry = '" + US_BILLING_COUNTRY + "' for the test Account via API", () -> {
            var accountToUpdate = new Account();
            accountToUpdate.setId(steps.salesFlow.account.getId());
            accountToUpdate.setFieldsToNull(new String[]{"RC_Brand__c"});
            accountToUpdate.setBillingCountry(US_BILLING_COUNTRY);
            enterpriseConnectionUtils.update(accountToUpdate);
        });

        step("5. Re-open the Lead Convert page, select New Business Account (from the 'Matched Accounts' table), " +
                "and click on 'Apply' button", () -> {
            leadConvertPage.openPage(steps.leadConvert.salesLead.getId());
            leadConvertPage.selectMatchedAccount(steps.salesFlow.account.getId());
        });

        //  CRM-35768
        step("6. Check that the 'Convert' button is disabled, hover over it, and check the displayed tooltip for it", () -> {
            checkConvertButtonDisabledAndTooltipExist();
        });
    }

    /**
     * Check that the 'Convert' button is disabled and displays the correct tooltip when hovering.
     */
    private void checkConvertButtonDisabledAndTooltipExist() {
        leadConvertPage.convertButton.shouldBe(disabled);
        leadConvertPage.convertButton.hover();
        leadConvertPage.convertButtonTooltip.shouldHave(exactTextCaseSensitive(NO_MATCHING_BUSINESS_IDENTITY_WAS_FOUND));
    }
}
