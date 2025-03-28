package base;

import com.aquiva.autotests.rc.model.ngbs.dto.account.PackageSummary;
import com.aquiva.autotests.rc.model.ngbs.dto.license.CatalogItem;
import com.aquiva.autotests.rc.model.ngbs.dto.license.RemovalRequestItem;
import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.ags.AGSRestApiClient;
import com.aquiva.autotests.rc.utilities.ngbs.DiscountNgbsFactory;
import com.sforce.soap.enterprise.sobject.Contract__c;
import io.qameta.allure.Step;

import java.util.*;

import static com.aquiva.autotests.rc.model.ngbs.dto.license.OrderRequestDTO.createOrderRequestToAddLicenses;
import static com.aquiva.autotests.rc.model.ngbs.dto.license.OrderRequestDTO.createOrderRequestToRemoveLicenses;
import static com.aquiva.autotests.rc.utilities.Constants.IS_GENERATE_ACCOUNTS;
import static com.aquiva.autotests.rc.utilities.ngbs.AccountPackageNgbsFactory.createAccountPackageDTO;
import static com.aquiva.autotests.rc.utilities.ngbs.ContractNgbsFactory.createContractForOneLicense;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.*;
import static com.codeborne.selenide.Selenide.sleep;
import static io.qameta.allure.Allure.step;
import static java.lang.Math.min;
import static java.time.Clock.systemUTC;
import static java.time.LocalDate.now;
import static java.util.stream.Collectors.toMap;

/**
 * Test methods for the flows related to NGBS integration.
 * E.g. creating NGBS accounts, adding contracts/discounts to the NGBS accounts, etc.
 */
public class NgbsSteps {
    private final Dataset data;

    /**
     * Set this variable to "true" in the test's preconditions,
     * if the test needs to generate a new NGBS account on every run.
     */
    public boolean isGenerateAccountsForSingleTest = false;

    /**
     * New instance for the class with the test methods/steps related to the NGBS integration functionality.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public NgbsSteps(Dataset data) {
        this.data = data;
    }

    /**
     * Is AGS Dynamic Account generation active for a given test?
     *
     * @return true, if either "-DgenerateAccounts=true" for the build
     * or {@code isGenerateAccountsForSingleTest = true} for a single test
     */
    public boolean isGenerateAccounts() {
        return IS_GENERATE_ACCOUNTS || isGenerateAccountsForSingleTest;
    }

    /**
     * Generate account in NGBS for testing of Existing Business functionality.
     * <p> Account generation works if account generation is active either globally or for a single test. </p>
     * <p> Account generation works according to scenarios that can be found in test data. </p>
     */
    public void generateBillingAccount() {
        generateBillingAccount(data);
    }

    /**
     * Generate account in NGBS for testing of Existing Business functionality.
     * <p> Account generation works if account generation is active either globally or for a single test. </p>
     * <p> Account generation works according to scenarios that can be found in test data. </p>
     *
     * @param data specific dataset with Existing Business Account's info (e.g. AGS scenario)
     */
    public void generateBillingAccount(Dataset data) {
        if (isGenerateAccounts() && data.scenario != null && !data.scenario.isBlank()) {
            step("Generate Existing Business Account in Billing for scenario '" + data.scenario + "'", () -> {
                var accountDetailsAGS = AGSRestApiClient.createAccount(data.scenario);

                data.billingId = accountDetailsAGS.getAccountBillingId();
                data.packageId = accountDetailsAGS.getAccountPackageId();
                data.rcUserId = accountDetailsAGS.rcUserId;
            });
        }
    }

