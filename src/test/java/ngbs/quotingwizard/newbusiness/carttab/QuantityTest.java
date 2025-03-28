package ngbs.quotingwizard.newbusiness.carttab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Dataset;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;

import static base.Pages.cartPage;
import static com.codeborne.selenide.Condition.exactValue;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("PDV")
@Tag("NGBS")
public class QuantityTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    //  Test data
    private final Product digitalLine;
    private final Product digitalLineUnlimited;
    private final Product digitalLineBasic;
    private final Product commonPhone;
    private final Product recoveryFee;
    private final Product e911Fee;

    public QuantityTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_Annual_Contract_AllTypesOfDLs.json",
                Dataset.class);
        steps = new Steps(data);

        digitalLine = data.getProductByDataName("LC_DL_75");
        digitalLineUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        digitalLineBasic = data.getProductByDataName("LC_DL-BAS_178");
        commonPhone = data.getProductByDataName("LC_DL-HDSK_177");
        recoveryFee = data.getProductByDataName("LC_CRF_51");
        e911Fee = data.getProductByDataName("LC_E911_52");
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-12889")
    @DisplayName("CRM-12889 - Auto calculation of items in cart")
    @Description("Verify auto calculation of interdependent items in cart when one of those interdependent items had changed")
    public void test() {
        step("1. Open the Quote Wizard for the Opportunity to add a new Sales Quote, " +
                "select a package for it, add some products on the Add Products tab", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectDefaultPackageFromTestData();
            steps.quoteWizard.addProductsOnProductsTab(data.getNewProductsToAdd());
        });

        step("2. Open the Price tab, and check initial quantity for interdependent items", () -> {
            cartPage.openTab();

            //  Quantity for parent products (digital line and fees) = sum of child products quantities
            //  (without fees and independent products)
            //  Initial quantity for each product = 1, their total quantity sum = total number of the linked products
            var initialTotalQuantity = String.valueOf(List.of(
                            digitalLineUnlimited, digitalLineBasic, commonPhone)
                    .size());

            cartPage.getQliFromCartByDisplayName(digitalLine.name)
                    .getQuantityInput()
                    .shouldHave(exactValue(initialTotalQuantity));

            cartPage.getQliFromCartByDisplayName(recoveryFee.name)
                    .getQuantityInput()
                    .shouldHave(exactValue(initialTotalQuantity));

            cartPage.getQliFromCartByDisplayName(e911Fee.name)
                    .getQuantityInput()
                    .shouldHave(exactValue(initialTotalQuantity));
        });

        step("3. Set quantities for interdependent & independent items and check quantity for parent items", () -> {
            steps.cartTab.setUpQuantities(data.getNewProductsToAdd());
            steps.cartTab.setUpQuantities(digitalLineUnlimited);

            //  Quantity for parent products (digital line and fees) = sum of child products quantities
            //  (without fees and independent products)
            var changedTotalQuantity = String.valueOf(
                    digitalLineUnlimited.quantity + digitalLineBasic.quantity + commonPhone.quantity);

            cartPage.getQliFromCartByDisplayName(digitalLine.name)
                    .getQuantityInput()
                    .shouldHave(exactValue(changedTotalQuantity));

            cartPage.getQliFromCartByDisplayName(recoveryFee.name)
                    .getQuantityInput()
                    .shouldHave(exactValue(changedTotalQuantity));

            cartPage.getQliFromCartByDisplayName(e911Fee.name)
                    .getQuantityInput()
                    .shouldHave(exactValue(changedTotalQuantity));
        });
    }
}
