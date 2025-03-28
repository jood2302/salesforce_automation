package com.aquiva.autotests.rc.model.ngbs.testdata;

import com.aquiva.autotests.rc.model.DataModel;
import com.aquiva.autotests.rc.utilities.JsonUtils;

import java.util.*;

import static com.aquiva.autotests.rc.utilities.StringHelper.EMPTY_STRING;
import static com.aquiva.autotests.rc.utilities.StringHelper.USD_CURRENCY_ISO_CODE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.RC_US_BUSINESS_IDENTITY_NAME;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.AccountHelper.US_BILLING_COUNTRY;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.RC_US_BRAND_NAME;
import static java.util.Arrays.asList;

/**
 * Main test data object for all the tests.
 * <p></p>
 * Normally, test data gets loaded from JSON files via {@link JsonUtils} into the objects of this type.
 * After that, actual test interacts with this object.
 * <p></p>
 * This object contains a structure for upper-level data (brand, currency, charge term;
 * IDs from NGBS for Existing business accounts; AGS scenario for dynamic accounts generation;
 * Package data for different test packages; etc...).
 */
public class Dataset extends DataModel {
    public String description;
    public String comment;
    public String billingId;
    public String packageId;
    public String rcUserId;
    public String chargeTerm;
    public String currencyISOCode;
    public String billingCountry;
    public String brandName;
    public String subBrandName;
    public BusinessIdentity businessIdentity;

    public int engageUsers;
    public String forecastedUsers;

    public String scenario;

    public PackageFolder[] packageFolders;
    public PackageFolder[] packageFoldersUpgrade;
    public PackageFolder[] packageFoldersUpgradeChargeTerm;

    /**
     * Products that are added in the Quote tool (the Price tab).
     * This variable is defined in its getter with default value
     * and can be overridden in the test classes via its setter, if necessary.
     */
    private Product[] newProductsToAdd;

    /**
     * Return the products to add during the quote creation in the Quote tool.
     * See {@link #newProductsToAdd}.
     */
    public Product[] getNewProductsToAdd() {
        return newProductsToAdd != null
                ? newProductsToAdd
                : packageFolders[0].packages[0].products;
    }

    /**
     * Set specific products for {@link #newProductsToAdd}
     * that are different from the default ones.
     */
    public void setNewProductsToAdd(Product[] newProductsToAdd) {
        this.newProductsToAdd = newProductsToAdd;
    }

    /**
     * Return the products that exist on the Existing Business account in the NGBS/Billing.
     */
    public Product[] getProductsFromBilling() {
        return packageFolders[0].packages[0].productsFromBilling;
    }

    /**
     * Return the products that are added by default during the Quote creation.
     */
    public Product[] getProductsDefault() {
        return packageFolders[0].packages[0].productsDefault;
    }

    /**
     * Return some "other" products, without any specific category.
     */
    public Product[] getProductsOther() {
        return packageFolders[0].packages[0].productsOther;
    }

    /**
     * Get account's numeric ID from Billing from test data.
     * <p>
     * Method is null-safe: if billingId is not present in test data, then return <b>empty string</b>.
     *
     * @return string value for account's Billing ID. (e.g. "5221409002")
     */
    public String getBillingId() {
        return Objects.requireNonNullElse(billingId, EMPTY_STRING);
    }

    /**
     * Get ISO code for current currency from test data.
     * <p>
     * Method is null-safe: if currencyISOCode is not present in test data,
     * then return <b>default ISO code</b>.
     *
     * @return string value for currency ISO code (e.g. "USD", "CAD", "EUR", etc...)
     */
    public String getCurrencyIsoCode() {
        return Objects.requireNonNullElse(currencyISOCode, USD_CURRENCY_ISO_CODE);
    }

    /**
     * Get the billing country from test data.
     * <p>
     * Method is null-safe: if billing country is not present in test data,
     * then return <b>default billing country</b>.
     *
     * @return string value for billing country (e.g. "United States", "Canada", etc...)
     */
    public String getBillingCountry() {
        return Objects.requireNonNullElse(billingCountry, US_BILLING_COUNTRY);
    }

    /**
     * Get brand name from test data.
     * <p>
     * Method is null-safe: if brandName is not present in test data,
     * then return <b>default brand name</b>.
     *
     * @return string value for brand name (e.g. "RingCentral", "RingCentral Canada", etc...)
     */
    public String getBrandName() {
        return Objects.requireNonNullElse(brandName, RC_US_BRAND_NAME);
    }

