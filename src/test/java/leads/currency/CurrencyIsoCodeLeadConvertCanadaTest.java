package leads.currency;

import base.BaseTest;
import base.Steps;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

@Tag("P0")
@Tag("LeadConvert")
public class CurrencyIsoCodeLeadConvertCanadaTest extends BaseTest {
    private final CurrencyIsoCodeLeadConvertSteps currencyIsoCodeLeadConvertSteps;
    private final Steps steps;

    public CurrencyIsoCodeLeadConvertCanadaTest() {
        currencyIsoCodeLeadConvertSteps = new CurrencyIsoCodeLeadConvertSteps(1);
        var data = currencyIsoCodeLeadConvertSteps.data;
        steps = new Steps(data);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.leadConvert.createSalesLead(salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-10802")
    @DisplayName("CRM-10802 - Correct population of CurrencyIsoCode on RingCentral Canada business identity")
    @Description("Verify that CurrencyIsoCode on the Opportunity is populated correctly after Lead conversion")
    public void test() {
        currencyIsoCodeLeadConvertSteps.currencyIsoCodeAfterLeadConvertTestSteps(steps.leadConvert.salesLead.getId());
    }
}
