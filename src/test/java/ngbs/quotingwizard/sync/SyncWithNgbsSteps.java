package ngbs.quotingwizard.sync;

import com.aquiva.autotests.rc.model.ngbs.testdata.Package;
import com.aquiva.autotests.rc.model.ngbs.testdata.*;
import com.aquiva.autotests.rc.utilities.salesforce.EnterpriseConnectionUtils;
import com.sforce.soap.enterprise.sobject.*;
import io.qameta.allure.Step;
import ngbs.quotingwizard.CartTabSteps;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static base.Pages.*;
import static com.aquiva.autotests.rc.model.ngbs.dto.contracts.ContractNgbsDTO.CONTRACT_TERMINATED;
import static com.aquiva.autotests.rc.model.ngbs.dto.license.CatalogItem.getItemFromTestData;
import static com.aquiva.autotests.rc.model.ngbs.dto.license.OrderRequestDTO.createOrderRequestToAddLicenses;
import static com.aquiva.autotests.rc.page.opportunity.modal.ProcessOrderModal.*;
import static com.aquiva.autotests.rc.utilities.StringHelper.*;
import static com.aquiva.autotests.rc.utilities.ngbs.NgbsRestApiClient.*;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.OpportunityFactory.createOpportunity;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjectfactories.QuoteFactory.createActiveSalesAgreement;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.MEETINGS_SERVICE;
import static com.aquiva.autotests.rc.utilities.salesforce.sobjecthelper.OpportunityHelper.OFFICE_SERVICE;
import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.sleep;
import static io.qameta.allure.Allure.step;
import static java.lang.String.format;
import static java.math.RoundingMode.HALF_EVEN;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods related to 'Sync with NGBS' functionality.
 */
public class SyncWithNgbsSteps {
    private final Dataset data;
    private final EnterpriseConnectionUtils enterpriseConnectionUtils;
    private final CartTabSteps cartTabSteps;

    /**
     * New instance for the class with the test methods/steps related to 'Sync with NGBS' functionality.
     *
     * @param data object parsed from the JSON files with the test data
     */
    public SyncWithNgbsSteps(Dataset data) {
        this.data = data;
        this.enterpriseConnectionUtils = EnterpriseConnectionUtils.getInstance();
        this.cartTabSteps = new CartTabSteps(data);
    }

    /**
     * Delete existing discounts on the NGBS account if there are any.
     */
    @Step("Delete created discounts from account's package")
    public void stepDeleteDiscountsOnAccount() {
        var discountIds = getDiscountsFromNGBS(data.billingId, data.packageId).stream()
                .map(discount -> discount.id)
                .toList();

        if (!discountIds.isEmpty()) {
            discountIds.forEach(id -> deleteDiscountFromNGBS(data.billingId, data.packageId, id));
        }
    }

    /**
     * Retrieve the list of account's contracts, and terminate the active contract, if it's present.
     */
    @Step("Terminate an active contract on account in NGBS")
    public void stepTerminateActiveContractOnAccount() {
        var contractsOnAccount = getContractsInNGBS(data.billingId, data.packageId);
        var activeContract = contractsOnAccount.stream()
                .filter(contract -> contract.isContractActive())
                .findFirst();
        activeContract.ifPresent(contractNgbsDTO ->
                terminateContractInNGBS(data.billingId, data.packageId, contractNgbsDTO.id));
    }

    /**
     * Reset contract information in NGBS to initial state.
     *
     * @param product Product on the contract that will be reset to its existing quantity
     *                (initial contractual quantity before test execution)
     */
    @Step("Update contract information to initial state")
    public void stepResetContractState(Product product) {
        var contractsOnAccount = getContractsInNGBS(data.billingId, data.packageId);
        var activeContract = contractsOnAccount.stream()
                .filter(contract -> contract.isContractActive())
                .findFirst().orElseThrow(() ->
                        new AssertionError("No active contracts found on the NGBS account!"));
        Arrays.stream(activeContract.licenses)
                .filter(license -> license.catalogId.equals(product.dataName))
                .findAny()
                .ifPresent(license -> license.contractualQty = product.existingQuantity);

        updateContractInNGBS(data.billingId, data.packageId, activeContract.id, activeContract);
    }

