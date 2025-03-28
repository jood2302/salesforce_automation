package ngbs.quotingwizard.newbusiness.signup;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.dto.discounts.DiscountNgbsDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.newbusiness.carttab.promotions.PromosSteps;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.MVP_SERVICE;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToInteger;
import static com.aquiva.autotests.rc.utilities.StringHelper.PERCENT;
import static com.aquiva.autotests.rc.utilities.TimeoutAssertions.assertWithTimeout;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getAccountInNGBS;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.getDiscountsFromNGBS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.CREDIT_CARD_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("P1")
@Tag("Promos")
@Tag("SignUp")
public class PromoFlexibleContractDiscountsSignUpTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final PromosSteps promosSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User testUserWithPromosFeature;

    //  Test data
    private final Promotion phonesUsdDiscountPromo;
    private final PromotionDiscountTemplate promoDiscountTemplate;
    private final Product dlUnlimited;
    private final Product ciscoPhone;
    private final Product polycomPhone;
    private final String mobileUserLicenseName;

    public PromoFlexibleContractDiscountsSignUpTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_AnnualAndMonthly_Contract_PhonesAndDLs_Promos.json",
                Dataset.class);

        steps = new Steps(data);
        promosSteps = new PromosSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        phonesUsdDiscountPromo = promosSteps.promotions[3];
        promoDiscountTemplate = phonesUsdDiscountPromo.discountTemplates[0];

        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        ciscoPhone = data.getProductByDataName("LC_HD_523");
        polycomPhone = data.getProductByDataName("LC_HD_936");

        dlUnlimited.discount = 5;
        ciscoPhone.discount = 20;

        mobileUserLicenseName = "Mobile User";
    }

    @BeforeEach
    public void setUpTest() {
        promosSteps.createPromotionsInNGBS();

        step("Find a user with 'Sales Rep - Lightning' profile, 'Enable_Promotions__c' feature toggle " +
                "and 'Allow Process Order Without Shipping' Permission Set", () -> {
            testUserWithPromosFeature = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withFeatureToggles(List.of(ENABLE_PROMOTIONS_FT))
                    .withPermissionSet(ALLOW_PROCESS_ORDER_WITHOUT_SHIPPING_PS)
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(testUserWithPromosFeature);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, testUserWithPromosFeature);

        step("Log in as a user with 'Sales Rep - Lightning' profile, 'Enable_Promotions__c' feature toggle " +
                "and 'Allow Process Order Without Shipping' Permission Set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(testUserWithPromosFeature);
        });
    }

    @Test
    @TmsLink("CRM-21439")
    @DisplayName("CRM-21439 - Sign up a quote with promo-discounts and flexible discounts, contracted")
    @Description("Verify that sales quote with flexible discounts and applied promo can be signed up. \n" +
            "Verify that flexible discounts and promo-discounts applied to the quote at the same time, are correctly passed to NGBS")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, add some products, and save changes on the Price tab", () ->
                steps.cartTab.prepareCartTabViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId())
        );

        step("2. Apply the Promo code to the added phone, " +
                "add flexible discounts to the DL Unlimited and the other phone with no applied promos, " +
                "and check that the Promo code is applied correctly", () -> {
            cartPage.applyPromoCode(phonesUsdDiscountPromo.promoCode);
            steps.cartTab.setUpDiscounts(ciscoPhone, dlUnlimited);

            promosSteps.stepCheckAppliedPromo(polycomPhone, phonesUsdDiscountPromo);
        });

        step("3. Open the Quote Details tab, select Payment Method = 'Credit Card', " +
                "populate Main Area Code, Initial Term and Start Date, and save changes", () -> {
            quotePage.openTab();
            quotePage.selectPaymentMethod(CREDIT_CARD_PAYMENT_METHOD);
            promosSteps.populateRequiredInformationOnQuoteDetailsTab();
        });

        step("4. Update the Quote to Active Agreement via API", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);
        });

        step("5. Open the Opportunity record page, press 'Process Order' button on the Opportunity's record page, " +
                "verify that 'Preparing Data' step is completed, " +
                "select the timezone, click 'Sign Up MVP', and check that the account is processed for signing up", () -> {
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());
            opportunityPage.clickProcessOrderButton();
            opportunityPage.processOrderModal.waitUntilMvpPreparingDataStepIsCompleted();

            opportunityPage.processOrderModal.selectDefaultTimezone();
            opportunityPage.processOrderModal.signUpButton.click();

            opportunityPage.processOrderModal.signUpMvpStatus
                    .shouldHave(exactTextCaseSensitive(format(YOUR_ACCOUNT_IS_BEING_PROCESSED_MESSAGE, MVP_SERVICE)), ofSeconds(60));
        });

        step("6. Check the discount information on the account in NGBS", () -> {
            var billingID = step("Wait until Account's Billing_ID__c and RC_User_ID__c will get the values from NGBS", () -> {
                return assertWithTimeout(() -> {
                    var accountUpdated = enterpriseConnectionUtils.querySingleRecord(
                            "SELECT Id, Billing_ID__c, RC_User_ID__c " +
                                    "FROM Account " +
                                    "WHERE Id = '" + steps.salesFlow.account.getId() + "'",
                            Account.class);
                    assertNotNull(accountUpdated.getBilling_ID__c(), "Account.Billing_ID__c field");
                    assertNotNull(accountUpdated.getRC_User_ID__c(), "Account.RC_User_ID__c field");

                    step("Account.Billing_ID__c = " + accountUpdated.getBilling_ID__c());
                    step("Account.RC_User_ID__c = " + accountUpdated.getRC_User_ID__c());

                    return accountUpdated.getBilling_ID__c();
                }, ofSeconds(120), ofSeconds(5));
            });

            step("Check that the account has the expected discounts in NGBS", () -> {
                var accountDTO = getAccountInNGBS(billingID);
                var packageID = accountDTO.getMainPackage().id;

                var allDiscountTemplateGroups = getDiscountsFromNGBS(billingID, packageID);
                assertThat(allDiscountTemplateGroups.size())
                        .as("Number of Discount Template Groups on the NGBS account")
                        //  2 discount groups: 1) flexible/contract discount; 2) promo discounts
                        .isEqualTo(2);

                step("Check that the NGBS account has only 1 promo discount " +
                        "with the correct value and type for the license with promotion in NGBS", () -> {
                    var promoDiscountTemplateGroups = allDiscountTemplateGroups.stream()
                            .filter(discount -> discount.promoCode.equals(phonesUsdDiscountPromo.promoCode))
                            .toList();
                    assertThat(promoDiscountTemplateGroups.size())
                            .as("Number of Discount Template Groups for the applied promotion = " + phonesUsdDiscountPromo.promoCode)
                            .isEqualTo(1);
                    var promoDiscountTemplatesInNGBS = promoDiscountTemplateGroups.get(0).discountTemplates;
                    assertThat(promoDiscountTemplatesInNGBS.length)
                            .as("Number of Discount Templates for the applied promotion = " + phonesUsdDiscountPromo.promoCode)
                            .isEqualTo(1);

                    checkDiscountTemplate(promoDiscountTemplatesInNGBS[0], promoDiscountTemplate.value, promoDiscountTemplate.type);
                });

                step("Check that the NGBS account has only 1 contract discount and 1 flexible discount " +
                        "with the correct values and types", () -> {
                    var contractAndFlexibleDiscountTemplateGroups = allDiscountTemplateGroups.stream()
                            .filter(discount -> !discount.promoCode.equals(phonesUsdDiscountPromo.promoCode))
                            .toList();
                    assertThat(contractAndFlexibleDiscountTemplateGroups.size())
                            .as("Number of Discount Template Groups for flexible and contract discounts")
                            .isEqualTo(1);

                    //  We should filter out the Mobile User discount from the checks
                    var contractAndFlexibleDiscountTemplates = Arrays.stream(contractAndFlexibleDiscountTemplateGroups.get(0).discountTemplates)
                            .filter(discount -> !discount.description.equals(mobileUserLicenseName))
                            .toList();
                    assertThat(contractAndFlexibleDiscountTemplates.size())
                            .as("Number of Discounts Templates for flexible and contract discount group")
                            //  1) DL Unlimited (contract discount); 2) Cisco phone (flexible discount)
                            .isEqualTo(2);

                    var productsWithFlexibleAndContractDiscounts = List.of(ciscoPhone, dlUnlimited);
                    productsWithFlexibleAndContractDiscounts.forEach(product -> {
                        var discountTemplateInNGBS = contractAndFlexibleDiscountTemplates.stream()
                                .filter(discount -> discount.applicableTo.getId().equals(product.dataName))
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("Discount Template for " + product.name + " is not found!"));

                        checkDiscountTemplate(discountTemplateInNGBS, product.discount, product.discountType);
                    });
                });
            });
        });
    }

    /**
     * Check that the discount template has the expected values of the discount and type.
     *
     * @param actualDiscountTemplate actual values of the Discount template from NGBS to check
     * @param expectedDiscountValue  Expected discount value to check
     * @param expectedDiscountType   Expected discount type to check
     */
    private void checkDiscountTemplate(DiscountNgbsDTO.DiscountTemplate actualDiscountTemplate,
                                       Integer expectedDiscountValue, String expectedDiscountType) {
        var actualDiscountValue = actualDiscountTemplate.values.oneTime != null ?
                actualDiscountTemplate.values.oneTime :
                actualDiscountTemplate.values.annual;
        assertThat(doubleToInteger(actualDiscountValue.value))
                .as("Discount Value for for the license with ID = " + actualDiscountTemplate.applicableTo.getId())
                .isEqualTo(expectedDiscountValue);

        var actualDiscountType = actualDiscountValue.unit.equals("Percent") ?
                PERCENT :
                data.getCurrencyIsoCode();
        assertThat(actualDiscountType)
                .as("Discount Type for the license with ID = " + actualDiscountTemplate.applicableTo.getId())
                .isEqualTo(expectedDiscountType);
    }
}
