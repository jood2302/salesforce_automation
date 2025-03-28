package ngbs.quotingwizard.newbusiness.carttab.promotions;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.DQ_Deal_Qualification__c;
import com.sforce.soap.enterprise.sobject.Opportunity;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.cartPage;
import static base.Pages.quotePage;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.NOT_REQUIRED_APPROVAL_STATUS;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage.REQUIRED_APPROVAL_STATUS;
import static com.aquiva.autotests.rc.utilities.StringHelper.TEST_STRING;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.DqDealQualificationHelper.APPROVED_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.CLOSED_WON_STAGE;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Tag("P1")
@Tag("Promos")
@Tag("PriceTab")
public class ApplyFlexibleAndPromoDiscountsAfterPromoTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final PromosSteps promosSteps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Promotion recurringUsdDiscountPromo;
    private final Promotion crfPercentDiscountPromo;
    private final Product dlUnlimited;
    private final Product polycomPhone;
    private final Product complianceRecoveryFee;

    public ApplyFlexibleAndPromoDiscountsAfterPromoTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_AnnualAndMonthly_Contract_PhonesAndDLs_Promos.json",
                Dataset.class);

        steps = new Steps(data);
        promosSteps = new PromosSteps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        recurringUsdDiscountPromo = promosSteps.promotions[0];
        crfPercentDiscountPromo = promosSteps.promotions[5];
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        polycomPhone = data.getProductByDataName("LC_HD_936");

        //  this value should exceed a 'ceiling' configured in the custom metadata types: 
        //  DiscountCeilingCBox__mdt, DiscountCeilingCategory__mdt, DiscountCeilingLicense__mdt
        polycomPhone.discount = 81;

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
    @TmsLink("CRM-21780")
    @TmsLink("CRM-21784")
    @DisplayName("CRM-21780 - Apply flexible discounts after applying promo. \n" +
            "CRM-21784 - Applying Promo code to the approved Quote with flexible discounts")
    @Description("CRM-21780 - To verify that flexible discounts can be added to Quote with already applied Promo code. \n" +
            "CRM-21784 - To verify that promo can be applied when quote already contains some approved flexible discounts")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, save changes, and add some products on the Add Products tab", () -> {
            steps.quoteWizard.prepareOpportunityViaQuoteWizardVfPage(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
        });

        step("2. Open the Price tab, apply any Promo code and check that it's applied correctly", () -> {
            cartPage.openTab();
            cartPage.applyPromoCode(recurringUsdDiscountPromo.promoCode);

            promosSteps.stepCheckAppliedPromo(dlUnlimited, recurringUsdDiscountPromo);
        });

        //  CRM-21780
        step("3. Add flexible discount to any item not in the promotion, save changes, " +
                "and check the Quote Approval Status", () -> {
            cartPage.setDiscountForQLItem(polycomPhone.name, polycomPhone.discount);
            cartPage.saveChanges();

            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(REQUIRED_APPROVAL_STATUS));
        });

        step("4. Open the Quote Details tab, " +
                "populate Main Area Code, Discount Justification, Initial Term, Start Date, and save changes", () -> {
            quotePage.openTab();
            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);
            quotePage.initialTermPicklist.selectOption(promosSteps.initialTerm);
            quotePage.setDefaultStartDate();
            //  required to submit a quote for approval later
            quotePage.discountJustificationTextArea.setValue(TEST_STRING);
            quotePage.saveChanges();
        });

        step("5. Open the Price tab, and submit the quote for approval via 'Submit for Approval' button", () -> {
            cartPage.openTab();
            cartPage.submitForApproval();
        });

        //  shortcut to avoid approving DQ via Deal Desk user on the Deal Qualification tab
        step("6. Set the related DQ_Deal_Qualification__c.Status__c = 'Approved' via API", () -> {
            var dealQualification = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM DQ_Deal_Qualification__c " +
                            "WHERE Opportunity__c = '" + steps.quoteWizard.opportunity.getId() + "'",
                    DQ_Deal_Qualification__c.class);
            dealQualification.setStatus__c(APPROVED_STATUS);
            enterpriseConnectionUtils.update(dealQualification);
        });

        //  CRM-21784
        step("7. Remove old promo, apply any promo that doesn't override existing flexible discount, save the changes" +
                "and check the Quote Approval Status", () -> {
            cartPage.changeAppliedPromo(crfPercentDiscountPromo.promoCode);

            promosSteps.stepCheckAppliedPromo(complianceRecoveryFee, crfPercentDiscountPromo);
            cartPage.saveChanges();

            cartPage.approvalStatus.shouldHave(exactTextCaseSensitive(NOT_REQUIRED_APPROVAL_STATUS));
        });

        step("8. Update the Quote to the Active Agreement via API", () -> {
            steps.quoteWizard.stepUpdateQuoteToApprovedActiveAgreement(steps.quoteWizard.opportunity);
        });

        //  CRM-21780, CRM-21784
        step("9. Close the Opportunity via API, and check its 'StageName' and 'IsClosed' fields' values in DB", () -> {
            steps.quoteWizard.stepCloseOpportunity(steps.quoteWizard.opportunity);

            var opportunityClosed = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, IsClosed, StageName " +
                            "FROM Opportunity " +
                            "WHERE Id = '" + steps.quoteWizard.opportunity.getId() + "'",
                    Opportunity.class);
            assertThat(opportunityClosed.getStageName())
                    .as("Opportunity.StageName value")
                    .isEqualTo(CLOSED_WON_STAGE);
            assertThat(opportunityClosed.getIsClosed())
                    .as("Opportunity.IsClosed value")
                    .isTrue();
        });
    }
}