    /**
     * Create a contract in NGBS with an initial state if there are no Active contracts on the Account.
     *
     * @param billingId     ID for the NGBS account (e.g. "235714001")
     * @param packageId     ID for the package on the account in NGBS (e.g. "235798001")
     * @param contractExtId special ID for a contract to map it to the custom SFDC contract object
     *                      (e.g. "Office", "Autotest_Contract_42").
     *                      See {@link Contract__c#getExtID__c()}.
     * @param contractItem  test data for the account's contracted item
     *                      (e.g. "DigitalLine Unlimited Standard").
     *                      Note: it should contain contract's quantity
     *                      and product's data name in NGBS (e.g. "LC_DL-UNL_50")!
     */
    public void stepCreateContractInNGBS(String billingId, String packageId,
                                         String contractExtId, Product contractItem) {
        var contractsOnAccount = getContractsInNGBS(billingId, packageId);
        var activeContract = contractsOnAccount.stream()
                .filter(contract -> contract.isContractActive())
                .findFirst();

        if (activeContract.isEmpty()) {
            step("Create a contract for the selected product on the NGBS account", () -> {
                var contractToCreate = createContractForOneLicense(contractExtId, contractItem);
                createContractInNGBS(billingId, packageId, contractToCreate);
            });
        }
    }

    /**
     * Create discount(s) for the NGBS account on the given product(s).
     * <br/>
     * Should be placed in {@code @BeforeAll/@BeforeEach} hooks for the tests
     * that require discounts on the existing NGBS account as a precondition.
     * <br/>
     * <b> Note: do NOT place this method in a loop to create multiple discounts!
     * Provide the collection of discounted products as an argument right away. </b>
     *
     * @param billingId             ID for the NGBS account (e.g. "235714001")
     * @param packageId             ID for the package on the account in NGBS (e.g. "235798001")
     * @param productsWithDiscounts test data for products that need to have a discount in NGBS
     *                              (must have non-null {@code dataName, discount, discountType, chargeTerm}
     *                              variables)
     */
    @Step("Create discount(s) for the given product(s) on the NGBS account")
    public void stepCreateDiscountsInNGBS(String billingId, String packageId,
                                          Product... productsWithDiscounts) {
        var discountFactory = new DiscountNgbsFactory();
        for (var product : productsWithDiscounts) {
            var discountTemplateGroup = discountFactory.createDiscountTemplateGroup(product);
            createDiscountInNGBS(billingId, packageId, discountTemplateGroup);
        }
    }

    /**
     * Create a contract in NGBS with an initial state if there are no Active contracts on Account.
     * <p></p>
     * Note: the contract is created for 'DigitalLine Unlimited' product.
     * Test data for the product should be available in
     * data.packageFolders[0].packages[0].productsFromBilling[5].
     */
    public void stepCreateContractInNGBS() {
        stepCreateContractInNGBS(
                data.billingId, data.packageId,
                data.packageFolders[0].packages[0].contractExtId,
                data.packageFolders[0].packages[0].productsFromBilling[5]
        );
    }

    /**
     * Order additional licenses for the NGBS account (mid-cycle, i.e. "to the shelf")
     * using the provided licenses data.
     *
     * @param billingId       ID for the NGBS account (e.g. "235714001")
     * @param packageId       ID for the package on the account in NGBS (e.g. "235798001")
     * @param licensesToOrder test data for licenses to order
     */
    public void purchaseAdditionalLicensesInNGBS(String billingId, String packageId, List<CatalogItem> licensesToOrder) {
        var orderRequest = createOrderRequestToAddLicenses(licensesToOrder);
        purchaseLicensesInNGBS(billingId, packageId, orderRequest);
    }

    /**
     * Order additional licenses for the NGBS account's package (mid-cycle, i.e. "to the shelf")
     * using the default test data.
     */
    @Step("Order additional licenses for the NGBS account's package")
    public void purchaseAdditionalLicensesInNGBS(CatalogItem catalogItem) {
        purchaseAdditionalLicensesInNGBS(data.billingId, data.packageId, List.of(catalogItem));
    }

    /**
     * Remove some licenses from the NGBS account's package (mid-cycle, i.e. "downsell")
     * using the provided licenses data.
     *
     * @param testDataProduct test data about product some of which should be removed
     *                        (from the Upgrade test data,
     *                        see {@link Dataset#getProductByDataNameFromUpgradeData(String)}).
     */
    @Step("Downsell licenses from the NGBS account's package")
    public void downsellLicensesInNGBS(Product testDataProduct) {
        downsellLicensesInNGBS(testDataProduct, testDataProduct.existingQuantity - testDataProduct.quantity);
    }

