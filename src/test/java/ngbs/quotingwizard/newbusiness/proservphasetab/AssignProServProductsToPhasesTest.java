package ngbs.quotingwizard.newbusiness.proservphasetab;

import base.BaseTest;
import base.Steps;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.JsonUtils;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.User;
import io.qameta.allure.Description;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static base.Pages.*;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.psphasestab.PhaseLineItem.TOTAL_QTY_FORMAT;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.psphasestab.PsCartItem.QTY_FORMAT;
import static com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.psphasestab.PsPhaseAddProductModal.DEFAULT_PRODUCT_QUANTITY;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectutils.UserUtils.*;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.lang.String.valueOf;

@Tag("P0")
@Tag("LTR-121")
@Tag("ProServInNGBS")
public class AssignProServProductsToPhasesTest extends BaseTest {
    private final Steps steps;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;

    private User salesRepUserWithProServInNgbsFT;
    private String quoteId;

    //  Test data
    private final Map<String, Package> packageFolderNameToPackageMap;

    private final Product productThree;
    private final Product productOne;
    private final Product productTwo;

    private final int productOneQtyMax;
    private final int productOneQtyAboveMax;
    private final int productTwoQtyMax;
    private final int productTwoQtyBelowMax;
    private final int productTwoQtyIncrement;
    private final int productTwoQtyRemaining;

    private final int phaseOneIndex;
    private final int phaseTwoIndex;

    public AssignProServProductsToPhasesTest() {
        var data = JsonUtils.readConfigurationResource(
                "data/ngbs/newbusiness/RC_MVP_ED_EV_CC_ProServ_Monthly_Contract.json",
                Dataset.class);
        steps = new Steps(data);
        enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();

        packageFolderNameToPackageMap = Map.of(
                data.packageFolders[0].name, data.packageFolders[0].packages[0],
                data.packageFolders[4].name, data.packageFolders[4].packages[0]
        );

        var proServPackage = data.packageFolders[4].packages[0];
        productOne = data.getProductByDataName("PS_UCOT_1431005", proServPackage);
        productTwo = data.getProductByDataName("PS_CCOT_1463005", proServPackage);
        productThree = data.getProductByDataName("PS_UCOT_1424005", proServPackage);

        productOneQtyMax = 10;
        productOneQtyAboveMax = productOneQtyMax + 1;
        productTwoQtyMax = 15;
        productTwoQtyBelowMax = 7;
        productTwoQtyIncrement = 1;
        productTwoQtyRemaining = productTwoQtyMax - productTwoQtyBelowMax - productTwoQtyIncrement;

        productOne.quantity = productOneQtyMax;
        productTwo.quantity = productTwoQtyMax;

        phaseOneIndex = 0;
        phaseTwoIndex = 1;
    }

    @BeforeEach
    public void setUpTest() {
        step("Find a user with 'Sales Rep - Lightning' profile and 'ProServ in NGBS' feature toggle", () -> {
            salesRepUserWithProServInNgbsFT = getUser()
                    .withProfile(SALES_REP_LIGHTNING_PROFILE)
                    .withFeatureToggles(List.of(PROSERV_IN_NGBS_FT))
                    .execute();
        });

        steps.salesFlow.createAccountWithContactAndContactRole(salesRepUserWithProServInNgbsFT);
        steps.quoteWizard.createOpportunity(steps.salesFlow.account, steps.salesFlow.contact, salesRepUserWithProServInNgbsFT);

        step("Log in as a user with 'Sales Rep - Lightning' profile, 'ProServ in NGBS' feature toggle, " +
                "and 'EnableSuperUserProServ In UQT' permission set", () -> {
            steps.sfdc.initLoginToSfdcAsTestUser(salesRepUserWithProServInNgbsFT);
        });
    }

