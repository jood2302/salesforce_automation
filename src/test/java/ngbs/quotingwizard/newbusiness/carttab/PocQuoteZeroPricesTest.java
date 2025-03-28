package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static base.Pages.cartPage;
import static base.Pages.productsPage;
import static com.aquiva.autotests.rc.utilities.StringHelper.ZERO_PRICE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;

@Tag("P0")
@Tag("NGBS")
public class PocQuoteZeroPricesTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final String zeroPrice;
    private final List<Product> feeProducts;
    private final List<Product> productsWithoutFees;
    private final String serviceGroupName;
    private final String serviceSubgroupName;
    private final String purchasePhonesGroupName;
    private final String purchasePhonesSubgroupName;

    public PocQuoteZeroPricesTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_Annual_RegularAndPOC.json",
                Dataset.class);
        steps = new Steps(data);

        //  E.g. "USD 0.00", "EUR 0.00", etc...
        zeroPrice = format("%s %s", data.getCurrencyIsoCode(), ZERO_PRICE);

        feeProducts = List.of(data.getProductByDataName("LC_CRF_51"), data.getProductByDataName("LC_E911_52"));
        productsWithoutFees = new ArrayList<>();
        productsWithoutFees.addAll(List.of(data.getNewProductsToAdd()));
        productsWithoutFees.addAll(List.of(data.getProductsDefault()));
        productsWithoutFees.removeAll(feeProducts);

        var commonPhone = data.getProductByDataName("LC_DL-HDSK_177");
        var purchasePhone = data.getProductByDataName("LC_HD_687");
        serviceGroupName = commonPhone.group;
        serviceSubgroupName = commonPhone.subgroup;
        purchasePhonesGroupName = purchasePhone.group;
        purchasePhonesSubgroupName = purchasePhone.subgroup;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-11686")
    @TmsLink("CRM-11687")
    @DisplayName("CRM-11686 - All the Prices for all POC Products are equal to 0. \n" +
            "CRM-11687 - All the POC Licenses are disabled for discounts")
    @Description("CRM-11686 - To check that all prices are equal to 0 for POC Packages. \n" +
            "CRM-11687 - To check that user is not able to give any discount for any item on POC Quote")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new POC Quote, " +
                "and select a package for it", () -> {
            steps.quoteWizard.openQuoteWizardForNewPocQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
        });

        //  CRM-11686
        step("2. Open the Add Products tab and check that all products' prices " +
                "in the 'Service' section - 'Main' subsection and 'Phones' section - 'Purchase' subsection are equal to 0", () -> {
            productsPage.openTab();

            productsPage.openGroup(serviceGroupName);
            productsPage.openSubgroup(serviceSubgroupName);
            productsPage.getAllVisibleProducts().forEach(productItem ->
                    productItem.getListPriceElement().shouldHave(exactText(zeroPrice)));

            productsPage.openGroup(purchasePhonesGroupName);
            productsPage.openSubgroup(purchasePhonesSubgroupName);
            productsPage.getAllVisibleProducts().forEach(productItem ->
                    productItem.getListPriceElement().shouldHave(exactText(zeroPrice)));
        });

        step("3. Add items to the cart on the Add Products tab", () ->
                steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd())
        );

        //  CRM-11686, CRM-11687
        step("4. Check that all products' prices are equal to 0 and discount input fields are disabled on the Price tab", () -> {
            cartPage.openTab();

            productsWithoutFees.forEach(product -> {
                var cartItem = cartPage.getQliFromCartByDisplayName(product.name);
                cartItem.getListPrice().shouldHave(exactText(zeroPrice));
                cartItem.getYourPrice().shouldHave(exactText(zeroPrice));
                cartItem.getDiscountInput().shouldBe(disabled);
                cartItem.getDiscountTypeSelect().shouldBe(disabled);
            });

            //  Discount fields should be invisible for 'Compliance and Administrative Cost Recovery Fee' and 'e911 Service Fee'
            feeProducts.forEach(product -> {
                var cartItem = cartPage.getQliFromCartByDisplayName(product.name);
                cartItem.getListPrice().shouldHave(exactText(zeroPrice));
                cartItem.getYourPrice().shouldHave(exactText(zeroPrice));
                cartItem.getDiscountInput().shouldNotBe(visible);
                cartItem.getDiscountTypeSelect().shouldNotBe(visible);
            });
        });
    }
}