    /**
     * Remove some licenses from the NGBS account's package (mid-cycle, i.e. "downsell")
     * using the provided licenses data.
     *
     * @param testDataProduct  test data about product some of which should be removed
     *                         (from the Upgrade test data,
     *                         see {@link Dataset#getProductByDataNameFromUpgradeData(String)}).
     * @param downsellQuantity quantity of licenses to remove from the account's package
     */
    @Step("Downsell licenses from the NGBS account's package")
    public void downsellLicensesInNGBS(Product testDataProduct, int downsellQuantity) {
        var accountPackageDTO = getAccountInNGBS(data.billingId).getMainPackage();

        var itemsToRemove = new ArrayList<RemovalRequestItem>();
        var remainingQuantity = downsellQuantity;
        for (var license : accountPackageDTO.licenses) {
            if (remainingQuantity > 0 && license.catalogId.equals(testDataProduct.dataName)) {
                var quantityToSet = min(remainingQuantity, license.qty);
                itemsToRemove.add(new RemovalRequestItem(license.id, quantityToSet));
                remainingQuantity -= quantityToSet;
            }
        }

        //  If the Next Billing Date is in the past, it's impossible to add/remove any licenses
        //  Therefore, we need to run recurring billing to update it to the future date
        //  See https://wiki.ringcentral.com/pages/viewpage.action?pageId=294862204 ("How to emulate recurring (NGBS)")
        if (accountPackageDTO.getNextBillingDateAsLocalDate().isBefore(now(systemUTC()))) {
            runRecurringBillingInNGBS(data.billingId, data.packageId);

            //  wait for all the Next Billing Date values to update on the licenses in NGBS
            sleep(5_000);
        }

        var orderRequest = createOrderRequestToRemoveLicenses(itemsToRemove);
        removeLicensesInNGBS(data.billingId, data.packageId, orderRequest);
    }

    /**
     * Add a new package to the NGBS account with the provided test data,
     * and activate it.
     *
     * @param productName     product's name for the package (e.g. "RingCentral Contact Center", "Engage Digital Standalone")
     * @param packageTestData test data for the package to add
     */
    @Step("Add a new package to the NGBS account")
    public PackageSummary addNewPackageToAccount(String productName, Package packageTestData) {
        var accountPackageDTO = createAccountPackageDTO(data.chargeTerm, productName, packageTestData);
        var addedPackage = addPackageToAccount(data.billingId, accountPackageDTO);
        activatePackageOnAccountInNGBS(data.billingId, addedPackage.id);

        packageTestData.ngbsPackageId = addedPackage.id;

        return addedPackage;
    }

    /**
     * Generate MP UB account for testing of Existing Business functionality:
     * <p> 1. Generate the office account in NGBS.
     * <p> 2. And create a contract in NGBS.
     * <p> 3. And add new packages to the NGBS account for other services from provided Test data (packageFolders[i].packages[0])
     * and activate them.
     *
     * @see #generateBillingAccount()
     * @see #stepCreateContractInNGBS()
     * @see #addNewPackageToAccount(String, Package)
     */
    @Step("Generate Multi-Product Unified Billing Account")
    public void generateMultiProductUnifiedBillingAccount() {
        generateBillingAccount();
        stepCreateContractInNGBS();
        var additionalPackagesFromBilling = Arrays.stream(data.packageFolders)
                //  The main account and its Office package are generated initially via AGS (see generateBillingAccount() method)
                .filter(packageFolder -> !packageFolder.name.equals("Office"))
                .collect(toMap(
                        packageFolder -> packageFolder.name,
                        packageFolder -> packageFolder.packages[0]
                ));

        for (var packageName : additionalPackagesFromBilling.keySet()) {
            addNewPackageToAccount(packageName, additionalPackagesFromBilling.get(packageName));
        }
    }
}
