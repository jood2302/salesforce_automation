package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.internal.proxy.SelenideProxyMocksDisabledExtension;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

@Tag("P1")
@Tag("NGBS")
@Tag("PriceTab")
@ExtendWith(SelenideProxyMocksDisabledExtension.class)
public class AreaCodeSelectorUkTest extends BaseTest {
    private final Steps steps;
    private final AreaCodeSelectorSteps areaCodeSelectorSteps;

    //  Test data
    private final Map<String, List<String>> countriesForRingCentralUkLines;

    public AreaCodeSelectorUkTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_UK_MVP_Monthly_NonContract.json",
                Dataset.class);
        steps = new Steps(data);
        areaCodeSelectorSteps = new AreaCodeSelectorSteps(data);

        var dlUnlimited = data.getProductByDataName("LC_DL-UNL_50").name;
        countriesForRingCentralUkLines = Map.of(dlUnlimited, List.of("United Kingdom"));
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-13292")
    @DisplayName("CRM-13292 - Area Code selector shows countries According to Package. Domestic Lines (RC UK)")
    @Description("Verify that Area Code Selector shows appropriate countries for the package (RC UK)")
    public void test() {
        areaCodeSelectorSteps.checkAreaCodeSelectorDifferentDigitalLinesTestSteps(steps.quoteWizard.opportunity.getId(),
                countriesForRingCentralUkLines);
    }
}