    /**
     * Reset the number of licenses in NGBS to initial state (after the possible downsell).
     *
     * @param product test data about product which state should be reset
     *                (from the Upgrade test data,
     *                see {@link Dataset#getProductByDataNameFromUpgradeData(String)}).
     */
    @Step("Update the number of licenses to the initial state")
    public void stepResetLicensesStateAfterDownsell(Product product) {
        var billingInfoLicenses = getBillingInfoSummaryLicenses(data.billingId, data.packageId);

        var licenseToCheck = Arrays.stream(billingInfoLicenses)
                .filter(license -> license.catalogId.equals(product.dataName))
                .findFirst();
        var existingQtyOnLicense = licenseToCheck.isPresent()
                ? licenseToCheck.get().qty
                : 0;

        var quantityDiff = product.existingQuantity - existingQtyOnLicense;
        if (quantityDiff > 0) {
            //  If the target quantity is less than the existing one, then we need to purchase the difference
            var itemToOrder = getItemFromTestData(product.dataName, quantityDiff);
            var orderRequest = createOrderRequestToAddLicenses(List.of(itemToOrder));
            purchaseLicensesInNGBS(data.billingId, data.packageId, orderRequest);
        } else {
            step("The number of licenses is already in the initial state");
        }
    }

    /**
     * Check Free Service Credit amount from NGBS.
     *
     * @param expectedAmount expected value for FSC amount
     */
    @Step("Check amount of Free Service Credit")
    public void stepCheckFreeServiceCreditAmount(double expectedAmount) {
        var serviceCredit = getFreeServiceCredit(data.billingId);
        assertThat(serviceCredit.amount)
                .as("Free Service Credit amount")
                .isEqualTo(expectedAmount);
    }

    /**
     * Set ServiceInfo__c.UpgradeStepStatus__c = true before Sync with NGBS.
     */
    public void stepSkipUpgradeSyncStep(String quoteId) {
        step("Set the ServiceInfo__c.UpgradeStepStatus__c = true via API", () -> {
            var serviceInfo = enterpriseConnectionUtils.querySingleRecord(
                    "SELECT Id " +
                            "FROM ServiceInfo__c " +
                            "WHERE Quote__c = '" + quoteId + "'",
                    ServiceInfo__c.class);
            serviceInfo.setUpgradeStepStatus__c(true);
            enterpriseConnectionUtils.update(serviceInfo);
        });
    }

    /**
     * Set ServiceInfo__c.UpgradeStepStatus__c = true before Sync with NGBS
     * (only for the records related to the corresponding Tech Quotes).
     *
     * @param masterQuoteId related master Quote's ID
     * @param services      list of services to change the Upgrade Step status for
     *                      (e.g. ["Office", "Engage Voice Standalone"])
     */
    public void stepSkipUpgradeSyncStepForMultiproductQuote(String masterQuoteId, List<String> services) {
        step("Set the ServiceInfo__c.UpgradeStepStatus__c = true for the services " + services + " via API", () -> {
            var serviceInfoRecords = enterpriseConnectionUtils.query(
                    "SELECT Id " +
                            "FROM ServiceInfo__c " +
                            "WHERE Quote__r.MasterQuote__c = '" + masterQuoteId + "' " +
                            "AND Quote__r.ServiceName__c IN " + getStringListAsString(services),
                    ServiceInfo__c.class);
            serviceInfoRecords.forEach(serviceInfo -> serviceInfo.setUpgradeStepStatus__c(true));
            enterpriseConnectionUtils.update(serviceInfoRecords);
        });
    }

    /**
     * Select a different package (with/without contract).
     *
     * @param upgradePackageFolderName package folder name for upgraded package (e.g. "Office")
     * @param upgradePackage           upgraded package test data with package's id, version, type (optional)
     * @param upgradeContractName      name for the contract from the upgraded package test data (e.g. "Office Contract")
     */
    public void stepUpgradeWithContract(String upgradePackageFolderName, Package upgradePackage, String upgradeContractName) {
        packagePage.packageSelector.selectPackage(data.chargeTerm, upgradePackageFolderName, upgradePackage);

        packagePage.packageSelector.getSelectedPackage().getName()
                .shouldHave(exactTextCaseSensitive(upgradePackage.getFullName()));
        var contractSelectedOrNot = upgradeContractName != null && !upgradeContractName.isBlank() && !upgradeContractName.equals(NONE)
                ? checked
                : not(checked);
        packagePage.packageSelector.packageFilter.contractSelectInput.shouldBe(contractSelectedOrNot);
    }

