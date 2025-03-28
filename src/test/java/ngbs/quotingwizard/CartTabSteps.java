package ngbs.quotingwizard;

import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.NGBSQuotingWizardPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.carttab.CartPage;
import com.aquiva.autotests.rc.page.opportunity.ngbsquotingwizard.producttab.ProductsPage;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.QuoteLineItem;
import io.qameta.allure.Step;

import java.math.BigDecimal;

import static base.Pages.cartPage;
import static base.Pages.deviceAssignmentPage;
import static com.aquiva.autotests.rc.utilities.NumberHelper.doubleToIntToString;
import static com.aquiva.autotests.rc.utilities.StringHelper.ZERO_PRICE;
import static com.codeborne.selenide.Condition.*;
import static io.qameta.allure.Allure.step;
import static java.math.RoundingMode.DOWN;
import static java.time.Duration.ofSeconds;

/**
 * Test methods for test cases related to {@link CartPage} of {@link NGBSQuotingWizardPage}.
 */
public class CartTabSteps {
    private final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final QuoteWizardSteps quoteWizardSteps;

    //  Test data
    private final String currencyPrefix;

    /**
     * New instance for the class with the test methods/steps related to {@link CartPage} of {@link NGBSQuotingWizardPage}.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public CartTabSteps(Dataset data) {
        this.data = data;
        this.enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        this.quoteWizardSteps = new QuoteWizardSteps(data);

        currencyPrefix = data.getCurrencyIsoCode() + " ";
    }

    /**
     * <p> 1. Open the Opportunity record page and switch to the Quote Wizard section.
     * <p> 2. Add a new Sales Quote from the Quote Selection landing page.
     * <p> 3. Select a Package from provided Test data on the Select Package tab of Quote Wizard (for New Business). </p>
     * <p> 4. Open {@link ProductsPage} and add products from test data (see {@link Dataset#getNewProductsToAdd()}).
     * <p> 5. Open {@link CartPage} and save changes.
     *
     * @param opportunityId ID of the Opportunity for which the Quote Wizard is open
     */
    public void prepareCartTab(String opportunityId) {
        quoteWizardSteps.openQuoteWizardOnOpportunityRecordPage(opportunityId);
        quoteWizardSteps.addNewSalesQuote();
        quoteWizardSteps.selectDefaultPackageFromTestData();
        quoteWizardSteps.addProductsOnProductsTab(data.getNewProductsToAdd());
        cartPage.openTab();
        cartPage.saveChanges();
    }

    /**
     * <p> 1. Open the Quote Wizard for the test Opportunity to create a new Sales Quote via direct VF page link.
     * <p> 2. Select a Package from provided Test data on the Select Package tab of the Quote Wizard (for New Business).
     * <p> 3. Open {@link ProductsPage} and add products from test data (see {@link Dataset#getNewProductsToAdd()}).
     * <p> 4. Open {@link CartPage} and save changes.
     *
     * @param opportunityId ID of the Opportunity for which the Quote Wizard is open
     */
    public void prepareCartTabViaQuoteWizardVfPage(String opportunityId) {
        quoteWizardSteps.openQuoteWizardForNewSalesQuoteDirect(opportunityId);
        quoteWizardSteps.selectDefaultPackageFromTestData();
        quoteWizardSteps.addProductsOnProductsTab(data.getNewProductsToAdd());
        cartPage.openTab();
        cartPage.saveChanges();
    }

