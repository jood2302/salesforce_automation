package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.wizardBodyPage;
import static base.Pages.wizardPage;
import static com.codeborne.selenide.Condition.hidden;
import static io.qameta.allure.Allure.step;

@Tag("P0")
@Tag("FVT")
@Tag("TaiwanMVP")
@Tag("ProServ")
@Tag("ContactCenter")
public class CcAndProServDisabledTest extends BaseTest {
    private final Steps steps;

    public CcAndProServDisabledTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Taiwan_Annual_Contract.json",
                Dataset.class);

        steps = new Steps(data);
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-27266")
    @TmsLink("CRM-27269")
    @DisplayName("CRM-27266 - Validate Contact Center tab is not shown on QW. \n" +
            "CRM-27269 - Validate ProServ tab and Engage ProServ button is not shown in QW")
    @Description("CRM-27266 - To Validate Contact Center tab is not shown on QW. \n" +
            "CRM-27269 - To Validate ProServ tab and Engage ProServ button is not shown in QW")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, add some products, and save changes on the Price tab", () ->
                steps.cartTab.prepareCartTabViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        //  CRM-27266
        step("2. Check the Contact Center tab should not be available in the Quote Wizard", () -> {
            wizardBodyPage.contactCenterTab.shouldBe(hidden);
        });

        //  CRM-27269
        step("3. Check that ProServ tab and Initiate ProServ button should not be available in the Quote Wizard", () -> {
            wizardBodyPage.proServTab.shouldBe(hidden);
            wizardPage.initiateProServButton.shouldBe(hidden);
        });
    }
}