    /**
     * Increase or decrease quantity for DL Unlimited, increase quantity for additional phone, set up discounts,
     * assign devices to area codes (if there's any phones to add).
     * <p>
     * Quantities for products are taken from their {@link Product#quantity} variables.
     * <p> In case of downsell, there should be <b>no</b> product entity for 'phonesToAdd'. </p>
     * <p> In case of upsell, there should be <b>only one</b> additional phone product. In this case,
     * there's also area code assignment process. </p>
     *
     * @param dlUnlimited   {@link Product} entity for DigitalLine Unlimited
     * @param localAreaCode area code in case there's a device to assign to DL
     * @param phonesToAdd   {@link Product} entity for additional phone.
     *                      This method uses 'varargs' method for more flexibility and less code usage.
     */
    public void stepSetupQuantitiesAndDiscounts(Product dlUnlimited, AreaCode localAreaCode, Product... phonesToAdd) {
        cartPage.openTab();

        cartPage.setNewQuantityForQLItem(dlUnlimited.name, dlUnlimited.quantity);
        var dlNewDiscount = dlUnlimited.newDiscount != null ? dlUnlimited.newDiscount : dlUnlimited.discount;
        cartPage.setDiscountTypeForQLItem(dlUnlimited.name, dlUnlimited.discountType);
        cartPage.setDiscountForQLItem(dlUnlimited.name, dlNewDiscount);

        if (phonesToAdd.length != 0) {
            var phoneToAdd = phonesToAdd[0];
            cartPage.setNewQuantityForQLItem(phoneToAdd.name, phoneToAdd.quantity);
            cartPage.setDiscountTypeForQLItem(phoneToAdd.name, phoneToAdd.discountType);
            cartPage.setDiscountForQLItem(phoneToAdd.name, phoneToAdd.discount);

            cartTabSteps.assignDevicesToDL(phoneToAdd.name, dlUnlimited.name, localAreaCode, phoneToAdd.quantity);
        }
    }

    /**
     * Switch to the Quote Details tab, populate Initial Term, Start date, and save changes.
     * <br/>
     * Useful for contracted Quotes.
     */
    public void stepPopulateRequiredContractedInformationOnQuoteDetailsTab() {
        quotePage.openTab();
        quotePage.initialTermPicklist.selectOption(data.getInitialTerm());
        quotePage.setDefaultStartDate();

        quotePage.saveChanges();
    }

    /**
     * Click 'Process Order' button and wait for modal window with 'sync' steps to appear.
     * <br/><br/>
     * <b> Note: only for "Office" service sync! </b>
     * For other services, see {@link #stepStartSyncWithNgbsViaProcessOrder(List, String)}.
     *
     * @param expectedSyncSteps ordered list of expected steps for the 'Sync with NGBS' process
     *                          (e.g. "Contract sync", "Discount sync",
     *                          "Upgrade (in external system)", "Up-sell (in external system)")
     */
    public void stepStartSyncWithNgbsViaProcessOrder(List<String> expectedSyncSteps) {
        stepStartSyncWithNgbsViaProcessOrder(expectedSyncSteps, OFFICE_SERVICE);
    }

    /**
     * Click 'Process Order' button and wait for modal window with 'sync' steps to appear.
     *
     * @param expectedSyncSteps ordered list of expected steps for the 'Sync with NGBS' process
     *                          (e.g. "Contract sync", "Discount sync",
     *                          "Upgrade (in external system)", "Up-sell (in external system)")
     * @param tierName          name of the service tier to sync with NGBS
     *                          (e.g. "Office", "Meetings", "Engage Digital Standalone", etc.)
     *                          <br/>
     *                          Note: not to be confused with services as they are displayed on the Process Order modal,
     *                          (e.g. "MVP" instead of "Office", "Engage Digital" instead of "Engage Digital Standalone"),
     *                          see the test data files instead!
     */
    public void stepStartSyncWithNgbsViaProcessOrder(List<String> expectedSyncSteps, String tierName) {
        opportunityPage.clickProcessOrderButton();

        switch (tierName) {
            case MEETINGS_SERVICE:
                processOrderModal.meetingsAllSyncStepNames.shouldHave(exactTexts(expectedSyncSteps), ofSeconds(120));
                break;
            case OFFICE_SERVICE:
            default:
                processOrderModal.mvpAllSyncStepNames.shouldHave(exactTexts(expectedSyncSteps), ofSeconds(120));
        }

        processOrderModal.signUpSpinner.shouldBe(hidden, ofSeconds(60));
        processOrderModal.spinner.shouldBe(hidden, ofSeconds(60));
        //  additional wait for the 'Next' button to become clickable
        sleep(1_000);
    }

