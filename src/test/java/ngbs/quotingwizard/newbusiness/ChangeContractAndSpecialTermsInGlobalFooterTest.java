package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import static base.Pages.*;
import static com.aquiva.autotests.rc.model.ngbs.dto.partner.PartnerNgbsDTO.ACTIVE_STATUS;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NgbsQuotingWizardFooter.CONTRACT_NONE;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.producttab.ProductsPage.RENTAL_PHONES_ARE_AVAILABLE_ONLY_UNDER_CONTRACT;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToIntToString;
import static com.aquiva.autotests.rc.utilities.StringHelper.NONE;
import static com.aquiva.autotests.rc.utilities.StringHelper.PERCENT;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.DISCOUNT_TYPE_CURRENCY;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.DISCOUNT_TYPE_PERCENTAGE;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThanOrEqual;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.sleep;
import static io.qameta.allure.Allure.step;
import static java.lang.String.valueOf;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("P0")
@Tag("QTFooter")
@Tag("UQT")
public class ChangeContractAndSpecialTermsInGlobalFooterTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    //  Test data
    private final Product dlUnlimitedWithContract;
    private final Product dlUnlimitedWithoutContract;
    private final Product rentalPhone;

    private final String initialTerm;
    private final String renewalTerm;
    private final String freeServiceCredit;
    private final String specialShippingTerms;

    public ChangeContractAndSpecialTermsInGlobalFooterTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Monthly_Contract_1PhoneAnd1DL_RegularAndPOC.json",
                Dataset.class);

        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        steps = new Steps(data);

        dlUnlimitedWithContract = data.getProductByDataName("LC_DL-UNL_50");
        dlUnlimitedWithoutContract = data.getProductByDataName("LC_DL-UNL_50",
                singletonList(data.packageFolders[0].packages[4].productsDefault));
        rentalPhone = data.getProductByDataName("LC_HDR_619");

        initialTerm = data.packageFolders[0].packages[0].contractTerms.initialTerm[0];
        renewalTerm = data.packageFolders[0].packages[0].contractTerms.renewalTerm;
        freeServiceCredit = "4 Free Months of Service";
        specialShippingTerms = "Free shipping";
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-26125")
    @TmsLink("CRM-26126")
    @DisplayName("CRM-26125 - Changing Contract Terms in Global Footer. \n" +
            "CRM-26126 - Changing Special terms in Global Footer.")
    @Description("CRM-26125 - Verify that Contract Terms can be changed in Footer of Quote Wizard and Quote updates accordingly. \n" +
            "CRM-26126 - Verify that Special Terms can be changed in Footer of Quote Wizard and Quote updates accordingly.")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package with Monthly charge term and without Contract, " +
                "open the Price tab, set up a discount for DL Unlimited, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
            packagePage.packageSelector.setContractSelected(false);

            cartPage.openTab();
            steps.cartTab.setUpDiscounts(dlUnlimitedWithContract);
            cartPage.saveChanges();
        });

        step("2. Set Quote.Approved_Status__c = 'Approved' via API", () ->
                steps.quoteWizard.stepUpdateQuoteToApprovedStatus(steps.quoteWizard.opportunity.getId())
        );

        //  CRM-26125
        step("3. Open the Add Products tab, and check Footer's fields", () -> {
            productsPage.openTab();

            productsPage.footer.footerContainer.shouldBe(visible);
            productsPage.footer.paymentPlan.shouldHave(exactTextCaseSensitive(data.chargeTerm));
            productsPage.footer.contract.shouldHave(exactTextCaseSensitive(CONTRACT_NONE));
            productsPage.footer.initialTerm.shouldBe(hidden);
            productsPage.footer.renewalTerm.shouldBe(hidden);
            productsPage.footer.freeServiceCredit.shouldBe(hidden);
            productsPage.footer.specialShippingTerms.shouldBe(hidden);
        });

        step("4. Click 'Billing Details and Terms' button, select the Contract checkbox, " +
                "select values in Initial Term, Renewal Term, Special Terms and Free Shipping picklists, apply changes, " +
                "and check that all the corresponding values are updated in the Footer", () -> {
            productsPage.footer.billingDetailsAndTermsButton.click();

            productsPage.billingDetailsAndTermsModal.setContractSelected(true);
            productsPage.billingDetailsAndTermsModal.initialTermPicklist.selectOption(initialTerm);
            productsPage.billingDetailsAndTermsModal.renewalTermPicklist.selectOption(renewalTerm);

            productsPage.billingDetailsAndTermsModal.specialTermsPicklist.selectOption(freeServiceCredit);
            productsPage.billingDetailsAndTermsModal.freeShippingTermsPicklist.selectOption(specialShippingTerms);
            productsPage.applyChangesInBillingDetailsAndTermsModal();

            //  CRM-26125
            productsPage.footer.contract.shouldHave(exactTextCaseSensitive(ACTIVE_STATUS));
            productsPage.footer.initialTerm.shouldHave(exactText(initialTerm));
            productsPage.footer.renewalTerm.shouldHave(exactText(renewalTerm));

            //  CRM-26126
            productsPage.footer.freeServiceCredit.shouldHave(exactTextCaseSensitive(freeServiceCredit));
            productsPage.footer.specialShippingTerms.shouldHave(exactTextCaseSensitive(specialShippingTerms));
        });

        step("5. Open to the Quote Details tab, set up the Area Codes, " +
                "and check that all the corresponding values are updated in the Footer", () -> {
            quotePage.openTab();

            //  TODO Remove this sleep when Known Issue PBC-20909 is fixed (without it Main Area Code is not available right away)
            sleep(2_000);

            quotePage.setMainAreaCode(steps.quoteWizard.localAreaCode);

            //  CRM-26125
            quotePage.footer.contract.shouldHave(exactTextCaseSensitive(ACTIVE_STATUS));
            quotePage.footer.initialTerm.shouldHave(exactText(initialTerm));
            quotePage.footer.renewalTerm.shouldHave(exactText(renewalTerm));

            //  CRM-26126
            quotePage.footer.freeServiceCredit.shouldHave(exactTextCaseSensitive(freeServiceCredit));
            quotePage.footer.specialShippingTerms.shouldHave(exactTextCaseSensitive(specialShippingTerms));
        });

        //  CRM-26125
        step("6. Open the Select Package tab, " +
                "and verify that 'Contract' checkbox is checked and the Footer is hidden", () -> {
            packagePage.openTab();
            packagePage.packageSelector.packageFilter.contractSelectInput.shouldBe(checked);
            quotingWizardFooter.footerContainer.shouldBe(hidden);
        });

        //  CRM-26125
        step("7. Switch to the Add Products tab, check that Contract = 'Active' in the Footer, " +
                "and add any rental phone", () -> {
            productsPage.openTab();
            productsPage.footer.contract.shouldHave(exactTextCaseSensitive(ACTIVE_STATUS));

            productsPage.addProduct(rentalPhone);
        });

        //  CRM-26125, CRM-26126
        step("8. Switch to the Price tab, save changes, " +
                "and check that flexible and contract discounts are applied to DL Unlimited", () -> {
            cartPage.openTab();
            cartPage.saveChanges();

            checkDiscountForDlUnlimited(dlUnlimitedWithContract);
        });

        //  CRM-26126
        step("9. Click 'Billing Details and Terms' button, " +
                "select 'None' for Special Terms and Free Shipping picklists, " +
                "apply changes, and check the corresponding values on the Footer", () -> {
            cartPage.footer.billingDetailsAndTermsButton.click();

            cartPage.billingDetailsAndTermsModal.specialTermsPicklist.selectOptionContainingText(NONE);
            cartPage.billingDetailsAndTermsModal.freeShippingTermsPicklist.selectOptionContainingText(NONE);
            cartPage.applyChangesInBillingDetailsAndTermsModal();

            cartPage.footer.freeServiceCredit.shouldHave(exactTextCaseSensitive(NONE));
            cartPage.footer.specialShippingTerms.shouldHave(exactTextCaseSensitive(NONE));
        });

        //  CRM-26125
        step("10. Click 'Billing Details and Terms' button, " +
                "uncheck Contract checkbox, save changes, and check the corresponding values on the Footer", () -> {
            cartPage.footer.billingDetailsAndTermsButton.click();

            cartPage.billingDetailsAndTermsModal.setContractSelected(false);
            cartPage.billingDetailsAndTermsModal.initialTermPicklist.shouldBe(hidden);
            cartPage.billingDetailsAndTermsModal.renewalTermPicklist.shouldBe(hidden);
            cartPage.applyChangesInBillingDetailsAndTermsModal();
            cartPage.saveChanges();

            cartPage.footer.contract.shouldHave(exactTextCaseSensitive(NONE));
            cartPage.footer.initialTerm.shouldBe(hidden);
            cartPage.footer.renewalTerm.shouldBe(hidden);
        });

        //  CRM-26125
        step("11. Open the Select Package tab, and check that the Contract checkbox is unchecked", () -> {
            packagePage.openTab();
            packagePage.packageSelector.packageFilter.contractSelectInput.shouldNotBe(checked);
        });

        //  CRM-26125
        step("12. Open the Add Products tab, check that all rental phones are unavailable for adding, " +
                "and that the tooltip message for the 'Add' button is correct", () -> {
            productsPage.openTab();
            productsPage.openGroup(rentalPhone.group);
            productsPage.openSubgroup(rentalPhone.subgroup);

            productsPage.addToCartButtons.shouldHave(sizeGreaterThanOrEqual(1));
            var numberOfProducts = productsPage.products.size();
            //  this also checks that the previously added rental phone is removed from the cart (no 'Remove' buttons)
            productsPage.addToCartButtons
                    .filter(and("visible and disabled", visible, disabled))
                    .shouldHave(size(numberOfProducts));

            productsPage.addToCartButtons.first().hover();
            productsPage.tooltip.shouldHave(exactTextCaseSensitive(RENTAL_PHONES_ARE_AVAILABLE_ONLY_UNDER_CONTRACT));
        });

        //  CRM-26125
        step("13. Open the Price tab, and check that the added rental phone is removed there and in DB, " +
                "and that the contract discount is removed and flexible discount is still applied to the DL Unlimited ", () -> {
            cartPage.openTab();
            cartPage.getQliFromCartByDisplayName(rentalPhone.name).getCartItemElement().shouldNot(exist);

            step("Check that the QuoteLineItem record for the rental phone is removed from DB", () -> {
                var rentalPhoneQLIs = enterpriseConnectionUtils.query(
                        "SELECT Id " +
                                "FROM QuoteLineItem " +
                                "WHERE QuoteId = '" + cartPage.getSelectedQuoteId() + "' " +
                                "AND Product2.ExtID__c = '" + rentalPhone.dataName + "'",
                        QuoteLineItem.class);
                assertThat(rentalPhoneQLIs.size())
                        .as("Number of QLIs for the added rental phone")
                        .isZero();
            });
        });

        //  CRM-26125
        step("14. Check that the contract discount is removed and flexible discount is still applied to the DL Unlimited", () -> {
            checkDiscountForDlUnlimited(dlUnlimitedWithoutContract);
        });

        //  CRM-26125
        step("15. Open the Quote Details tab, and check that the Initial Term and Renewal Term fields are disabled", () -> {
            quotePage.openTab();
            quotePage.initialTermPicklist.shouldBe(visible, disabled);
            quotePage.renewalTermPicklist.shouldBe(visible, disabled);
        });
    }

    /**
     * Check a new discounted price, discount and discount type on the Price tab
     * and on the QuoteLineItem record for the DigitalLine Unlimited.
     *
     * @param dlExpected expected test data for the DL Unlimited
     */
    private void checkDiscountForDlUnlimited(Product dlExpected) {
        step("Check Your Price, Your Discount, Discount Type values for the DL Unlimited on the Price tab", () -> {
            var cartItem = cartPage.getQliFromCartByDisplayName(dlExpected.name);
            cartItem.getYourPrice().shouldHave(exactTextCaseSensitive(steps.quoteWizard.currencyPrefix + dlExpected.yourPrice));
            cartItem.getDiscountInput().shouldHave(exactValue(valueOf(dlExpected.discount)));
            cartItem.getDiscountTypeSelect().getSelectedOption().shouldHave(exactTextCaseSensitive(dlExpected.discountType));
        });

        step("Check EffectivePrice__c, Discount_number__c, Discount_type__c on the QuoteLineItem record for the DL Unlimited", () -> {
            var qli = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id, EffectivePrice__c, Discount_number__c, Discount_type__c " +
                            "FROM QuoteLineItem " +
                            "WHERE QuoteId = '" + cartPage.getSelectedQuoteId() + "' " +
                            "AND Product2.ExtID__c = '" + dlExpected.dataName + "'",
                    QuoteLineItem.class);

            assertThat(qli.getEffectivePrice__c())
                    .as("QuoteLineItem.EffectivePrice__c value of DL Unlimited")
                    .isEqualTo(Double.valueOf(dlExpected.yourPrice));
            assertThat(doubleToIntToString(qli.getDiscount_number__c()))
                    .as("QuoteLineItem.Discount_number__c value of DL Unlimited")
                    .isEqualTo(String.valueOf(dlExpected.discount));

            var expectedDiscountType = dlExpected.discountType.equals(PERCENT) ?
                    DISCOUNT_TYPE_PERCENTAGE :
                    DISCOUNT_TYPE_CURRENCY;
            assertThat(qli.getDiscount_type__c())
                    .as("QuoteLineItem.Discount_type__c value of DL Unlimited")
                    .isEqualTo(expectedDiscountType);
        });
    }
}