    /**
     * Set 'Quantity'/'New Quantity' values on the Price tab for products using provided {@link Product} collection.
     * <p>
     * Method skips "fee" products and products with zero prices (old and new) - inputs are disabled for them anyway.
     * Exceptions are any products under "categories" like "Phones" (Rental Phones, Refurbished Phones, etc.)
     * and "DigitalLines" (e.g. "DigitalLine Unlimited", "Common Phone", except for parent "DigitalLine" and "Global MVP DigitalLine").
     * <p> Note: method works for both New Business and Existing Business opportunities. </p>
     *
     * @param products {@link Product} collection from test data.
     */
    @Step("Set Quantity in Cart with provided Products data")
    public void setUpQuantities(Product... products) {
        var zeroPrice = new BigDecimal(ZERO_PRICE);

        for (var currentProduct : products) {
            step("Setting up quantity for '" + currentProduct.name + "'", () -> {
                var currentProductPrice = new BigDecimal(currentProduct.price);

                var isNonZeroPriceNonFeeProduct = currentProductPrice.compareTo(zeroPrice) != 0 && !currentProduct.name.contains("Fee");
                var isPhone = currentProduct.group.contains("Phones");
                var isEditableDigitalLine = currentProduct.group.contains("Services") && !currentProduct.name.endsWith("DigitalLine");

                if (isNonZeroPriceNonFeeProduct || isPhone || isEditableDigitalLine) {
                    //  Existing Business products use different input field
                    if (data.getBillingId().isEmpty()) {
                        cartPage.setQuantityForQLItem(currentProduct.name, currentProduct.quantity);
                    } else {
                        cartPage.setNewQuantityForQLItem(currentProduct.name, currentProduct.quantity);
                    }
                }
            });
        }
    }

    /**
     * Set 'Discount' and 'Discount type' values on Price tab for products using provided {@link Product} collection.
     * <p> Note: method works for both New Business and Existing Business opportunities. </p>
     *
     * @param products {@link Product} collection from test data.
     */
    @Step("Set Discounts in Cart with provided Products data")
    public void setUpDiscounts(Product... products) {
        for (var currentProduct : products) {
            step("Setting up discount for '" + currentProduct.name + "'", () -> {
                if ((currentProduct.discount != 0 || currentProduct.newDiscount != null)) {
                    cartPage.setDiscountTypeForQLItem(currentProduct.name, currentProduct.discountType);

                    var discountToSet = currentProduct.newDiscount != null
                            ? currentProduct.newDiscount
                            : currentProduct.discount;
                    cartPage.setDiscountForQLItem(currentProduct.name, discountToSet);
                }
            });
        }
    }

    /**
     * Assign devices to digital lines.
     * <p>
     * Method links devices with DigitalLine products with the given quantity.
     * Every device can be assigned to one or more area codes.
     *
     * @param productToAssignName device's name to assign area code to
     * @param dlCartItemName      DL product name for device assignment
     *                            (e.g. DigitalLine Unlimited, Common Phone, DigitalLine Basic)
     * @param areaCode            Area Code entity with name of the country, state, city and the code
     * @param quantity            number of devices to assign for a given area code
     * @param isChangeAreaCode    true, if there's an existing area code on the device,
     *                            and it needs to be changed with a new one.
     */
    public void assignDevicesToDL(String productToAssignName, String dlCartItemName,
                                  AreaCode areaCode, int quantity, boolean isChangeAreaCode) {
        cartPage.getQliFromCartByDisplayName(dlCartItemName).getDeviceAssignmentButton()
                .scrollIntoView("{block: \"center\"}")
                .shouldBe(visible, ofSeconds(10))
                .click();
        deviceAssignmentPage.assignDevices(productToAssignName, areaCode, quantity, isChangeAreaCode);
    }

    /**
     * Assign devices to digital lines.
     * <p>
     * Method links devices with DigitalLine products with the given quantity.
     * Every device can be assigned to one or more area codes.
     * <p></p>
     * Note: this method only works if there's no existing area code assigned to the given device on the Digital Line!
     * If you need to change the existing area code on the device
     * use {@link #assignDevicesToDL(String, String, AreaCode, int, boolean)} instead.
     *
     * @param digitalLinePhoneAreaCode object representing the mapping between
     *                                 Digital Line, Phone, Area Code, and the number of assigned devices
     */
    public void assignDevicesToDL(DigitalLinePhoneAreaCode digitalLinePhoneAreaCode) {
        assignDevicesToDL(digitalLinePhoneAreaCode.phone.name, digitalLinePhoneAreaCode.digitalLine.name,
                digitalLinePhoneAreaCode.areaCode, digitalLinePhoneAreaCode.quantity, false);
    }

