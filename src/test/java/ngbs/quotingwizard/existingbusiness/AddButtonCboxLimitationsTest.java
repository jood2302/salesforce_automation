package ngbs.quotingwizard.existingbusiness;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.codeborne.selenide.WebElementCondition;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.stream.Stream;

import static base.Pages.cartPage;
import static base.Pages.productsPage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.PackageFactory.createBillingAccountPackage;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.CREDIT_CARD_PAYMENT_METHOD;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.PAID_RC_ACCOUNT_STATUS;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static io.qameta.allure.Allure.step;

@Tag("P1")
@Tag("Cbox")
@Tag("UQT")
@Tag("ProductsTab")
@Tag("MultiProduct-UB")
@Tag("LTR-569")
public class AddButtonCboxLimitationsTest extends BaseTest {
    private final Dataset data;
    private final Steps steps;

    private User salesRepUser;

    //  Test data
    private final String officeServiceName;
    private final Package officePackage;
    private final String rcCcServiceName;
    private final Package rcCcPackage;
    private final String engageDigitalServiceName;
    private final Package engageDigitalPackage;

    private final Product mainTollFreeNumber;
    private final Product mainLocalNumber;
    private final Product dlBasic;
    private final Product ringCentralRoomsLicense;
    private final Product dlUnlimited;
    private final Product commonPhoneCore;
    private final Product domesticRoomsPhoneLineAddOn;
    private final Product botBuilderLargePackage;
    private final Product botBuilderMediumPackage;
    private final Product botBuilderSmallPackage;
    private final Product botBuilderXlVolume;
    private final Product engageDigitalSeatEmailChannelOnly;
    private final Product engageDigitalSeatChatChannelOnly;
    private final Product engageDigitalConcurrentSeatOmniChannel;
    private final Product performanceManagement;
    private final Product performanceManagementGamificationPerConfiguredUser;
    private final Product performanceManagementGamificationPerConcurrentUser;
    private final List<Product> configuredUserCboxProducts;
    private final List<Product> concurrentUserCboxProducts;

    private final int dlBasicQuantityAboveRuleThreshold;
    private final int dlBasicQuantityBelowRuleThreshold;
    private final int dlUnlimitedNewQuantity;

    public AddButtonCboxLimitationsTest() {
        data = JsonUtils.readConfigurationResource(
                "data/ngbs/existingbusiness/RC_MVP_RCCC_ED_Monthly_Contract_180709013.json",
                Dataset.class);

        steps = new Steps(data);

        steps.ngbs.isGenerateAccountsForSingleTest = true;

        officeServiceName = data.packageFolders[0].name;
        officePackage = data.packageFolders[0].packages[0];
        rcCcServiceName = data.packageFolders[1].name;
        rcCcPackage = data.packageFolders[1].packages[0];
        engageDigitalServiceName = data.packageFolders[2].name;
        engageDigitalPackage = data.packageFolders[2].packages[0];

        mainTollFreeNumber = data.getProductByDataName("LC_MTN_32");
        mainLocalNumber = data.getProductByDataName("LC_MLN_31");
        dlBasic = data.getProductByDataName("LC_DL-BAS_178");
        ringCentralRoomsLicense = data.getProductByDataName("LC_RCRM_77");
        dlUnlimited = data.getProductByDataName("LC_DL-UNL_50");
        commonPhoneCore = data.getProductByDataName("LC_DL-HDSK_177");
        domesticRoomsPhoneLineAddOn = data.getProductByDataName("LC_DL-ROOMS_969");
        botBuilderLargePackage = data.getProductByDataName("CC_BBSPKG_629", rcCcPackage);
        botBuilderMediumPackage = data.getProductByDataName("CC_BBSPKG_627", rcCcPackage);
        botBuilderSmallPackage = data.getProductByDataName("CC_BBSPKG_625", rcCcPackage);
        botBuilderXlVolume = data.getProductByDataName("CC_BBSPKG_631", rcCcPackage);
        engageDigitalSeatEmailChannelOnly = data.getProductByDataName("SA_SEAT_1", engageDigitalPackage);
        engageDigitalSeatChatChannelOnly = data.getProductByDataName("SA_SEAT_3", engageDigitalPackage);
        engageDigitalConcurrentSeatOmniChannel = data.getProductByDataName("SA_SEAT_5", engageDigitalPackage);
        performanceManagement = data.getProductByDataName("CC_WOIVPM_56", rcCcPackage);
        performanceManagementGamificationPerConfiguredUser = data.getProductByDataName("CC_WOIVGAM_58", rcCcPackage);
        performanceManagementGamificationPerConcurrentUser = data.getProductByDataName("CC_WOIVGACONC_712", rcCcPackage);
        configuredUserCboxProducts = List.of(
                data.getProductByDataName("CC_WOIVSTW_57", rcCcPackage),
                data.getProductByDataName("CC_WOIVGAM_58", rcCcPackage),
                data.getProductByDataName("CC_WOIVCLM_59", rcCcPackage)
        );

        concurrentUserCboxProducts = List.of(
                data.getProductByDataName("CC_WOIVPMCONC_708", rcCcPackage),
                data.getProductByDataName("CC_WOIVGACONC_712", rcCcPackage),
                data.getProductByDataName("CC_WOIVSTCONC_710", rcCcPackage),
                data.getProductByDataName("CC_WOIVCLCONC_714", rcCcPackage)
        );

        dlBasicQuantityAboveRuleThreshold = 49_999;
        dlBasicQuantityBelowRuleThreshold = dlBasicQuantityAboveRuleThreshold - 1;
        dlUnlimitedNewQuantity = 1;
    }