    @Test
    @TmsLink("CRM-36626")
    @TmsLink("CRM-36639")
    @DisplayName("CRM-36626 - Assigning ProServ licenses to Phases via Add Product button. \n" +
            "CRM-36639 - Assigning ProServ licenses to Phases via 'Move all unassigned products here' button")
    @Description("CRM-36626 - Verify that: \n" +
            "- Professional Services licenses and their specified quantities can be assigned to Phases, " +
            "and only unassigned ProServ licenses can be added to the Phase; \n" +
            "- 'Add Product' modal automatically displays a preselected item from the remaining unassigned products; \n" +
            "- 'Add Product' button is disabled if all products in the Cart Items section have been assigned to Phases.\n\n" +
            "CRM-36639 - Verify that: \n" +
            "- Professional Services licenses can be assigned to the Phase using 'Move all unassigned products here' button; \n" +
            "- 'Move all unassigned products here' button is disabled if all products in the Cart Items section have been assigned to Phases.")
    public void test() {
        step("1. Open the Quote Wizard for the New Business Opportunity to add a new Sales Quote, " +
                "select MVP and Professional Services packages, and save changes", () -> {
            steps.quoteWizard.prepareOpportunityForMultiProduct(steps.quoteWizard.opportunity.getId(), packageFolderNameToPackageMap);

            quoteId = wizardPage.getSelectedQuoteId();
        });

        step("2. Re-login as a user with profile = 'Professional Services Lightning' and 'ProServ in NGBS' feature toggle, " +
                "transfer the ownership of the Account, Contact, and Opportunity to this user, " +
                "and open the created Quote in the Quote Wizard again", () -> {
            var proServUser = getUser()
                    .withProfile(PROFESSIONAL_SERVICES_LIGHTNING_PROFILE)
                    .withFeatureToggles(List.of(PROSERV_IN_NGBS_FT))
                    .execute();

            steps.salesFlow.account.setOwnerId(proServUser.getId());
            steps.salesFlow.contact.setOwnerId(proServUser.getId());
            steps.quoteWizard.opportunity.setOwnerId(proServUser.getId());
            enterpriseConnectionUtils.update(steps.salesFlow.account, steps.salesFlow.contact, steps.quoteWizard.opportunity);

            steps.sfdc.reLoginAsUser(proServUser);
            wizardPage.openPage(steps.quoteWizard.opportunity.getId(), quoteId);
        });

        //  From here, all steps and checks for CRM-36626
        step("3. Add two ProServ products on the Add Products tab, " +
                "open the Price tab, and increase the added products' quantities", () -> {
            steps.quoteWizard.addProductsOnProductsTab(productOne, productTwo);

            cartPage.openTab();
            steps.cartTab.setUpQuantities(productOne, productTwo);
        });

        step("4. Open the PS Phases tab, add a new Phase, click 'Add Product' button for this Phase, " +
                "and check the 'Add Product' modal window's contents", () -> {
            psPhasesPage.openTab();
            psPhasesPage.addPhaseButton.click();
            psPhasesPage.getPhaseByIndex(phaseOneIndex).getSelf().shouldBe(visible);

            psPhasesPage.getPhaseByIndex(phaseOneIndex).getAddProductButton().click();

            psPhasesPage.addProductModal.quantityInput.shouldHave(exactValue(DEFAULT_PRODUCT_QUANTITY));
            psPhasesPage.addProductModal.productNamePicklist.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(productOne.name));
            psPhasesPage.addProductModal.productNamePicklist.getOptions()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(productOne.name, productTwo.name));
        });

        step("5. Select Product Name = '" + productOne.name + "', " +
                "set Quantity = " + productOneQtyAboveMax + ", " +
                "and check the Quantity input field", () -> {
            psPhasesPage.addProductModal.productNamePicklist.selectOption(productOne.name);
            psPhasesPage.addProductModal.quantityInput
                    .setValue(valueOf(productOneQtyAboveMax))
                    .unfocus();

            psPhasesPage.addProductModal.quantityInput.shouldHave(exactValue(valueOf(productOneQtyMax)));
        });

        step("6. Click 'Apply' button, and check the added Phase Line Item for the Phase", () -> {
            psPhasesPage.addProductModal.applyButton.click();

            var productOneItem = psPhasesPage.getPhaseByIndex(phaseOneIndex).getPhaseLineItem(productOne.name);
            productOneItem.getSelf().shouldBe(visible);
            productOneItem.getQuantityInput().shouldHave(exactValue(valueOf(productOneQtyMax)));
            productOneItem.getTotalQuantity().shouldHave(exactText(format(TOTAL_QTY_FORMAT, productOneQtyMax)));
        });

        step("7. Click 'Add Product' button for the Phase, " +
                "and check the 'Add Product' modal window's contents", () -> {
            psPhasesPage.getPhaseByIndex(phaseOneIndex).getAddProductButton().click();

            psPhasesPage.addProductModal.quantityInput.shouldHave(exactValue(DEFAULT_PRODUCT_QUANTITY));
            psPhasesPage.addProductModal.productNamePicklist.getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(productTwo.name));
            psPhasesPage.addProductModal.productNamePicklist.getOptions()
                    .shouldHave(exactTextsCaseSensitiveInAnyOrder(productTwo.name));
        });

        step("8. Select Product Name = '" + productTwo.name + "', " +
                "set Quantity = " + productTwoQtyBelowMax + ", " +
                "click 'Apply' button, and check the added Phase Line Item for the Phase", () -> {
            psPhasesPage.addProductModal.productNamePicklist.selectOption(productTwo.name);
            psPhasesPage.addProductModal.quantityInput.setValue(valueOf(productTwoQtyBelowMax));
            psPhasesPage.addProductModal.applyButton.click();

            var productTwoItem = psPhasesPage.getPhaseByIndex(phaseOneIndex).getPhaseLineItem(productTwo.name);
            productTwoItem.getSelf().shouldBe(visible);
            productTwoItem.getQuantityInput().shouldHave(exactValue(valueOf(productTwoQtyBelowMax)));
            productTwoItem.getTotalQuantity().shouldHave(exactText(format(TOTAL_QTY_FORMAT, productTwoQtyMax)));
        });

        step("9. Click 'Add Product' button for the Phase, " +
                "Select Product Name = '" + productTwo.name + "', " +
                "set Quantity = " + productTwoQtyIncrement + ", " +
                "click 'Apply' button, and check the updated Phase Line Item for the Phase", () -> {
            psPhasesPage.getPhaseByIndex(phaseOneIndex).getAddProductButton().click();
            psPhasesPage.addProductModal.productNamePicklist.selectOption(productTwo.name);
            psPhasesPage.addProductModal.quantityInput.setValue(valueOf(productTwoQtyIncrement));
            psPhasesPage.addProductModal.applyButton.click();

            //  To make sure that the previously added product does NOT add additional Phase Line Items
            psPhasesPage.getPhaseByIndex(phaseOneIndex).getAllPhaseLineItems().shouldHave(size(2));

            var productTwoItem = psPhasesPage.getPhaseByIndex(phaseOneIndex).getPhaseLineItem(productTwo.name);
            productTwoItem.getSelf().shouldBe(visible);
            productTwoItem.getQuantityInput().shouldHave(exactValue(valueOf(productTwoQtyBelowMax + productTwoQtyIncrement)));
            productTwoItem.getTotalQuantity().shouldHave(exactText(format(TOTAL_QTY_FORMAT, productTwoQtyMax)));
        });

        step("10. Add a new Phase, click 'Add Product' button for this new Phase, " +
                "select Product Name = '" + productTwo.name + "', " +
                "set Quantity = " + productTwoQtyMax + ", " +
                "and check the Quantity input field", () -> {
            psPhasesPage.addPhaseButton.click();
            psPhasesPage.getPhaseByIndex(phaseTwoIndex).getSelf().shouldBe(visible);

            psPhasesPage.getPhaseByIndex(phaseTwoIndex).getAddProductButton().click();

            psPhasesPage.addProductModal.productNamePicklist.selectOption(productTwo.name);
            psPhasesPage.addProductModal.quantityInput
                    .setValue(valueOf(productTwoQtyMax))
                    .unfocus();

            //  Quantity is reset to the max number of "unassigned" products
            psPhasesPage.addProductModal.quantityInput.shouldHave(exactValue(valueOf(productTwoQtyRemaining)));
        });

        step("11. Click 'Apply' button, check the added Phase Line Item for the new Phase, " +
                "and the 'Add Product' buttons for all the added phases", () -> {
            psPhasesPage.addProductModal.applyButton.click();

            var productTwoItemForPhaseTwo = psPhasesPage.getPhaseByIndex(phaseTwoIndex).getPhaseLineItem(productTwo.name);
            productTwoItemForPhaseTwo.getSelf().shouldBe(visible);
            productTwoItemForPhaseTwo.getQuantityInput().shouldHave(exactValue(valueOf(productTwoQtyRemaining)));
            productTwoItemForPhaseTwo.getTotalQuantity().shouldHave(exactText(format(TOTAL_QTY_FORMAT, productTwoQtyMax)));

            //  Because all the available products (all quantities) are assigned to the phases 
            psPhasesPage.getPhaseByIndex(phaseOneIndex).getAddProductButton().shouldBe(disabled);
            psPhasesPage.getPhaseByIndex(phaseTwoIndex).getAddProductButton().shouldBe(disabled);
        });

        step("12. Add one more ProServ product on the Add Products tab, " +
                "open the PS Phases tab, check the Cart Items section " +
                "and the 'Add Product' buttons for all the added phases", () -> {
            steps.quoteWizard.addProductsOnProductsTab(productThree);

            psPhasesPage.openTab();

            psPhasesPage.getPsCartItem(productThree.name).getSelf().shouldBe(visible);
            psPhasesPage.getPhaseByIndex(phaseOneIndex).getAddProductButton().shouldBe(enabled);
            psPhasesPage.getPhaseByIndex(phaseTwoIndex).getAddProductButton().shouldBe(enabled);
        });

        //  Reset the state before the next test, CRM-36639
        step("13. Delete all the created Phases by clicking on the 'delete' buttons for them, " +
                "open the Price tab, and remove the product = " + productThree.name, () -> {
            psPhasesPage.getPhaseByIndex(phaseTwoIndex).getDeleteButton().click();
            psPhasesPage.getPhaseByIndex(phaseOneIndex).getDeleteButton().click();

            cartPage.openTab();
            cartPage.getQliFromCartByDisplayName(productThree.name).getDeleteButton().click();
        });

        //  From here, all steps and checks for CRM-36639
        step("14. Open the PS Phases tab, add a new Phase, click 'Move all unassigned items here', " +
                "check that the Phase contains all the available ProServ cart items, " +
                "Cart Items section is empty, and 'Move all unassigned items here' button is disabled", () -> {
            psPhasesPage.openTab();
            psPhasesPage.addPhaseButton.click();
            psPhasesPage.getPhaseByIndex(phaseOneIndex).getMoveAllUnassignedItemsHereButton().click();

            psPhasesPage.getPhaseByIndex(phaseOneIndex).getAllPhaseLineItems().shouldHave(size(2));

            var productOneItem = psPhasesPage.getPhaseByIndex(phaseOneIndex).getPhaseLineItem(productOne.name);
            productOneItem.getSelf().shouldBe(visible);
            productOneItem.getQuantityInput().shouldHave(exactValue(valueOf(productOneQtyMax)));
            productOneItem.getTotalQuantity().shouldHave(exactText(format(TOTAL_QTY_FORMAT, productOneQtyMax)));

            var productTwoItem = psPhasesPage.getPhaseByIndex(phaseOneIndex).getPhaseLineItem(productTwo.name);
            productTwoItem.getSelf().shouldBe(visible);
            productTwoItem.getQuantityInput().shouldHave(exactValue(valueOf(productTwoQtyMax)));
            productTwoItem.getTotalQuantity().shouldHave(exactText(format(TOTAL_QTY_FORMAT, productTwoQtyMax)));

            psPhasesPage.allCartItems.shouldHave(size(0));

            psPhasesPage.getPhaseByIndex(phaseOneIndex).getMoveAllUnassignedItemsHereButton().shouldBe(disabled);
        });

        step("15. Remove the Product = " + productOne.name + " from the Phase, " +
                "and check the contents of the Phase and the Cart Items section", () -> {
            psPhasesPage.getPhaseByIndex(phaseOneIndex).getPhaseLineItem(productOne.name)
                    .getRemoveButton().click();

            psPhasesPage.getPhaseByIndex(phaseOneIndex).getAllPhaseLineItems().shouldHave(size(1));
            psPhasesPage.getPhaseByIndex(phaseOneIndex).getPhaseLineItem(productOne.name).getSelf().shouldNot(exist);

            psPhasesPage.allCartItems.shouldHave(size(1));
            var productOneCartItem = psPhasesPage.getPsCartItem(productOne.name);
            productOneCartItem.getSelf().shouldBe(visible);
            productOneCartItem.getQuantity().shouldHave(exactText(format(QTY_FORMAT, productOneQtyMax, productOneQtyMax)));

            psPhasesPage.getPhaseByIndex(phaseOneIndex).getMoveAllUnassignedItemsHereButton().shouldBe(enabled);
        });

        step("16. Add a new Phase, click 'Move all unassigned items here' button for it, " +
                "and check the contents of the Phase and the Cart Items section", () -> {
            psPhasesPage.addPhaseButton.click();
            psPhasesPage.getPhaseByIndex(phaseTwoIndex).getMoveAllUnassignedItemsHereButton().click();

            psPhasesPage.getPhaseByIndex(phaseOneIndex).getAllPhaseLineItems().shouldHave(size(1));
            psPhasesPage.getPhaseByIndex(phaseTwoIndex).getAllPhaseLineItems().shouldHave(size(1));

            var productOneItemForPhaseTwo = psPhasesPage.getPhaseByIndex(phaseTwoIndex).getPhaseLineItem(productOne.name);
            productOneItemForPhaseTwo.getSelf().shouldBe(visible);
            productOneItemForPhaseTwo.getQuantityInput().shouldHave(exactValue(valueOf(productOneQtyMax)));
            productOneItemForPhaseTwo.getTotalQuantity().shouldHave(exactText(format(TOTAL_QTY_FORMAT, productOneQtyMax)));

            psPhasesPage.allCartItems.shouldHave(size(0));

            //  Because all the available products (all quantities) are assigned to the phases
            psPhasesPage.getPhaseByIndex(phaseOneIndex).getMoveAllUnassignedItemsHereButton().shouldBe(disabled);
            psPhasesPage.getPhaseByIndex(phaseTwoIndex).getMoveAllUnassignedItemsHereButton().shouldBe(disabled);
        });

        step("17. Set the new quantity for the Product = " + productTwo.name + " in the 1st Phase, " +
                "and check the contents of the Phases and the Cart Items section", () -> {
            var productTwoPhaseLineItem = psPhasesPage.getPhaseByIndex(phaseOneIndex).getPhaseLineItem(productTwo.name);
            productTwoPhaseLineItem.getQuantityInput().setValue(valueOf(productTwoQtyBelowMax + productTwoQtyIncrement));

            productTwoPhaseLineItem.getQuantityInput().shouldHave(exactValue(valueOf(productTwoQtyBelowMax + productTwoQtyIncrement)));
            productTwoPhaseLineItem.getTotalQuantity().shouldHave(exactText(format(TOTAL_QTY_FORMAT, productTwoQtyMax)));

            var productTwoCartItem = psPhasesPage.getPsCartItem(productTwo.name);
            productTwoCartItem.getSelf().shouldBe(visible);
            productTwoCartItem.getQuantity().shouldHave(exactText(format(QTY_FORMAT, productTwoQtyRemaining, productTwoQtyMax)));

            psPhasesPage.getPhaseByIndex(phaseOneIndex).getMoveAllUnassignedItemsHereButton().shouldBe(enabled);
            psPhasesPage.getPhaseByIndex(phaseTwoIndex).getMoveAllUnassignedItemsHereButton().shouldBe(enabled);
        });

        step("18. Click 'Move all unassigned items here' on the 1st Phase, " +
                "and check the contents of the Phases and the Cart Items section", () -> {
            psPhasesPage.getPhaseByIndex(phaseOneIndex).getMoveAllUnassignedItemsHereButton().click();

            psPhasesPage.getPhaseByIndex(phaseOneIndex).getAllPhaseLineItems().shouldHave(size(1));

            var productTwoItem = psPhasesPage.getPhaseByIndex(phaseOneIndex).getPhaseLineItem(productTwo.name);
            productTwoItem.getSelf().shouldBe(visible);
            productTwoItem.getQuantityInput().shouldHave(exactValue(valueOf(productTwoQtyMax)));
            productTwoItem.getTotalQuantity().shouldHave(exactText(format(TOTAL_QTY_FORMAT, productTwoQtyMax)));

            psPhasesPage.allCartItems.shouldHave(size(0));

            //  Because all the available products (all quantities) are assigned to the phases
            psPhasesPage.getPhaseByIndex(phaseOneIndex).getMoveAllUnassignedItemsHereButton().shouldBe(disabled);
            psPhasesPage.getPhaseByIndex(phaseTwoIndex).getMoveAllUnassignedItemsHereButton().shouldBe(disabled);
        });

        step("19. Add one more ProServ product on the Add Products tab, " +
                "open the PS Phases tab, check the Cart Items section " +
                "and the 'Move all unassigned items here' buttons for all the added phases", () -> {
            steps.quoteWizard.addProductsOnProductsTab(productThree);

            psPhasesPage.openTab();

            psPhasesPage.getPsCartItem(productThree.name).getSelf().shouldBe(visible);
            psPhasesPage.getPhaseByIndex(phaseOneIndex).getMoveAllUnassignedItemsHereButton().shouldBe(enabled);
            psPhasesPage.getPhaseByIndex(phaseTwoIndex).getMoveAllUnassignedItemsHereButton().shouldBe(enabled);
        });
    }
}
