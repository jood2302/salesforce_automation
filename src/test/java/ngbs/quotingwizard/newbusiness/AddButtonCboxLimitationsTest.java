package ngbs.quotingwizard.newbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static base.Pages.cartPage;
import static base.Pages.productsPage;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("ProductsTab")
@Tag("UQT")
@Tag("Multiproduct-Lite")
@Tag("Cbox")
public class AddButtonCboxLimitationsTest extends BaseTest {
    private final Steps steps;

    //  Test data
    private final Map<String, Package> packageFolderNameToPackageMap;
    private final Product mainLocalNumber;
    private final Product engageDigitalConcurrentSeatOmniChannel;
    private final Product mainTollFreeNumber;
    private final Product dlUnlimited;
    private final Product dlBasic;
    private final Product commonPhoneCore;
    private final Product ringCentralRoomsLicense;
    private final Product domesticRoomsPhoneLineAddOn;
    private final Product botBuilderLargePackage;
    private final Product botBuilderMediumPackage;
    private final Product botBuilderSmallPackage;
    private final Product botBuilderXlVolume;
    private final Product engageDigitalSeatEmailChannelOnly;
    private final Product engageDigitalSeatChatChannelOnly;
    private final Product tollFreeBundle;
    private final Product performanceManagement;
    private final List<Product> sameChildCboxProducts;
    private final List<Product> otherChildCboxProducts;

    private final int dlUnlimitedQuantityAboveRuleThreshold;
    private final int dlUnlimitedQuantityBelowRuleThreshold;

    public AddButtonCboxLimitationsTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_ProServ_Monthly_Contract.json",
                Dataset.class);

        steps = new Steps(data);

        var mvpPackage = data.packageFolders[0].packages[0];
        var rcCcPackage = data.packageFolders[1].packages[0];
        var engageDigitalPackage = data.packageFolders[3].packages[0];
        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, mvpPackage,
                data.packageFolders[1].name, rcCcPackage,
                data.packageFolders[3].name, engageDigitalPackage
        );

        mainLocalNumber = data.getProductByDataName("LC_MLN_31");
        engageDigitalConcurrentSeatOmniChannel = data.getProductByDataName("SA_SEAT_5", engageDigitalPackage);
        mainTollFreeNumber = data.getProductByDataName("LC_MTN_32");
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        dlBasic = data.getProductByDataName("LC_DL-BAS_178");
        commonPhoneCore = data.getProductByDataName("LC_DL-HDSK_177");
        ringCentralRoomsLicense = data.getProductByDataName("LC_RCRM_77");
        domesticRoomsPhoneLineAddOn = data.getProductByDataName("LC_DL-ROOMS_969");
        botBuilderLargePackage = data.getProductByDataName("CC_BBSPKG_629", rcCcPackage);
        botBuilderMediumPackage = data.getProductByDataName("CC_BBSPKG_627", rcCcPackage);
        botBuilderSmallPackage = data.getProductByDataName("CC_BBSPKG_625", rcCcPackage);
        botBuilderXlVolume = data.getProductByDataName("CC_BBSPKG_631", rcCcPackage);
        engageDigitalSeatEmailChannelOnly = data.getProductByDataName("SA_SEAT_1", engageDigitalPackage);
        engageDigitalSeatChatChannelOnly = data.getProductByDataName("SA_SEAT_3", engageDigitalPackage);
        tollFreeBundle = data.getProductByDataName("LC_TB_373");
        performanceManagement = data.getProductByDataName("CC_WOIVPM_56", rcCcPackage);

        sameChildCboxProducts = List.of(
                data.getProductByDataName("CC_WOIVSTW_57", rcCcPackage),
                data.getProductByDataName("CC_WOIVGAM_58", rcCcPackage),
                data.getProductByDataName("CC_WOIVCLM_59", rcCcPackage)
        );

        otherChildCboxProducts = List.of(
                data.getProductByDataName("CC_WOIVPMCONC_708", rcCcPackage),
                data.getProductByDataName("CC_WOIVGACONC_712", rcCcPackage),
                data.getProductByDataName("CC_WOIVSTCONC_710", rcCcPackage),
                data.getProductByDataName("CC_WOIVCLCONC_714", rcCcPackage)
        );

        dlUnlimitedQuantityAboveRuleThreshold = 50_000;
        dlUnlimitedQuantityBelowRuleThreshold = 49_999;
    }

    @BeforeEach
    public void setUpTest() {
        var salesRepUser = steps.salesFlow.getSalesRepUser();
        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);
        steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser);
    }

    @Test
    @TmsLink("CRM-26922")
    @TmsLink("CRM-35989")
    @DisplayName("CRM-26922 - 'Add' button is disabled on NB if license cannot be added (cbox limitations). \n" +
            "CRM-35989 - 'Add' button is disabled on NB if license cannot be added (existing multi-level cbox limitations)")
    @Description("CRM-26922 - Verify that 'Add' button on Product tab of NB Quotes is disabled " +
            "regarding cbox and a parent license rules validation. \n" +
            "CRM-35989 - Verify that 'Add' button on Product tab of NB Quotes is disabled regarding multi-level cbox rules " +
            "(nesting structure of multi-level cboxes and their rules must be considered)")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote, " +
                "select MVP, RC CC and ED packages for it, and save changes", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
            steps.quoteWizard.selectPackagesForMultiProductQuote(packageFolderNameToPackageMap);
        });

        //  CRM-26922
        step("2. Open the Price tab and check that 'Main Local Number' " +
                "and 'Engage Digital concurrent seat, omni-channel' licenses are added to Cart", () -> {
            cartPage.openTab();

            cartPage.getQliFromCartByDisplayName(mainLocalNumber.name).getDisplayName().shouldBe(visible);
            cartPage.getQliFromCartByDisplayName(engageDigitalConcurrentSeatOmniChannel.name).getDisplayName().shouldBe(visible);
        });

        //  CRM-26922
        step("3. Open the Add Products tab and check that 'Add' button for 'Main Toll-Free Number' license is disabled", () -> {
            productsPage.openTab();

            productsPage.findProduct(mainTollFreeNumber).getAddButtonElement().shouldBe(disabled);
        });

        //  CRM-26922
        step("4. Remove 'Main Local Number' license and check that 'Add' buttons for 'Main Toll-Free Number' " +
                "and 'Main Local Number' licenses are enabled", () -> {
            productsPage.findProduct(mainLocalNumber).getRemoveButtonElement().click();

            productsPage.findProduct(mainTollFreeNumber).getAddButtonElement().shouldBe(enabled);
            productsPage.findProduct(mainLocalNumber).getAddButtonElement().shouldBe(enabled);
        });

        //  CRM-26922
        step("5. Open the Price tab, increase quantity of DL up to " + dlUnlimitedQuantityAboveRuleThreshold + ", " +
                "open the Add Products tab and check that 'Add' buttons are disabled for 'DigitalLine Basic' " +
                "and 'Common Phone Core' licenses", () -> {
            cartPage.openTab();
            cartPage.setQuantityForQLItem(dlUnlimited.name, dlUnlimitedQuantityAboveRuleThreshold);

            productsPage.openTab();
            productsPage.findProduct(dlBasic).getAddButtonElement().shouldBe(disabled);
            productsPage.findProduct(commonPhoneCore).getAddButtonElement().shouldBe(disabled);
        });

        //  CRM-26922
        step("6. Add 'RingCentral Rooms License' to the Cart " +
                "and check that 'Add' button is disabled for 'Domestic Rooms Phone Line Add-On' license", () -> {
            productsPage.addProduct(ringCentralRoomsLicense);

            productsPage.findProduct(domesticRoomsPhoneLineAddOn).getAddButtonElement().shouldBe(disabled);
        });

        //  CRM-26922
        step("7. Open the Price tab, decrease quantity of DL up to " + dlUnlimitedQuantityBelowRuleThreshold + ", " +
                "open the Add Products tab and check that 'Add' buttons are enabled for 'DigitalLine Basic', " +
                "'Common Phone Core' and 'Domestic Rooms Phone Line Add-On' licenses", () -> {
            cartPage.openTab();
            cartPage.setQuantityForQLItem(dlUnlimited.name, dlUnlimitedQuantityBelowRuleThreshold);

            productsPage.openTab();
            productsPage.findProduct(dlBasic).getAddButtonElement().shouldBe(enabled);
            productsPage.findProduct(commonPhoneCore).getAddButtonElement().shouldBe(enabled);
            productsPage.findProduct(domesticRoomsPhoneLineAddOn).getAddButtonElement().shouldBe(enabled);
        });

        //  CRM-26922
        step("8. Add DigitalLine Basic (LC_DL-BAS_178) to the Cart and check 'Add' buttons for 'Common Phone Core' " +
                "and 'Domestic Rooms Phone Line Add-On' licenses are disabled", () -> {
            productsPage.addProduct(dlBasic);

            productsPage.findProduct(commonPhoneCore).getAddButtonElement().shouldBe(disabled);
            productsPage.findProduct(domesticRoomsPhoneLineAddOn).getAddButtonElement().shouldBe(disabled);
        });

        //  CRM-26922
        step("9. Add 'Bot Builder Large Package' to the Cart and check that 'Add' buttons for 'Bot Builder Medium Package', " +
                "'Bot Builder Small Package' and 'Bot Builder XL Volume' licenses are disabled", () -> {
            productsPage.addProduct(botBuilderLargePackage);

            productsPage.findProduct(botBuilderMediumPackage).getAddButtonElement().shouldBe(disabled);
            productsPage.findProduct(botBuilderSmallPackage).getAddButtonElement().shouldBe(disabled);
            productsPage.findProduct(botBuilderXlVolume).getAddButtonElement().shouldBe(disabled);
        });

        //  CRM-26922
        step("10. Remove 'Bot Builder Large Package' and check that 'Add' buttons for 'Bot Builder Large Package', " +
                "'Bot Builder Medium Package', 'Bot Builder Small Package' and 'Bot Builder XL Volume' licenses are enabled", () -> {
            productsPage.removeProduct(botBuilderLargePackage);

            productsPage.findProduct(botBuilderLargePackage).getAddButtonElement().shouldBe(enabled);
            productsPage.findProduct(botBuilderMediumPackage).getAddButtonElement().shouldBe(enabled);
            productsPage.findProduct(botBuilderSmallPackage).getAddButtonElement().shouldBe(enabled);
            productsPage.findProduct(botBuilderXlVolume).getAddButtonElement().shouldBe(enabled);
        });

        //  CRM-26922
        step("11. Check that 'Add' buttons for 'Engage Digital concurrent seat, email channel only' " +
                "and 'Engage Digital concurrent seat, chat channel only' licenses are disabled", () -> {
            productsPage.findProduct(engageDigitalSeatEmailChannelOnly).getAddButtonElement().shouldBe(disabled);
            productsPage.findProduct(engageDigitalSeatChatChannelOnly).getAddButtonElement().shouldBe(disabled);
        });

        //  CRM-26922
        step("12. Remove 'Engage Digital concurrent seat, omni-channel' license and check that 'Add' buttons for " +
                "'Engage Digital concurrent seat, omni-channel', 'Engage Digital concurrent seat, email channel only' " +
                "and 'Engage Digital concurrent seat, chat channel only' licenses are enabled", () -> {
            productsPage.removeProduct(engageDigitalConcurrentSeatOmniChannel);

            productsPage.findProduct(engageDigitalConcurrentSeatOmniChannel).getAddButtonElement().shouldBe(enabled);
            productsPage.findProduct(engageDigitalSeatEmailChannelOnly).getAddButtonElement().shouldBe(enabled);
            productsPage.findProduct(engageDigitalSeatChatChannelOnly).getAddButtonElement().shouldBe(enabled);
        });

        //  CRM-26922
        step("13. Add '1,000 minute toll-free bundle' license to the Cart " +
                "and check that 'Add' buttons for the rest of the licenses on the Toll Free tab are disabled", () -> {
            productsPage.addProduct(tollFreeBundle);

            var allOtherTollFreeProducts = productsPage.getAllVisibleProducts()
                    .stream()
                    .filter(p -> !p.getNameElement().has(exactTextCaseSensitive(tollFreeBundle.name)))
                    .toList();
            for (var productItem : allOtherTollFreeProducts) {
                step("Check that the product item '" + productItem.getNameElement().getText() + "' has disabled 'Add' button", () -> {
                    productItem.getAddButtonElement().shouldBe(disabled);
                });
            }
        });

        //  CRM-26922
        step("14. Remove '1,000 minute toll-free bundle' license " +
                "and check that 'Add' buttons for the all licenses on the Toll Free tab are enabled", () -> {
            productsPage.removeProduct(tollFreeBundle);

            var allTollFreeProducts = productsPage.getAllVisibleProducts();
            for (var productItem : allTollFreeProducts) {
                step("Check that the product item '" + productItem.getNameElement().getText() + "' has enabled 'Add' button", () -> {
                    productItem.getAddButtonElement().shouldBe(enabled);
                });
            }
        });

        //  CRM-35989
        step("15. Add 'Performance Management (per Configured User)' license to the Cart " +
                "and check that 'Add' buttons for all licenses from the same child cbox are still enabled " +
                "and 'Add' buttons for all licenses from other child cboxes under same parent cbox are disabled", () -> {
            productsPage.addProduct(performanceManagement);

            for (var product : sameChildCboxProducts) {
                step("Check that the product item '" + product.name + "' has enabled 'Add' button", () -> {
                    productsPage.findProduct(product).getAddButtonElement().shouldBe(enabled);
                });
            }

            for (var product : otherChildCboxProducts) {
                step("Check that the product item '" + product.name + "' has disabled 'Add' button", () -> {
                    productsPage.findProduct(product).getAddButtonElement().shouldBe(disabled);
                });
            }
        });

        //  CRM-35989
        step("16. Remove 'Performance Management (per Configured User)' license " +
                "and check that 'Add' buttons for all licenses from all child cboxes under same parent cbox are enabled", () -> {
            productsPage.removeProduct(performanceManagement);

            for (var product : Stream.concat(sameChildCboxProducts.stream(), otherChildCboxProducts.stream()).toList()) {
                step("Check that the product item '" + product.name + "' has enabled 'Add' button", () -> {
                    productsPage.findProduct(product).getAddButtonElement().shouldBe(enabled);
                });
            }
        });
    }
}