    /**
     * Check contract information from NGBS for given product on this contract.
     *
     * @param productOnContract {@link Product} entity to check contractual information for (e.g. DL Unlimited)
     */
    public void stepCheckContractInformation(Product productOnContract) {
        var contracts = getContractsInNGBS(data.billingId, data.packageId);
        assertThat(contracts)
                .as("Contracts on the account in NGBS")
                .hasSizeGreaterThanOrEqualTo(1);

        var activeContract = contracts.stream()
                .filter(contract -> contract.isContractActive())
                .findFirst().orElseThrow(() ->
                        new AssertionError("No active contracts found on the NGBS account!"));
        var licenses = activeContract.licenses;
        assertThat(licenses)
                .as("Licenses on the contract from NGBS")
                .hasSize(1);

        var dlUnlimitedLicense = licenses[0];
        assertThat(dlUnlimitedLicense.catalogId)
                .as("Catalog ID for " + productOnContract.name)
                .isEqualTo(productOnContract.dataName);
        assertThat(dlUnlimitedLicense.contractualQty)
                .as("Contractual quantity of " + productOnContract.name)
                .isEqualTo(productOnContract.quantity);
    }

    /**
     * Check that all contracts on Account is in 'Terminated' status in NGBS.
     */
    public void stepCheckContractTerminated() {
        var contractsOnAccount = getContractsInNGBS(data.billingId, data.packageId);
        contractsOnAccount.forEach(contract ->
                step("Check contract's status for a contract with ID = " + contract.id, () -> {
                    assertThat(contract.startBillingCycleNumber)
                            .as("Contract's status ('startBillingCycleNumber' value)")
                            .isEqualTo(CONTRACT_TERMINATED);
                })
        );
    }

    /**
     * Check synced discount information from NGBS.
     * <p>
     * <b>Note that this method only works for packages with 'Monthly' charge terms!</b>
     *
     * @param productWithDiscount {@link Product} entity with discount (e.g. DL Unlimited)
     * @param packageData         package test data with package's id, version, name, and contract's data (name, extId)
     * @throws Exception in case of malformed DB queries or network errors
     */
    public void stepCheckDiscountsInformation(Product productWithDiscount, Package packageData) throws Exception {
        var discounts = getDiscountsFromNGBS(data.billingId, data.packageId);
        assertThat(discounts)
                .as("Number of discounts on the NGBS account")
                .hasSize(1);

        var officeEditionCatalogId = discounts.get(0).target.catalogId;
        assertThat(officeEditionCatalogId)
                .as("Catalog ID for " + packageData.getFullName())
                .isEqualTo(packageData.id);

        var dlUnlimitedDiscountTemplate =
                Arrays.stream(discounts.get(0).discountTemplates)
                        .filter(discountTemplate -> discountTemplate.description.equals(productWithDiscount.name))
                        .findFirst();

        assertThat(dlUnlimitedDiscountTemplate)
                .as("Discount Template for " + productWithDiscount.name)
                .isPresent();

        var expectedDLDiscountType = dlUnlimitedDiscountTemplate.get().values.monthly.unit.equals("Percent") ?
                PERCENT :
                data.getCurrencyIsoCode();

        assertThat(expectedDLDiscountType)
                .as("Discount Type for " + productWithDiscount.name)
                .isEqualTo(productWithDiscount.discountType);

        var productDiscount = productWithDiscount.newDiscount != null ?
                productWithDiscount.newDiscount : productWithDiscount.discount;
        var expectedDLDiscount = data.packageFoldersUpgrade == null ?
                data.packageFolders[0].packages[0].contract.equals("None") ?
                        productDiscount :
                        getTotalExpectedDiscount(productWithDiscount, packageData.contractExtId) :
                data.packageFoldersUpgrade[0].packages[0].contract.equals("None") ?
                        productDiscount :
                        getTotalExpectedDiscount(productWithDiscount, packageData.contractExtId);

        assertThat(dlUnlimitedDiscountTemplate.get().values.monthly.value)
                .as("Discount Value for " + productWithDiscount.name)
                .isEqualTo(expectedDLDiscount);
    }

    /**
     * Create an Opportunity with a Quote and make this Quote an Active Agreement via API.
     *
     * <p> Useful only for Sync with NGBS flow. </p>
     */
    public Quote stepCreateAdditionalActiveSalesAgreement(Account account, Contact contact, User ownerUser) {
        return step("Create an Existing Business Opportunity " +
                "and an Active Sales Agreement (Quote record) on this Opportunity (all via API)", () -> {
            var bufferOpportunity = createOpportunity(account, contact, data.getBillingId().isEmpty(),
                    data.getBrandName(), data.businessIdentity.id, ownerUser, data.getCurrencyIsoCode(),
                    data.packageFolders[0].name);

            return createActiveSalesAgreement(bufferOpportunity, data.getInitialTerm());
        });
    }

