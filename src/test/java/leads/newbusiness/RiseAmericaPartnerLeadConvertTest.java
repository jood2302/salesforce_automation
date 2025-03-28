package leads.newbusiness;

import base.BaseTest;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.RISE_AMERICA_USERS_GROUP;

@Tag("P0")
@Tag("GSPBrands")
@Tag("LeadConvert")
public class RiseAmericaPartnerLeadConvertTest extends BaseTest {
    private final ProServOnlyPartnerLeadConvertSteps proServOnlyPartnerLeadConvertSteps;

    public RiseAmericaPartnerLeadConvertTest() {
        proServOnlyPartnerLeadConvertSteps = new ProServOnlyPartnerLeadConvertSteps(6);
    }

    @BeforeEach
    public void setUpTest() {
        proServOnlyPartnerLeadConvertSteps.preparePartnerLeadConversionSteps(RISE_AMERICA_USERS_GROUP);
    }

    @Test
    @TmsLink("CRM-35549")
    @DisplayName("CRM-35549 - Lead Conversion flow for Partner Brands ('Rise America' brand)")
    @Description("Verify that after the Lead conversion the created Opportunity, Account and Contact will have the same data " +
            "as was set on Lead Conversion Page for the Lead with 'Rise America' brand")
    public void test() {
        proServOnlyPartnerLeadConvertSteps.proServOnlyPartnerLeadConvertTestSteps(proServOnlyPartnerLeadConvertSteps.partnerLead);
    }
}
