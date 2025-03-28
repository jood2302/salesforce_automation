package ngbs.quotingwizard.newbusiness.quotetab.specialterms;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

@Tag("P0")
@Tag("Lambda")
@Tag("NGBS")
@Tag("QuoteTab")
public class CreditAmountOneTimeTest extends BaseTest {
    private final Steps steps;
    private final SpecialTermsSteps specialTermsSteps;

    //  Test data
    private final Product oneTimeChargeTermProduct;

    public CreditAmountOneTimeTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_2TypesOfDLs_RegularAndPOC.json",
                Dataset.class);
        steps = new Steps(data);
        specialTermsSteps = new SpecialTermsSteps(data);

        oneTimeChargeTermProduct = data.getProductByDataName("LC_HD_959");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-7574")
    @DisplayName("CRM-7574 - 'Credit Amount' with 'One - Time' products")
    @Description("Verify Credit Amount' calculation doesn't count 'One - Time' products'")
    public void test() {
        specialTermsSteps.creditAmountTestSteps(steps.quoteWizard.opportunity.getId(), oneTimeChargeTermProduct);
    }
}
