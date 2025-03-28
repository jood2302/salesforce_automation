package ngbs.quotingwizard.newbusiness.carttab.promotions;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static base.Pages.packagePage;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;

@Tag("P1")
@Tag("PriceTab")
@Tag("Promos")
public class PromoResetAfterChargeTermChangeTest extends BaseTest {
    private final Steps steps;
    private final PromosSteps promosSteps;

    //  Test data
    private final String monthlyChargeTerm;
    private final Promotion polycomUsdDiscountPromo;

    private final Product polycomPhone;
    private final Product ciscoPhone;

    public PromoResetAfterChargeTermChangeTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_AnnualAndMonthly_Contract_PhonesAndDLs_Promos.json",
                Dataset.class);

        steps = new Steps(data);
        promosSteps = new PromosSteps(data);

        monthlyChargeTerm = data.packageFolders[1].chargeTerm;
        polycomUsdDiscountPromo = promosSteps.promotions[2];

        polycomPhone = data.getProductByDataName("LC_HD_936");
        ciscoPhone = data.getProductByDataName("LC_HD_523");
    }

    @BeforeEach
    public void setUpTest() {
        promosSteps.createPromotionsInNGBS();

        var testUserWithPromosFeature = promosSteps.getTestUserWithPromosFeature();
        steps.salesFlow.createAccountWithContactAndContactRole(testUserWithPromosFeature);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, testUserWithPromosFeature);
        promosSteps.loginAsTestUserWithPromosFeature();
    }

    @Test
    @TmsLink("CRM-21743")
    @DisplayName("CRM-21743 - Applied promotion is removed after changing of Charge terms")
    @Description("To verify case when applied promo code should be removed after Charge Terms changing")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, save changes, and add some products on the Add Products tab", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addProductsOnProductsTab(polycomPhone, ciscoPhone);
        });

        step("2. Open the Price tab, apply promo for Polycom phone and verify that it's applied to the cart item", () -> {
            cartPage.openTab();
            cartPage.applyPromoCode(polycomUsdDiscountPromo.promoCode);
            cartPage.saveChanges();

            promosSteps.stepCheckAppliedPromo(polycomPhone, polycomUsdDiscountPromo);
        });

        step("3. Open the Select Package tab, and change charge term", () -> {
            packagePage.openTab();
            packagePage.packageSelector.packageFilter.selectChargeTerm(monthlyChargeTerm);
        });

        step("4. Open the Price tab and verify that previously added items still exist", () -> {
            cartPage.openTab();

            cartPage.getQliFromCartByDisplayName(polycomPhone.name)
                    .getCartItemElement()
                    .shouldBe(visible);
            cartPage.getQliFromCartByDisplayName(ciscoPhone.name)
                    .getCartItemElement()
                    .shouldBe(visible);
        });

        step("5. Verify that previously added promo is not applied to the cart item", () -> {
            var productQLI = cartPage.getQliFromCartByDisplayName(polycomPhone.name);
            productQLI.getDiscountInput()
                    .shouldHave(exactValue(valueOf(polycomPhone.discount)));
            productQLI.getDiscountTypeSelect().getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(polycomPhone.discountType));

            productQLI.getPromoIcon().shouldNot(exist);
        });
    }
}