    /**
     * Get business identity name from test data.
     * <p>
     * Method is null-safe: if businessIdentityName is not present in test data,
     * then return <b>default business identity name</b>.
     *
     * @return string value for brand name (e.g. "RingCentral Inc.", "RingCentral UK Ltd.", etc...)
     */
    public String getBusinessIdentityName() {
        return Objects.requireNonNullElse(businessIdentity.name, RC_US_BUSINESS_IDENTITY_NAME);
    }

    /**
     * Get the Initial Term for the default package.
     *
     * @return first item from the {@code packageFolders[0].packages[0].contractTerms.initialTerm[0]},
     * or {@code null} if there's no contractTerms for the default package.
     */
    public String getInitialTerm() {
        var contractTerms = packageFolders[0].packages[0].contractTerms;
        return contractTerms != null && contractTerms.initialTerm != null
                ? packageFolders[0].packages[0].contractTerms.initialTerm[0]
                : null;
    }

    /**
     * Get {@link Product} representation from test data.
     * <p></p>
     * Please, note! Method only searches for a product information inside these blocks:
     * <p> - data.packageFolders[0].packages[0].products
     * (see value for {@link #getNewProductsToAdd()}) </p>
     * <p> - data.packageFolders[0].packages[0].productsFromBilling
     * (see value for {@link #getProductsFromBilling()}) </p>
     * <p> - data.packageFolders[0].packages[0].productsDefault
     * (see value for {@link #getProductsDefault()} ()}) </p>
     * <p> - data.packageFolders[0].packages[0].productsOther
     * (see value for {@link #getProductsOther()}) </p>
     *
     * @param dataName code name for the product (e.g. "LC_DL_75", "LC_DL-UNL_50"...)
     * @return {@link Product} entity with related data to it (name, price, quantity...)
     * @throws IllegalArgumentException if there's no product data with provided dataName in test data
     */
    public Product getProductByDataName(String dataName) {
        return getProductByDataName(dataName, asList(
                getNewProductsToAdd(),
                getProductsFromBilling(),
                getProductsDefault(),
                getProductsOther())
        );
    }

    /**
     * Get {@link Product} representation from test data.
     * <p></p>
     * Please, note! Method only searches for a product information inside these blocks:
     * <p> - data.packageFoldersUpgrade[0].packages[0].products </p>
     * <p> - data.packageFoldersUpgrade[0].packages[0].productsFromBilling </p>
     *
     * @param dataName code name for the product (e.g. "LC_DL_75", "LC_DL-UNL_50"...)
     * @return {@link Product} entity with related data to it (name, price, quantity...)
     * @throws IllegalArgumentException if there's no product data with provided dataName in test data
     */
    public Product getProductByDataNameFromUpgradeData(String dataName) {
        return getProductByDataName(dataName, asList(
                packageFoldersUpgrade[0].packages[0].products,
                packageFoldersUpgrade[0].packages[0].productsFromBilling)
        );
    }

    /**
     * Get {@link Product} representation from one of the {@link Package} in the test data.
     * <br/>
     * Please, note! Method searches for a product information inside these blocks:
     * <p> - package.products </p>
     * <p> - package.productsFromBilling </p>
     * <p> - package.productsDefault </p>
     * <p> - package.productsOther </p>
     *
     * @param dataName            code name for the product (e.g. "LC_DL_75", "LC_DL-UNL_50"...)
     * @param packageWithProducts any package in the test data that contains products collections
     * @return {@link Product} entity with related data to it (name, price, quantity...)
     * @throws IllegalArgumentException if there's no product data with provided dataName in test data
     */
    public Product getProductByDataName(String dataName, Package packageWithProducts) {
        return getProductByDataName(dataName, asList(
                packageWithProducts.products,
                packageWithProducts.productsFromBilling,
                packageWithProducts.productsDefault,
                packageWithProducts.productsOther)
        );
    }

    /**
     * Get {@link Product} representation from test data.
     *
     * @param dataName            code name for the product (e.g. "LC_DL_75", "LC_DL-UNL_50", etc.)
     * @param productsCollections list of {@link Product} collections from test data
     *                            (e.g. data.packageFolders[0].packages[0].products)
     * @return {@link Product} entity with related data to it (name, price, quantity...)
     * @throws IllegalArgumentException if there's no product data with provided dataName in test data
     */
    public Product getProductByDataName(String dataName, List<Product[]> productsCollections) {
        return productsCollections.stream()
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .filter(p -> p.dataName != null && p.dataName.equals(dataName))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product with dataName '" + dataName + "' is not found in the provided test data!"
                ));
    }
}
