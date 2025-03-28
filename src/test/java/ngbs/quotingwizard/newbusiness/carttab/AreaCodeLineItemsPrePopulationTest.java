package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;

@Tag("P1")
@Tag("NGBS")
public class AreaCodeLineItemsPrePopulationTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final Product polycomPhone;
    private final Product mainLocalNumber;
    private final Product addTollFreeNumber;
    private final Product digitalLineUnlimited;

    public AreaCodeLineItemsPrePopulationTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_AnnualAndMonthly_Contract_PhonesAndDLs_Promos.json",
                Dataset.class);
        steps = new Steps(data);

        polycomPhone = data.getProductByDataName("LC_HD_936");
        mainLocalNumber = data.getProductByDataName("LC_MLN_31");
        addTollFreeNumber = data.getProductByDataName("LC_ATN_39");
        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-11259")
    @DisplayName("CRM-11259 - Area code population for local items after addition to cart")
    @Description("Verify that after population of Main Area Code field on Quote Details tab " +
            "items that are compatible with this area code get their area code populated with it")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Populate the Main Area Code on the Quote Details tab", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
        });

        step("3. Open the Add Products tab, and add some products", () -> {
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
            steps.quoteWizard.addProductsOnProductsTab(addTollFreeNumber);
        });

        step("4. Open the Price tab, and check the Area Code on the Main Local Number", () -> {
            cartPage.openTab();
            cartPage.getQliFromCartByDisplayName(mainLocalNumber.name)
                    .getAreaCodeButton()
                    .click();
            areaCodePage.getFirstAreaCodeItem().getAreaCodeSelector()
                    .getSelectedAreaCodeFullName()
                    .shouldHave(exactTextCaseSensitive(steps.quoteWizard.localAreaCode.fullName));
            areaCodePage.cancelButton.click();
        });

        step("5. Check the Area Code on Additional Toll-Free Number", () -> {
            cartPage.getQliFromCartByDisplayName(addTollFreeNumber.name)
                    .getAreaCodeButton()
                    .click();
            areaCodePage.areaCodeLineItems.shouldHave(size(0));
            areaCodePage.cancelButton.click();
        });

        step("6. Check the Area Code on the DigitalLine Unlimited", () -> {
            cartPage.getQliFromCartByDisplayName(digitalLineUnlimited.name)
                    .getDeviceAssignmentButton()
                    .click();
            deviceAssignmentPage.getProductItemByName(polycomPhone.name)
                    .getNameElement()
                    .shouldHave(exactText(polycomPhone.name), ofSeconds(60));
            deviceAssignmentPage.getFirstAreaCodeItem(polycomPhone.name).getAreaCodeSelector()
                    .getSelectedAreaCodeFullName()
                    .shouldHave(exactTextCaseSensitive(steps.quoteWizard.localAreaCode.fullName));
        });
    }
}
