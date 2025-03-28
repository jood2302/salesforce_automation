package leads.newbusiness;

import base.BaseTest;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.ZAYO_USERS_GROUP;

@Tag("P0")
@Tag("GSPBrands")
@Tag("LeadConvert")
public class RiseAmericaZayoPartnerLeadConvertTest extends BaseTest {
    private final ProServOnlyPartnerLeadConvertSteps proServOnlyPartnerLeadConvertSteps;

    //  Test data
    private final String zayoSubBrand;

    public RiseAmericaZayoPartnerLeadConvertTest() {
        proServOnlyPartnerLeadConvertSteps = new ProServOnlyPartnerLeadConvertSteps(5);

        zayoSubBrand = "3000.Zayo";
    }

    @BeforeEach
    public void setUpTest() {
        proServOnlyPartnerLeadConvertSteps.preparePartnerLeadConversionSteps(ZAYO_USERS_GROUP, zayoSubBrand);
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
