package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.quotetab.QuotePage.AGREEMENT_STAGE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("PriceTab")
public class NotAllDigitalLinesAssignedToDevicesNoValidationMessageTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product dlUnlimited;
    private final Product phoneToAdd;
    private final int numberOfPhonesToAssign;

    public NotAllDigitalLinesAssignedToDevicesNoValidationMessageTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_PhonesAndDLs.json",
                Dataset.class);
        steps = new Steps(data);

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        phoneToAdd = data.getProductByDataName("LC_HD_523");
        numberOfPhonesToAssign = phoneToAdd.quantity - 2;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-34777")
    @DisplayName("CRM-34777 - No validation that all DLs are assigned to devices if UI-Less feature toggle is turned on")
    @Description("Verify that when Enable UI-Less Process Order feature toggle is turned on " +
            "and not all DLs are assigned to the devices, then: \n" +
            " - There is no error message on the Price tab; \n" +
            " - Quote Stage field on the Quote Details tab is enabled and can be changed to Agreement")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity, add a new Sales quote, " +
                "select a package for it, add a phone, and save changes on the Price tab", () ->
                steps.cartTab.prepareCartTabViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Check that DigitalLine is not assigned to the phone and that there's no error notification " +
                "about that DL Unlimited doesn't have equal number of assigned child licenses on the Price tab", () -> {
            cartPage.getQliFromCartByDisplayName(dlUnlimited.name).getDeviceAssignmentButton().shouldHave(exactText("0"));

            cartPage.notificationBar.shouldBe(hidden);
        });

        step("3. Open the Quote Details tab, set Main Area Code, save changes " +
                "and check that Quote Stage picklist is enabled and 'Agreement' option is available to select", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.saveChanges();

            quotePage.stagePicklist.shouldBe(enabled);
            quotePage.stagePicklist.getOptions()
                    .findBy(exactTextCaseSensitive(AGREEMENT_STAGE))
                    .shouldBe(enabled);
        });

        step("4. Open the Price tab, assign part of DLs to the phones, save changes " +
                "and check that there's still no error notification about that DL Unlimited doesn't have equal number " +
                "of assigned child licenses on the Price tab", () -> {
            cartPage.openTab();
            steps.cartTab.setUpQuantities(dlUnlimited, phoneToAdd);
            steps.cartTab.assignDevicesToDLWithoutSettingAreaCode(phoneToAdd.name, dlUnlimited.name, numberOfPhonesToAssign);
            cartPage.saveChanges();

            cartPage.notificationBar.shouldBe(hidden);
        });

        step("5. Open the Quote Details tab and check that Quote Stage picklist is enabled " +
                "and 'Agreement' option is available to select", () -> {
            quotePage.openTab();

            quotePage.stagePicklist.shouldBe(enabled);
            quotePage.stagePicklist.getOptions()
                    .findBy(exactTextCaseSensitive(AGREEMENT_STAGE))
                    .shouldBe(enabled);
        });
    }
}