    /**
     * Assign devices to digital lines.
     * <p>
     * Method links devices with DigitalLine products with the given quantity.
     * Every device can be assigned to one or more area codes.
     * <p></p>
     * Note: this method only works if there's no existing area code assigned to the given device on the Digital Line!
     * If you need to change the existing area code on the device
     * use {@link #assignDevicesToDL(String, String, AreaCode, int, boolean)} instead.
     *
     * @param productToAssignName device's name to assign area code to
     * @param dlCartItemName      DL product name for device assignment
     *                            (e.g. DigitalLine Unlimited, Common Phone, DigitalLine Basic)
     * @param areaCode            Area Code entity with name of the country, state, city and the code
     * @param quantity            number of devices to assign for a given area code
     */
    public void assignDevicesToDL(String productToAssignName, String dlCartItemName,
                                  AreaCode areaCode, int quantity) {
        assignDevicesToDL(productToAssignName, dlCartItemName, areaCode, quantity, false);
    }

    /**
     * Assign devices to digital lines without changing existing area code.
     * <p>
     * Method links devices with DigitalLine products with the given quantity.
     * Note: this method only works if there's existing area code assigned to the given device on the Digital Line
     * and there's no need to change it.
     *
     * @param productToAssignName device's name to assign to DigitalLine
     * @param dlCartItemName      DL product name for device assignment
     *                            (e.g. DigitalLine Unlimited, Common Phone, DigitalLine Basic)
     * @param quantity            number of devices to assign to DigitalLine
     */
    public void assignDevicesToDLWithoutSettingAreaCode(String productToAssignName, String dlCartItemName, int quantity) {
        cartPage.getQliFromCartByDisplayName(dlCartItemName).getDeviceAssignmentButton()
                .shouldBe(visible, ofSeconds(10))
                .click();
        deviceAssignmentPage.assignDevicesWithoutSettingAreaCode(productToAssignName, quantity);
    }

    /**
     * Assign devices to digital lines and save changes in the Cart after that.
     * <p>
     * Method links devices with DigitalLine products with the given quantity.
     * Every device can be assigned to one or more area codes.
     * <p></p>
     * Note: this method only works if there's no existing area code assigned to the given device on the Digital Line!
     * If you need to change the existing area code on the device
     * use {@link #assignDevicesToDL(String, String, AreaCode, int, boolean)} instead.
     *
     * @param productToAssignName device's name to assign area code to
     * @param dlCartItemName      DL product name for device assignment
     *                            (e.g. DigitalLine Unlimited, Common Phone, DigitalLine Basic)
     * @param areaCode            Area Code entity with name of the country, state, city and the code
     * @param quantity            number of devices to assign for a given area code
     */
    public void assignDevicesToDLAndSave(String productToAssignName, String dlCartItemName,
                                         AreaCode areaCode, int quantity) {
        assignDevicesToDL(productToAssignName, dlCartItemName, areaCode, quantity, false);
        cartPage.saveChanges();
    }

    /**
     * Check that EffectivePriceNew__c, Discount_number__c and Discount_type__c QuoteLineItem in DB
     * are the same as 'Your Price', 'Discount', and 'Discount Type' actual fields on the cart item in QW
     * using the corresponding product's test data.
     *
     * @param products       expected test data for products under test.
     * @param currencyPrefix string value for currency ISO code and empty prefix (e.g. "USD + " ", "CAD + " " etc...).
     */
    public void checkCartItemsAgainstQuoteLineItemsTestSteps(String currencyPrefix, Product... products) {
        var quoteId = cartPage.getSelectedQuoteId();
        for (var product : products) {
            step("Check Price and Discount values in QW and SFDC for '" + product.name + "'", () -> {
                var currentCartItem = cartPage.getQliFromCartByDisplayName(product.name);
                currentCartItem.getCartItemElement().shouldBe(visible);

                var qli = enterpriseConnectionUtils.querySingleRecord(
                        "SELECT Id, EffectivePriceNew__c, Discount_type__c, Discount_number__c " +
                                "FROM QuoteLineItem " +
                                "WHERE QuoteId = '" + quoteId + "' " +
                                "AND Product2.ExtID__c = '" + product.dataName + "'",
                        QuoteLineItem.class);

                var expectedPrice = BigDecimal.valueOf(qli.getEffectivePriceNew__c()).setScale(2, DOWN);
                currentCartItem.getYourPrice().shouldHave(exactText(currencyPrefix + expectedPrice));

                var expectedDiscount = doubleToIntToString(qli.getDiscount_number__c());
                currentCartItem.getDiscountInput().shouldHave(exactValue(expectedDiscount));

                var expectedDiscountType = qli.getDiscount_type__c();
                currentCartItem.getDiscountTypeSelect().shouldHave(value(expectedDiscountType));
            });
        }
    }

