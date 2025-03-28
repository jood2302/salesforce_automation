package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartItem.*;
import static com.codeborne.selenide.Condition.attribute;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("PriceTab")
@Tag("TargetPrice")
public class DiscretionCircleColorTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Product dlUnlimited;
    private final Product tollFreeBundle;

    private final int newDlUnlimitedQuantity;
    private final int discountValueAboveTargetPrice;
    private final int discountValueBelowCeilingValue;
    private final int discountValueAboveCeilingValue;

    public DiscretionCircleColorTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_NoPhones.json",
                Dataset.class);
        steps = new Steps(data);

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        tollFreeBundle = data.getProductByDataName("LC_TB_379");

        newDlUnlimitedQuantity = 10;
        discountValueAboveTargetPrice = 5;

        //  this value should be less than a 'ceiling' configured in the custom metadata type (DiscountCeilingLicense__mdt)
        discountValueBelowCeilingValue = 79;

        //  this value should exceed a 'ceiling' configured in the custom metadata type (DiscountCeilingLicense__mdt)
        discountValueAboveCeilingValue = 81;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-26776")
    @DisplayName("CRM-26776 - Color of the Discretion circle for DL")
    @Description("Verify that discretion circles are colored properly: \n\n" +
            "- Color is green when your price is greater than target price. \n" +
            "- Color is orange when your price is less than target price and discount is less than ceiling value. \n" +
            "- Color is red when your price is less than target price and discount is greater than ceiling value")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                        "select a package for it, and save changes", () ->
                steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Open the Add Products tab, add a recurring license, open the Price tab, and save changes", () -> {
            steps.quoteWizard.addProductsOnProductsTab(tollFreeBundle);

            cartPage.openTab();
            cartPage.saveChanges();
        });

        step("3. Check that initially discretion circle is green for the DL Unlimited", () ->
                checkDiscretionCircleColor(GREEN_COLOR_STYLE_VALUE)
        );

        step("4. Set quantity of DL Unlimited = " + newDlUnlimitedQuantity + " and check that its discretion circle is green", () -> {
            //  'Your Price' > 'Target Price', 'Discount' = 0
            cartPage.setQuantityForQLItem(dlUnlimited.name, newDlUnlimitedQuantity);

            checkDiscretionCircleColor(GREEN_COLOR_STYLE_VALUE);
        });

        step("5. Set discount for DL Unlimited to a value < 80% that makes 'Your Price' greater than Target Price, " +
                "and check that its discretion circle is green", () -> {
            //  'Your Price' > 'Target Price', 0 < 'Discount' < discount ceiling
            cartPage.setDiscountForQLItem(dlUnlimited.name, discountValueAboveTargetPrice);

            checkDiscretionCircleColor(GREEN_COLOR_STYLE_VALUE);
        });

        step("6. Set discount for DL Unlimited to a value < 80% that makes 'Your Price' less than Target Price, " +
                "and check that its discretion circle is orange", () -> {
            //  'Your Price' < 'Target Price', 0 < 'Discount' < discount ceiling 
            cartPage.setDiscountForQLItem(dlUnlimited.name, discountValueBelowCeilingValue);

            checkDiscretionCircleColor(ORANGE_COLOR_STYLE_VALUE);
        });

        step("7. Set discount for DL Unlimited to a value > 80% that makes 'Your Price' less than Target Price, " +
                "and check that its discretion circle is red", () -> {
            //  'Your Price' < 'Target Price', 'Discount' > discount ceiling 
            cartPage.setDiscountForQLItem(dlUnlimited.name, discountValueAboveCeilingValue);

            checkDiscretionCircleColor(RED_COLOR_STYLE_VALUE);
        });
    }

    /**
     * Check the color of discretion circle for DigitalLine Unlimited
     * depending on its resulting 'Your Price' and 'Discount' values.
     *
     * @param colorValue a discretion circle color value to check (as a value for 'style' attribute)
     */
    private void checkDiscretionCircleColor(String colorValue) {
        cartPage.getQliFromCartByDisplayName(dlUnlimited.name)
                .getDiscretionCircle()
                .shouldHave(attribute("style", colorValue));
    }
}
