package ngbs.quotingwizard.existingbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.dto.discounts.DiscountNgbsDTO;
import com.aquiva.autotests.rc.model.ngbs.dto.discounts.PromotionDiscountNgbsDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.Opportunity;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import ngbs.quotingwizard.newbusiness.carttab.promotions.PromosSteps;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.model.ngbs.dto.license.CatalogItem.getItemFromTestData;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.createPromoDiscountInNGBS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_STAGE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteHelper.AGREEMENT_QUOTE_TYPE;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.sleep;
import static io.qameta.allure.Allure.step;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P1")
@Tag("PriceTab")
@Tag("Promos")
public class PromoDiscountPrePopulationExistingBusinessTest extends BaseTest {
    private final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final Steps steps;
    private final PromosSteps promosSteps;

    //  Test data
    private final String ngbsPackageId;
    private final String ngbsPackageVersion;
    private final Promotion promotion;
    private final Product dlBasic;

    public PromoDiscountPrePopulationExistingBusinessTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_Monthly_Contract_217104013_Promo.json",
                Dataset.class);

        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        steps = new Steps(data);
        promosSteps = new PromosSteps(data);

        ngbsPackageId = data.packageFolders[0].packages[0].id;
        ngbsPackageVersion = data.packageFolders[0].packages[0].version;
        promotion = promosSteps.promotions[0];

        dlBasic = data.getProductByDataName("LC_DL-BAS_178");
    }

    @BeforeEach
    public void setUpTest() {
        promosSteps.createPromotionsInNGBS();

        if (steps.ngbs.isGenerateAccounts()) {
            steps.ngbs.generateBillingAccount();
            steps.ngbs.stepCreateContractInNGBS();
            steps.ngbs.purchaseAdditionalLicensesInNGBS(getItemFromTestData(dlBasic.dataName, dlBasic.quantity));

            //  add the promo discount on the Existing Business Account in NGBS
            var promoDiscountTarget = new DiscountNgbsDTO.Target(ngbsPackageId, ngbsPackageVersion);
            var promoDiscountDTO = new PromotionDiscountNgbsDTO(promotion.promoCode, promoDiscountTarget);
            createPromoDiscountInNGBS(data.billingId, data.packageId, promoDiscountDTO);
        }

        var salesRepUserWithPromotionFlowEnabled = promosSteps.getTestUserWithPromosFeature();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithPromotionFlowEnabled);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithPromotionFlowEnabled);
        promosSteps.loginAsTestUserWithPromosFeature();
    }

    @Test
    @TmsLink("CRM-21670")
    @DisplayName("CRM-21670 - Correct pre-population of discounts on the upsell quote if there are only promo discounts on the account")
    @Description("To verify case when upsell quote is created on account with only promotional discounts")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "and keep the same preselected package for it", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
        });

        step("2. Open the Price tab and check that the promotion for the NGBS account is applied " +
                "and prepopulated as a flexible one and save changes", () -> {
            cartPage.openTab();

            promosSteps.stepCheckAppliedPromo(dlBasic, promotion);

            //  "flexible discount" = a discount that can be modified by the user in the QW
            var dlCartItem = cartPage.getQliFromCartByDisplayName(dlBasic.name);
            dlCartItem.getDiscountInput().shouldBe(enabled);
            dlCartItem.getDiscountTypeSelect().shouldBe(enabled);

            cartPage.saveChanges();
        });

        step("3. Open the Quote Details tab, set Quote Stage = 'Agreement', set Start Date, and save changes", () -> {
            quotePage.openTab();

            quotePage.stagePicklist.selectOption(AGREEMENT_QUOTE_TYPE);
            quotePage.setDefaultStartDate();
            quotePage.saveChanges();
        });

        step("4. Set the Quote to Active Agreement via API", () ->
                steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity)
        );

        step("5. Open the Opportunity record page, click 'Close' button, " +
                "and check that Opportunity.StageName = '7. Closed Won'", () -> {
            opportunityPage.openPage(steps.quoteWizard.opportunity.getId());

            opportunityPage.clickCloseButton();
            opportunityPage.spinner.shouldBe(visible, ofSeconds(10));
            opportunityPage.spinner.shouldBe(hidden, ofSeconds(30));
            sleep(2_000);   //  the error message might appear a little later
            opportunityPage.alertNotificationBlock.shouldNot(exist);

            var updatedOpportunity = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, StageName " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Opportunity.class);
            assertThat(updatedOpportunity.getStageName())
                    .as("Opportunity.StageName value")
                    .isEqualTo(CLOSED_WON_STAGE);
        });
    }
}
