package ngbs.quotingwizard.newbusiness.carttab.promotions;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static com.codeborne.selenide.Condition.exactValue;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;

@Tag("P1")
@Tag("Promos")
@Tag("PriceTab")
public class ApplyProductAndCategoryLevelsPromosTest extends BaseTest {
    private final Steps steps;
    private final PromosSteps promosSteps;

    //  Test data
    private final Promotion recurringUsdDiscountPromo;
    private final Promotion recurringPercentDiscountPromo;
    private final Promotion oneTimeUsdDiscountPromo;
    private final Promotion oneTimePercentDiscountPromo;
    private final Promotion phonesUsdDiscountPromo;
    private final Promotion crfPercentDiscountPromo;

    private final Product polycomPhone;
    private final Product ciscoPhone;
    private final Product dlUnlimited;
    private final Product complianceRecoveryFee;

    public ApplyProductAndCategoryLevelsPromosTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_AnnualAndMonthly_Contract_PhonesAndDLs_Promos.json",
                Dataset.class);

        steps = new Steps(data);
        promosSteps = new PromosSteps(data);

        recurringUsdDiscountPromo = promosSteps.promotions[0];
        recurringPercentDiscountPromo = promosSteps.promotions[1];
        oneTimeUsdDiscountPromo = promosSteps.promotions[2];
        oneTimePercentDiscountPromo = promosSteps.promotions[3];
        phonesUsdDiscountPromo = promosSteps.promotions[4];
        crfPercentDiscountPromo = promosSteps.promotions[5];

        polycomPhone = data.getProductByDataName("LC_HD_936");
        ciscoPhone = data.getProductByDataName("LC_HD_523");
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        complianceRecoveryFee = data.getProductByDataName("LC_CRF_51");
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
    @TmsLink("CRM-21276")
    @TmsLink("CRM-21352")
    @DisplayName("CRM-21276 - Applying promo discounts (product level). \n" +
            "CRM-21352 - Applying promo discounts (categories level)")
    @Description("CRM-21276 - Verify that discounts are applied correctly after submitting the promo. \n" +
            "CRM-21352 - Verify that discounts are applied correctly to all products from the promo category")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, add some products, and save changes on the Price tab", () ->
                steps.cartTab.prepareCartTabViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        //  For CRM-21276
        step("2. Apply currency promo for Polycom phone and verify that it's applied to the cart item", () -> {
            cartPage.applyPromoCode(oneTimeUsdDiscountPromo.promoCode);

            promosSteps.stepCheckAppliedPromo(polycomPhone, oneTimeUsdDiscountPromo);
        });

        //  For CRM-21276
        step("3. Remove selected promo in Promotions Manager modal, apply percent promo for Polycom phone, " +
                "verify that discount and discount type are applied", () -> {
            cartPage.changeAppliedPromo(oneTimePercentDiscountPromo.promoCode);

            promosSteps.stepCheckAppliedPromo(polycomPhone, oneTimePercentDiscountPromo);
        });

        //  For CRM-21276
        step("4. Remove selected promo in Promotions Manager modal, apply currency promo for DL Unlimited, " +
                "verify that discount and discount type are applied", () -> {
            cartPage.changeAppliedPromo(recurringUsdDiscountPromo.promoCode);

            promosSteps.stepCheckAppliedPromo(dlUnlimited, recurringUsdDiscountPromo);

            //  Make sure that previously applied promo is cancelled
            cartPage.getQliFromCartByDisplayName(polycomPhone.name).getDiscountInput()
                    .shouldHave(exactValue(valueOf(polycomPhone.discount)));
        });

        //  For CRM-21276
        step("5. Remove selected promo in Promotions Manager modal, apply percent promo for DL Unlimited, " +
                "verify that discount and discount type are applied", () -> {
            cartPage.changeAppliedPromo(recurringPercentDiscountPromo.promoCode);

            promosSteps.stepCheckAppliedPromo(dlUnlimited, recurringPercentDiscountPromo);
        });

        //  For CRM-21352
        step("6. Remove selected promo in Promotions Manager modal, apply currency promo for Phones category, " +
                "verify that discount and discount type are applied", () -> {
            cartPage.changeAppliedPromo(phonesUsdDiscountPromo.promoCode);

            promosSteps.stepCheckAppliedPromo(polycomPhone, phonesUsdDiscountPromo);
            promosSteps.stepCheckAppliedPromo(ciscoPhone, phonesUsdDiscountPromo);

            cartPage.getQliFromCartByDisplayName(dlUnlimited.name).getDiscountInput()
                    .shouldHave(exactValue(valueOf(dlUnlimited.discount)));
        });

        //  For CRM-21352
        step("7. Remove selected promo in Promotions Manager modal, apply percent promo for CRF category, " +
                "verify that discount and discount type are applied", () -> {
            cartPage.changeAppliedPromo(crfPercentDiscountPromo.promoCode);

            promosSteps.stepCheckAppliedPromo(complianceRecoveryFee, crfPercentDiscountPromo);

            cartPage.getQliFromCartByDisplayName(polycomPhone.name).getDiscountInput()
                    .shouldHave(exactValue(valueOf(polycomPhone.discount)));

            cartPage.getQliFromCartByDisplayName(ciscoPhone.name).getDiscountInput()
                    .shouldHave(exactValue(valueOf(ciscoPhone.discount)));
        });
    }
}