    @BeforeEach
    public void setUpTest() {
        steps.ngbs.generateMultiProductUnifiedBillingAccount();

        step("Find a user with 'Sales Rep - Lightning' profile and 'Enable Multi-Product Unified Billing' Feature Toggle", () -> {
            salesRepUser = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withFeatureToggles(List.of(ENABLE_MULTIPRODUCT_UNIFIED_BILLING_FT))
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUser);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUser);

        step("Create new Billing Account Package objects (Package__c) for the Account " +
                "for the Office, RC CC and ED NGBS packages via SFDC API", () -> {
            createBillingAccountPackage(steps.salesFlow.account.getId(), data.packageId, officePackage.id,
                    data.brandName, officeServiceName, CREDIT_CARD_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);

            createBillingAccountPackage(steps.salesFlow.account.getId(), rcCcPackage.ngbsPackageId, rcCcPackage.id,
                    data.brandName, rcCcServiceName, CREDIT_CARD_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);

            createBillingAccountPackage(steps.salesFlow.account.getId(), engageDigitalPackage.ngbsPackageId, engageDigitalPackage.id,
                    data.brandName, engageDigitalServiceName, CREDIT_CARD_PAYMENT_METHOD, PAID_RC_ACCOUNT_STATUS);
        });

        step("Log in as a user with 'Sales Rep - Lightning' profile and 'Enable Multi-Product Unified Billing' Feature Toggle", () ->
                steps.sfdc.initLoginToSfdcAsTestUser(salesRepUser)
        );
    }

