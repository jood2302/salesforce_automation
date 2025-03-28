package leads.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("Vodafone")
@Tag("LeadConvert")
public class VodafoneWithRcPartnerLeadConvertTest extends BaseTest {
    private final ProServOnlyPartnerLeadConvertSteps proServOnlyPartnerLeadConvertSteps;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    public VodafoneWithRcPartnerLeadConvertTest() {
        proServOnlyPartnerLeadConvertSteps = new ProServOnlyPartnerLeadConvertSteps(2);

        var data = proServOnlyPartnerLeadConvertSteps.data;
        steps = new Steps(data);

        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createPartnerAccountAndLead(salesRepUser);

        step("Set Partner Account.BusinessIdentity__c = '" + proServOnlyPartnerLeadConvertSteps.businessIdentityName + "' " +
                "via API", () -> {
            steps.leadConvert.partnerAccount.setBusinessIdentity__c(proServOnlyPartnerLeadConvertSteps.businessIdentityName);
            enterpriseConnectionUtils.update(steps.leadConvert.partnerAccount);
        });

        step("Set the test Partner Lead.Country__c = '" + proServOnlyPartnerLeadConvertSteps.billingCountry + "' via API", () -> {
            steps.leadConvert.partnerLead.setCountry__c(proServOnlyPartnerLeadConvertSteps.billingCountry);
            enterpriseConnectionUtils.update(steps.leadConvert.partnerLead);
        });

        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-35549")
    @DisplayName("CRM-35549 - Lead Conversion flow for Partner Brands ('Vodafone Business with RingCentral' brand)")
    @Description("Verify that after the Lead conversion the created Opportunity, Account and Contact will have the same data " +
            "as was set on Lead Conversion Page for the Lead with 'Vodafone Business with RingCentral' brand")
    public void test() {
        proServOnlyPartnerLeadConvertSteps.proServOnlyPartnerLeadConvertTestSteps(steps.leadConvert.partnerLead);
    }
}
