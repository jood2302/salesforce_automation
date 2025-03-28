package com.aquiva.autotests.rc.utilities.ngbs;

import com.aquiva.autotests.rc.model.ngbs.dto.license.CatalogItem;
import com.aquiva.autotests.rc.model.ngbs.dto.license.PackageTemplateDTO;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.Product;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;

import static com.aquiva.autotests.rc.model.ngbs.dto.license.CatalogItem.*;
import static com.aquiva.autotests.rc.model.ngbs.dto.license.PackageTemplateDTO.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.getRandomPositiveInteger;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.QuoteLineItemHelper.*;

/**
 * Factory for generating instances of {@link PackageTemplateDTO} objects.
 */
public class AccountPackageNgbsFactory {

    /**
     * Creates a new instance of {@link PackageTemplateDTO} object with the given parameters.
     *
     * @param chargeTerm      the charge term for the package (e.g. "Monthly", "Annual")
     * @param productName     the name of the product group/service (e.g. "RingCentral Contact Center", "Engage Digital Standalone")
     * @param packageTestData the test data object for the package to create
     * @return package template object to pass on in the NGBS REST API request methods
     */
    public static PackageTemplateDTO createAccountPackageDTO(String chargeTerm, String productName, Package packageTestData) {
        var packageTemplate = new PackageTemplateDTO();

        packageTemplate.serviceAccountId = getRandomPositiveInteger();
        packageTemplate.masterDuration = chargeTerm;
        packageTemplate.productLine = getProductLine(productName);

        packageTemplate.catalogId = packageTestData.id;
        packageTemplate.version = packageTestData.version;
        packageTemplate.catalogLicenses = getCatalogLicensesFromProductsData(packageTestData.productsFromBilling);

        packageTemplate.status = INITIAL_PACKAGE_STATUS;
        packageTemplate.billingStartDate = Clock.systemUTC().instant().toString();

        return packageTemplate;
    }

    /**
     * Get the product line's value for the given service name.
     *
     * @param serviceName the name of the service (e.g. "RingCentral Contact Center", "Engage Digital Standalone")
     * @return the product line for the given service name (e.g. "Engage" for "Engage Digital Standalone")
     */
    private static String getProductLine(String serviceName) {
        if (serviceName.contains("Engage")) {
            return ENGAGE_PRODUCT_LINE;
        } else if (serviceName.contains("Contact Center")) {
            return CONTACT_CENTER_PRODUCT_LINE;
        } else if (serviceName.contains("Professional Services")) {
            return PROFESSIONAL_SERVICES_PRODUCT_LINE;
        } else {
            throw new AssertionError("Unknown product line for service: " + serviceName);
        }
    }

    /**
     * Transform the test data for the package's products
     * into catalog licenses objects for the package template.
     *
     * @param products list of products to transform
     * @return the list of catalog licenses for the package template DTO
     */
    private static List<CatalogItem> getCatalogLicensesFromProductsData(Product[] products) {
        return Arrays.stream(products)
                .map(product -> {
                    var catalogItem = new CatalogItem();
                    catalogItem.catalogId = product.dataName;

                    catalogItem.billingCycleDuration = switch (product.chargeTerm) {
                        case MONTHLY_CHARGE_TERM -> MONTHLY_BILLING_CYCLE;
                        case ANNUAL_CHARGE_TERM -> ANNUAL_BILLING_CYCLE;
                        case ONE_TIME_CHARGE_TERM -> ONETIME_BILLING_CYCLE;
                        default -> throw new AssertionError("Cannot find billing cycle for this charge term: " +
                                product.chargeTerm);
                    };

                    catalogItem.qty = product.quantity;
                    catalogItem.billingState = INITIAL_BILLING_STATE;
                    return catalogItem;
                })
                .toList();
    }
}