    @Test
    @TmsLink("CRM-36081")
    @TmsLink("CRM-36082")
    @DisplayName("CRM-36081 - 'Add' button is disabled on EB if license cannot be added (cbox limitations). \n" +
            "CRM-36082 - 'Add' button is disabled on EB if license cannot be added (multi-level cbox limitations)")
    @Description("CRM-36081 - Verify that 'Add' button on Add Products tab of EB Quotes is disabled regarding cbox " +
            "and a parent license rules validation. \n" +
            "CRM-36082 - Verify that 'Add' button on Add Products tab of EB Quotes is disabled regarding multi-level cbox rules " +
            "(nesting structure of multi-level cboxes and their rules must be considered)")
    public void test() {
        step("1. Open the Quote Wizard for the test Opportunity to add a new Sales Quote " +
                "and leave the preselected packages the same as on the Account in NGBS", () -> {
            steps.quoteWizard.openQuoteWizardForNewSalesQuoteDirect(steps.quoteWizard.opportunity.getId());
        });

        //  CRM-36081
        step("2. Open the Add Products tab and check that the 'Add' button is disabled for 'Main Toll-Free Number' license", () -> {
            productsPage.openTab();
            productsPage.findProduct(mainTollFreeNumber).getAddButtonElement().shouldBe(disabled);
        });

        //  CRM-36081
        step("3. Open the Price tab and set quantity for Main Local Number to 0, open the Add Products tab " +
                "and check that the 'Add' button is enabled for 'Main Toll-Free Number' license", () -> {
            cartPage.openTab();
            cartPage.setNewQuantityForQLItem(mainLocalNumber.name, 0);

            productsPage.openTab();
            productsPage.findProduct(mainTollFreeNumber).getAddButtonElement().shouldBe(enabled);
        });

        step("4. Add 'DigitalLine Basic' and 'RingCentral Rooms License' licenses to the Cart, " +
                "open the Price tab, set quantity for added licenses DigitalLine Basic = " + dlBasicQuantityAboveRuleThreshold +
                " and DL Unlimited = " + dlUnlimitedNewQuantity, () -> {
            productsPage.addProduct(dlBasic);
            productsPage.addProduct(ringCentralRoomsLicense);

            cartPage.openTab();
            cartPage.setNewQuantityForQLItem(dlBasic.name, dlBasicQuantityAboveRuleThreshold);
            cartPage.setNewQuantityForQLItem(dlUnlimited.name, dlUnlimitedNewQuantity);
        });

        //  CRM-36081
        step("5. Open the Add Products tab and check that Add buttons for 'Common Phone Core' " +
                "and 'Domestic Rooms Phone Line Add-On' licenses are disabled", () -> {
            productsPage.openTab();

            productsPage.findProduct(commonPhoneCore).getAddButtonElement().shouldBe(disabled);
            productsPage.findProduct(domesticRoomsPhoneLineAddOn).getAddButtonElement().shouldBe(disabled);
        });

        //  CRM-36081
        step("6. Open the Price tab and set quantity for the DL Basic license = " + dlBasicQuantityBelowRuleThreshold +
                " and DL Unlimited = " + dlUnlimitedNewQuantity + ", open the Add Products tab " +
                "and check that Add buttons for 'Common Phone Core' and 'Domestic Rooms Phone Line Add-On' licenses are enabled", () -> {
            cartPage.openTab();
            cartPage.setNewQuantityForQLItem(dlBasic.name, dlBasicQuantityBelowRuleThreshold);
            cartPage.setNewQuantityForQLItem(dlUnlimited.name, dlUnlimitedNewQuantity);

            productsPage.openTab();

            productsPage.findProduct(commonPhoneCore).getAddButtonElement().shouldBe(enabled);
            productsPage.findProduct(domesticRoomsPhoneLineAddOn).getAddButtonElement().shouldBe(enabled);
        });

        //  CRM-36081
        step("7. Open 'Bot and Virtual Agent Products' group in RC CC section, open 'Bot Builder' subgroup " +
                "and check that Add buttons for 'Bot Builder Medium Package', 'Bot Builder Small Package' " +
                "and 'Bot Builder XL Volume' licenses are disabled", () -> {
            productsPage.openGroup(botBuilderMediumPackage.group);
            productsPage.openSubgroup(botBuilderMediumPackage.subgroup);

            checkAddButton(disabled, List.of(botBuilderMediumPackage, botBuilderSmallPackage, botBuilderXlVolume));
        });

        //  CRM-36081
        step("8. Open the Price tab and set quantity for 'Bot Builder Large Package' to 0, " +
                "open the Add Products tab, open the 'Bot and Virtual Agent Products' group in RC CC section, open 'Bot Builder' subgroup " +
                "and check that Add buttons for 'Bot Builder Medium Package', 'Bot Builder Small Package' " +
                "and 'Bot Builder XL Volume' licenses are enabled", () -> {
            cartPage.openTab();
            cartPage.setNewQuantityForQLItem(botBuilderLargePackage.name, 0);

            productsPage.openTab();
            productsPage.openGroup(botBuilderMediumPackage.group);
            productsPage.openSubgroup(botBuilderMediumPackage.subgroup);

            checkAddButton(enabled, List.of(botBuilderMediumPackage, botBuilderSmallPackage, botBuilderXlVolume));
        });

        //  CRM-36081
        step("9. Add 'Bot Builder Medium Package' license to the Cart and check that 'Add' buttons " +
                "for 'Bot Builder Small Package' and 'Bot Builder XL Volume' licenses are disabled", () -> {
            productsPage.addProduct(botBuilderMediumPackage);

            checkAddButton(disabled, List.of(botBuilderSmallPackage, botBuilderXlVolume));
        });

        //  CRM-36081
        step("10. Open 'Services' group in ED section, open 'Purchase' subgroup " +
                "and check that 'Add' buttons for 'Engage Digital concurrent seat, email channel only' " +
                "and 'Engage Digital concurrent seat, chat channel only' licenses is disabled", () -> {
            productsPage.openGroup(engageDigitalServiceName, engageDigitalSeatChatChannelOnly.group);
            productsPage.openSubgroup(engageDigitalSeatChatChannelOnly.subgroup);

            checkAddButton(disabled, List.of(engageDigitalSeatEmailChannelOnly, engageDigitalSeatChatChannelOnly));
        });

        //  CRM-36081
        step("11. Open the Price tab and set quantity for 'Engage Digital concurrent seat, omni-channel' to 0, " +
                "open the Add Products tab, add 'Engage Digital concurrent seat, email channel only' license to the Cart " +
                "and check that 'Add' button for 'Engage Digital concurrent seat, chat channel only' license is disabled", () -> {
            cartPage.openTab();
            cartPage.setNewQuantityForQLItem(engageDigitalConcurrentSeatOmniChannel.name, 0);

            productsPage.openTab();
            productsPage.addProduct(engageDigitalSeatEmailChannelOnly);

            productsPage.getProductItem(engageDigitalSeatChatChannelOnly).getAddButtonElement().shouldBe(disabled);
        });

        //  CRM-36082
        step("12. Open 'Workforce Engagement Management' group for RC CC section, open 'Performance Management' subgroup, " +
                "check that Add buttons for all licenses from same child cbox as the license from Account are still enabled, " +
                "and Add buttons for all licenses from all other child cboxes under same Parent cbox are disabled, " +
                "and check that Remove button for 'Performance Management (per Configured User)' license is disabled", () -> {
            productsPage.openGroup(performanceManagement.group);
            productsPage.openSubgroup(performanceManagement.subgroup);

            checkAddButton(enabled, configuredUserCboxProducts);
            checkAddButton(disabled, concurrentUserCboxProducts);
            productsPage.getProductItem(performanceManagement).getRemoveButtonElement().shouldBe(disabled);
        });

        //  CRM-36082
        step("13. Open the Price tab and set quantity for 'Performance Management (per Configured User)' to 0, " +
                "open the Add Products tab, open 'Workforce Engagement Management' group for RC CC section, open 'Performance Management' subgroup, " +
                "check that Add button for all licenses from same child cbox and from all other child cboxes under same Parent C-Box are enabled, " +
                "and check that Remove button for 'Performance Management (per Configured User)' license is disabled", () -> {
            cartPage.openTab();
            cartPage.setNewQuantityForQLItem(performanceManagement.name, 0);

            productsPage.openTab();
            productsPage.openGroup(performanceManagement.group);
            productsPage.openSubgroup(performanceManagement.subgroup);

            checkAddButton(enabled, Stream.concat(configuredUserCboxProducts.stream(), concurrentUserCboxProducts.stream()).toList());

            productsPage.getProductItem(performanceManagement).getRemoveButtonElement().shouldBe(disabled);
        });

        //  CRM-36082
        step("14. Add 'Performance Management - Gamification (per Configured User)' license to the Cart " +
                "and check that 'Add' buttons for all licenses from the same child cbox are enabled, " +
                "Add buttons for all licenses from other child cboxes under same parent cbox are disabled " +
                "and Remove button for 'Performance Management (per Configured User)' license is disabled", () -> {
            productsPage.addProduct(performanceManagementGamificationPerConfiguredUser);

            checkAddButton(enabled, configuredUserCboxProducts.stream()
                    .filter(p -> !p.equals(performanceManagementGamificationPerConfiguredUser)).toList());
            checkAddButton(disabled, concurrentUserCboxProducts);
            productsPage.getProductItem(performanceManagement).getRemoveButtonElement().shouldBe(disabled);
        });

        //  CRM-36082
        step("15. Remove 'Performance Management - Gamification (per Configured User)' license from the Cart, " +
                "add 'Performance Management - Gamification (per Concurrent User)' license to the Cart " +
                "and check that Add buttons for all licenses from the same child cbox are enabled, " +
                "Add buttons for all licenses from other child cboxes under same parent cbox are disabled, " +
                "and Remove button for 'Performance Management (per Configured User)' license is disabled", () -> {
            productsPage.removeProduct(performanceManagementGamificationPerConfiguredUser);
            productsPage.addProduct(performanceManagementGamificationPerConcurrentUser);

            checkAddButton(enabled, concurrentUserCboxProducts.stream()
                    .filter(p -> !p.equals(performanceManagementGamificationPerConcurrentUser)).toList());
            checkAddButton(disabled, configuredUserCboxProducts.stream()
                    .filter(p -> !p.equals(performanceManagementGamificationPerConfiguredUser)).toList());
            productsPage.getProductItem(performanceManagement).getRemoveButtonElement().shouldBe(disabled);
        });
    }

    /**
     * Check that the 'Add' button for the specified products has the specified condition.
     *
     * @param condition the condition of 'Add' button to check
     * @param products  the products to check
     */
    private void checkAddButton(WebElementCondition condition, List<Product> products) {
        for (var product : products) {
            step("Check that the product item '" + product.name + "' has " + condition + " 'Add' button", () -> {
                productsPage.getProductItem(product).getAddButtonElement().shouldBe(condition);
            });
        }
    }
}