    /**
     * Follow 'Reprice' step and check that proper success notification is displayed.
     */
    public void checkRepriceStep() {
        clickNextButtonForSync();
        checkIsSyncStepCompleted(REPRICE_STEP);
        processOrderModal.successNotifications
                .shouldHave(itemWithText(format(PRICE_SUCCESSFULLY_CHANGED_MESSAGE, MVP_SERVICE)));
    }

    /**
     * Check that the Sync step is completed successfully.
     *
     * @param stepName name of the sync step to check (e.g. "Contract sync", "Discount sync")
     */
    public void checkIsSyncStepCompleted(String stepName) {
        processOrderModal.getSyncStepElement(stepName).shouldHave(cssClass("slds-is-completed"), ofSeconds(60));
    }

    /**
     * Check that the sync process has finished successfully.
     *
     * @param expectedSyncSteps list of expected sync steps
     *                          (e.g. ["Contract sync", "Discount sync"])
     */
    public void checkSyncStepsFinished(List<String> expectedSyncSteps) {
        processOrderModal.mvpTierStatus.shouldHave(exactTextCaseSensitive(SYNCED_WITH_NGBS_STATUS), ofSeconds(60));

        processOrderModal.mvpCompletedSyncStepNames.shouldHave(size(expectedSyncSteps.size()));
    }

    /**
     * Press 'Next' button in the Process Order modal, wait until the step is finished,
     * and expand the current notifications.
     */
    @Step("Click 'Next' button in the Process Order modal and expand notifications after processing")
    public void clickNextButtonForSync() {
        processOrderModal.nextButton.click();

        processOrderModal.spinner.shouldBe(visible);
        processOrderModal.spinner.shouldBe(hidden, ofSeconds(60));
        processOrderModal.expandNotifications();
    }

    /**
     * Getting expected value for product's discount
     * including the applied contract discount (from {@code Contract_Discount__c}).
     * <p>
     * It can be calculated using the following formulas:
     * <p><b>For discount type = Currency (USD): </b></p>
     * <p><i>contractDiscount + quoteDiscountCurrency</i></p>
     * <p><b>For discount type = Percent (%): </b></p>
     * <p><i>(1 - priceWithDiscountAndContract / fullPriceWithoutContract ) * 100 </i> </p>
     * </p>
     * E.g. for discount type = Percent (%), given:
     * <p> - contractDiscount = 7 (USD)</p>
     * <p> - fullPriceWithoutContract = 37.99 (USD)</p>
     * <p> - fullPriceWithContract = 37.99 - 7 = 30.99 (USD)</p>
     * <p> - quoteDiscount = 9 (%)</p>
     * <p> - priceWithDiscountAndContract = fullPriceWithContract * (1 - quoteDiscount / 100) = 30.99 * (1 - 9 / 100) = 30.99 * 0.91 = 28.20 (USD) </p>
     * <p> then: </p>
     * <i>expectedDiscount = (1 - 28.20 / 37.99 ) * 100 = <b>25.77 (%)</b></i>
     *
     * @param product       Product to get the discount for (e.g. {@link Product} entity for DL Unlimited)
     * @param contractExtID external ID for contract entity (e.g. "Office_Canada", "Office_10_US"...)
     * @return expected value for total discount for a product
     * @throws Exception in case of malformed DB queries or network errors
     */
    public Double getTotalExpectedDiscount(Product product, String contractExtID) throws Exception {
        var contractDiscount = enterpriseConnectionUtils.querySingleRecord(
                "SELECT Id, Discount__c " +
                        "FROM Contract_Discount__c " +
                        "WHERE Contract__r.ExtID__c  = '" + contractExtID + "' " +
                        "AND ApplicableTo__c = '" + product.dataName + "'",
                Contract_Discount__c.class);
        var contractDiscountValue = contractDiscount.getDiscount__c();
        var dlPriceDouble = Double.parseDouble(product.price);
        var fullPriceNoContract = dlPriceDouble + contractDiscountValue;

        //  see SignUpCustomersBilling.applyContract() Apex method for the implementation on the CRM side
        if (product.discountType.equals(PERCENT)) {
            var priceWithNewDiscountAndContractDiscount = dlPriceDouble * (1 - product.discount / 100D);
            var totalDiscount = (1 - priceWithNewDiscountAndContractDiscount / fullPriceNoContract) * 100D;
            return new BigDecimal(totalDiscount)
                    .setScale(2, HALF_EVEN)
                    .doubleValue();
        } else {
            return contractDiscountValue + product.discount;
        }
    }
}