    /**
     * Check 'Discount' and 'Discount Type' fields values for products on the Price tab with provided test data.
     *
     * @param products test data with all the Products' data
     */
    public void checkDiscountsInCartExistingBusiness(Product... products) {
        for (var product : products) {
            var currentCartItem = cartPage.getQliFromCartByDisplayName(product.name);
            currentCartItem.getDiscountInput().shouldHave(exactValue(product.discount.toString()));
            currentCartItem.getDiscountTypeSelect().getSelectedOption()
                    .shouldHave(exactTextCaseSensitive(product.discountType));
        }
    }

    /**
     * Check actual fields' values for visible products with provided test data.
     * <p>
     * Note: Method is suitable for checking products from <b>New Business accounts</b>.
     *
     * @param products test data with all the Products' data
     */
    public void checkProductsInCartNewBusiness(Product... products) {
        for (var product : products) {
            checkNewCartItemAgainstData(product);
        }
    }

    /**
     * Check actual fields' values for visible products with provided test data.
     * <p>
     * Note: Method is suitable for checking products from <b>Existing Business accounts</b>.
     *
     * @param products test data with all the Products' data
     */
    public void checkProductsInCartExistingBusiness(Product... products) {
        for (var product : products) {
            checkExistingCartItemAgainstData(product);
        }
    }

    /**
     * Check that fields on {@link CartPage} for current new product populated correctly.
     *
     * @param dataProduct product to check from test data.
     */
    @Step("Check new item in the cart using provided data object")
    public void checkNewCartItemAgainstData(Product dataProduct) {
        var currentCartItem = cartPage.getQliFromCartByDisplayName(dataProduct.name);
        currentCartItem.getCartItemElement().shouldBe(visible);

        currentCartItem.getChargeTerm().shouldHave(text(dataProduct.chargeTerm));
        currentCartItem.getQuantityInput().shouldHave(exactValue(dataProduct.quantity.toString()));

        currentCartItem.getListPrice()
                .shouldHave(exactTextCaseSensitive(currencyPrefix + dataProduct.price));
        currentCartItem.getYourPrice()
                .shouldHave(exactTextCaseSensitive(currencyPrefix + dataProduct.yourPrice));
        currentCartItem.getDiscountInput().shouldHave(exactValue(dataProduct.discount.toString()));
        currentCartItem.getDiscountTypeSelect().getSelectedOption()
                .shouldHave(exactTextCaseSensitive(dataProduct.discountType));
    }

    /**
     * Check that fields on {@link CartPage} for current existing product populated correctly.
     * (All discounts should present)
     *
     * @param dataProduct product to check from test data.
     */
    @Step("Check existing item (from billing account) in the cart using provided data object")
    public void checkExistingCartItemAgainstData(Product dataProduct) {
        var currentCartItem = cartPage.getQliFromCartByDisplayName(dataProduct.name);
        currentCartItem.getCartItemElement().shouldBe(visible);

        currentCartItem.getChargeTerm().shouldHave(text(dataProduct.chargeTerm));
        currentCartItem.getNewQuantityInput().shouldHave(exactValue(dataProduct.quantity.toString()));

        if (dataProduct.existingQuantity != null) {
            currentCartItem.getExistingQuantityInput().shouldHave(exactValue(dataProduct.existingQuantity.toString()));
        }

        currentCartItem.getListPrice()
                .shouldHave(exactTextCaseSensitive(currencyPrefix + dataProduct.price));
        currentCartItem.getYourPrice()
                .shouldHave(exactTextCaseSensitive(currencyPrefix + dataProduct.yourPrice));
        currentCartItem.getDiscountInput().shouldHave(exactValue(dataProduct.discount.toString()));
        currentCartItem.getDiscountTypeSelect().getSelectedOption()
                .shouldHave(exactTextCaseSensitive(dataProduct.discountType));
    }
}
