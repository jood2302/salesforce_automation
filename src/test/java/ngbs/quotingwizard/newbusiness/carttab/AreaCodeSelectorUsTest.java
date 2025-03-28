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
public class AreaCodeSelectorUsTest extends BaseTest {
    private final Steps steps;
    private final AreaCodeSelectorSteps areaCodeSelectorSteps;

    //  Test data
    private final Map<String, List<String>> countriesForRingCentralUsLines;

    public AreaCodeSelectorUsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_GlobalMVP.json",
                Dataset.class);
        steps = new Steps(data);
        areaCodeSelectorSteps = new AreaCodeSelectorSteps(data);

        var dlUnlimited = data.getProductByDataName("LC_DL-UNL_50").name;
        var globalMvpEMEA = data.getProductByDataName("LC_IBO_284").name;
        var globalMvpUK = data.getProductByDataName("LC_IBO_286").name;
        var globalMvpAPAC = data.getProductByDataName("LC_IBO_288").name;
        var globalMvpLATAM = data.getProductByDataName("LC_IBO_290").name;

        countriesForRingCentralUsLines = Map.of(
                dlUnlimited, List.of("United States", "Canada", "Puerto Rico"),
                globalMvpEMEA, List.of("Poland", "Austria", "Belgium", "Croatia", "Czech Republic", "Denmark",
                        "Estonia", "Finland", "France", "Germany", "Greece", "Hungary", "Ireland", "Israel", "Italy",
                        "Lithuania", "Luxembourg", "Netherlands", "Norway", "Portugal", "Romania", "Slovakia", "Slovenia",
                        "South Africa", "Spain", "Sweden", "Switzerland"),
                globalMvpUK, List.of("United Kingdom"),
                globalMvpAPAC, List.of("Australia", "New Zealand", "Hong Kong", "Japan", "Singapore"),
                globalMvpLATAM, List.of("Mexico", "Argentina", "Brazil", "Chile", "Colombia", "Costa Rica", "Peru")
        );
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-13293")
    @TmsLink("CRM-13292")
    @DisplayName("CRM-13293 - Area Code selector shows countries According to Package. International Lines. \n" +
            "CRM-13292 - Area Code selector shows countries According to Package. Domestic Lines (RC US).")
    @Description("CRM-13293 - Verify that Area Code selector shows appropriate country for IBO DigitalLines. \n" +
            "CRM-13292 - Verify that Area Code Selector shows appropriate countries for the package (RC US)")
    public void test() {
        areaCodeSelectorSteps.checkAreaCodeSelectorDifferentDigitalLinesTestSteps(steps.quoteWizard.opportunity.getId(),
                countriesForRingCentralUsLines);
    }
}
